package com.clickflow.android.capture

/**
 * Step 69 — Pure-Kotlin decision layer for template matching.
 *
 * Accepts raw float scores from a future image-similarity provider
 * and decides whether each score constitutes a match for a given [CaptureTemplate].
 * No bitmaps, no Android APIs, no tap dispatch.
 *
 * @param nowProvider  Injected clock; defaults to [System.currentTimeMillis].
 */
class TemplateMatcher(val nowProvider: () -> Long = { System.currentTimeMillis() }) {

    /** Expose the clock so [ImageTargetController] can stamp error results. */
    fun nowMs(): Long = nowProvider()

    /**
     * Evaluate one [candidate] against [template].
     *
     * - Clamps [MatchCandidate.rawScore] to [0, 1].
     * - Sets [MatchResult.matched] when confidence ≥ [CaptureTemplate.matchThreshold].
     * - Exposes [MatchResult.location] only for matched results; no-match results have null location.
     */
    fun evaluate(template: CaptureTemplate, candidate: MatchCandidate): MatchResult {
        val confidence = candidate.rawScore.coerceIn(0f, 1f)
        val matched = confidence >= template.matchThreshold
        return MatchResult(
            templateId = template.id,
            confidence = confidence,
            matched = matched,
            location = if (matched) candidate.location.clampedToUnit() else null,
            evaluatedAtMs = nowProvider()
        )
    }

    /**
     * Evaluate all [candidates] and return the result for the one with the
     * highest [MatchCandidate.rawScore]. Returns [MatchResult.noMatch] when
     * [candidates] is empty.
     */
    fun evaluateBest(template: CaptureTemplate, candidates: List<MatchCandidate>): MatchResult {
        if (candidates.isEmpty()) return MatchResult.noMatch(template.id, 0f, nowProvider())
        val best = candidates.maxByOrNull { it.rawScore }!!
        return evaluate(template, best)
    }

    /**
     * Filter [evaluations] to only matched results, sorted by confidence
     * descending.
     */
    fun matchesOnly(evaluations: List<MatchResult>): List<MatchResult> =
        evaluations.filter { it.matched }.sortedByDescending { it.confidence }
}
