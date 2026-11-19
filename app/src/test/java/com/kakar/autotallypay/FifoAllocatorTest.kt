package com.kakar.autotallypay

import com.kakar.autotallypay.domain.Allocation
import com.kakar.autotallypay.domain.BillDue
import com.kakar.autotallypay.domain.Fifo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** FIFO allocation, oldest-first — 8 tests. */
class FifoAllocatorTest {

    @Test
    fun exactPayment_coversSingleBill() {
        val allocs = Fifo.allocate(listOf(BillDue(1, 500000, 0, 1000)), 500000)
        assertEquals(listOf(Allocation(1, 500000)), allocs)
        assertEquals(0L, Fifo.unallocated(500000, allocs))
    }

    @Test
    fun partialPayment_coversPartOfOldestOnly() {
        val bills = listOf(BillDue(1, 500000, 0, 1000), BillDue(2, 300000, 0, 2000))
        assertEquals(listOf(Allocation(1, 200000)), Fifo.allocate(bills, 200000))
    }

    @Test
    fun paymentSplitsAcrossTwoBills() {
        val bills = listOf(BillDue(1, 500000, 0, 1000), BillDue(2, 300000, 0, 2000))
        assertEquals(
            listOf(Allocation(1, 500000), Allocation(2, 100000)),
            Fifo.allocate(bills, 600000)
        )
    }

    @Test
    fun paymentExceedsAllBills_leavesUnallocated() {
        val bills = listOf(BillDue(1, 500000, 0, 1000), BillDue(2, 300000, 0, 2000))
        val allocs = Fifo.allocate(bills, 1000000)
        assertEquals(listOf(Allocation(1, 500000), Allocation(2, 300000)), allocs)
        assertEquals(200000L, Fifo.unallocated(1000000, allocs))
    }

    @Test
    fun oldestFirst_regardlessOfInputOrder() {
        val bills = listOf(BillDue(2, 300000, 0, 2000), BillDue(1, 500000, 0, 1000))
        assertEquals(
            listOf(Allocation(1, 500000), Allocation(2, 100000)),
            Fifo.allocate(bills, 600000)
        )
    }

    @Test
    fun alreadyPartiallyPaidBill_topsUpFirst() {
        val bills = listOf(BillDue(1, 500000, 400000, 1000), BillDue(2, 300000, 0, 2000))
        assertEquals(
            listOf(Allocation(1, 100000), Allocation(2, 100000)),
            Fifo.allocate(bills, 200000)
        )
    }

    @Test
    fun zeroOrNegativePayment_returnsEmpty() {
        val bills = listOf(BillDue(1, 500000, 0, 1000))
        assertTrue(Fifo.allocate(bills, 0).isEmpty())
        assertTrue(Fifo.allocate(bills, -100).isEmpty())
    }

    @Test
    fun fullyPaidBillsAreSkipped() {
        val bills = listOf(BillDue(1, 500000, 500000, 1000), BillDue(2, 300000, 0, 2000))
        assertEquals(listOf(Allocation(2, 100000)), Fifo.allocate(bills, 100000))
        assertEquals(300000L, Fifo.totalOutstanding(bills))
    }
}
