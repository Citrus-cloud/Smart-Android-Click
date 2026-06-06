package com.clickflow.android.scenario

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Persistence for the premium "Scenario" mode (Phase 1).
 *
 * Stores a list of user-created [Scenario]s as a JSON string in SharedPreferences
 * (using the built-in org.json — no extra dependency) plus the id of the active
 * scenario. Reads are crash-safe and opportunistically migrate/clean persisted
 * data into the current model constraints.
 */
object ScenarioLibraryStore {

    private const val PREFS = "clickflow_scenarios"
    private const val KEY_LIST = "scenarios_json"
    private const val KEY_ACTIVE = "active_scenario_id"

    fun loadAll(context: Context): List<Scenario> {
        val prefs = prefs(context)
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        val scenarios = runCatching { decodeList(raw) }.getOrDefault(emptyList())
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .map { it.normalized() }
            .filter { it.isValid }

        val migrated = encodeList(scenarios)
        if (migrated != raw) prefs.edit().putString(KEY_LIST, migrated).apply()

        val activeId = prefs.getString(KEY_ACTIVE, null)
        if (!activeId.isNullOrBlank() && scenarios.none { it.id == activeId }) {
            prefs.edit().remove(KEY_ACTIVE).apply()
        }
        return scenarios
    }

    fun saveAll(context: Context, scenarios: List<Scenario>) {
        val safe = scenarios
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .map { it.normalized() }
            .filter { it.isValid }
        prefs(context).edit().putString(KEY_LIST, encodeList(safe)).apply()
    }

    fun get(context: Context, id: String): Scenario? = loadAll(context).firstOrNull { it.id == id }

    fun upsert(context: Context, scenario: Scenario) {
        val safe = scenario.normalized()
        if (!safe.isValid) return
        val list = loadAll(context).toMutableList()
        val idx = list.indexOfFirst { it.id == safe.id }
        if (idx >= 0) list[idx] = safe else list.add(safe)
        saveAll(context, list)
    }

    fun delete(context: Context, id: String) {
        saveAll(context, loadAll(context).filterNot { it.id == id })
        if (getActiveId(context) == id) prefs(context).edit().remove(KEY_ACTIVE).apply()
    }

    /** Create a brand-new empty scenario and persist it. */
    fun create(context: Context, name: String): Scenario {
        val scenario = Scenario(id = newId(), name = name.ifBlank { "Новый сценарий" }.take(60)).normalized()
        upsert(context, scenario)
        return scenario
    }

    /** Deep-copy an existing scenario (fresh ids) and persist the copy. */
    fun duplicate(context: Context, id: String): Scenario? {
        val src = get(context, id) ?: return null
        val copy = src.copy(
            id = newId(),
            name = (src.name + " (копия)").take(60),
            steps = src.steps.map { it.copy(id = newId()) },
        ).normalized()
        upsert(context, copy)
        return copy
    }

    fun getActiveId(context: Context): String? = prefs(context).getString(KEY_ACTIVE, null)

    fun setActive(context: Context, id: String) {
        if (get(context, id) != null) prefs(context).edit().putString(KEY_ACTIVE, id).apply()
        else prefs(context).edit().remove(KEY_ACTIVE).apply()
    }

    fun getActive(context: Context): Scenario? = getActiveId(context)?.let { get(context, it) }

    fun newId(): String = UUID.randomUUID().toString()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- JSON encoding / decoding (org.json) ------------------------------

    private fun encodeList(scenarios: List<Scenario>): String {
        val arr = JSONArray()
        scenarios.forEach { arr.put(encode(it.normalized())) }
        return arr.toString()
    }

    private fun decodeList(raw: String): List<Scenario> {
        val arr = JSONArray(raw)
        val out = ArrayList<Scenario>(arr.length())
        for (i in 0 until arr.length()) {
            decode(arr.getJSONObject(i))?.let { out.add(it) }
        }
        return out
    }

