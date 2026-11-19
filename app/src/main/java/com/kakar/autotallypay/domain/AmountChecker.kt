package com.kakar.autotallypay.domain

/** Minimal payment shape the amount checker needs. */
data class PaymentLike(
    val id: Long,
    val amountPaisa: Long,
    val trxId: String?,
    val senderName: String?,
    val source: String,
    val receivedAt: Long,
    val verified: Boolean
)

/**
 * "Did the money come?" — exact-amount lookup across VERIFIED
 * payments only. Fake and unknown-sender payments never answer YES.
 */
object AmountChecker {

    fun findByAmount(payments: List<PaymentLike>, amountPaisa: Long): List<PaymentLike> =
        payments.filter { it.verified && it.amountPaisa == amountPaisa }
            .sortedByDescending { it.receivedAt }
}
