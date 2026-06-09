package com.clickflow.android.permissions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ClickFlowAccessibilityService : AccessibilityService() {

    // Screenshots are copied into a full-screen ARGB_8888 bitmap, which is expensive. Doing that
    // copy on the accessibility service's main thread every scan cycle caused repeated jank/ANR,
    // which Android surfaces as "ClickFlow isn't working correctly" and can disable the service.
    // Running the callback (and the copy) on a dedicated background thread keeps the main thread free.
    private val screenshotExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        liveInstance = this
    }

    override fun onDestroy() {
        if (liveInstance === this) liveInstance = null
        runCatching { screenshotExecutor.shutdownNow() }
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun performSingleTap(x: Int, y: Int, durationMs: Long): Boolean {
        if (x < 0 || y < 0) return false
        val safeDuration = durationMs.coerceIn(50L, 250L)
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, safeDuration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return try { dispatchGesture(gesture, null, null) } catch (_: Throwable) { false }
    }

    /**
     * Like [performSingleTap] but suspends until the gesture actually finishes. Only one gesture
     * can be in flight at a time: if several taps are dispatched back-to-back the framework drops
     * all but one (and on aggressive OEMs sometimes even that one). Awaiting completion lets a
     * caller tap several points within the same scan cycle (multitap) reliably, one after another.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun performSingleTapAwait(x: Int, y: Int, durationMs: Long): Boolean {
        if (x < 0 || y < 0) return false
        val safeDuration = durationMs.coerceIn(50L, 250L)
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, safeDuration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return suspendCancellableCoroutine { cont ->
            val dispatched = try {
                dispatchGesture(
                    gesture,
                    object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            if (cont.isActive) cont.resume(true)
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            if (cont.isActive) cont.resume(false)
                        }
                    },
                    null,
                )
            } catch (_: Throwable) {
                false
            }
            if (!dispatched && cont.isActive) cont.resume(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun captureScreenBitmap(onResult: (Bitmap?) -> Unit) {
        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                screenshotExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        val bitmap = try {
                            val wrapped = Bitmap.wrapHardwareBuffer(
                                screenshotResult.hardwareBuffer,
                                screenshotResult.colorSpace,
                            )
                            wrapped?.copy(Bitmap.Config.ARGB_8888, false)
                        } catch (_: Throwable) {
                            null
                        } finally {
                            runCatching { screenshotResult.hardwareBuffer.close() }
                        }
                        onResult(bitmap)
                    }

                    override fun onFailure(errorCode: Int) {
                        onResult(null)
                    }
                },
            )
        } catch (_: Throwable) {
            onResult(null)
        }
    }

    companion object {
        @Volatile
        var liveInstance: ClickFlowAccessibilityService? = null
            private set

        fun isConnected(): Boolean = liveInstance != null

        /** True if our accessibility service is enabled in system settings, even if the
         *  service instance has not (re)connected yet in this process. */
        fun isEnabledInSettings(context: Context): Boolean {
            val expected = "${context.packageName}/${ClickFlowAccessibilityService::class.java.name}"
            val flat = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            return flat.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        /** Wait for the service to (re)connect. The app process can be killed in the
         *  background; when it restarts the accessibility service reconnects a moment later,
         *  and on aggressive OEMs (MIUI/EMUI) that rebind can take several seconds, so we
         *  poll generously instead of failing early with a false "not enabled" message. */
        suspend fun awaitInstance(timeoutMs: Long = 12000L): ClickFlowAccessibilityService? {
            var waited = 0L
            while (liveInstance == null && waited < timeoutMs) {
                delay(100L)
                waited += 100L
            }
            return liveInstance
        }
    }
}
