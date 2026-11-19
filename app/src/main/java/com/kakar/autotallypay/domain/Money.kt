package com.kakar.autotallypay.domain

import java.text.NumberFormat
import java.util.Locale

/**
 * Money is ALWAYS stored as Long paisa. Never floats.
 * 1 rupee = 100 paisa. All parsing/formatting goes through here.
 */
object Money {

    /** Parse "5,000.00", "Rs 1,250", "PKR 10,000.50" -> paisa. Null on bad input. */
    fun rupeesToPaisa(text: String): Long? {
        var clean = text.trim()
        clean = clean.replace(",", "")
        clean = clean.replace(Regex("(?i)Rs\\.?"), "")
        clean = clean.replace(Regex("(?i)PKR"), "")
        clean = clean.trim()
        if (clean.isEmpty()) return null
        val parts = clean.split(".")
        if (parts.size > 2) return null
        val whole = parts[0].ifEmpty { "0" }
        if (whole.isEmpty() || !whole.all { it.isDigit() }) return null
        val frac = if (parts.size == 2) parts[1] else ""
        if (frac.length > 2 || !frac.all { it.isDigit() }) return null
        val fracPadded = (frac + "00").substring(0, 2)
        return try {
            whole.toLong() * 100 + fracPadded.toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** 500000 -> "Rs 5,000.00" */
    fun formatRupees(paisa: Long): String {
        val negative = paisa < 0
        val abs = if (paisa == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(paisa)
        val rupees = abs / 100
        val p = abs % 100
        val grouped = NumberFormat.getNumberInstance(Locale.US).format(rupees)
        val sign = if (negative) "-" else ""
        return "Rs $sign$grouped.${p.toString().padStart(2, '0')}"
    }
}
