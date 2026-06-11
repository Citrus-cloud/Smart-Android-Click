package com.clickflow.android.textclick

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextClickStoreTest {

    // ---- normalized() tests ----

    @Test
    fun normalized_trimsQueries() {
        val config = TextClickConfig(queries = listOf("  OK  ", "  Hello  "))
        val normalized = config.normalized()
        assertEquals(listOf("OK", "Hello"), normalized.queries)
    }

    @Test
    fun normalized_filtersBlankQueries() {
        val config = TextClickConfig(queries = listOf("OK", "", "  ", "Go"))
        val normalized = config.normalized()
        assertEquals(listOf("OK", "Go"), normalized.queries)
    }

    @Test
    fun normalized_deduplicatesQueries() {
        val config = TextClickConfig(queries = listOf("OK", "ok", "OK"))
        val normalized = config.normalized()
        // With ignoreCase=true (default), "OK" and "ok" are different strings
        // but distinct() keeps both since it's string equality
        assertEquals(2, normalized.queries.size)
    }

    @Test
    fun normalized_maxTenQueries() {
        val queries = (1..20).map { "Query $it" }
        val config = TextClickConfig(queries = queries)
        val normalized = config.normalized()
        assertEquals(10, normalized.queries.size)
    }

    @Test
    fun normalized_queryMaxLength200() {
        val longQuery = "A".repeat(300)
        val config = TextClickConfig(queries = listOf(longQuery))
        val normalized = config.normalized()
        assertEquals(200, normalized.queries[0].length)
    }

    @Test
    fun normalized_emptyQueries_fallbackToDefault() {
        val config = TextClickConfig(queries = listOf("", "  "))
        val normalized = config.normalized()
        assertEquals(1, normalized.queries.size)
        assertEquals("OK", normalized.queries[0])
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
            queries = listOf("OK"),
            contains = false,
            ignoreCase = false,
            continuous = true,
            infinite = true,
        )
        val normalized = config.normalized()
        assertFalse(normalized.contains)
        assertFalse(normalized.ignoreCase)
        assertTrue(normalized.continuous)
        assertTrue(normalized.infinite)
    }

    @Test
    fun normalized_singleQuery() {
        val config = TextClickConfig(queries = listOf("Test"))
        val normalized = config.normalized()
        assertEquals(1, normalized.queries.size)
        assertEquals("Test", normalized.queries[0])
    }
}
