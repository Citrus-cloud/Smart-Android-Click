package com.clickflow.android.permissions

/**
* Read-only snapshot of the Android permission states relevant to ClickFlow automation.
* These flags are surfaced for diagnostics and the Permissions screen only. Granting a
* permission never starts automation by itself — the user still has to enable the
* Accessibility service and start taps manually.
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