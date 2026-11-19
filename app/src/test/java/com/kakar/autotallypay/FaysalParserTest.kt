package com.kakar.autotallypay

import com.kakar.autotallypay.sms.PaymentSource
import com.kakar.autotallypay.sms.SmsParser
import com.kakar.autotallypay.sms.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Faysal Bank (sender FAYSAL) — 7 tests. Sample data is masked/fictional. */
class FaysalParserTest {

    @Test
    fun validFaysalSms_parsesAllFields() {
        val parsed = SmsParser.parseSms(
            "FAYSAL",
            "Your Faysal Bank A/C **5678 has been credited with PKR 7,500.00 on 19-Nov-2026. Ref: FT123456789. Faysal Bank"
        )
        assertNotNull(parsed)
        assertEquals(PaymentSource.FAYSAL, parsed!!.source)
        assertEquals(750000L, parsed.amountPaisa)
        assertEquals("AC*5678", parsed.accountHint)
        assertEquals("FT123456789", parsed.trxId)
        assertEquals(VerificationStatus.VERIFIED, parsed.status)
    }

    @Test
    fun withReferenceNumber_parses() {
        val parsed = SmsParser.parseSms(
            "FAYSAL",
            "PKR 15,000.00 credited to your Faysal Bank A/C *3344. Reference No. FB99887766. Faysal Bank"
        )
        assertNotNull(parsed)
        assertEquals(1500000L, parsed!!.amountPaisa)
        assertEquals("FB99887766", parsed.trxId)
        assertEquals("AC*3344", parsed.accountHint)
    }

    @Test
    fun senderNameByPhrase_parses() {
        val parsed = SmsParser.parseSms(
            "FAYSAL",
            "Your A/C *7788 credited with PKR 2,200.00 by USMAN TARIQ on 19-Nov-2026. Faysal Bank"
        )
        assertNotNull(parsed)
        assertEquals(220000L, parsed!!.amountPaisa)
        assertEquals("USMAN TARIQ", parsed.senderName)
    }

    @Test
    fun debitMessage_returnsNull() {
        val parsed = SmsParser.parseSms(
            "FAYSAL",
            "Your Faysal Bank A/C **5678 debited with PKR 1,000.00. Ref: FT000111222."
        )
        assertNull(parsed)
    }

    @Test
    fun noAmount_returnsNull() {
        val parsed = SmsParser.parseSms(
            "FAYSAL",
            "Your Faysal Bank A/C **5678 has been credited. Faysal Bank"
        )
        assertNull(parsed)
    }

    @Test
    fun amountWithPaisa_parses() {
        val parsed = SmsParser.parseSms(
            "FAYSAL",
            "PKR 999.99 credited to your A/C *1212. Faysal Bank"
        )
        assertNotNull(parsed)
        assertEquals(99999L, parsed!!.amountPaisa)
    }

    @Test
    fun promoMessage_returnsNull() {
        val parsed = SmsParser.parseSms(
            "FAYSAL",
            "Faysal Bank: Enjoy 10% off with your debit card this weekend!"
        )
        assertNull(parsed)
    }
}
