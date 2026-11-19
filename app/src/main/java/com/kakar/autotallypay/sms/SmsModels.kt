package com.kakar.autotallypay.sms

enum class PaymentSource {
    EASYPAISA,
    JAZZCASH,
    MEEZAN,
    FAYSAL,
    BANK_AL_HABIB,
    UNKNOWN
}

/**
 * VERIFIED: sender is a known bank/wallet short code or sender ID — counts as paid.
 * FAKE: sender is an ordinary mobile number — NEVER counts as paid, shown in red.
 * UNKNOWN_SENDER: sender is not a known bank/wallet sender — never counts as paid.
 */
enum class VerificationStatus {
    VERIFIED,
    FAKE,
    UNKNOWN_SENDER
}

data class ParsedPayment(
    val source: PaymentSource,
    val amountPaisa: Long,
    val senderName: String?,
    val accountHint: String?,
    val trxId: String?,
    val status: VerificationStatus,
    val fakeReason: String?
)

/** Fields one bank parser extracts from a message body. */
data class ParsedFields(
    val amountPaisa: Long,
    val senderName: String?,
    val accountHint: String?,
    val trxId: String?
)
