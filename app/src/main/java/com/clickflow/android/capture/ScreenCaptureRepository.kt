package com.clickflow.android.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-local bridge between [ScreenCaptureService] (producer) and the capture UI (consumer),
 * added in Step 66 Part 2.
 *
 * Wraps a single [ScreenCaptureController] and republishes its state as a [StateFlow] so Compose
 * can observe it. Holds NO frame pixels and NO Android Context — only the in-memory state model.
 * Nothing here is ever persisted to disk, exported, or analyzed.
 */
object ScreenCaptureRepository {

    private val controller = ScreenCaptureController()
    private val _state = MutableStateFlow(controller.state())
    val state: StateFlow<ScreenCaptureState> = _state.asStateFlow()

    @Synchronized
    fun requestPermission() {
        _state.value = controller.requestPermission()
    }

    @Synchronized
    fun onPermissionResult(granted: Boolean) {
        _state.value = controller.onPermissionResult(granted)
    }

    @Synchronized
    fun startCapture() {
        _state.value = controller.startCapture()
    }

    @Synchronized
    fun onFrameCaptured(width: Int, height: Int, capturedAtMs: Long) {
        _state.value = controller.onFrameCaptured(width, height, capturedAtMs)
    }

    @Synchronized
    fun stop() {
        _state.value = controller.stop()
    }

    @Synchronized
    fun onError(message: String) {
        _state.value = controller.onError(message)
    }

    @Synchronized
    fun reset() {
        _state.value = controller.reset()
    }
}
