package com.clickflow.android.capture

/**
 * Step 71 — Abstraction over the on-device OCR engine.
 *
 * In production this will be backed by ML Kit Text Recognition or Tesseract.
 * In tests and simulation it is backed by [StubOcrProvider].
 *
 * The interface is intentionally narrow: implementations receive pre-processed
 * region hints ([candidates]) so that the real engine can focus its work, and
 * return a complete [OcrResult] for the controller layer.
 *
 * No Android imports at the interface level.
 */
interface OcrProvider {
    /**
     * Recognize text in the current frame.
     *
     * @param candidates  Optional hint regions to focus recognition on.
     *                    Pass an empty list for a full-frame scan.
     * @return [OcrResult] with all recognized text regions.
     */
    fun recognize(candidates: List<CaptureRegion> = emptyList()): OcrResult
}

/**
 * Stub implementation of [OcrProvider] for tests and simulation.
 *
 * Returns a fixed list of [OcrTextRegion]s supplied at construction time.
 * Useful for unit tests and preview mode without ML Kit.
 *
 * @param injectedRegions  Text regions that this provider will return on every call.
 * @param nowProvider      Injected clock for [OcrResult.recognizedAtMs].
 */
class StubOcrProvider(
    private val injectedRegions: List<OcrTextRegion>,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : OcrProvider {

    override fun recognize(candidates: List<CaptureRegion>): OcrResult =
        OcrResult.from(injectedRegions, nowProvider())
}
