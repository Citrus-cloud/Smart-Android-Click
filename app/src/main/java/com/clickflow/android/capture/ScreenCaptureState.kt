package com.clickflow.android.capture

/**
 * Pure, framework-free model of the screen-capture lifecycle (Step 66, Part 1).
 *
 * SAFETY / PRIVACY INVARIANTS (enforced by [ScreenCaptureController]):
 *   - A captured frame lives ONLY in memory. [persistedToDisk] is always false.
 *   - This step performs CAPTURE ONLY. No analysis (OCR / template matching) runs;
 *     [analysisPerformed] is always false.
 *   - No frame pixels are stored here — only metadata (size + timestamp).
 *
 * The real MediaProjection / ImageReader / VirtualDisplay plumbing arrives in
 * Step 66 Part 2 and will drive this controller's transitions.
 */

enum class CapturePermission { NOT_REQUESTED, GRANTED, DENIED }

enum class CaptureStatus { IDLE, AWAITING_PERMISSION, CAPTURING, FRAME_READY, STOPPED, ERROR }

/** Metadata-only description of an in-memory frame. Holds no pixels. */
data class CaptureFrame(
    val width: Int,
    val height: Int,
    val capturedAtMs: Long,
)

data class ScreenCaptureState(
    val permission: CapturePermission = CapturePermission.NOT_REQUESTED,
    val status: CaptureStatus = CaptureStatus.IDLE,
    val frame: CaptureFrame? = null,
    val error: String? = null,
    /** Always true — captured frames are kept in RAM only. */
    val inMemoryOnly: Boolean = true,
    /** Always false — Step 66 never writes a frame to disk. */
    val persistedToDisk: Boolean = false,
    /** Always false — Step 66 captures only; no OCR / template matching. */
    val analysisPerformed: Boolean = false,
) {
    val hasFrame: Boolean get() = frame != null
    val isCapturing: Boolean get() = status == CaptureStatus.CAPTURING

    companion object {
        val IDLE = ScreenCaptureState()
    }
}
