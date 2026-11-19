package com.kakar.autotallypay

import com.kakar.autotallypay.domain.NameMemory
import com.kakar.autotallypay.domain.SenderMappingLike
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Name memory matching — 2 tests. */
class NameMemoryTest {

    @Test
    fun matchNormalizesCaseAndSeparators() {
        val mappings = listOf(
            SenderMappingLike("0300-1234567", 42),
            SenderMappingLike("AHMED RAZA", 7)
        )
        assertEquals(42L, NameMemory.match(mappings, "03001234567"))
        assertEquals(42L, NameMemory.match(mappings, " 0300 123 4567 "))
        assertEquals(7L, NameMemory.match(mappings, "ahmed raza"))
        assertEquals(7L, NameMemory.match(mappings, "Ahmed-Raza"))
    }

    @Test
    fun noMatch_returnsNull() {
        val mappings = listOf(SenderMappingLike("0300-1234567", 42))
        assertNull(NameMemory.match(mappings, "03009998888"))
        assertNull(NameMemory.match(emptyList(), "03001234567"))
        assertNull(NameMemory.match(mappings, ""))
        assertEquals("AC1234", NameMemory.normalizeKey("AC*1234"))
    }
}
