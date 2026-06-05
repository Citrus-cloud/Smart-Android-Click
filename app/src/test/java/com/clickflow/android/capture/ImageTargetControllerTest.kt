package com.clickflow.android.capture

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ImageTargetControllerTest {

    private lateinit var manager: TemplateManager
    private lateinit var matcher: TemplateMatcher
    private lateinit var controller: ImageTargetController

    private val fixedNow = 12_000L
    private val region = CaptureRegion(0.1f, 0.1f, 0.5f, 0.5f)
    private val templateId get() = manager.list().first().id

    @Before
    fun setUp() {
        manager = TemplateManager { fixedNow }
        matcher = TemplateMatcher { fixedNow }
        controller = ImageTargetController(manager, matcher)
        // Add a template with threshold 0.8
        manager.add("Target", region, 100, 100, 0.8f)
    }

    // 1. Match above threshold → Matched outcome
    @Test
    fun matchAboveThreshold_returnsMatched() {
        val candidate = MatchCandidate(region, rawScore = 0.9f)
        val outcome = controller.evaluate(templateId, listOf(candidate))
        assertTrue(outcome is ImageTargetOutcome.Matched)
        val result = (outcome as ImageTargetOutcome.Matched).result
        assertTrue(result.matched)
        assertEquals(0.9f, result.confidence, 0.0001f)
        assertEquals(region, result.highlight)
    }

    // 2. Score below threshold → NoMatch outcome
    @Test
    fun scoreBelowThreshold_returnsNoMatch() {
        val candidate = MatchCandidate(region, rawScore = 0.5f)
        val outcome = controller.evaluate(templateId, listOf(candidate))
        assertTrue(outcome is ImageTargetOutcome.NoMatch)
        val result = (outcome as ImageTargetOutcome.NoMatch).result
        assertFalse(result.matched)
        assertNull(result.highlight)
    }

    // 3. Exactly at threshold → Matched
    @Test
    fun scoreExactlyAtThreshold_returnsMatched() {
        val candidate = MatchCandidate(region, rawScore = 0.8f)
        val outcome = controller.evaluate(templateId, listOf(candidate))
        assertTrue(outcome is ImageTargetOutcome.Matched)
    }

    // 4. Empty candidates → NoMatch
    @Test
    fun emptyCandidates_returnsNoMatch() {
        val outcome = controller.evaluate(templateId, emptyList())
        assertTrue(outcome is ImageTargetOutcome.NoMatch)
        val result = (outcome as ImageTargetOutcome.NoMatch).result
        assertFalse(result.matched)
    }

    // 5. Unknown templateId → Error with reason template_not_found
    @Test
    fun unknownTemplateId_returnsError() {
        val outcome = controller.evaluate("tpl-999", emptyList())
        assertTrue(outcome is ImageTargetOutcome.Error)
        assertEquals("template_not_found", (outcome as ImageTargetOutcome.Error).reason)
    }

    // 6. Best candidate is picked from multiple
    @Test
    fun bestCandidatePicked_fromMultiple() {
        val region2 = CaptureRegion(0.5f, 0.5f, 0.9f, 0.9f)
        val candidates = listOf(
            MatchCandidate(region, rawScore = 0.6f),
            MatchCandidate(region2, rawScore = 0.95f),
            MatchCandidate(region, rawScore = 0.7f)
        )
        val outcome = controller.evaluate(templateId, candidates)
        assertTrue(outcome is ImageTargetOutcome.Matched)
        val result = (outcome as ImageTargetOutcome.Matched).result
        assertEquals(0.95f, result.confidence, 0.0001f)
        assertEquals(region2, result.highlight)
    }

    // 7. evaluatedAtMs comes from nowProvider
    @Test
    fun evaluatedAtMs_fromNowProvider() {
        val candidate = MatchCandidate(region, rawScore = 0.9f)
        val outcome = controller.evaluate(templateId, listOf(candidate))
        val result = (outcome as ImageTargetOutcome.Matched).result
        assertEquals(fixedNow, result.evaluatedAtMs)
    }

    // 8. Raw score > 1 clamped → still Matched
    @Test
    fun rawScoreAboveOne_clampedToOne() {
        val candidate = MatchCandidate(region, rawScore = 1.5f)
        val outcome = controller.evaluate(templateId, listOf(candidate))
        assertTrue(outcome is ImageTargetOutcome.Matched)
        assertEquals(1.0f, (outcome as ImageTargetOutcome.Matched).result.confidence, 0.0001f)
    }

    // 9. Negative raw score clamped to 0 → NoMatch
    @Test
    fun negativeRawScore_clampedToZero_noMatch() {
        val candidate = MatchCandidate(region, rawScore = -0.5f)
        val outcome = controller.evaluate(templateId, listOf(candidate))
        assertTrue(outcome is ImageTargetOutcome.NoMatch)
        assertEquals(0f, (outcome as ImageTargetOutcome.NoMatch).result.confidence, 0.0001f)
    }

    // 10. Error result has null highlight
    @Test
    fun errorOutcome_hasNullHighlight() {
        val outcome = controller.evaluate("nonexistent", emptyList())
        val result = (outcome as ImageTargetOutcome.Error).result
        assertNull(result.highlight)
        assertFalse(result.matched)
    }
}
