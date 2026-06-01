package com.clickflow.android.audit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

/** Aggregate counts for the audit log, shown in the UI/Diagnostics. */
data class AuditSummary(
    val totalEvents: Int = 0,
    val infoCount: Int = 0,
    val warningCount: Int = 0,
    val errorCount: Int = 0,
    val safetyCount: Int = 0,
    val lastEventType: String? = null,
    val storageReady: Boolean = false,
    val corruptedAuditRecovered: Boolean = false,
)

/**
 * Persistent, bounded audit log (Step 55).
 *
 * Stored as JSON Lines in INTERNAL storage (`filesDir/audit-log.jsonl`) — one event per line, newest
 * first. No permissions, no external storage. Corrupted files never crash the app: unreadable files
 * recover to an empty log (flagging [corruptedAuditRecovered] + a warning event); individual bad
 * lines are skipped.
 *
 * PRIVACY: events carry only non-sensitive, app-generated text. Metadata is sanitized — image/screen
 * blobs are dropped and values are length-capped. No screenshots, no base64.
 */
class AuditLogManager(
    private val storageFile: File? = null,
    private val maxEvents: Int = 1000,
) {
    private val _events = MutableStateFlow<List<AuditEvent>>(emptyList())
    val events: StateFlow<List<AuditEvent>> = _events.asStateFlow()

    @Volatile var storageReady: Boolean = false; private set
    @Volatile var corruptedAuditRecovered: Boolean = false; private set

    private var seq: Long = 0
    private val maxValueLen = 200
    private val bannedMetaKeys = setOf("screenshot", "image", "base64", "bitmap", "pixels")

    // ---- load / persist ----------------------------------------------------

    fun loadAuditEvents() {
        corruptedAuditRecovered = false
        val file = storageFile
        if (file == null) { storageReady = true; return }
        val loaded = runCatching {
            if (!file.exists()) return@runCatching emptyList<AuditEvent>()
            val out = ArrayList<AuditEvent>()
            file.readLines().forEach { line ->
                if (line.isBlank()) return@forEach
                runCatching { out.add(fromJson(JSONObject(line))) } // skip bad lines
            }
            out
        }.getOrElse {
            corruptedAuditRecovered = true
            emptyList()
        }
        // Stored newest-first already; cap.
        _events.value = loaded.take(maxEvents)
        storageReady = true
        if (corruptedAuditRecovered) {
            log(AuditType.STORAGE_RECOVERED, AuditSeverity.WARNING, "Corrupted audit log recovered to empty")
        }
    }

    private fun persist() {
        val file = storageFile ?: return
        runCatching {
            val sb = StringBuilder()
            _events.value.forEach { sb.append(toJson(it).toString()).append('\n') }
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(sb.toString())
            if (!tmp.renameTo(file)) { file.writeText(sb.toString()); tmp.delete() }
            storageReady = true
        }
    }

    // ---- mutate ------------------------------------------------------------

    @Synchronized
    fun appendEvent(event: AuditEvent): AuditEvent {
        val capped = (listOf(event) + _events.value).take(maxEvents)
        _events.value = capped
        persist()
        return event
    }

    @Synchronized
    fun log(
        type: String,
        severity: AuditSeverity,
        message: String,
        scenarioId: String? = null,
        actionId: String? = null,
        metadata: Map<String, String> = emptyMap(),
    ): AuditEvent = appendEvent(
        AuditEvent(
            id = "evt_${System.currentTimeMillis()}_${++seq}",
            timestamp = System.currentTimeMillis(),
            type = type,
            severity = severity,
            scenarioId = scenarioId,
            actionId = actionId,
            message = message.take(500),
            metadata = sanitize(metadata),
        ),
    )

    fun getEvents(): List<AuditEvent> = _events.value

    fun clearEvents() { _events.value = emptyList(); persist() }

    /** Hard recovery hook: wipe in-memory + on-disk log to a clean state. */
    fun recoverCorruptedAuditLog() {
        corruptedAuditRecovered = true
        _events.value = emptyList()
        persist()
    }

    // ---- read --------------------------------------------------------------

    fun count(): Int = _events.value.size
    fun lastType(): String? = _events.value.firstOrNull()?.type

    fun getAuditSummary(): AuditSummary {
        val e = _events.value
        return AuditSummary(
            totalEvents = e.size,
            infoCount = e.count { it.severity == AuditSeverity.INFO },
            warningCount = e.count { it.severity == AuditSeverity.WARNING },
            errorCount = e.count { it.severity == AuditSeverity.ERROR },
            safetyCount = e.count { it.severity == AuditSeverity.SAFETY },
            lastEventType = e.firstOrNull()?.type,
            storageReady = storageReady,
            corruptedAuditRecovered = corruptedAuditRecovered,
        )
    }

    /** Plain-text export (newest first). No sensitive data — safe to share. */
    fun exportAsText(): String {
        val s = getAuditSummary()
        val header = "ClickFlow Android — Audit Log\n" +
            "events=${s.totalEvents} info=${s.infoCount} warning=${s.warningCount} " +
            "error=${s.errorCount} safety=${s.safetyCount}\n" +
            "---"
        val body = _events.value.joinToString("\n") { e ->
            "[${e.timestamp}] ${e.severity} ${e.type} ${e.message}"
        }
        return "$header\n$body"
    }

    // ---- json + sanitization ----------------------------------------------

    private fun sanitize(metadata: Map<String, String>): Map<String, String> =
        metadata.entries
            .filterNot { (k, _) -> bannedMetaKeys.any { k.lowercase().contains(it) } }
            .associate { (k, v) -> k.take(64) to v.take(maxValueLen) }

    private fun toJson(e: AuditEvent): JSONObject = JSONObject().apply {
        put("id", e.id); put("timestamp", e.timestamp); put("type", e.type)
        put("severity", e.severity.name); put("message", e.message)
        e.scenarioId?.let { put("scenarioId", it) }
        e.actionId?.let { put("actionId", it) }
        if (e.metadata.isNotEmpty()) {
            val m = JSONObject()
            e.metadata.forEach { (k, v) -> m.put(k, v) }
            put("metadata", m)
        }
    }

    private fun fromJson(o: JSONObject): AuditEvent {
        val sev = runCatching { AuditSeverity.valueOf(o.optString("severity")) }
            .getOrDefault(AuditSeverity.INFO)
        val meta = HashMap<String, String>()
        o.optJSONObject("metadata")?.let { m ->
            m.keys().forEach { k -> meta[k] = m.optString(k) }
        }
        return AuditEvent(
            id = o.optString("id").ifBlank { "evt_${++seq}" },
            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
            type = o.optString("type").ifBlank { "unknown" },
            severity = sev,
            scenarioId = if (o.has("scenarioId")) o.optString("scenarioId") else null,
            actionId = if (o.has("actionId")) o.optString("actionId") else null,
            message = o.optString("message"),
            metadata = sanitize(meta),
        )
    }
}
