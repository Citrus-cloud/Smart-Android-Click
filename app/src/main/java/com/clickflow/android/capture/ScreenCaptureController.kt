package com.clickflow.android.capture

/**
 * Pure, framework-free state machine for the screen-capture lifecycle (Step 66, Part 1).
 *
 * Holds NO Android dependencies, so it is unit-testable on the JVM. The real
 * MediaProjection / ImageReader / VirtualDisplay plumbing arrives in Step 66 Part 2
 * and will drive this controller's transitions.
 *
 * Enforces the Step 66 invariants on every transition:
 *   - captured frames are in-memory only (never persisted to disk);
 *   - capture only — no analysis (OCR / template matching) is performed.
 *
 * Mirrors the proven RealTapController pattern: build the brain as pure logic
 * guarded by tests first, then wire the real I/O afterwards.
 */
class ScreenCaptureController {

    private var current: ScreenCaptureState = ScreenCaptureState.IDLE

    @Synchronized
    fun state(): ScreenCaptureState = current

    /** User asked to start capture; we now await the system MediaProjection consent. */
    @Synchronized
    fun requestPermission(): ScreenCaptureState {
        current = current.copy(
            status = CaptureStatus.AWAITING_PERMISSION,
            error = null,
        )
        return current
    }

    /** Result of the system consent dialog. */
    @Synchronized
    fun onPermissionResult(granted: Boolean): ScreenCaptureState {
        current = if (granted) {
            current.copy(
                permission = CapturePermission.GRANTED,
                status = CaptureStatus.IDLE,
                error = null,
            )
        } else {
            current.copy(
                permission = CapturePermission.DENIED,
                status = CaptureStatus.ERROR,
                error = ERROR_PERMISSION_DENIED,
            )
        }
        return current
    }

    /** Begin a capture session. Requires granted permission. */
    @Synchronized
    fun startCapture(): ScreenCaptureState {
        if (current.permission != CapturePermission.GRANTED) {
            current = current.copy(status = CaptureStatus.ERROR, error = ERROR_NO_PERMISSION)
            return current
        }
        current = current.copy(status = CaptureStatus.CAPTURING, frame = null, error = null)
        return current
    }

    /**
     * A single frame was produced by the capture pipeline. We record only its
     * metadata; pixels stay in memory inside the capture service and are never
     * written to disk, and no analysis is run on them.
     */
    @Synchronized
    fun onFrameCaptured(width: Int, height: Int, capturedAtMs: Long): ScreenCaptureState {
        if (current.status != CaptureStatus.CAPTURING) {
            current = current.copy(status = CaptureStatus.ERROR, error = ERROR_NOT_CAPTURING)
            return current
        }
        if (width <= 0 || height <= 0) {
            current = current.copy(status = CaptureStatus.ERROR, error = ERROR_INVALID_FRAME)
            return current
        }
        current = current.copy(
            status = CaptureStatus.FRAME_READY,
            frame = CaptureFrame(width = width, height = height, capturedAtMs = capturedAtMs),
            inMemoryOnly = true,
            persistedToDisk = false,
            analysisPerformed = false,
            error = null,
        )
        return current
    }

    /** Stop the session and drop the in-memory frame. */
    @Synchronized
    fun stop(): ScreenCaptureState {
        current = current.copy(
            status = CaptureStatus.STOPPED,
            frame = null,
            error = null,
        )
        return current
    }

    /** Record a pipeline error. */
    @Synchronized
    fun onError(message: String): ScreenCaptureState {
        current = current.copy(status = CaptureStatus.ERROR, error = message)
        return current
    }

    /** Reset to the initial idle state, preserving a previously granted permission. */
    @Synchronized
    fun reset(): ScreenCaptureState {
        current = ScreenCaptureState.IDLE.copy(permission = current.permission)
        return current
    }

    companion object {
        const val ERROR_PERMISSION_DENIED = "screen_capture_permission_denied"
        const val ERROR_NO_PERMISSION = "screen_capture_no_permission"
        const val ERROR_NOT_CAPTURING = "screen_capture_not_capturing"
        const val ERROR_INVALID_FRAME = "screen_capture_invalid_frame"
    }
}
