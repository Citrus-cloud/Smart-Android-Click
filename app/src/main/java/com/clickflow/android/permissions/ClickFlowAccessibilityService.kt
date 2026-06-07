package com.clickflow.android.permissions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import kotlinx.coroutines.delay

class ClickFlowAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        liveInstance = this
    }

    override fun onDestroy() {
        if (liveInstance === this) liveInstance = null
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

    @RequiresApi(Build.VERSION_CODES.R)
    fun captureScreenBitmap(onResult: (Bitmap?) -> Unit) {
        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
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
