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
 * scenario. All reads are crash-safe: malformed data decodes to an empty list.
 */
object ScenarioLibraryStore {

    private const val PREFS = "clickflow_scenarios"
    private const val KEY_LIST = "scenarios_json"
    private const val KEY_ACTIVE = "active_scenario_id"

    fun loadAll(context: Context): List<Scenario> {
        val raw = prefs(context).getString(KEY_LIST, null) ?: return emptyList()
        return runCatching { decodeList(raw) }.getOrDefault(emptyList())
    }

    fun saveAll(context: Context, scenarios: List<Scenario>) {
        prefs(context).edit().putString(KEY_LIST, encodeList(scenarios)).apply()
    }

    fun get(context: Context, id: String): Scenario? = loadAll(context).firstOrNull { it.id == id }

    fun upsert(context: Context, scenario: Scenario) {
        val list = loadAll(context).toMutableList()
        val idx = list.indexOfFirst { it.id == scenario.id }
        if (idx >= 0) list[idx] = scenario else list.add(scenario)
        saveAll(context, list)
    }

    fun delete(context: Context, id: String) {
        saveAll(context, loadAll(context).filterNot { it.id == id })
        if (getActiveId(context) == id) prefs(context).edit().remove(KEY_ACTIVE).apply()
    }

    /** Create a brand-new empty scenario and persist it. */
    fun create(context: Context, name: String): Scenario {
        val scenario = Scenario(id = newId(), name = name.ifBlank { "\u041d\u043e\u0432\u044b\u0439 \u0441\u0446\u0435\u043d\u0430\u0440\u0438\u0439" }.take(60))
        upsert(context, scenario)
        return scenario
    }

    /** Deep-copy an existing scenario (fresh ids) and persist the copy. */
    fun duplicate(context: Context, id: String): Scenario? {
        val src = get(context, id) ?: return null
        val copy = src.copy(
            id = newId(),
            name = (src.name + " (\u043a\u043e\u043f\u0438\u044f)").take(60),
            steps = src.steps.map { it.copy(id = newId()) },
        )
        upsert(context, copy)
        return copy
    }

    fun getActiveId(context: Context): String? = prefs(context).getString(KEY_ACTIVE, null)
    fun setActive(context: Context, id: String) { prefs(context).edit().putString(KEY_ACTIVE, id).apply() }
    fun getActive(context: Context): Scenario? = getActiveId(context)?.let { get(context, it) }

    fun newId(): String = UUID.randomUUID().toString()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- JSON encoding / decoding (org.json) ------------------------------

    private fun encodeList(scenarios: List<Scenario>): String {
        val arr = JSONArray()
        scenarios.forEach { arr.put(encode(it)) }
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
            name = o.optString("name", "\u0421\u0446\u0435\u043d\u0430\u0440\u0438\u0439"),
            icon = o.optString("icon", "\uD83C\uDFAC"),
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
}
