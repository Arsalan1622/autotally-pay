package com.kakar.autotallypay.domain

/**
 * "Paid" is NEVER stored. It is always calculated from allocations.
 */
object PaidCalc {

    fun paidForBill(allocations: List<Allocation>, billId: Long): Long =
        allocations.filter { it.billId == billId }.sumOf { it.amountPaisa }

    fun isFullyPaid(billAmountPaisa: Long, paidPaisa: Long): Boolean =
        billAmountPaisa > 0 && paidPaisa >= billAmountPaisa

    fun outstanding(billAmountPaisa: Long, paidPaisa: Long): Long =
        (billAmountPaisa - paidPaisa).coerceAtLeast(0)
}
