package com.clickflow.android.audit

/** Severity of an audit event. */
enum class AuditSeverity { INFO, WARNING, ERROR, SAFETY }

/**
 * Canonical audit event type identifiers. Kept as constants (not an enum) so they read like the
 * dotted names used across the project and serialize as-is.
 */
object AuditType {
    const val SCENARIO_STARTED = "scenario.started"
    const val SCENARIO_COMPLETED = "scenario.completed"
    const val SCENARIO_STOPPED = "scenario.stopped"
    const val SCENARIO_EMERGENCY_STOPPED = "scenario.emergencyStopped"
    const val ACTION_SIMULATED_TAP = "action.simulatedTap"
    const val ACTION_WAIT = "action.wait"
    const val ACTION_NOTE = "action.note"
    const val VALIDATION_FAILED = "validation.failed"
    const val STORAGE_RECOVERED = "storage.recovered"
    const val STORAGE_MIGRATED = "storage.migrated"
    const val SAFETY_REAL_TAP_BLOCKED = "safety.realTapBlocked"
    const val BACKUP_EXPORT_REQUESTED = "backup.export.requested"
    const val BACKUP_EXPORT_SHARED = "backup.export.shared"
    const val BACKUP_EXPORT_FAILED = "backup.export.failed"
    const val BACKUP_IMPORT_VALIDATION_STARTED = "backup.import.validationStarted"
    const val BACKUP_IMPORT_VALIDATION_FAILED = "backup.import.validationFailed"
    const val BACKUP_IMPORT_COMPLETED = "backup.import.completed"
    const val BACKUP_IMPORT_SKIPPED_INVALID_ITEM = "backup.import.skippedInvalidItem"
    const val BACKUP_IMPORT_REPLACE_ALL_REQUESTED = "backup.import.replaceAllRequested"
    const val BACKUP_IMPORT_REPLACE_ALL_CONFIRMED = "backup.import.replaceAllConfirmed"
}

/**
 * A single audit-log entry describing something the app did during a simulation run or lifecycle
 * event.
 *
 * PRIVACY: audit events carry only non-sensitive, app-generated text. No screenshots, no base64,
 * no personal data, no captured screen content — by design there is nothing of the sort to capture.
 */
data class AuditEvent(
    val id: String,
    val timestamp: Long,
    val type: String,
    val severity: AuditSeverity,
    val scenarioId: String? = null,
    val actionId: String? = null,
    val message: String,
    val metadata: Map<String, String> = emptyMap(),
)
