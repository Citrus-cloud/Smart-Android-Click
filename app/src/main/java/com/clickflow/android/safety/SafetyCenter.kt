package com.clickflow.android.safety

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
 */
class SafetyCenter(private val gate: SafetyGate = SafetyGate()) {

    fun items(): List<SafetyItem> = listOf(
        SafetyItem("Simulation-only", "enabled", blocked = false),
        SafetyItem("Click marker", "in-app only (not a system overlay)", blocked = false),
        SafetyItem("Start button", "runs simulation only", blocked = false),
        SafetyItem("System overlay", "not implemented", blocked = true),
        SafetyItem("Multi-step scenarios", "simulation only", blocked = false),
        SafetyItem("Profiles", "local only", blocked = false),
        SafetyItem("Audit log", "persistent (internal)", blocked = false),
        SafetyItem("Audit export", "share text only", blocked = false),
        SafetyItem("Backup export/import", "text only (no audit log)", blocked = false),
        SafetyItem("Local storage", "used (internal, no permissions)", blocked = false),
        SafetyItem("Real taps", "disabled (canRunRealTap=false)", blocked = true),
        SafetyItem("Accessibility Service", "planned", blocked = true),
        SafetyItem("Screen capture / MediaProjection", "planned", blocked = true),
        SafetyItem("Overlay permission", "planned", blocked = true),
        SafetyItem("Android permissions", "none requested", blocked = false),
        SafetyItem("Prohibited automation", "blocked", blocked = true),
    )

    fun blockedReasons(): List<String> = gate.getBlockedReasons()
}
