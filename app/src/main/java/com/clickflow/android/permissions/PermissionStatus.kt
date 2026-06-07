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
    /**
     * ClickFlowAccessibilityService is toggled ON in the system Accessibility settings
     * list. Mirrors the switch the user sees in Android Settings, even if the service has
     * not actually (re)bound in our process yet.
     */
    val accessibilityEnabledInSettings: Boolean = false,
    /**
     * Our AccessibilityService is actually bound and running in this process
     * (onServiceConnected has fired). A service can be toggled on in settings yet not be
     * running — most commonly right after reinstalling the APK on aggressive OEMs
     * (MIUI/EMUI). In that state taps do NOT work until the user toggles the service off
     * and on again to force a rebind.
     */
    val accessibilityRunning: Boolean = false,
    /** True if the system check itself failed (e.g. read error). Treated as "not granted". */
    val readError: Boolean = false,
) {
    /** Enabled in settings AND actually bound — real taps will work. */
    val accessibilityReady: Boolean get() = accessibilityEnabledInSettings && accessibilityRunning

    /** Toggled on in settings but not bound — the user must toggle it off and on again. */
    val accessibilityNeedsRestart: Boolean get() = accessibilityEnabledInSettings && !accessibilityRunning

    /** Backward-compatible alias: toggled on in the system settings list. */
    val accessibilityEnabled: Boolean get() = accessibilityEnabledInSettings

    /** Convenience: nothing is granted yet. */
    val nothingGranted: Boolean get() = !overlayGranted && !accessibilityEnabledInSettings

    companion object {
        val EMPTY = PermissionStatus()
    }
}
