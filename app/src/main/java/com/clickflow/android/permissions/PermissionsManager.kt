package com.clickflow.android.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager

/**
* Step 61: Read-only checker for the two real-input gates Android exposes:
*   - SYSTEM_ALERT_WINDOW (Settings.canDrawOverlays) — required to draw a floating marker.
*   - Accessibility Service enablement — required to dispatch real taps.
*
* This manager NEVER toggles, requests, or grants anything. It only inspects current state
* and produces Intents that hand the user off to the system Settings screens.
*
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

   /** Reads current permission state. Never throws. */
   fun read(): PermissionStatus = try {
       PermissionStatus(
           overlayGranted = readOverlayGranted(),
           accessibilityEnabled = readAccessibilityEnabled(),
           readError = false,
       )
   } catch (t: Throwable) {
       PermissionStatus(readError = true)
   }

   /** Alias for [read]. Re-checks system state and returns a fresh [PermissionStatus]. */
   fun refresh(): PermissionStatus = read()

   /**
    * Intent that opens the system "Display over other apps" / overlay-permission settings
    * page for THIS package. Falls back to the generic overlay-management list if the
    * package-specific page is unavailable.
    */
   fun overlaySettingsIntent(): Intent = Intent(
       Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
       Uri.parse("package:" + context.packageName),
   )

   /**
    * Intent that opens the system Accessibility-services settings page. The user must
    * tap our service entry there to enable it. We do NOT and CANNOT toggle it here.
    */
   fun accessibilitySettingsIntent(): Intent =
       Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

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
