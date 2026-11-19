package com.kakar.autotallypay.sms

/**
 * Single entry point for incoming SMS text.
 * Returns null when the message is not a payment-received message
 * (debit, OTP, promo, or no amount found).
 * Fake/unknown senders still parse (so the inbox can show them in red)
 * but their status never counts as paid.
 */
object SmsParser {

    fun parseSms(sender: String, body: String): ParsedPayment? {
        val verdict = SenderVerifier.verify(sender)
        val source = if (verdict.status == VerificationStatus.VERIFIED) {
            verdict.source
        } else {
            detectSourceFromBody(body)
        }
        val fields = when (source) {
            PaymentSource.EASYPAISA ->
                BankParsers.parseEasypaisa(body) ?: BankParsers.parseGeneric(body)
            PaymentSource.JAZZCASH ->
                BankParsers.parseJazzCash(body) ?: BankParsers.parseGeneric(body)
            PaymentSource.MEEZAN ->
                BankParsers.parseMeezan(body) ?: BankParsers.parseGeneric(body)
            PaymentSource.FAYSAL ->
                BankParsers.parseFaysal(body) ?: BankParsers.parseGeneric(body)
            PaymentSource.BANK_AL_HABIB ->
                BankParsers.parseBankAlHabib(body) ?: BankParsers.parseGeneric(body)
            PaymentSource.UNKNOWN ->
                BankParsers.parseGeneric(body)
        } ?: return null
        return ParsedPayment(
            source = source,
            amountPaisa = fields.amountPaisa,
            senderName = fields.senderName,
            accountHint = fields.accountHint,
            trxId = fields.trxId,
            status = verdict.status,
            fakeReason = verdict.reason
        )
    }

    /** Best-effort source guess from the message text (for fake/unknown senders). */
    fun detectSourceFromBody(body: String): PaymentSource {
        val lower = body.lowercase()
        return when {
            lower.contains("easypaisa") -> PaymentSource.EASYPAISA
            lower.contains("jazzcash") -> PaymentSource.JAZZCASH
            lower.contains("meezan") -> PaymentSource.MEEZAN
            lower.contains("faysal") -> PaymentSource.FAYSAL
            lower.contains("al habib") || lower.contains("al-habib") -> PaymentSource.BANK_AL_HABIB
            else -> PaymentSource.UNKNOWN
        }
    }
}
