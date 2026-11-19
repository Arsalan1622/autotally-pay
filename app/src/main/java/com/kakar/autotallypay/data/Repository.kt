package com.kakar.autotallypay.data

import com.kakar.autotallypay.domain.BillDue
import com.kakar.autotallypay.domain.Fifo
import com.kakar.autotallypay.domain.NameMemory
import com.kakar.autotallypay.domain.PaidCalc
import com.kakar.autotallypay.sms.ParsedPayment
import com.kakar.autotallypay.sms.SmsParser
import com.kakar.autotallypay.sms.VerificationStatus

/** What happened when one SMS was processed. */
data class ProcessResult(
    /** Null when the text was not a payment SMS at all. */
    val payment: Payment?,
    /** Bills that became fully paid by this payment (red-to-green moment). */
    val newlyPaidBillIds: List<Long>,
    /** True when this exact payment (Trx ID + amount) was already stored. */
    val duplicate: Boolean
)

/**
 * All database work goes through here. Money stays Long paisa;
 * "paid" is always calculated from allocations, never stored.
 */
class Repository(private val db: AppDatabase) {

    val customers = db.customers().all()
    val bills = db.bills().all()
    val payments = db.payments().all()
    val allocations = db.allocations().all()
    val mappings = db.mappings().all()

    fun billsFor(customerId: Long) = db.bills().forCustomer(customerId)
    fun paidForBill(billId: Long) = db.allocations().paidForBill(billId)

    suspend fun customerById(id: Long): Customer? = db.customers().byId(id)
    suspend fun paymentById(id: Long): Payment? = db.payments().byId(id)

    suspend fun addCustomer(name: String, phone: String): Long =
        db.customers().insert(Customer(name = name.trim(), phone = phone.trim()))

    suspend fun addBill(customerId: Long, amountPaisa: Long, description: String): Long =
        db.bills().insert(
            Bill(customerId = customerId, amountPaisa = amountPaisa, description = description.trim())
        )

    suspend fun deleteBill(bill: Bill) = db.bills().delete(bill)
    suspend fun deleteCustomer(customer: Customer) = db.customers().delete(customer)
    suspend fun deleteMapping(mapping: SenderMapping) = db.mappings().delete(mapping)

    /**
     * Process one incoming SMS. Fake/unknown senders are stored flagged
     * (shown red) and NEVER allocated to bills.
     */
    suspend fun processSms(
        sender: String,
        body: String,
        receivedAt: Long = System.currentTimeMillis()
    ): ProcessResult {
        val parsed = SmsParser.parseSms(sender, body)
            ?: return ProcessResult(payment = null, newlyPaidBillIds = emptyList(), duplicate = false)

        // Dedupe: same transaction ID + amount already stored (e.g. SMS re-read).
        if (parsed.trxId != null) {
            val existing = db.payments().byTrxIdAndAmount(parsed.trxId, parsed.amountPaisa)
            if (existing != null) {
                return ProcessResult(payment = existing, newlyPaidBillIds = emptyList(), duplicate = true)
            }
        }

        val customerId = if (parsed.status == VerificationStatus.VERIFIED) {
            findCustomerFor(sender, parsed)
        } else {
            null
        }
        val payment = Payment(
            customerId = customerId,
            amountPaisa = parsed.amountPaisa,
            source = parsed.source.name,
            status = parsed.status.name,
            senderId = sender,
            senderName = parsed.senderName,
            accountHint = parsed.accountHint,
            trxId = parsed.trxId,
            rawSms = body,
            fakeReason = parsed.fakeReason,
            receivedAt = receivedAt,
            matchedAutomatically = customerId != null
        )
        val id = db.payments().insert(payment)
        val stored = payment.copy(id = id)
        val newlyPaid = if (customerId != null) {
            allocatePayment(id, customerId, parsed.amountPaisa)
        } else {
            emptyList()
        }
        return ProcessResult(payment = stored, newlyPaidBillIds = newlyPaid, duplicate = false)
    }

