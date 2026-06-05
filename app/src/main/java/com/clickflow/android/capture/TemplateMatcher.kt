package com.clickflow.android.capture

/**
 * Pure-Kotlin decision layer for template matching.
 *
 * It does NOT read pixels and does NOT tap. Given a [CaptureTemplate] and one or
 * more scored [MatchCandidate]s (supplied by the caller / a future real matcher),
 * it clamps and compares the best candidate's confidence to the template's
 * threshold and produces a [MatchResult] whose [MatchResult.highlight] marks WHERE
 * a match would be shown. Nothing is captured, analyzed, persisted, or tapped here.
 *
 * @param nowProvider injected clock so timestamps stay deterministic in tests.
 */
class TemplateMatcher(private val nowProvider: () -> Long = { 0L }) {

	/** Evaluate a single [candidate] against [template]. */
	fun evaluate(template: CaptureTemplate, candidate: MatchCandidate): MatchResult =
		evaluateBest(template, listOf(candidate))

	/**
	 * Evaluate [candidates] and return the result for the highest-scoring one.
	 * With no candidates, returns a no-match at zero confidence.
	 */
	fun evaluateBest(template: CaptureTemplate, candidates: List<MatchCandidate>): MatchResult {
		val now = nowProvider()
		val best = candidates.maxByOrNull { it.rawScore }
			?: return MatchResult.noMatch(template.id, 0f, now)

		val confidence = best.rawScore.coerceIn(0f, 1f)
		val matched = confidence >= template.matchThreshold
		return MatchResult(
			templateId = template.id,
			confidence = confidence,
			matched = matched,
			location = if (matched) best.location.clampedToUnit() else null,
			evaluatedAtMs = now,
		)
	}

	/**
	 * Evaluate several templates against their own candidate lists and return only
	 * the results that matched, sorted by confidence descending.
	 */
	fun matchesOnly(
		evaluations: List<Pair<CaptureTemplate, List<MatchCandidate>>>,
	): List<MatchResult> =
		evaluations
			.map { (template, candidates) -> evaluateBest(template, candidates) }
			.filter { it.matched }
			.sortedByDescending { it.confidence }
}
