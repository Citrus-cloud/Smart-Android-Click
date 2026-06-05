package com.clickflow.android.capture

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OcrControllerTest {

    private val fixedNow = 55_000L
    private val regionA = CaptureRegion(0.0f, 0.0f, 0.3f, 0.1f)
    private val regionB = CaptureRegion(0.0f, 0.2f, 0.5f, 0.3f)
    private val regionC = CaptureRegion(0.5f, 0.5f, 1.0f, 1.0f)

    // Text regions injected into the stub
    private val regions = listOf(
        OcrTextRegion("Submit", regionA, confidence = 0.95f),
        OcrTextRegion("Cancel", regionB, confidence = 0.80f),
        OcrTextRegion("submit order", regionC, confidence = 0.70f)
    )

    private lateinit var controller: OcrController

    @Before
    fun setUp() {
        val provider = StubOcrProvider(regions) { fixedNow }
        controller = OcrController(provider)
    }

    // 1. recognize() returns all injected regions
    @Test
    fun recognize_returnsAllRegions() {
        val result = controller.recognize()
        assertEquals(3, result.regions.size)
        assertEquals(fixedNow, result.recognizedAtMs)
    }

    // 2. pageText is built from all region texts
    @Test
    fun pageText_concatenatesRegions() {
        val result = controller.recognize()
        assertEquals("Submit Cancel submit order", result.pageText)
    }

    // 3. findText case-insensitive contains
    @Test
    fun findText_caseInsensitive_matchesMultiple() {
        val result = controller.recognize()
        val matches = controller.findText("submit", result)
        assertEquals(2, matches.size) // "Submit" and "submit order"
    }

    // 4. findText case-sensitive contains
    @Test
    fun findText_caseSensitive_matchesExact() {
        val result = controller.recognize()
        val matches = controller.findText("Submit", result, caseSensitive = true)
        assertEquals(1, matches.size)
        assertEquals("Submit", matches.first().text)
    }

    // 5. findText empty query returns empty list
    @Test
    fun findText_emptyQuery_returnsEmpty() {
        val result = controller.recognize()
        assertTrue(controller.findText("", result).isEmpty())
    }

    // 6. findText no match returns empty list
    @Test
    fun findText_noMatch_returnsEmpty() {
        val result = controller.recognize()
        assertTrue(controller.findText("nonexistent", result).isEmpty())
    }

    // 7. findExact case-insensitive
    @Test
    fun findExact_caseInsensitive() {
        val result = controller.recognize()
        val matches = controller.findExact("submit", result)
        assertEquals(1, matches.size)
        assertEquals("Submit", matches.first().text)
    }

    // 8. findExact case-sensitive — does not match different case
    @Test
    fun findExact_caseSensitive_noMatch() {
        val result = controller.recognize()
        val matches = controller.findExact("submit", result, caseSensitive = true)
        assertTrue(matches.isEmpty())
    }

    // 9. bestMatch returns highest-confidence region
    @Test
    fun bestMatch_returnsHighestConfidence() {
        val result = controller.recognize()
        val best = controller.bestMatch("submit", result)
        assertNotNull(best)
        assertEquals("Submit", best!!.text)
        assertEquals(0.95f, best.confidence, 0.0001f)
    }

    // 10. bestMatch no match returns null
    @Test
    fun bestMatch_noMatch_returnsNull() {
        val result = controller.recognize()
        assertNull(controller.bestMatch("xyz", result))
    }

    // 11. OcrResult.empty() has no regions and empty pageText
    @Test
    fun ocrResultEmpty_hasNoRegions() {
        val empty = OcrResult.empty(fixedNow)
        assertTrue(empty.regions.isEmpty())
        assertEquals("", empty.pageText)
        assertEquals(fixedNow, empty.recognizedAtMs)
    }

    // 12. OcrTextRegion.isUsable is false for invalid region
    @Test
    fun ocrTextRegion_isUsable_falseForInvalidBounds() {
        val invalid = OcrTextRegion("Hello", CaptureRegion(0.5f, 0.5f, 0.1f, 0.1f), 0.9f)
        assertFalse(invalid.bounds.isValid)
        assertFalse(invalid.isUsable)
    }
}
