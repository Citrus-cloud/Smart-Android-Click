package com.clickflow.android.capture

/**
 * The result of one image-target evaluation run.
 *
 * Step 70 — pure data, no Android imports, no tap dispatch.
 *
 * @param templateId  Id of the [CaptureTemplate] that was evaluated.
 * @param matched     True when confidence ≥ template.matchThreshold.
 * @param confidence  Clamped to [0, 1].
 * @param highlight   The winning [CaptureRegion] when matched, null otherwise.
 * @param evaluatedAtMs  Wall-clock ms from the injected provider.
 * @param errorReason    Non-null only for [ImageTargetOutcome.Error].
 */
data class ImageTargetResult(
    val templateId: String,
    val matched: Boolean,
    val confidence: Float,
    val highlight: CaptureRegion?,
    val evaluatedAtMs: Long,
    val errorReason: String? = null
)

/** High-level outcome of one image-target lookup. */
sealed class ImageTargetOutcome {
    /** Template found with confidence ≥ threshold. */
    data class Matched(val result: ImageTargetResult) : ImageTargetOutcome()

    /** Template not found (confidence < threshold or no candidates). */
    data class NoMatch(val result: ImageTargetResult) : ImageTargetOutcome()

    /**
     * Evaluation could not run (e.g. template not registered).
     * [reason] codes: `template_not_found`.
     */
    data class Error(val result: ImageTargetResult) : ImageTargetOutcome() {
        val reason: String get() = result.errorReason ?: "unknown"
    }
}
