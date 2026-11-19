package com.kakar.autotallypay.domain

/** One bill's outstanding state, as seen by the allocator. */
data class BillDue(
    val billId: Long,
    val amountPaisa: Long,
    val alreadyPaidPaisa: Long,
    val dateMillis: Long
)

/** One slice of a payment applied to one bill. */
data class Allocation(val billId: Long, val amountPaisa: Long)

/**
 * FIFO allocation: one payment splits across a customer's bills
 * oldest-first. Partial splits allowed. "Paid" is never stored —
 * it is always the sum of allocations.
 */
object Fifo {

    fun remaining(bill: BillDue): Long =
        (bill.amountPaisa - bill.alreadyPaidPaisa).coerceAtLeast(0)

    fun allocate(bills: List<BillDue>, paymentPaisa: Long): List<Allocation> {
        if (paymentPaisa <= 0) return emptyList()
        var left = paymentPaisa
        val out = mutableListOf<Allocation>()
        val sorted = bills.sortedWith(compareBy({ it.dateMillis }, { it.billId }))
        for (bill in sorted) {
            if (left <= 0) break
            val due = remaining(bill)
            if (due <= 0) continue
            val take = minOf(due, left)
            out += Allocation(bill.billId, take)
            left -= take
        }
        return out
    }

    /** Payment money left over after all bills are covered (unallocated credit). */
    fun unallocated(paymentPaisa: Long, allocations: List<Allocation>): Long =
        (paymentPaisa - allocations.sumOf { it.amountPaisa }).coerceAtLeast(0)

    fun totalOutstanding(bills: List<BillDue>): Long = bills.sumOf { remaining(it) }
}
