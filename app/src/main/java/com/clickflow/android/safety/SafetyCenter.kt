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
* Step 62: adds read-only reporting for the single real-tap prototype (per-session
* Safety Review pass, active session, fresh per-tap consent). Bulk real taps remain
* blocked — `canRunRealTap()` still returns false. The prototype items reflect
* in-memory state only; nothing is persisted, exported, or backed up.
*/
class SafetyCenter(
private val gate: SafetyGate = SafetyGate(),
private val permissions: PermissionStatus = PermissionStatus.EMPTY,
) {

/**
 * Renders the safety items. Step 62 prototype-state parameters default to `false`
 * so existing callers stay compatible; the UI passes the live values from the
 * ViewModel when known.
 */
fun items(
    realTapSafetyReviewPassed: Boolean = false,
    realTapSessionActive: Boolean = false,
    realTapConsentFresh: Boolean = false,
): List<SafetyItem> = listOf(
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
    SafetyItem("Real taps (bulk / looped / scheduled)", "disabled (canRunRealTap=false)", blocked = true),
    SafetyItem(
        "Single real-tap prototype",
        "gated — Safety Review + active session + fresh consent (10s TTL) + API ≥ 24",
        blocked = true,
    ),
    SafetyItem(
        "Real-tap Safety Review",
        if (realTapSafetyReviewPassed) "passed (this session, in-memory only)" else "required (per session)",
        blocked = !realTapSafetyReviewPassed,
    ),
    SafetyItem(
        "Real-tap session",
        if (realTapSessionActive) "active (in-memory only)" else "inactive",
        blocked = !realTapSessionActive,
    ),
    SafetyItem(
        "Real-tap consent",
        if (realTapConsentFresh) "fresh (single-use nonce, ≤ 10s TTL)" else "not requested",
        blocked = !realTapConsentFresh,
    ),
    SafetyItem(
        "Accessibility Service",
        when {
            permissions.accessibilityEnabled -> "enabled (Step 62: single-tap dispatch only; no event subscriptions, no window content reads)"
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
    SafetyItem(
        "Bulk / looped / scenario real taps",
        "blocked by SafetyGate (prototype does not unlock these)",
        blocked = true,
    ),
)

fun blockedReasons(): List<String> = gate.getBlockedReasons()
}
