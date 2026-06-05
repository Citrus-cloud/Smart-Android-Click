package com.clickflow.android.capture

/**
 * Step 72 — Result of one text-target lookup.
 *
 * @param query          The text query that was searched.
 * @param matched        True when at least one region was found.
 * @param highlight      Bounding [CaptureRegion] of the best match (null when not matched).
 * @param matchedText    The actual text of the best-matched region (null when not matched).
 * @param confidence     Confidence of the best match, clamped to [0, 1].
 * @param evaluatedAtMs  Wall-clock ms from the injected provider.
 * @param errorReason    Non-null only for [TextTargetOutcome.Error]. Stable code: `empty_query`.
 */
data class TextTargetResult(
    val query: String,
    val matched: Boolean,
    val highlight: CaptureRegion?,
    val matchedText: String?,
    val confidence: Float,
    val evaluatedAtMs: Long,
    val errorReason: String? = null
)

/** High-level outcome of one text-target lookup. */
sealed class TextTargetOutcome {
    /** At least one region matched the query. */
    data class Matched(val result: TextTargetResult) : TextTargetOutcome()

    /** OCR ran but no region matched the query. */
    data class NoMatch(val result: TextTargetResult) : TextTargetOutcome()

    /**
     * Lookup could not run.
     * [reason] codes: `empty_query`.
     */
    data class Error(val result: TextTargetResult) : TextTargetOutcome() {
        val reason: String get() = result.errorReason ?: "unknown"
    }
}
