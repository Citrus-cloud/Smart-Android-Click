package com.clickflow.android.permissions

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager

/**
 * Step 61: Read-only checker for the two real-input gates Android exposes:
 *   - SYSTEM_ALERT_WINDOW (Settings.canDrawOverlays) — required to draw a floating marker.
 *   - Accessibility Service enablement — required to dispatch real taps.
 *
 * This manager NEVER toggles, requests, or grants anything. It only inspects current state.
 * Real-tap dispatch is still hard-blocked by SafetyGate.canRunRealTap()=false irrespective
 * of any return value here. The grants only unlock the ability for the user to see the
 * floating marker preview later — not real input.
 */
class PermissionsManager(
    private val context: Context,
    /** Fully-qualified name of our AccessibilityService. */
    private val accessibilityServiceComponent: String =
        "com.clickflow.android/com.clickflow.android.permissions.ClickFlowAccessibilityService",
) {

    fun read(): PermissionStatus = try {
        PermissionStatus(
            overlayGranted = readOverlayGranted(),
            accessibilityEnabled = readAccessibilityEnabled(),
            readError = false,
        )
    } catch (t: Throwable) {
        PermissionStatus(readError = true)
    }

    private fun readOverlayGranted(): Boolean = Settings.canDrawOverlays(context)

    /**
     * Reads the system-wide list of enabled Accessibility Services and checks if ours is in it.
     * Uses `enabled_accessibility_services` (Settings.Secure) which is the canonical list.
     */
    private fun readAccessibilityEnabled(): Boolean {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false
        if (!accessibilityManager.isEnabled) return false

        val enabledServices: String = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(accessibilityServiceComponent, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
