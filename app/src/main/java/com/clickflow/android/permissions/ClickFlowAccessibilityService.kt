package com.clickflow.android.permissions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

/**
* Step 61: declared so the user can practice enabling it; service did nothing.
*
* Step 62: adds a *single* entry point — [performSingleTap] — that dispatches one
* short GestureDescription via the system. The service stays otherwise inert:
*
*   - onAccessibilityEvent is still empty (no event subscriptions).
*   - onInterrupt is still empty.
*   - No window content is read.
*   - No batched / looped / scheduled gesture is ever built.
*   - dispatchGesture is callable ONLY through [performSingleTap], which requires
*     SafetyGate.canRunRealTapSingleProto() to have returned true upstream in
*     RealTapController. There is no convenience overload, no public coordinate
*     queue, and no recorded macro.
*
* Singleton access: the service registers itself in onServiceConnected/onDestroy
* so RealTapController can locate the live instance without ad-hoc IPC.
*
* SAFETY: extending this class with any new input-emitting method requires a
* fresh safety review entry in docs/REAL_TAP_SAFETY_REVIEW.md.
*/
class ClickFlowAccessibilityService : AccessibilityService() {

   override fun onAccessibilityEvent(event: AccessibilityEvent?) {
       // Intentionally empty.
   }

   override fun onInterrupt() {
       // Intentionally empty.
   }

   override fun onServiceConnected() {
       super.onServiceConnected()
       liveInstance = this
   }

   override fun onDestroy() {
       if (liveInstance === this) liveInstance = null
       super.onDestroy()
   }

   /**
    * Dispatch exactly one short tap at ([x],[y]). Returns true if the system
    * accepted the gesture for dispatch, false otherwise.
    *
    * This method MUST NOT be called directly from UI code. It is the terminal
    * step inside RealTapController, which performs all safety checks first.
    */
   @RequiresApi(Build.VERSION_CODES.N)
   fun performSingleTap(x: Int, y: Int, durationMs: Long): Boolean {
       require(x >= 0 && y >= 0) { "coords must be non-negative" }
       require(durationMs in 50L..250L) { "durationMs out of range" }

       val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
       val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
       val gesture = GestureDescription.Builder().addStroke(stroke).build()
       return try {
           dispatchGesture(gesture, null, null)
       } catch (_: Throwable) {
           false
       }
   }

   companion object {
       /**
        * The currently-connected service instance, or null if the service is not
        * enabled/connected. Set in onServiceConnected, cleared in onDestroy.
        */
       @Volatile
       var liveInstance: ClickFlowAccessibilityService? = null
           private set

       /** True iff the AccessibilityService is currently bound to the system. */
       fun isConnected(): Boolean = liveInstance != null
   }
}
