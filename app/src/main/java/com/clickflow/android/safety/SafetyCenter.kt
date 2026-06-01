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
 * Step 52: intentionally exposes NO control to enable real taps. It only
 * reports status. There is deliberately no `enableRealTaps()` method.
 */
class SafetyCenter(private val gate: SafetyGate = SafetyGate()) {

    fun items(): List<SafetyItem> = listOf(
        SafetyItem("Simulation-only", "enabled", blocked = false),
        SafetyItem("Real taps", "not implemented", blocked = true),
        SafetyItem("Accessibility Service", "planned", blocked = true),
        SafetyItem("Screen capture / MediaProjection", "planned", blocked = true),
        SafetyItem("Overlay permission", "planned", blocked = true),
        SafetyItem("Emergency Stop", "available", blocked = false),
        SafetyItem("Audit logs", "planned", blocked = true),
        SafetyItem("Prohibited automation", "blocked", blocked = true),
    )

    fun blockedReasons(): List<String> = gate.getBlockedReasons()
}
