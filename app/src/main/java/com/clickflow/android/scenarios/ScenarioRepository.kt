package com.clickflow.android.scenarios

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists scenarios as JSON in the app's INTERNAL storage.
 *
 * Storage choice: **Option B — JSON in internal app storage** (`filesDir/scenarios.json`).
 * Chosen over DataStore for simplicity and crash-safety: a single file, hand-rolled JSON via
 * `org.json` (bundled with Android, no serialization plugin), with a hard fallback to a default
 * scenario when the file is missing or corrupted.
 *
 * Guarantees:
 *  - No permissions required, no external storage.
 *  - Corrupted/invalid JSON never crashes the app — it falls back to the default scenario and
 *    sets [corruptedStorageRecovered].
 *  - Exactly one scenario is active at a time.
 *
 * SAFETY: this layer stores simulation parameters only. It cannot and does not enable real taps.
 */
class ScenarioRepository(private val storageFile: File) {

    @Volatile
    var corruptedStorageRecovered: Boolean = false
        private set

    @Volatile
    var storageReady: Boolean = false
        private set

    private var seq: Long = 0

    companion object {
        const val FILE_NAME = "scenarios.json"

        fun default(now: Long): Scenario = Scenario(
            id = "scn_default",
            name = "Demo simulation tap",
            type = ScenarioType.SIMPLE_TAP_SIMULATION,
            settings = ScenarioSettings(x = 500, y = 800, repeatCount = 5, intervalMs = 500L),
            createdAt = now,
            updatedAt = now,
            isActive = true,
        )
    }

    private fun nowMillis(): Long = System.currentTimeMillis()

    private fun nextId(): String = "scn_${nowMillis()}_${++seq}"

    // ---- Load / save -------------------------------------------------------

    /**
     * Loads scenarios from disk. Never throws. On any problem (missing file, bad JSON,
     * empty list) returns a single default scenario and flags recovery when corruption
     * was the cause.
     */
    fun loadScenarios(): List<Scenario> {
        corruptedStorageRecovered = false
        val result = runCatching {
            if (!storageFile.exists()) return@runCatching seedDefault(persist = true)
            val text = storageFile.readText()
            if (text.isBlank()) return@runCatching seedDefault(persist = true)
            val parsed = parse(text)
            if (parsed.isEmpty()) seedDefault(persist = true) else ensureSingleActive(parsed)
        }.getOrElse {
            // Corrupted JSON or read error → safe fallback, do not crash.
            corruptedStorageRecovered = true
            seedDefault(persist = true)
        }
        storageReady = true
        return result
    }

    fun saveScenarios(scenarios: List<Scenario>) {
        runCatching {
            val arr = JSONArray()
            scenarios.forEach { arr.put(toJson(it)) }
            val root = JSONObject().put("version", 1).put("scenarios", arr)
            // Atomic-ish write: write to temp then rename.
            val tmp = File(storageFile.parentFile, "${FILE_NAME}.tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(storageFile)) {
                storageFile.writeText(root.toString())
                tmp.delete()
            }
            storageReady = true
        }
    }

    private fun seedDefault(persist: Boolean): List<Scenario> {
        val list = listOf(default(nowMillis()))
        if (persist) saveScenarios(list)
        return list
    }

    // ---- CRUD --------------------------------------------------------------

    /** Creates and persists a new scenario. Caller is expected to pre-validate input. */
    fun createScenario(input: ScenarioInput, current: List<Scenario>): List<Scenario> {
        val now = nowMillis()
        val scenario = Scenario(
            id = nextId(),
            name = input.name.trim(),
            type = input.type,
            settings = ScenarioSettings(input.x, input.y, input.repeatCount, input.intervalMs),
            createdAt = now,
            updatedAt = now,
            isActive = current.isEmpty(),
        )
        val updated = current + scenario
        saveScenarios(updated)
        return updated
    }

    fun updateScenario(id: String, input: ScenarioInput, current: List<Scenario>): List<Scenario> {
        val now = nowMillis()
        val updated = current.map {
            if (it.id == id) it.copy(
                name = input.name.trim(),
                type = input.type,
                settings = ScenarioSettings(input.x, input.y, input.repeatCount, input.intervalMs),
                updatedAt = now,
            ) else it
        }
        saveScenarios(updated)
        return updated
    }

    fun deleteScenario(id: String, current: List<Scenario>): List<Scenario> {
        val remaining = current.filterNot { it.id == id }
        if (remaining.isEmpty()) return seedDefault(persist = true)
        // If we removed the active one, promote the first remaining scenario.
        val hadActive = current.firstOrNull { it.id == id }?.isActive == true
        val fixed = if (hadActive) remaining.mapIndexed { i, s -> s.copy(isActive = i == 0) }
        else ensureSingleActive(remaining)
        saveScenarios(fixed)
        return fixed
    }

    fun setActiveScenario(id: String, current: List<Scenario>): List<Scenario> {
        if (current.none { it.id == id }) return current
        val updated = current.map { it.copy(isActive = it.id == id) }
        saveScenarios(updated)
        return updated
    }

    fun getActiveScenario(current: List<Scenario>): Scenario? =
        current.firstOrNull { it.isActive } ?: current.firstOrNull()

    /** Wipes storage back to the single default scenario. */
    fun resetScenarios(): List<Scenario> = seedDefault(persist = true)

    // ---- JSON helpers ------------------------------------------------------

    private fun parse(text: String): List<Scenario> {
        val root = JSONObject(text)
        val arr = root.getJSONArray("scenarios")
        val out = ArrayList<Scenario>(arr.length())
        for (i in 0 until arr.length()) {
            out.add(fromJson(arr.getJSONObject(i)))
        }
        return out
    }

    private fun toJson(s: Scenario): JSONObject = JSONObject().apply {
        put("id", s.id)
        put("name", s.name)
        put("type", s.type.name)
        put("x", s.settings.x)
        put("y", s.settings.y)
        put("repeatCount", s.settings.repeatCount)
        put("intervalMs", s.settings.intervalMs)
        put("createdAt", s.createdAt)
        put("updatedAt", s.updatedAt)
        put("isActive", s.isActive)
    }

    private fun fromJson(o: JSONObject): Scenario {
        val type = runCatching { ScenarioType.valueOf(o.optString("type")) }
            .getOrDefault(ScenarioType.SIMPLE_TAP_SIMULATION)
        val name = o.optString("name").ifBlank { "Scenario" }
        return Scenario(
            id = o.optString("id").ifBlank { nextId() },
            name = name,
            type = type,
            settings = ScenarioSettings(
                x = o.optInt("x", 0).coerceAtLeast(0),
                y = o.optInt("y", 0).coerceAtLeast(0),
                repeatCount = o.optInt("repeatCount", 1)
                    .coerceIn(ScenarioValidator.MIN_REPEAT, ScenarioValidator.MAX_REPEAT),
                intervalMs = o.optLong("intervalMs", 500L)
                    .coerceAtLeast(ScenarioValidator.MIN_INTERVAL_MS),
            ),
            createdAt = o.optLong("createdAt", nowMillis()),
            updatedAt = o.optLong("updatedAt", nowMillis()),
            isActive = o.optBoolean("isActive", false),
        )
    }

    /** Ensures exactly one scenario is marked active (the first, if none/multiple). */
    private fun ensureSingleActive(list: List<Scenario>): List<Scenario> {
        if (list.isEmpty()) return list
        val activeIndex = list.indexOfFirst { it.isActive }.let { if (it < 0) 0 else it }
        return list.mapIndexed { i, s -> s.copy(isActive = i == activeIndex) }
    }
}