    private fun encode(s: Scenario): JSONObject {
        val steps = JSONArray()
        s.steps.forEach { steps.put(encodeStep(it)) }
        return JSONObject().apply {
            put("id", s.id)
            put("name", s.name)
            put("icon", s.icon)
            put("loopInfinite", s.loopInfinite)
            put("loopCount", s.loopCount)
            put("steps", steps)
        }
    }

    private fun encodeStep(st: ScenarioStep): JSONObject = JSONObject().apply {
        put("id", st.id)
        put("type", st.type.name)
        put("label", st.label)
        put("x", st.x)
        put("y", st.y)
        put("photoTemplateId", st.photoTemplateId)
        put("photoPath", st.photoPath)
        put("text", st.text)
        put("textContains", st.textContains)
        put("textIgnoreCase", st.textIgnoreCase)
        put("repeat", st.repeat)
        put("intervalMs", st.intervalMs)
        put("waitMs", st.waitMs)
        put("notFound", st.notFound.name)
        put("notFoundWaitMs", st.notFoundWaitMs)
        put("notFoundRetries", st.notFoundRetries)
    }

    private fun decode(o: JSONObject): Scenario? {
        val id = o.optString("id")
        if (id.isBlank()) return null
        val stepsArr = o.optJSONArray("steps") ?: JSONArray()
        val steps = ArrayList<ScenarioStep>(stepsArr.length())
        for (i in 0 until stepsArr.length()) {
            decodeStep(stepsArr.getJSONObject(i))?.let { steps.add(it) }
        }
        return Scenario(
            id = id,
            name = o.optString("name", "Сценарий"),
            icon = o.optString("icon", "🎬"),
            steps = steps,
            loopInfinite = o.optBoolean("loopInfinite", false),
            loopCount = o.optInt("loopCount", 1),
        )
    }

    private fun decodeStep(o: JSONObject): ScenarioStep? {
        val id = o.optString("id")
        if (id.isBlank()) return null
        val type = runCatching { StepType.valueOf(o.optString("type", "MARKER")) }.getOrNull() ?: return null
        val notFound = runCatching { NotFoundPolicy.valueOf(o.optString("notFound", "SKIP")) }.getOrDefault(NotFoundPolicy.SKIP)
        return ScenarioStep(
            id = id,
            type = type,
            label = o.optString("label", ""),
            x = o.optInt("x", 0),
            y = o.optInt("y", 0),
            photoTemplateId = o.optString("photoTemplateId", ""),
            photoPath = o.optString("photoPath", ""),
            text = o.optString("text", ""),
            textContains = o.optBoolean("textContains", true),
            textIgnoreCase = o.optBoolean("textIgnoreCase", true),
            repeat = o.optInt("repeat", 1),
            intervalMs = o.optLong("intervalMs", 500L),
            waitMs = o.optLong("waitMs", 500L),
            notFound = notFound,
            notFoundWaitMs = o.optLong("notFoundWaitMs", 2000L),
            notFoundRetries = o.optInt("notFoundRetries", 5),
        )
    }

    private fun Scenario.normalized(): Scenario = copy(
        name = name.trim().ifBlank { "Сценарий" }.take(60),
        icon = icon.trim().ifBlank { "🎬" }.take(8),
        loopCount = loopCount.coerceIn(1, 100000),
        steps = steps.distinctBy { it.id }.take(50).map { it.normalized() }.filter { it.isValid },
    )

    private fun ScenarioStep.normalized(): ScenarioStep = copy(
        label = label.trim().take(60),
        x = x.coerceAtLeast(0),
        y = y.coerceAtLeast(0),
        photoTemplateId = photoTemplateId.trim(),
        photoPath = photoPath.trim(),
        text = text.trim().take(200),
        repeat = repeat.coerceIn(1, 100000),
        intervalMs = intervalMs.coerceIn(50L, 600000L),
        waitMs = waitMs.coerceIn(50L, 600000L),
        notFoundWaitMs = notFoundWaitMs.coerceIn(50L, 600000L),
        notFoundRetries = notFoundRetries.coerceIn(0, 100000),
    )
}
