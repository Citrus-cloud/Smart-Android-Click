package com.clickflow.android.scenarios

import com.clickflow.android.profiles.ProfileDefaults
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists scenarios as JSON in INTERNAL app storage (`filesDir/scenarios.json`).
 *
 * Step 54 schema = **version 2 (multi-step)**: each scenario has an ordered [Scenario.actions]
 * list. The repository transparently **migrates** version-1 (Step 53) scenarios — which stored a
 * single tap as top-level `x/y` — into a single `SIMULATED_TAP` action, preserving
 * `repeatCount`/`intervalMs`.
 *
 * Guarantees: no permissions, no external storage, never crashes on corrupted JSON (falls back to a
 * default multi-step scenario and flags [corruptedStorageRecovered]).
 *
 * SAFETY: stores simulation parameters only; cannot enable real taps.
 */
class ScenarioRepository(private val storageFile: File) {

    @Volatile var corruptedStorageRecovered: Boolean = false; private set
    @Volatile var storageMigrated: Boolean = false; private set
    @Volatile var storageReady: Boolean = false; private set

    private var seq: Long = 0

    companion object {
        const val FILE_NAME = "scenarios.json"
        const val SCHEMA_VERSION = 2

        fun defaultScenario(now: Long, idSeed: String = "scn_default"): Scenario = Scenario(
            id = idSeed,
            name = "Demo multi-step simulation",
            type = ScenarioType.MULTI_STEP_SIMULATION,
            settings = ScenarioSettings(repeatCount = 1, intervalMs = 500L),
            actions = listOf(
                ScenarioAction(id = "act_d1", type = ScenarioActionType.NOTE, message = "Demo scenario started"),
                ScenarioAction(id = "act_d2", type = ScenarioActionType.SIMULATED_TAP, x = 500, y = 800, label = "Demo tap"),
                ScenarioAction(id = "act_d3", type = ScenarioActionType.WAIT, durationMs = 500L),
                ScenarioAction(id = "act_d4", type = ScenarioActionType.NOTE, message = "Demo scenario completed"),
            ),
            createdAt = now,
            updatedAt = now,
            isActive = true,
            version = SCHEMA_VERSION,
            profileId = ProfileDefaults.DEFAULT_PROFILE_ID,
        )
    }

    private fun nowMillis(): Long = System.currentTimeMillis()
    fun nextId(prefix: String = "scn"): String = "${prefix}_${nowMillis()}_${++seq}"

    // ---- Load / save -------------------------------------------------------

    fun loadScenarios(): List<Scenario> {
        corruptedStorageRecovered = false
        storageMigrated = false
        val result = runCatching {
            if (!storageFile.exists()) return@runCatching seedDefault(persist = true)
            val text = storageFile.readText()
            if (text.isBlank()) return@runCatching seedDefault(persist = true)
            val parsed = parse(text)
            if (parsed.isEmpty()) seedDefault(persist = true) else {
                val fixed = ensureSingleActive(parsed)
                // Persist if we migrated, so the file is upgraded to v2 on disk.
                if (storageMigrated) saveScenarios(fixed)
                fixed
            }
        }.getOrElse {
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
            val root = JSONObject().put("version", SCHEMA_VERSION).put("scenarios", arr)
            val tmp = File(storageFile.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(storageFile)) {
                storageFile.writeText(root.toString())
                tmp.delete()
            }
            storageReady = true
        }
    }

    private fun seedDefault(persist: Boolean): List<Scenario> {
        val list = listOf(defaultScenario(nowMillis()))
        if (persist) saveScenarios(list)
        return list
    }

    // ---- CRUD (scenario level) --------------------------------------------

    /** Creates a new multi-step scenario (bound to [profileId]) seeded with one starter action. */
    fun createScenario(input: ScenarioInput, current: List<Scenario>, profileId: String): List<Scenario> {
        val now = nowMillis()
        // Active within its own profile if it's the first scenario for that profile.
        val firstInProfile = current.none { it.profileId == profileId }
        val scenario = Scenario(
            id = nextId(),
            name = input.name.trim(),
            type = ScenarioType.MULTI_STEP_SIMULATION,
            settings = ScenarioSettings(input.repeatCount, input.intervalMs),
            actions = listOf(
                ScenarioAction(id = nextId("act"), type = ScenarioActionType.NOTE, message = "New scenario"),
            ),
            createdAt = now,
            updatedAt = now,
            isActive = firstInProfile,
            version = SCHEMA_VERSION,
            profileId = profileId,
        )
        val updated = current + scenario
        saveScenarios(updated)
        return updated
    }

    fun updateScenarioMeta(id: String, input: ScenarioInput, current: List<Scenario>): List<Scenario> {
        val now = nowMillis()
        val updated = current.map {
            if (it.id == id) it.copy(
                name = input.name.trim(),
                settings = ScenarioSettings(input.repeatCount, input.intervalMs),
                updatedAt = now,
            ) else it
        }
        saveScenarios(updated)
        return updated
    }

    /** Replaces the action list of a scenario (used by all action-level edits). */
    fun replaceActions(id: String, actions: List<ScenarioAction>, current: List<Scenario>): List<Scenario> {
        val now = nowMillis()
        val updated = current.map { if (it.id == id) it.copy(actions = actions, updatedAt = now) else it }
        saveScenarios(updated)
        return updated
    }

