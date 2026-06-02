package com.clickflow.android.safety

import com.clickflow.android.permissions.PermissionStatus

/** A single line item rendered in the Safety Center UI. */
data class SafetyItem(
   val label: String,
   val status: String,
   val blocked: Boolean,
)

/**
* Read-only view model of the safety posture for the Safety Center screen.
*
* Step 54: still exposes NO control to enable real taps. It only reports status.
* Step 61: now also reflects the live overlay + accessibility permission status as
* read by PermissionsManager. Real-tap dispatch is unchanged — SafetyGate.canRunRealTap()
* remains false regardless of any permission grant.
*/
class SafetyCenter(
   private val gate: SafetyGate = SafetyGate(),
   private val permissions: PermissionStatus = PermissionStatus.EMPTY,
) {

   fun items(): List<SafetyItem> = listOf(
       SafetyItem("Simulation-only", "enabled", blocked = false),
       SafetyItem("Click marker", "in-app only (not a system overlay)", blocked = false),
       SafetyItem("Start button", "runs simulation only", blocked = false),
       SafetyItem(
           "System overlay permission",
           if (permissions.overlayGranted) "granted (preview only — no real input)" else "not granted",
           blocked = !permissions.overlayGranted,
       ),
       SafetyItem("Multi-step scenarios", "simulation only", blocked = false),
       SafetyItem("Profiles", "local only", blocked = false),
       SafetyItem("Audit log", "persistent (internal)", blocked = false),
       SafetyItem("Audit export", "share text only", blocked = false),
       SafetyItem("Backup export/import", "text only (no audit log)", blocked = false),
       SafetyItem("Local storage", "used (internal, no permissions)", blocked = false),
       SafetyItem("Real taps", "disabled (canRunRealTap=false)", blocked = true),
       SafetyItem(
           "Accessibility Service",
           when {
               permissions.accessibilityEnabled -> "enabled (no events processed — no-op skeleton)"
               else -> "declared but disabled by user"
           },
           blocked = !permissions.accessibilityEnabled,
       ),
       SafetyItem("Screen capture / MediaProjection", "planned", blocked = true),
       SafetyItem(
           "Overlay drawing",
           if (permissions.overlayGranted) "available for preview marker (no taps)" else "not available",
           blocked = !permissions.overlayGranted,
       ),
       SafetyItem("Android permissions", "SYSTEM_ALERT_WINDOW declared (opt-in by user)", blocked = false),
       SafetyItem("Prohibited automation", "blocked", blocked = true),
   )

   fun blockedReasons(): List<String> = gate.getBlockedReasons()
}
