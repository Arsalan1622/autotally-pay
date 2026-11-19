package com.kakar.autotallypay

import com.kakar.autotallypay.sms.PaymentSource
import com.kakar.autotallypay.sms.SmsParser
import com.kakar.autotallypay.sms.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Bank AL Habib (sender BAHL / ALHABIB) — 7 tests. Sample data is masked/fictional. */
class BankAlHabibParserTest {

    @Test
    fun validBankAlHabibSms_parsesAllFields() {
        val parsed = SmsParser.parseSms(
            "BAHL",
            "Dear Customer, PKR 3,200.00 credited to your A/C *9012 on 19/11/2026. Bank AL Habib"
        )
        assertNotNull(parsed)
        assertEquals(PaymentSource.BANK_AL_HABIB, parsed!!.source)
        assertEquals(320000L, parsed.amountPaisa)
        assertEquals("AC*9012", parsed.accountHint)
        assertEquals(VerificationStatus.VERIFIED, parsed.status)
    }

    @Test
    fun withSenderName_parses() {
        val parsed = SmsParser.parseSms(
            "BAHL",
            "PKR 6,400.00 credited to your A/C *3456 from KHALID MEHMOOD on 19/11/2026. Bank AL Habib"
        )
        assertNotNull(parsed)
        assertEquals(640000L, parsed!!.amountPaisa)
        assertEquals("KHALID MEHMOOD", parsed.senderName)
        assertEquals("AC*3456", parsed.accountHint)
    }

    @Test
    fun withTxnId_parses() {
        val parsed = SmsParser.parseSms(
            "BAHL",
            "PKR 11,000.00 credited to your A/C *7890. Txn ID: BAHL55667788. Bank AL Habib"
        )
        assertNotNull(parsed)
        assertEquals(1100000L, parsed!!.amountPaisa)
        assertEquals("BAHL55667788", parsed.trxId)
    }

    @Test
    fun debitMessage_returnsNull() {
        val parsed = SmsParser.parseSms(
            "BAHL",
            "PKR 2,000.00 debited from your A/C *9012 on 19/11/2026. Bank AL Habib"
        )
        assertNull(parsed)
    }

    @Test
    fun otpMessage_returnsNull() {
        val parsed = SmsParser.parseSms(
            "BAHL",
            "Your Bank AL Habib OTP is 654321. Do not share."
        )
        assertNull(parsed)
    }

    @Test
    fun largeAmount_parses() {
        val parsed = SmsParser.parseSms(
            "BAHL",
            "PKR 1,250,000.00 credited to your A/C *1122 on 19/11/2026. Bank AL Habib"
        )
        assertNotNull(parsed)
        assertEquals(125000000L, parsed!!.amountPaisa)
    }

    @Test
    fun alHabibSenderId_isVerified() {
        val parsed = SmsParser.parseSms(
            "ALHABIB",
            "Dear Customer, PKR 3,200.00 credited to your A/C *9012 on 19/11/2026. Bank AL Habib"
        )
        assertNotNull(parsed)
        assertEquals(PaymentSource.BANK_AL_HABIB, parsed!!.source)
        assertEquals(VerificationStatus.VERIFIED, parsed.status)
    }
}
