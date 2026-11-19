package com.kakar.autotallypay.sms

import com.kakar.autotallypay.domain.Money

/**
 * Per-bank SMS parsers, built from typical real Pakistani bank/wallet
 * message formats. All sample data in tests is masked/fictional.
 *
 * Typical formats handled:
 * - Easypaisa (3737): "Rs 5,000.00 received from 0300-XXXXXXX (AHMED RAZA)
 *   on 19-Nov-26 at 10:15 AM. Trx ID: 1234567890. Easypaisa"
 * - JazzCash (8558): "You have received Rs. 2,500.00 from 0301-XXXXXXX
 *   (FATIMA KHAN). Trx ID 9876543210. JazzCash"
 * - Meezan: "Your A/C *1234 credited with PKR 10,000.00 on 19-NOV-26
 *   by AHMED RAZA. Avl Bal PKR 45,000.00. Meezan Bank"
 * - Faysal: "Your Faysal Bank A/C **5678 has been credited with
 *   PKR 7,500.00 on 19-Nov-2026. Ref: FT123456789."
 * - Bank AL Habib: "PKR 3,200.00 credited to your A/C *9012
 *   on 19/11/2026. Bank AL Habib"
 */
object BankParsers {

    private val AMOUNT_REGEX =
        Regex("""(?:Rs\.?|PKR)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val TRX_REGEX = Regex(
        """(?:Trx\.?\s*ID|Txn\.?\s*ID|Transaction\s*ID|Ref(?:erence)?(?:\s*No\.?)?)\s*[:#]?\s*([A-Za-z0-9\-]{4,})""",
        RegexOption.IGNORE_CASE
    )
    private val PAREN_NAME_REGEX = Regex("""\(\s*([A-Za-z .]{3,40})\s*\)""")
    private val FROM_NAME_REGEX = Regex(
        """\bfrom\s+([A-Za-z][A-Za-z .]{2,30}?)(?:\.|,| on | at | Trx| Txn| Ref|$)""",
        RegexOption.IGNORE_CASE
    )
    private val BY_NAME_REGEX = Regex(
        """\bby\s+([A-Za-z][A-Za-z .]{2,30}?)(?:\.|,| on |$)""",
        RegexOption.IGNORE_CASE
    )
    private val AC_HINT_REGEX = Regex("""A/?C\s*\*+(\d{4})""", RegexOption.IGNORE_CASE)
    private val ENDING_HINT_REGEX = Regex("""ending\s+(\d{4})""", RegexOption.IGNORE_CASE)
    private val MOBILE_HINT_REGEX = Regex("""(03\d{2}[\- ]?\d{7})""")

    private val CREDIT_WORDS = listOf("received", "credited")
    private val DEBIT_WORDS =
        listOf("debited", "sent to", "withdrawn", "payment sent", "deducted", "paid to")

    /** True only for an incoming-money message (never for a debit/sent message). */
    fun isCreditMessage(body: String): Boolean {
        val lower = body.lowercase()
        if (DEBIT_WORDS.any { lower.contains(it) }) return false
        return CREDIT_WORDS.any { lower.contains(it) }
    }

    /** First Rs/PKR amount in the message (the credited amount, not the balance). */
    fun extractAmountPaisa(body: String): Long? {
        val m = AMOUNT_REGEX.find(body) ?: return null
        return Money.rupeesToPaisa(m.groupValues[1])
    }

    fun extractTrxId(body: String): String? =
        TRX_REGEX.find(body)?.groupValues?.get(1)?.trim()

    /** "Usman Ghani. " -> "USMAN GHANI" (trims stray dots/spaces, collapses gaps). */
    private fun cleanName(raw: String): String =
        raw.trim().trimEnd('.', ' ').replace(Regex("\\s+"), " ").trim().uppercase()

    /** Wallet style: "from 0300-XXXXXXX (AHMED RAZA)". */
    fun extractWalletName(body: String): String? {
        PAREN_NAME_REGEX.find(body)?.let { return cleanName(it.groupValues[1]) }
        FROM_NAME_REGEX.find(body)?.let { return cleanName(it.groupValues[1]) }
        return null
    }

    /** Bank style: "credited ... by AHMED RAZA". */
    fun extractBankName(body: String): String? {
        BY_NAME_REGEX.find(body)?.let { return cleanName(it.groupValues[1]) }
        FROM_NAME_REGEX.find(body)?.let { return cleanName(it.groupValues[1]) }
        PAREN_NAME_REGEX.find(body)?.let { return cleanName(it.groupValues[1]) }
        return null
    }

    fun extractAccountHint(body: String): String? {
        AC_HINT_REGEX.find(body)?.let { return "AC*" + it.groupValues[1] }
        ENDING_HINT_REGEX.find(body)?.let { return "AC*" + it.groupValues[1] }
        MOBILE_HINT_REGEX.find(body)?.let { return it.groupValues[1] }
        return null
    }

    private fun parseWith(
        body: String,
        nameExtractor: (String) -> String?
    ): ParsedFields? {
        if (!isCreditMessage(body)) return null
        val amount = extractAmountPaisa(body) ?: return null
        return ParsedFields(
            amountPaisa = amount,
            senderName = nameExtractor(body),
            accountHint = extractAccountHint(body),
            trxId = extractTrxId(body)
        )
    }

    fun parseEasypaisa(body: String): ParsedFields? =
        parseWith(body, ::extractWalletName)

    fun parseJazzCash(body: String): ParsedFields? =
        parseWith(body, ::extractWalletName)

    fun parseMeezan(body: String): ParsedFields? =
        parseWith(body, ::extractBankName)

    fun parseFaysal(body: String): ParsedFields? =
        parseWith(body, ::extractBankName)

    fun parseBankAlHabib(body: String): ParsedFields? =
        parseWith(body, ::extractBankName)

    fun parseGeneric(body: String): ParsedFields? =
        parseWith(body) { b -> extractWalletName(b) ?: extractBankName(b) }
}
