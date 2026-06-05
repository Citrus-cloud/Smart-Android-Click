package com.clickflow.android.realtap

/**
 * Step 76 — Audit events for smart tap sessions.
 *
 * Every transition in [SmartTargetTapController] and [ControlledTapSessionManager]
 * emits one [SmartSessionAuditEvent]. The log is in-memory, bounded, and
 * never persisted. No Android imports.
 */
data class SmartSessionAuditEvent(
    val type: SmartSessionAuditType,
    val sessionId: String?,
    val detail: String = "",
    val recordedAtMs: Long
)

enum class SmartSessionAuditType {
    // Session lifecycle
    SESSION_STARTED,
    SESSION_ENDED,
    SESSION_EXPIRED,
    SESSION_EMERGENCY_STOPPED,

    // Tap flow
    TAP_CONSENT_RECORDED,
    TAP_CONSENT_CLEARED,
    TAP_DISPATCHED,
    TAP_BLOCKED,

    // Gate / invariant checks
    GATE_CHECKED,
    INVARIANT_VIOLATION
}

/**
 * Bounded in-memory audit log for smart tap session events.
 *
 * @param maxEvents   Maximum events to retain (oldest discarded when exceeded).
 * @param nowProvider Injected clock.
 */
class SmartSessionAuditLog(
    private val maxEvents: Int = 200,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val _events: ArrayDeque<SmartSessionAuditEvent> = ArrayDeque()

    /** Immutable snapshot of all recorded events (oldest first). */
    val events: List<SmartSessionAuditEvent> get() = _events.toList()

    /** Number of events recorded so far. */
    val count: Int get() = _events.size

    /**
     * Record a new event, discarding the oldest if [maxEvents] is reached.
     */
    fun record(
        type: SmartSessionAuditType,
        sessionId: String? = null,
        detail: String = ""
    ): SmartSessionAuditEvent {
        val event = SmartSessionAuditEvent(
            type = type,
            sessionId = sessionId,
            detail = detail,
            recordedAtMs = nowProvider()
        )
        if (_events.size >= maxEvents) _events.removeFirst()
        _events.addLast(event)
        return event
    }

    /** Return all events of [type], oldest first. */
    fun eventsOfType(type: SmartSessionAuditType): List<SmartSessionAuditEvent> =
        _events.filter { it.type == type }

    /** Clear all recorded events. */
    fun clear() { _events.clear() }

    /** Plain-text export (one line per event). */
    fun exportText(): String =
        _events.joinToString("\n") { e ->
            "[${e.recordedAtMs}] ${e.type} sid=${e.sessionId} ${e.detail}".trimEnd()
        }
}

/**
 * Step 76 — Emergency-stop handler for smart tap sessions.
 *
 * Coordinates [ControlledTapSessionManager], [SmartTargetTapController], and
 * [SmartSessionAuditLog] into a single atomic Emergency-Stop call.
 * No Android imports.
 */
class SmartSessionEmergencyStop(
    private val sessionManager: ControlledTapSessionManager,
    private val tapController: SmartTargetTapController,
    private val auditLog: SmartSessionAuditLog
) {
    /**
     * Execute an emergency stop:
     * 1. Clear any pending consent in the tap controller.
     * 2. Terminate (end) the controlled session.
     * 3. Record a [SmartSessionAuditType.SESSION_EMERGENCY_STOPPED] event.
     *
     * Safe to call when no session is active (becomes a no-op audit entry).
     */
    fun execute(detail: String = "user-initiated") {
        val sid = sessionManager.session?.sessionId
        tapController.clearConsent()
        sessionManager.emergencyStop()
        auditLog.record(
            SmartSessionAuditType.SESSION_EMERGENCY_STOPPED,
            sessionId = sid,
            detail = detail
        )
    }
}
