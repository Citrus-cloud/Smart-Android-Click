package com.clickflow.android.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM JUnit 4 coverage for [ScreenCaptureController] (Step 66, Part 1).
 * No Android framework / Robolectric — runs under `./gradlew testDebugUnitTest`.
 */
class ScreenCaptureControllerTest {

    @Test
    fun initialStateIsIdleNotRequested() {
        val c = ScreenCaptureController()
        val s = c.state()
        assertEquals(CaptureStatus.IDLE, s.status)
        assertEquals(CapturePermission.NOT_REQUESTED, s.permission)
        assertNull(s.frame)
        assertTrue(s.inMemoryOnly)
        assertFalse(s.persistedToDisk)
        assertFalse(s.analysisPerformed)
    }

    @Test
    fun requestPermissionMovesToAwaiting() {
        val c = ScreenCaptureController()
        val s = c.requestPermission()
        assertEquals(CaptureStatus.AWAITING_PERMISSION, s.status)
    }

    @Test
    fun deniedPermissionSetsError() {
        val c = ScreenCaptureController()
        c.requestPermission()
        val s = c.onPermissionResult(false)
        assertEquals(CapturePermission.DENIED, s.permission)
        assertEquals(CaptureStatus.ERROR, s.status)
        assertEquals(ScreenCaptureController.ERROR_PERMISSION_DENIED, s.error)
    }

    @Test
    fun startCaptureRequiresPermission() {
        val c = ScreenCaptureController()
        val s = c.startCapture()
        assertEquals(CaptureStatus.ERROR, s.status)
        assertEquals(ScreenCaptureController.ERROR_NO_PERMISSION, s.error)
    }

    @Test
    fun grantedThenCaptureProducesInMemoryFrame() {
        val c = ScreenCaptureController()
        c.requestPermission()
        c.onPermissionResult(true)
        val capturing = c.startCapture()
        assertEquals(CaptureStatus.CAPTURING, capturing.status)
        val ready = c.onFrameCaptured(1080, 2400, 1000L)
        assertEquals(CaptureStatus.FRAME_READY, ready.status)
        assertTrue(ready.hasFrame)
        assertEquals(1080, ready.frame?.width)
        assertEquals(2400, ready.frame?.height)
        assertEquals(1000L, ready.frame?.capturedAtMs)
        assertTrue(ready.inMemoryOnly)
        assertFalse(ready.persistedToDisk)
        assertFalse(ready.analysisPerformed)
    }

    @Test
    fun frameCapturedRequiresCapturingState() {
        val c = ScreenCaptureController()
        c.requestPermission()
        c.onPermissionResult(true)
        val s = c.onFrameCaptured(100, 100, 1L)
        assertEquals(CaptureStatus.ERROR, s.status)
        assertEquals(ScreenCaptureController.ERROR_NOT_CAPTURING, s.error)
    }

    @Test
    fun invalidFrameDimensionsRejected() {
        val c = ScreenCaptureController()
        c.requestPermission()
        c.onPermissionResult(true)
        c.startCapture()
        val s = c.onFrameCaptured(0, 100, 1L)
        assertEquals(CaptureStatus.ERROR, s.status)
        assertEquals(ScreenCaptureController.ERROR_INVALID_FRAME, s.error)
    }

    @Test
    fun stopDropsFrameFromMemory() {
        val c = ScreenCaptureController()
        c.requestPermission()
        c.onPermissionResult(true)
        c.startCapture()
        c.onFrameCaptured(1080, 1920, 5L)
        val s = c.stop()
        assertEquals(CaptureStatus.STOPPED, s.status)
        assertNull(s.frame)
        assertFalse(s.hasFrame)
    }

    @Test
    fun resetPreservesGrantedPermission() {
        val c = ScreenCaptureController()
        c.requestPermission()
        c.onPermissionResult(true)
        c.startCapture()
        c.onFrameCaptured(10, 10, 1L)
        val s = c.reset()
        assertEquals(CaptureStatus.IDLE, s.status)
        assertEquals(CapturePermission.GRANTED, s.permission)
        assertNull(s.frame)
    }

    @Test
    fun onErrorSetsErrorState() {
        val c = ScreenCaptureController()
        val s = c.onError("boom")
        assertEquals(CaptureStatus.ERROR, s.status)
        assertEquals("boom", s.error)
    }
}
