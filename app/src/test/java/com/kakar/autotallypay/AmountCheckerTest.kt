package com.kakar.autotallypay

import com.kakar.autotallypay.domain.AmountChecker
import com.kakar.autotallypay.domain.PaymentLike
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Did the money come?" — 1 test (verified exact-amount matches only). */
class AmountCheckerTest {

    @Test
    fun findByAmount_matchesVerifiedOnly_newestFirst() {
        val payments = listOf(
            PaymentLike(1, 500000, "111", "AHMED RAZA", "EASYPAISA", 1000, true),
            PaymentLike(2, 500000, "222", "FATIMA KHAN", "JAZZCASH", 3000, true),
            PaymentLike(3, 500000, "333", "SCAMMER", "EASYPAISA", 2000, false),
            PaymentLike(4, 600000, "444", "BILAL AHMED", "MEEZAN", 4000, true)
        )
        val found = AmountChecker.findByAmount(payments, 500000)
        assertEquals(listOf(2L, 1L), found.map { it.id })
        assertEquals("222", found[0].trxId)
        assertTrue(AmountChecker.findByAmount(payments, 700000).isEmpty())
    }
}
