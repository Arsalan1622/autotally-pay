package com.kakar.autotallypay

import com.kakar.autotallypay.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Paisa math — 4 tests. Money is Long paisa, never floats. */
class MoneyTest {

    @Test
    fun parseWithCommasAndDecimals() {
        assertEquals(500000L, Money.rupeesToPaisa("5,000.00"))
        assertEquals(1234550L, Money.rupeesToPaisa("12,345.50"))
    }

    @Test
    fun parseWithRsPrefix() {
        assertEquals(125000L, Money.rupeesToPaisa("Rs 1,250"))
        assertEquals(700000L, Money.rupeesToPaisa("PKR 7,000.00"))
        assertEquals(50L, Money.rupeesToPaisa("0.50"))
    }

    @Test
    fun parseInvalid_returnsNull() {
        assertNull(Money.rupeesToPaisa("abc"))
        assertNull(Money.rupeesToPaisa(""))
        assertNull(Money.rupeesToPaisa("12.345"))
        assertNull(Money.rupeesToPaisa("1,2x0"))
    }

    @Test
    fun formatRupees_groupsThousands() {
        assertEquals("Rs 5,000.00", Money.formatRupees(500000))
        assertEquals("Rs 1,250.50", Money.formatRupees(125050))
        assertEquals("Rs 0.00", Money.formatRupees(0))
    }
}
