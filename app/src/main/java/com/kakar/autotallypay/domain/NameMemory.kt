package com.kakar.autotallypay.domain

/** One row of the name-memory table (sender key -> customer). */
data class SenderMappingLike(val senderKey: String, val customerId: Long)

/**
 * Name memory: assign a bank payment to a customer once, and their
 * next payments match by themselves. Keys are normalised so
 * "0300-1234567", "03001234567" and " 0300 123 4567 " all match.
 */
object NameMemory {

    fun normalizeKey(raw: String): String =
        raw.uppercase().filter { it.isLetterOrDigit() }

    fun match(mappings: List<SenderMappingLike>, senderKeyRaw: String): Long? {
        val key = normalizeKey(senderKeyRaw)
        if (key.isEmpty()) return null
        return mappings.firstOrNull { normalizeKey(it.senderKey) == key }?.customerId
    }

    /** Keys to try for a payment, most specific first. */
    fun candidateKeys(senderId: String?, accountHint: String?, senderName: String?): List<String> =
        listOfNotNull(senderId, accountHint, senderName)
            .map { normalizeKey(it) }
            .filter { it.isNotEmpty() }
            .distinct()
}
