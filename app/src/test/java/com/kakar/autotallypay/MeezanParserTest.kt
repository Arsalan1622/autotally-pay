package com.kakar.autotallypay

import com.kakar.autotallypay.sms.PaymentSource
import com.kakar.autotallypay.sms.SmsParser
import com.kakar.autotallypay.sms.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Meezan Bank (sender MEEZAN) — 7 tests. Sample data is masked/fictional. */
class MeezanParserTest {

    @Test
    fun validMeezanSms_parsesAllFields() {
        val parsed = SmsParser.parseSms(
            "MEEZAN",
            "Dear Customer, Your A/C *1234 credited with PKR 10,000.00 on 19-NOV-26 by AHMED RAZA. Avl Bal PKR 45,000.00. Meezan Bank"
        )
        assertNotNull(parsed)
        assertEquals(PaymentSource.MEEZAN, parsed!!.source)
        assertEquals(1000000L, parsed.amountPaisa)
        assertEquals("AHMED RAZA", parsed.senderName)
        assertEquals("AC*1234", parsed.accountHint)
        assertNull(parsed.trxId)
        assertEquals(VerificationStatus.VERIFIED, parsed.status)
    }

    @Test
    fun withTxnId_parses() {
        val parsed = SmsParser.parseSms(
            "MEEZAN",
            "Your A/C *5678 credited with PKR 25,000.00 on 19-NOV-26. Txn ID: MZ987654321. Meezan Bank"
        )
        assertNotNull(parsed)
        assertEquals(2500000L, parsed!!.amountPaisa)
        assertEquals("MZ987654321", parsed.trxId)
        assertEquals("AC*5678", parsed.accountHint)
        assertNull(parsed.senderName)
    }

    @Test
    fun amountWithoutCommas_parses() {
        val parsed = SmsParser.parseSms(
            "MEEZAN",
            "A/C *9012 credited with PKR 5000.00 by BILAL AHMED. Meezan Bank"
        )
        assertNotNull(parsed)
        assertEquals(500000L, parsed!!.amountPaisa)
        assertEquals("BILAL AHMED", parsed.senderName)
    }

    @Test
    fun debitMessage_returnsNull() {
        val parsed = SmsParser.parseSms(
            "MEEZAN",
            "Your A/C *1234 debited with PKR 3,000.00 on 19-NOV-26. Meezan Bank"
        )
        assertNull(parsed)
    }

    @Test
    fun balanceOnlyMessage_returnsNull() {
        val parsed = SmsParser.parseSms(
            "MEEZAN",
            "Your A/C *1234 balance is PKR 45,000.00. Meezan Bank"
        )
        assertNull(parsed)
    }

    @Test
    fun accountEndingHint_parses() {
        val parsed = SmsParser.parseSms(
            "MEEZAN",
            "PKR 8,750.00 credited to your account ending 4433 on 19-NOV-26 by SARA KHAN. Meezan Bank"
        )
        assertNotNull(parsed)
        assertEquals(875000L, parsed!!.amountPaisa)
        assertEquals("AC*4433", parsed.accountHint)
        assertEquals("SARA KHAN", parsed.senderName)
    }

    @Test
    fun lowercaseBody_parses() {
        val parsed = SmsParser.parseSms(
            "MEEZAN",
            "your a/c *2222 credited with pkr 1,200.00 on 19-nov-26 by ali raza. meezan bank"
        )
        assertNotNull(parsed)
        assertEquals(120000L, parsed!!.amountPaisa)
        assertEquals("ALI RAZA", parsed.senderName)
        assertEquals("AC*2222", parsed.accountHint)
    }
}
