package com.clickflow.android.capture

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TextTargetControllerTest {

    private val fixedNow = 77_000L
    private val regionA = CaptureRegion(0.0f, 0.0f, 0.4f, 0.1f)
    private val regionB = CaptureRegion(0.0f, 0.2f, 0.6f, 0.3f)

    private val regions = listOf(
        OcrTextRegion("OK", regionA, confidence = 0.92f),
        OcrTextRegion("Cancel", regionB, confidence = 0.85f),
        OcrTextRegion("ok button", CaptureRegion(0.5f, 0.5f, 0.9f, 0.6f), confidence = 0.70f)
    )

    private lateinit var controller: TextTargetController

    @Before
    fun setUp() {
        val provider = StubOcrProvider(regions) { fixedNow }
        val ocrController = OcrController(provider)
        controller = TextTargetController(ocrController) { fixedNow }
    }

    // 1. Match found → Matched outcome
    @Test
    fun queryFound_returnsMatched() {
        val outcome = controller.evaluate("ok")
        assertTrue(outcome is TextTargetOutcome.Matched)
        val result = (outcome as TextTargetOutcome.Matched).result
        assertTrue(result.matched)
        assertNotNull(result.highlight)
        assertNotNull(result.matchedText)
    }

    // 2. Highlight is the bounds of the best (highest-confidence) match
    @Test
    fun matchedHighlight_isBestRegionBounds() {
        val outcome = controller.evaluate("ok") as TextTargetOutcome.Matched
        // "OK" has confidence 0.92 vs "ok button" 0.70 — "OK" should win
        assertEquals(regionA, outcome.result.highlight)
        assertEquals("OK", outcome.result.matchedText)
        assertEquals(0.92f, outcome.result.confidence, 0.0001f)
    }

    // 3. No match → NoMatch outcome
    @Test
    fun queryNotFound_returnsNoMatch() {
        val outcome = controller.evaluate("nonexistent")
        assertTrue(outcome is TextTargetOutcome.NoMatch)
        val result = (outcome as TextTargetOutcome.NoMatch).result
        assertFalse(result.matched)
        assertNull(result.highlight)
        assertNull(result.matchedText)
    }

    // 4. Empty query → Error with reason empty_query
    @Test
    fun emptyQuery_returnsError() {
        val outcome = controller.evaluate("")
        assertTrue(outcome is TextTargetOutcome.Error)
        assertEquals("empty_query", (outcome as TextTargetOutcome.Error).reason)
    }

    // 5. Blank query → Error
    @Test
    fun blankQuery_returnsError() {
        val outcome = controller.evaluate("   ")
        assertTrue(outcome is TextTargetOutcome.Error)
    }

    // 6. Case-insensitive by default
    @Test
    fun caseInsensitiveByDefault() {
        val outcome = controller.evaluate("cancel")
        assertTrue(outcome is TextTargetOutcome.Matched)
        assertEquals("Cancel", (outcome as TextTargetOutcome.Matched).result.matchedText)
    }

    // 7. Case-sensitive misses different-case region
    @Test
    fun caseSensitive_noMatch_differentCase() {
        val outcome = controller.evaluate("cancel", caseSensitive = true)
        assertTrue(outcome is TextTargetOutcome.NoMatch)
    }

    // 8. Case-sensitive matches exact case
    @Test
    fun caseSensitive_matches_exactCase() {
        val outcome = controller.evaluate("Cancel", caseSensitive = true)
        assertTrue(outcome is TextTargetOutcome.Matched)
    }

    // 9. evaluatedAtMs comes from nowProvider
    @Test
    fun evaluatedAtMs_fromNowProvider() {
        val outcome = controller.evaluate("ok") as TextTargetOutcome.Matched
        assertEquals(fixedNow, outcome.result.evaluatedAtMs)
    }

    // 10. query field is preserved in result
    @Test
    fun queryField_preservedInResult() {
        val outcome = controller.evaluate("Cancel") as TextTargetOutcome.Matched
        assertEquals("Cancel", outcome.result.query)
    }

    // 11. NoMatch has null highlight and zero confidence
    @Test
    fun noMatch_nullHighlightAndZeroConfidence() {
        val outcome = controller.evaluate("xyz") as TextTargetOutcome.NoMatch
        assertNull(outcome.result.highlight)
        assertEquals(0f, outcome.result.confidence, 0.0001f)
    }
}