    /** Assign a payment to a customer by hand — and remember the sender for next time. */
    suspend fun assignPaymentToCustomer(paymentId: Long, customerId: Long): List<Long> {
        val payment = db.payments().byId(paymentId) ?: return emptyList()
        val keys = NameMemory.candidateKeys(payment.senderId, payment.accountHint, payment.senderName)
        for (key in keys) {
            db.mappings().insert(
                SenderMapping(
                    senderKey = key,
                    customerId = customerId,
                    label = payment.senderName ?: payment.senderId
                )
            )
        }
        db.payments().update(payment.copy(customerId = customerId))
        if (payment.status != VerificationStatus.VERIFIED.name) return emptyList()
        return allocatePayment(paymentId, customerId, payment.amountPaisa)
    }

    /** Re-match unmatched verified payments (used by the WorkManager worker). */
    suspend fun rematchUnmatched(): Int {
        var matched = 0
        for (payment in db.payments().unmatchedVerified()) {
            val parsed = ParsedPayment(
                source = runCatching {
                    com.kakar.autotallypay.sms.PaymentSource.valueOf(payment.source)
                }.getOrDefault(com.kakar.autotallypay.sms.PaymentSource.UNKNOWN),
                amountPaisa = payment.amountPaisa,
                senderName = payment.senderName,
                accountHint = payment.accountHint,
                trxId = payment.trxId,
                status = VerificationStatus.VERIFIED,
                fakeReason = null
            )
            val customerId = findCustomerFor(payment.senderId, parsed) ?: continue
            db.payments().update(payment.copy(customerId = customerId, matchedAutomatically = true))
            allocatePayment(payment.id, customerId, payment.amountPaisa)
            matched++
        }
        return matched
    }

    /** "Did the money come?" — verified payments of exactly this amount. */
    suspend fun findVerifiedByAmount(amountPaisa: Long): List<Payment> =
        db.payments().verifiedByAmount(amountPaisa)

    private suspend fun findCustomerFor(sender: String, parsed: ParsedPayment): Long? {
        val keys = NameMemory.candidateKeys(sender, parsed.accountHint, parsed.senderName)
        for (key in keys) {
            val mapping = db.mappings().byKey(key)
            if (mapping != null) return mapping.customerId
        }
        return null
    }

    /**
     * FIFO-allocate a payment across the customer's bills, oldest first.
     * Returns the bills that became fully paid by this allocation.
     */
    private suspend fun allocatePayment(
        paymentId: Long,
        customerId: Long,
        amountPaisa: Long
    ): List<Long> {
        val alreadyAllocated = db.allocations().allocatedForPayment(paymentId)
        val remainingPayment = amountPaisa - alreadyAllocated
        if (remainingPayment <= 0) return emptyList()
        val bills = db.bills().forCustomerOnce(customerId)
        val dues = bills.map { bill ->
            BillDue(
                billId = bill.id,
                amountPaisa = bill.amountPaisa,
                alreadyPaidPaisa = db.allocations().paidForBillOnce(bill.id),
                dateMillis = bill.dateMillis
            )
        }
        val allocs = Fifo.allocate(dues, remainingPayment)
        if (allocs.isEmpty()) return emptyList()
        db.allocations().insertAll(
            allocs.map {
                PaymentAllocation(paymentId = paymentId, billId = it.billId, amountPaisa = it.amountPaisa)
            }
        )
        // Which bills did this payment just finish?
        val newlyPaid = mutableListOf<Long>()
        for (alloc in allocs) {
            val due = dues.first { it.billId == alloc.billId }
            val paidBefore = due.alreadyPaidPaisa
            val paidAfter = paidBefore + allocs.filter { it.billId == alloc.billId }.sumOf { it.amountPaisa }
            if (!PaidCalc.isFullyPaid(due.amountPaisa, paidBefore) &&
                PaidCalc.isFullyPaid(due.amountPaisa, paidAfter)
            ) {
                newlyPaid += alloc.billId
            }
        }
        return newlyPaid
    }
}
