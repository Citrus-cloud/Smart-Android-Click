package com.clickflow.android.permissions

/**
* Step 61: Read-only snapshot of Android permission states relevant to the (still-disabled)
* real-input pathways. The app NEVER enables real taps based on these flags — they are surfaced
* for diagnostics and the Permissions screen only. SafetyGate.canRunRealTap() remains false
* regardless of any value here.
*/
data class PermissionStatus(
   /** SYSTEM_ALERT_WINDOW granted by the user (Settings.canDrawOverlays). */
   val overlayGranted: Boolean = false,
   /** ClickFlowAccessibilityService is enabled in system Accessibility settings. */
   val accessibilityEnabled: Boolean = false,
   /** True if the system check itself failed (e.g. read error). Treated as "not granted". */
   val readError: Boolean = false,
) {
   /** Convenience: nothing is granted yet. */
   val nothingGranted: Boolean get() = !overlayGranted && !accessibilityEnabled

   companion object {
       val EMPTY = PermissionStatus()
   }
}