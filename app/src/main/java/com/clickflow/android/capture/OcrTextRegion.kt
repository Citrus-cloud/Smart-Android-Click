package com.clickflow.android.capture

/**
 * Step 71 — One text region recognized by the OCR provider.
 *
 * @param text        The recognized text string (may be empty for low-confidence regions).
 * @param bounds      Normalized [CaptureRegion] on the captured frame.
 * @param confidence  Recognition confidence, clamped to [0, 1].
 */
data class OcrTextRegion(
    val text: String,
    val bounds: CaptureRegion,
    val confidence: Float
) {
    /** True when [bounds] is valid and [confidence] > 0. */
    val isUsable: Boolean
        get() = bounds.isValid && confidence > 0f
}

/**
 * The full result of one OCR pass over a captured frame.
 *
 * @param regions         All recognized text regions, ordered top-to-bottom / left-to-right.
 * @param pageText        Concatenated plain text of all regions (joined by space).
 * @param recognizedAtMs  Wall-clock ms from the injected provider.
 */
data class OcrResult(
    val regions: List<OcrTextRegion>,
    val pageText: String,
    val recognizedAtMs: Long
) {
    companion object {
        /** Convenience factory: builds [pageText] automatically. */
        fun from(regions: List<OcrTextRegion>, recognizedAtMs: Long): OcrResult =
            OcrResult(
                regions = regions,
                pageText = regions.joinToString(" ") { it.text }.trim(),
                recognizedAtMs = recognizedAtMs
            )

        /** Empty result (no regions found). */
        fun empty(recognizedAtMs: Long): OcrResult =
            OcrResult(emptyList(), "", recognizedAtMs)
    }
}
