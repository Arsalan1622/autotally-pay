package com.kakar.autotallypay

import com.kakar.autotallypay.sms.PaymentSource
import com.kakar.autotallypay.sms.SmsParser
import com.kakar.autotallypay.sms.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Easypaisa (sender 3737) — 8 tests. Sample data is masked/fictional. */
class EasypaisaParserTest {

    @Test
    fun validEasypaisaSms_parsesAllFields() {
        val parsed = SmsParser.parseSms(
            "3737",
            "Rs 5,000.00 received from 0300-1234567 (AHMED RAZA) on 19-Nov-26 at 10:15 AM. Trx ID: 1234567890. Easypaisa"
        )
        assertNotNull(parsed)
        assertEquals(PaymentSource.EASYPAISA, parsed!!.source)
        assertEquals(500000L, parsed.amountPaisa)
        assertEquals("AHMED RAZA", parsed.senderName)
        assertEquals("0300-1234567", parsed.accountHint)
        assertEquals("1234567890", parsed.trxId)
        assertEquals(VerificationStatus.VERIFIED, parsed.status)
    }

    @Test
    fun amountWithoutDecimals_parses() {
        val parsed = SmsParser.parseSms(
            "3737",
            "Rs 2500 received from 0300-1234567 (ALI KHAN). Trx ID: 555666777. Easypaisa"
        )
        assertNotNull(parsed)
        assertEquals(250000L, parsed!!.amountPaisa)
        assertEquals("ALI KHAN", parsed.senderName)
        assertEquals("555666777", parsed.trxId)
    }

    @Test
    fun amountWithCommasAndPaisa_parses() {
        val parsed = SmsParser.parseSms(
            "3737",
            "Rs 12,345.50 received from 0300-9999999 (SARA BIBI). Easypaisa"
        )
        assertNotNull(parsed)
        assertEquals(1234550L, parsed!!.amountPaisa)
    }

    @Test
    fun missingTrxId_returnsNullTrxId() {
        val parsed = SmsParser.parseSms(
            "3737",
            "Rs 1,000.00 received from 0300-1111111 (BILAL AHMED). Easypaisa"
        )
        assertNotNull(parsed)
        assertEquals(100000L, parsed!!.amountPaisa)
        assertNull(parsed.trxId)
        assertEquals(VerificationStatus.VERIFIED, parsed.status)
    }

    @Test
    fun debitMessage_returnsNull() {
        val parsed = SmsParser.parseSms(
            "3737",
            "Rs 5,000.00 sent to 0300-1234567 (AHMED RAZA). Trx ID: 1234567890. Easypaisa"
        )
        assertNull(parsed)
    }

    @Test
    fun nonPaymentMessage_returnsNull() {
        val parsed = SmsParser.parseSms(
            "3737",
            "Your Easypaisa OTP is 482913. Do not share it with anyone."
        )
        assertNull(parsed)
    }

    @Test
    fun noAmount_returnsNull() {
        val parsed = SmsParser.parseSms(
            "3737",
            "Payment received from AHMED RAZA. Easypaisa"
        )
        assertNull(parsed)
    }

    @Test
    fun senderNameWithoutParens_parsesFromPhrase() {
        val parsed = SmsParser.parseSms(
            "3737",
            "Rs 3,000.00 received from Usman Ghani. Trx ID: 777888999. Easypaisa"
        )
        assertNotNull(parsed)
        assertEquals(300000L, parsed!!.amountPaisa)
        assertEquals("USMAN GHANI", parsed.senderName)
        assertEquals("777888999", parsed.trxId)
    }
}
