package com.clickflow.android.realtap

import com.clickflow.android.safety.SafetyGate

/**
 * Step 74 — Manager for controlled tap sessions.
 *
 * Owns the lifecycle of a single active [ControlledTapSession] and arbitrates
 * each tap attempt against the session constraints and the [SafetyGate].
 *
 * Invariants:
 * - Only one session active at a time.
 * - `SafetyGate.canRunRealTap()` (bulk) is always checked and remains `false`;
 *   this path can never produce a real tap until Step 75 wires the gateway.
 * - `SafetyGate.canRunControlledRealTapSession(sessionId)` gates session start.
 * - All state is process-local; nothing is persisted.
 *
 * @param gate        The safety gate (injected for testability).
 * @param nowProvider Injected clock.
 */
class ControlledTapSessionManager(
    private val gate: SafetyGate,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var _session: ControlledTapSession? = null

    /** The currently active session, or null. */
    val session: ControlledTapSession? get() = _session

    /** True when a session is open and not yet expired/terminated. */
    fun hasActiveSession(): Boolean = _session?.isActive(nowProvider()) == true

    // ---- Session lifecycle ------------------------------------------------

    /**
     * Start a new controlled tap session.
     *
     * @param sessionId  Unique id for the session.
     * @param maxTaps    1–10 taps allowed.
     * @param ttlMs      Session TTL: 1_000–60_000 ms.
     * @return [SessionResult.Ok] / [SessionResult.Error] with stable reason codes:
     *   `already_active`, `invalid_params`, `gate_closed`.
     */
    fun startSession(
        sessionId: String,
        maxTaps: Int = 1,
        ttlMs: Long = 10_000L
    ): SessionResult {
        if (hasActiveSession()) return SessionResult.Error("already_active")
        if (maxTaps !in 1..10) return SessionResult.Error("invalid_params")
        if (ttlMs !in 1_000L..60_000L) return SessionResult.Error("invalid_params")
        if (!gate.canRunControlledRealTapSession(sessionId)) {
            return SessionResult.Error("gate_closed")
        }
        _session = ControlledTapSession(
            sessionId = sessionId,
            maxTaps = maxTaps,
            ttlMs = ttlMs,
            startedAtMs = nowProvider()
        )
        return SessionResult.Ok
    }

    /**
     * End the current session gracefully.
     * No-op when there is no active session.
     */
    fun endSession() {
        _session?.terminate()
        _session = null
    }

    /**
     * Emergency stop — terminates any active session immediately.
     */
    fun emergencyStop() { endSession() }

    // ---- Tap arbitration --------------------------------------------------

    /**
     * Check whether a tap is allowed right now.
     *
     * Does NOT dispatch a real tap (Step 75 wires that).
     * Returns [ControlledTapDispatchResult.Allowed] only when all checks pass.
     * Reason codes map to [ControlledTapBlockReason].
     */
    fun evaluateTap(): ControlledTapDispatchResult {
        val s = _session
            ?: return ControlledTapDispatchResult.Blocked(ControlledTapBlockReason.SESSION_INACTIVE)

        val now = nowProvider()

        if (s.terminated) {
            return ControlledTapDispatchResult.Blocked(ControlledTapBlockReason.SESSION_TERMINATED)
        }
        if (!s.isActive(now)) {
            return ControlledTapDispatchResult.Blocked(ControlledTapBlockReason.SESSION_EXPIRED)
        }
        if (s.isExhausted()) {
            return ControlledTapDispatchResult.Blocked(ControlledTapBlockReason.TAP_LIMIT_REACHED)
        }
        // Bulk gate is always checked last — returns false until Step 75
        if (!gate.canRunRealTap()) {
            return ControlledTapDispatchResult.Blocked(ControlledTapBlockReason.GATE_CLOSED)
        }
        return ControlledTapDispatchResult.Allowed
    }

    // ---- Result type -------------------------------------------------------

    sealed class SessionResult {
        object Ok : SessionResult()
        data class Error(val reason: String) : SessionResult()
    }
}
