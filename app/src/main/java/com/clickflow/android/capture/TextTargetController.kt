package com.clickflow.android.capture

/**
 * Step 72 — Text-target scenario controller.
 *
 * Wires an [OcrController] into a single [evaluate] call that:
 *  1. Validates the query (non-empty).
 *  2. Runs OCR via [OcrController.recognize].
 *  3. Finds the best matching region via [OcrController.bestMatch].
 *  4. Returns a typed [TextTargetOutcome] with a highlighted [CaptureRegion].
 *
 * Pure Kotlin, no Android imports, no tap dispatch.
 * Simulation/preview only: highlights where a tap *would* go, never dispatches one.
 *
 * @param ocrController  OCR orchestrator (backed by a real or stub provider).
 * @param nowProvider    Injected clock for result timestamps.
 */
class TextTargetController(
    private val ocrController: OcrController,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    /**
     * Evaluate the best OCR match for [query].
     *
     * @param query          Text substring to search for.
     * @param candidates     Optional hint regions forwarded to the OCR provider.
     * @param caseSensitive  If false (default), case-insensitive search.
     * @return [TextTargetOutcome.Error] for empty query;
     *         [TextTargetOutcome.Matched] / [TextTargetOutcome.NoMatch] otherwise.
     */
    fun evaluate(
        query: String,
        candidates: List<CaptureRegion> = emptyList(),
        caseSensitive: Boolean = false
    ): TextTargetOutcome {
        if (query.isBlank()) {
            return TextTargetOutcome.Error(
                TextTargetResult(
                    query = query,
                    matched = false,
                    highlight = null,
                    matchedText = null,
                    confidence = 0f,
                    evaluatedAtMs = nowProvider(),
                    errorReason = "empty_query"
                )
            )
        }

        val ocrResult = ocrController.recognize(candidates)
        val best = ocrController.bestMatch(query, ocrResult, caseSensitive)

        return if (best != null) {
            TextTargetOutcome.Matched(
                TextTargetResult(
                    query = query,
                    matched = true,
                    highlight = best.bounds,
                    matchedText = best.text,
                    confidence = best.confidence.coerceIn(0f, 1f),
                    evaluatedAtMs = nowProvider()
                )
            )
        } else {
            TextTargetOutcome.NoMatch(
                TextTargetResult(
                    query = query,
                    matched = false,
                    highlight = null,
                    matchedText = null,
                    confidence = 0f,
                    evaluatedAtMs = nowProvider()
                )
            )
        }
    }
}
