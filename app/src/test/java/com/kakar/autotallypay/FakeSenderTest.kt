package com.kakar.autotallypay

import com.kakar.autotallypay.sms.PaymentSource
import com.kakar.autotallypay.sms.SenderVerifier
import com.kakar.autotallypay.sms.SmsParser
import com.kakar.autotallypay.sms.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fake-SMS protection — 8 tests. A mobile-number sender is NEVER verified. */
class FakeSenderTest {

    private val easypaisaBody =
        "Rs 5,000.00 received from 0300-1234567 (AHMED RAZA). Trx ID: 1234567890. Easypaisa"

    @Test
    fun mobileSender_isFake() {
        val parsed = SmsParser.parseSms("03001234567", easypaisaBody)
        assertNotNull(parsed)
        assertEquals(VerificationStatus.FAKE, parsed!!.status)
        assertEquals(PaymentSource.EASYPAISA, parsed.source)
        assertEquals(500000L, parsed.amountPaisa)
        assertNotNull(parsed.fakeReason)
        assertTrue(SenderVerifier.isMobileNumberSender("03001234567"))
    }

    @Test
    fun plus92MobileSender_isFake() {
        val parsed = SmsParser.parseSms("+92 300 1234567", easypaisaBody)
        assertNotNull(parsed)
        assertEquals(VerificationStatus.FAKE, parsed!!.status)
        assertTrue(SenderVerifier.isMobileNumberSender("+92 300 1234567"))
    }

    @Test
    fun mobileSenderWithDashes_isFake() {
        val parsed = SmsParser.parseSms("0300-1234567", easypaisaBody)
        assertNotNull(parsed)
        assertEquals(VerificationStatus.FAKE, parsed!!.status)
    }

    @Test
    fun unknownAlphanumericSender_isUnknown() {
        val parsed = SmsParser.parseSms("PAYMENT", easypaisaBody)
        assertNotNull(parsed)
        assertEquals(VerificationStatus.UNKNOWN_SENDER, parsed!!.status)
        assertNotNull(parsed.fakeReason)
    }

    @Test
    fun unknownShortCode_isUnknown() {
        val parsed = SmsParser.parseSms(
            "6778",
            "Your A/C *1234 credited with PKR 10,000.00 on 19-NOV-26 by AHMED RAZA. Meezan Bank"
        )
        assertNotNull(parsed)
        assertEquals(VerificationStatus.UNKNOWN_SENDER, parsed!!.status)
    }

    @Test
    fun verifiedShortCode3737_isVerified() {
        val parsed = SmsParser.parseSms("3737", easypaisaBody)
        assertNotNull(parsed)
        assertEquals(VerificationStatus.VERIFIED, parsed!!.status)
        assertNull(parsed.fakeReason)
        assertFalse(SenderVerifier.isMobileNumberSender("3737"))
    }

    @Test
    fun verifiedShortCode8558_isVerified() {
        val parsed = SmsParser.parseSms(
            "8558",
            "You have received Rs. 2,500.00 from 0301-7654321 (FATIMA KHAN). Trx ID 9876543210. JazzCash"
        )
        assertNotNull(parsed)
        assertEquals(VerificationStatus.VERIFIED, parsed!!.status)
        assertEquals(PaymentSource.JAZZCASH, parsed.source)
    }

    @Test
    fun verifiedBankSenderId_isVerified() {
        val parsed = SmsParser.parseSms(
            "MEEZAN",
            "Your A/C *1234 credited with PKR 10,000.00 on 19-NOV-26 by AHMED RAZA. Meezan Bank"
        )
        assertNotNull(parsed)
        assertEquals(VerificationStatus.VERIFIED, parsed!!.status)
        assertEquals(PaymentSource.MEEZAN, parsed.source)
    }
}
