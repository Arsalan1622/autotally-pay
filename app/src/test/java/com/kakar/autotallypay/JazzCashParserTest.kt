package com.kakar.autotallypay

import com.kakar.autotallypay.sms.PaymentSource
import com.kakar.autotallypay.sms.SmsParser
import com.kakar.autotallypay.sms.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

/** JazzCash (sender 8558) — 8 tests. Sample data is masked/fictional. */
class JazzCashParserTest {

    @Test
    fun validJazzCashSms_parsesAllFields() {
        val parsed = SmsParser.parseSms(
            "8558",
            "You have received Rs. 2,500.00 from 0301-7654321 (FATIMA KHAN). Trx ID 9876543210. Thank you for using JazzCash."
        )
        assertNotNull(parsed)
        assertEquals(PaymentSource.JAZZCASH, parsed!!.source)
        assertEquals(250000L, parsed.amountPaisa)
        assertEquals("FATIMA KHAN", parsed.senderName)
        assertEquals("0301-7654321", parsed.accountHint)
        assertEquals("9876543210", parsed.trxId)
        assertEquals(VerificationStatus.VERIFIED, parsed.status)
    }

    @Test
    fun pkrPrefix_parses() {
        val parsed = SmsParser.parseSms(
            "8558",
            "PKR 7,000.00 received from 0301-2222222 (HASSAN ALI). Trx ID: 1122334455. JazzCash"
        )
        assertNotNull(parsed)
        assertEquals(700000L, parsed!!.amountPaisa)
        assertEquals("HASSAN ALI", parsed.senderName)
    }

    @Test
    fun smallAmount_parses() {
        val parsed = SmsParser.parseSms(
            "8558",
            "Rs 50.00 received from 0301-3333333 (MARYAM NAWAZ). JazzCash"
        )
        assertNotNull(parsed)
        assertEquals(5000L, parsed!!.amountPaisa)
    }

    @Test
    fun missingTrxId_returnsNullTrxId() {
        val parsed = SmsParser.parseSms(
            "8558",
            "Rs 900.00 received from 0301-4444444 (ZAINAB BIBI). JazzCash"
        )
        assertNotNull(parsed)
        assertEquals(90000L, parsed!!.amountPaisa)
        assertNull(parsed.trxId)
    }

    @Test
    fun debitMessage_returnsNull() {
        val parsed = SmsParser.parseSms(
            "8558",
            "Rs 2,000.00 paid to 0301-5555555. JazzCash"
        )
        assertNull(parsed)
    }

    @Test
    fun otpMessage_returnsNull() {
        val parsed = SmsParser.parseSms(
            "8558",
            "Your JazzCash OTP is 123456. Valid for 5 minutes."
        )
        assertNull(parsed)
    }

    @Test
    fun amountWithSingleDecimal_parses() {
        val parsed = SmsParser.parseSms(
            "8558",
            "Rs 1,250.5 received from 0301-6666666 (OMAR FAROOQ). JazzCash"
        )
        assertNotNull(parsed)
        assertEquals(125050L, parsed!!.amountPaisa)
    }

    @Test
    fun longTransactionId_parses() {
        val parsed = SmsParser.parseSms(
            "8558",
            "Rs 4,000.00 received from 0301-7777777 (AYESHA SIDDIQA). Transaction ID: JC-2026-88990011. JazzCash"
        )
        assertNotNull(parsed)
        assertEquals(400000L, parsed!!.amountPaisa)
        assertEquals("JC-2026-88990011", parsed.trxId)
    }
}
