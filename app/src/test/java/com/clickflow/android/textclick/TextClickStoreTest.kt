package com.clickflow.android.textclick

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextClickStoreTest {

    @Test
    fun normalized_trimsQuery() {
        val config = TextClickConfig(query = "  OK  ")
        val normalized = config.normalized()
        assertEquals("OK", normalized.query)
    }

    @Test
    fun normalized_blankQuery_fallsBackToDefault() {
        val config = TextClickConfig(query = "")
        val normalized = config.normalized()
        assertEquals("OK", normalized.query)
    }

    @Test
    fun normalized_whitespaceQuery_fallsBackToDefault() {
        val config = TextClickConfig(query = "   ")
        val normalized = config.normalized()
        assertEquals("OK", normalized.query)
    }

    @Test
    fun normalized_queryMaxLength200() {
        val longQuery = "A".repeat(300)
        val config = TextClickConfig(query = longQuery)
        val normalized = config.normalized()
        assertEquals(200, normalized.query.length)
    }

    @Test
    fun normalized_intervalMs_coerced() {
        val config = TextClickConfig(intervalMs = 50L)
        val normalized = config.normalized()
        assertEquals(300L, normalized.intervalMs)

        val high = config.copy(intervalMs = 999999L).normalized()
        assertEquals(600000L, high.intervalMs)
    }

    @Test
    fun normalized_repeatCount_coerced() {
        val config = TextClickConfig(repeatCount = 0)
        val normalized = config.normalized()
        assertEquals(1, normalized.repeatCount)

        val high = config.copy(repeatCount = 999999).normalized()
        assertEquals(100000, high.repeatCount)
    }

    @Test
    fun normalized_preservesOtherFields() {
        val config = TextClickConfig(
            query = "Test",
            contains = false,
            ignoreCase = false,
            continuous = true,
            infinite = true,
        )
        val normalized = config.normalized()
        assertEquals("Test", normalized.query)
        assertFalse(normalized.contains)
        assertFalse(normalized.ignoreCase)
        assertTrue(normalized.continuous)
        assertTrue(normalized.infinite)
    }

    @Test
    fun normalized_singleQuery() {
        val config = TextClickConfig(query = "Submit")
        val normalized = config.normalized()
        assertEquals("Submit", normalized.query)
    }
}
