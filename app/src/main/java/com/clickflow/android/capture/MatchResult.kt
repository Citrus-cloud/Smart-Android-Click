package com.clickflow.android.capture

/**
 * A candidate location proposed for a template inside a captured frame, paired
 * with a raw similarity score in [0, 1].
 *
 * In Phase 2 the score is supplied by the caller (and, later, by a real on-device
 * matcher). This step does NOT read pixels — [MatchCandidate] is the typed handoff
 * between "something scored a region" and the pure decision layer in [TemplateMatcher].
 *
 * @param location normalized region the candidate occupies on the frame.
 * @param rawScore raw similarity in [0, 1] (clamped by the matcher before use).
 */
data class MatchCandidate(
	val location: CaptureRegion,
	val rawScore: Float,
)

/**
 * Outcome of matching one template against a frame.
 *
 * Preview only: it describes WHERE a match would be and HOW confident we are, but
 * never performs a tap. The [highlight] is what a future UI pass would draw over
 * the captured frame.
 *
 * @param templateId   id of the evaluated template.
 * @param confidence   best candidate confidence in [0, 1].
 * @param matched      true when [confidence] reached the template threshold.
 * @param location     best matched region (normalized) or null when not matched.
 * @param evaluatedAtMs timestamp of the evaluation (injected, for testability).
 */
data class MatchResult(
	val templateId: String,
	val confidence: Float,
	val matched: Boolean,
	val location: CaptureRegion?,
	val evaluatedAtMs: Long,
) {
	/** Region to highlight on the frame, or null when there is nothing to show. */
	val highlight: CaptureRegion?
		get() = if (matched) location else null

	companion object {
		fun noMatch(templateId: String, confidence: Float, evaluatedAtMs: Long): MatchResult =
			MatchResult(
				templateId = templateId,
				confidence = confidence.coerceIn(0f, 1f),
				matched = false,
				location = null,
				evaluatedAtMs = evaluatedAtMs,
			)
	}
}
