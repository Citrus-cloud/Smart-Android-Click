package com.clickflow.android.permissions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

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
   }
}
