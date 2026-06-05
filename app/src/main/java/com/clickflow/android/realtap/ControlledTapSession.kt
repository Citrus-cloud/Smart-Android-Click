package com.clickflow.android.realtap

/**
 * Step 74 — Controlled tap session domain model.
 *
 * A controlled session allows a limited number of real taps inside a fixed
 * time window, gated by [com.clickflow.android.safety.SafetyGate].
 * All state is process-local and never persisted.
 *
 * @param sessionId      Unique identifier for this session.
 * @param maxTaps        Maximum number of taps allowed in this session (1–10).
 * @param ttlMs          Session time-to-live in milliseconds (1_000–60_000).
 * @param startedAtMs    Wall-clock ms when the session was opened.
 */
data class ControlledTapSession(
    val sessionId: String,
    val maxTaps: Int,
    val ttlMs: Long,
    val startedAtMs: Long
) {
    /** True while the session has not been terminated and the TTL has not expired. */
    fun isActive(nowMs: Long): Boolean =
        !terminated && (nowMs - startedAtMs) < ttlMs

    /** True when the maximum tap count has been reached. */
    fun isExhausted(): Boolean = tapsDispatched >= maxTaps

    /** Number of taps dispatched so far. */
    var tapsDispatched: Int = 0
        private set

    /** True when the session has been explicitly terminated (Emergency Stop or end-session). */
    var terminated: Boolean = false
        private set

    /** Record one dispatched tap. Returns false if the session is exhausted or inactive. */
    fun recordTap(nowMs: Long): Boolean {
        if (!isActive(nowMs) || isExhausted()) return false
        tapsDispatched++
        return true
    }

    /** Terminate the session immediately (Emergency Stop or explicit end). */
    fun terminate() { terminated = true }

    /** Remaining taps allowed. */
    fun remainingTaps(): Int = (maxTaps - tapsDispatched).coerceAtLeast(0)

    /** Remaining TTL in ms. Returns 0 when expired or terminated. */
    fun remainingTtlMs(nowMs: Long): Long =
        if (terminated) 0L
        else (ttlMs - (nowMs - startedAtMs)).coerceAtLeast(0L)
}

/** Reason a controlled tap was blocked. */
enum class ControlledTapBlockReason {
    SESSION_INACTIVE,
    SESSION_EXPIRED,
    SESSION_TERMINATED,
    TAP_LIMIT_REACHED,
    GATE_CLOSED
}

/** Result of a controlled-tap dispatch attempt. */
sealed class ControlledTapDispatchResult {
    object Allowed : ControlledTapDispatchResult()
    data class Blocked(val reason: ControlledTapBlockReason) : ControlledTapDispatchResult()
}
