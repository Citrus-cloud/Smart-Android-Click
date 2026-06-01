package com.clickflow.android.audit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory audit log for Step 54.
 *
 * Holds simulation/lifecycle events as a bounded list exposed via [StateFlow] for Compose.
 * Persistence to disk is intentionally deferred (see docs/ANDROID_AUDIT_LOG.md). No permissions,
 * no network, no sensitive data.
 */
class AuditLogManager(private val maxEvents: Int = 500) {

    private val _events = MutableStateFlow<List<AuditEvent>>(emptyList())
    /** Newest-first list of events. */
    val events: StateFlow<List<AuditEvent>> = _events.asStateFlow()

    private var seq: Long = 0

    @Synchronized
    fun log(
        type: String,
        severity: AuditSeverity,
        message: String,
        scenarioId: String? = null,
        actionId: String? = null,
        metadata: Map<String, String> = emptyMap(),
    ): AuditEvent {
        val event = AuditEvent(
            id = "evt_${System.currentTimeMillis()}_${++seq}",
            timestamp = System.currentTimeMillis(),
            type = type,
            severity = severity,
            scenarioId = scenarioId,
            actionId = actionId,
            message = message,
            metadata = metadata,
        )
        // Prepend (newest first) and cap size.
        val updated = ArrayList<AuditEvent>(minOf(_events.value.size + 1, maxEvents))
        updated.add(event)
        for (e in _events.value) {
            if (updated.size >= maxEvents) break
            updated.add(e)
        }
        _events.value = updated
        return event
    }

    fun clear() { _events.value = emptyList() }

    fun count(): Int = _events.value.size

    fun lastType(): String? = _events.value.firstOrNull()?.type

    /** Plain-text export (newest first). Safe to copy/share — contains no sensitive data. */
    fun exportText(): String = _events.value.joinToString("\n") { e ->
        "[${e.timestamp}] ${e.severity} ${e.type} ${e.message}"
    }
}
