package com.kakar.autotallypay

import com.kakar.autotallypay.domain.Allocation
import com.kakar.autotallypay.domain.PaidCalc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Paid" is always calculated, never stored — 2 tests. */
class PaidCalcTest {

    @Test
    fun paidForBill_sumsAllocations() {
        val allocs = listOf(
            Allocation(1, 100000),
            Allocation(1, 50000),
            Allocation(2, 70000)
        )
        assertEquals(150000L, PaidCalc.paidForBill(allocs, 1))
        assertEquals(70000L, PaidCalc.paidForBill(allocs, 2))
        assertEquals(0L, PaidCalc.paidForBill(allocs, 99))
    }

    @Test
    fun fullyPaidAndOutstanding() {
        assertTrue(PaidCalc.isFullyPaid(500000, 500000))
        assertTrue(PaidCalc.isFullyPaid(500000, 600000))
        assertFalse(PaidCalc.isFullyPaid(500000, 499999))
        assertFalse(PaidCalc.isFullyPaid(0, 0))
        assertEquals(300000L, PaidCalc.outstanding(500000, 200000))
        assertEquals(0L, PaidCalc.outstanding(500000, 600000))
    }
}