    fun deleteScenario(id: String, current: List<Scenario>): List<Scenario> {
        val remaining = current.filterNot { it.id == id }
        if (remaining.isEmpty()) return seedDefault(persist = true)
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

    fun resetScenarios(): List<Scenario> = seedDefault(persist = true)

    /** Replaces all scenarios (used by backup import). Persists; never leaves an empty store. */
    fun replaceAll(scenarios: List<Scenario>): List<Scenario> {
        if (scenarios.isEmpty()) return seedDefault(persist = true)
        saveScenarios(scenarios)
        return scenarios
    }

    fun newActionId(): String = nextId("act")

    // ---- JSON + migration --------------------------------------------------

    private fun parse(text: String): List<Scenario> {
        val root = JSONObject(text)
        val arr = root.getJSONArray("scenarios")
        val out = ArrayList<Scenario>(arr.length())
        for (i in 0 until arr.length()) out.add(fromJson(arr.getJSONObject(i)))
        return out
    }

    private fun toJson(s: Scenario): JSONObject = JSONObject().apply {
        put("id", s.id)
        put("name", s.name)
        put("type", s.type.name)
        put("version", SCHEMA_VERSION)
        put("settings", JSONObject()
            .put("repeatCount", s.settings.repeatCount)
            .put("intervalMs", s.settings.intervalMs))
        val acts = JSONArray()
        s.actions.forEach { a ->
            val ao = JSONObject().put("id", a.id).put("type", a.type.name)
            a.x?.let { ao.put("x", it) }
            a.y?.let { ao.put("y", it) }
            a.durationMs?.let { ao.put("durationMs", it) }
            a.message?.let { ao.put("message", it) }
            a.label?.let { ao.put("label", it) }
            acts.put(ao)
        }
        put("actions", acts)
        put("createdAt", s.createdAt)
        put("updatedAt", s.updatedAt)
        put("isActive", s.isActive)
        put("profileId", s.profileId)
    }

    private fun fromJson(o: JSONObject): Scenario {
        val name = o.optString("name").ifBlank { "Scenario" }
        // Settings can be nested (v2) or top-level (v1).
        val settingsObj = o.optJSONObject("settings")
        val repeatCount = (settingsObj?.optInt("repeatCount") ?: o.optInt("repeatCount", 1))
            .coerceIn(ScenarioValidator.MIN_REPEAT, ScenarioValidator.MAX_REPEAT)
        val intervalMs = (settingsObj?.optLong("intervalMs") ?: o.optLong("intervalMs", 500L))
            .coerceAtLeast(ScenarioValidator.MIN_INTERVAL_MS)

        val actions: List<ScenarioAction>
        val type: ScenarioType
        if (o.has("actions")) {
            // v2
            actions = parseActions(o.getJSONArray("actions"))
            type = runCatching { ScenarioType.valueOf(o.optString("type")) }
                .getOrDefault(ScenarioType.MULTI_STEP_SIMULATION)
        } else {
            // v1 (Step 53) → migrate single tap into one SIMULATED_TAP action.
            storageMigrated = true
            actions = listOf(
                ScenarioAction(
                    id = newActionId(),
                    type = ScenarioActionType.SIMULATED_TAP,
                    x = o.optInt("x", 0).coerceAtLeast(0),
                    y = o.optInt("y", 0).coerceAtLeast(0),
                    label = "Migrated tap",
                ),
            )
            type = ScenarioType.MULTI_STEP_SIMULATION
        }

        return Scenario(
            id = o.optString("id").ifBlank { nextId() },
            name = name,
            type = type,
            settings = ScenarioSettings(repeatCount, intervalMs),
            actions = if (actions.isEmpty())
                listOf(ScenarioAction(id = newActionId(), type = ScenarioActionType.NOTE, message = "Recovered scenario"))
            else actions,
            createdAt = o.optLong("createdAt", nowMillis()),
            updatedAt = o.optLong("updatedAt", nowMillis()),
            isActive = o.optBoolean("isActive", false),
            version = SCHEMA_VERSION,
            profileId = run {
                val pid = if (o.has("profileId") && !o.isNull("profileId")) o.optString("profileId") else ""
                if (pid.isBlank()) { storageMigrated = true; ProfileDefaults.DEFAULT_PROFILE_ID } else pid
            },
        )
    }

    private fun parseActions(arr: JSONArray): List<ScenarioAction> {
        val out = ArrayList<ScenarioAction>(arr.length())
        for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            val type = runCatching { ScenarioActionType.valueOf(a.optString("type")) }
                .getOrDefault(ScenarioActionType.NOTE)
            out.add(
                ScenarioAction(
                    id = a.optString("id").ifBlank { newActionId() },
                    type = type,
                    x = if (a.has("x") && !a.isNull("x")) a.optInt("x").coerceAtLeast(0) else null,
                    y = if (a.has("y") && !a.isNull("y")) a.optInt("y").coerceAtLeast(0) else null,
                    durationMs = if (a.has("durationMs") && !a.isNull("durationMs"))
                        a.optLong("durationMs").coerceAtLeast(ScenarioValidator.MIN_WAIT_MS) else null,
                    message = if (a.has("message") && !a.isNull("message")) a.optString("message") else null,
                    label = if (a.has("label") && !a.isNull("label")) a.optString("label") else null,
                ),
            )
        }
        return out
    }

    private fun ensureSingleActive(list: List<Scenario>): List<Scenario> {
        if (list.isEmpty()) return list
        val activeIndex = list.indexOfFirst { it.isActive }.let { if (it < 0) 0 else it }
        return list.mapIndexed { i, s -> s.copy(isActive = i == activeIndex) }
    }
}
