package com.clickflow.android.permissions

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
* Step 61: Bare skeleton of an AccessibilityService. Declared in the manifest so the user can
* see it in system Accessibility settings, but it does NOTHING. Specifically:
*
*   - onAccessibilityEvent is empty.
*   - onInterrupt is empty.
*   - No GestureDescription is ever dispatched (no real taps).
*   - No event types are subscribed to in res/xml/accessibility_service_config.xml.
*
* This service exists only so the user can practice the enable/disable flow and so the app
* can read its enabled state via PermissionsManager. Real-tap dispatch remains hard-blocked
* by SafetyGate.canRunRealTap()=false.
*
* SAFETY: do NOT add dispatchGesture(), performGlobalAction(), or any input-emitting call to
* this class without first removing the SafetyGate block and completing a safety review.
*/
class ClickFlowAccessibilityService : AccessibilityService() {

   override fun onAccessibilityEvent(event: AccessibilityEvent?) {
       // Intentionally empty. No event processing in Step 61.
   }

   override fun onInterrupt() {
       // Intentionally empty.
   }

   override fun onServiceConnected() {
       super.onServiceConnected()
       // Intentionally minimal. No state mutation.
   }
}
