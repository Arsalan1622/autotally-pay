package com.kakar.autotallypay.sms

/**
 * Fake-SMS protection. Only known alphanumeric/short-code senders
 * count as verified. A "payment received" message from an ordinary
 * mobile number (03xx / +92 3xx) is NEVER counted as paid.
 */
object SenderVerifier {

    val VERIFIED_SENDERS: Map<String, PaymentSource> = mapOf(
        "3737" to PaymentSource.EASYPAISA,
        "EASYPAISA" to PaymentSource.EASYPAISA,
        "8558" to PaymentSource.JAZZCASH,
        "JAZZCASH" to PaymentSource.JAZZCASH,
        "MEEZAN" to PaymentSource.MEEZAN,
        "MEEZANBANK" to PaymentSource.MEEZAN,
        "FAYSAL" to PaymentSource.FAYSAL,
        "FAYSALBANK" to PaymentSource.FAYSAL,
        "BAHL" to PaymentSource.BANK_AL_HABIB,
        "ALHABIB" to PaymentSource.BANK_AL_HABIB,
        "BANKALHABIB" to PaymentSource.BANK_AL_HABIB
    )

    fun normalizeSender(sender: String): String =
        sender.uppercase().filter { it.isLetterOrDigit() }

    /** True when the sender looks like an ordinary Pakistani mobile number. */
    fun isMobileNumberSender(sender: String): Boolean {
        val digits = sender.filter { it.isDigit() }
        val normalized = when {
            digits.startsWith("92") && digits.length == 12 -> "0" + digits.substring(2)
            else -> digits
        }
        return normalized.length == 11 && normalized.startsWith("03")
    }

    data class Verdict(
        val source: PaymentSource,
        val status: VerificationStatus,
        val reason: String?
    )

    fun verify(sender: String): Verdict {
        if (isMobileNumberSender(sender)) {
            return Verdict(
                PaymentSource.UNKNOWN,
                VerificationStatus.FAKE,
                "Sender is an ordinary mobile number, not a bank or wallet short code"
            )
        }
        val key = normalizeSender(sender)
        val source = VERIFIED_SENDERS[key]
        return if (source != null) {
            Verdict(source, VerificationStatus.VERIFIED, null)
        } else {
            Verdict(
                PaymentSource.UNKNOWN,
                VerificationStatus.UNKNOWN_SENDER,
                "Unknown sender — not a verified bank or wallet sender"
            )
        }
    }
}
