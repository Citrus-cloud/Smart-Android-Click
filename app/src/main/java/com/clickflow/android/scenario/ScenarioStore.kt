package com.clickflow.android.scenario

import android.content.Context

private const val TAPPER_PREFS = "clickflow_tapper"
private const val KEY_INTERVAL_MS = "interval_ms"
private const val KEY_REPEAT_COUNT = "repeat_count"
private const val KEY_INFINITE = "infinite"
private const val KEY_OVERLAY_MARKERS = "overlay_markers"

data class ScenarioMarker(val id: Int, val x: Int, val y: Int)

data class ScenarioConfig(
    val intervalMs: Long = 500L,
    val repeatCount: Int = 30,
    val infinite: Boolean = false,
    val markers: List<ScenarioMarker> = emptyList(),
)

object ScenarioStore {
    fun load(context: Context): ScenarioConfig {
        val prefs = context.getSharedPreferences(TAPPER_PREFS, Context.MODE_PRIVATE)
        return ScenarioConfig(
            intervalMs = prefs.getLong(KEY_INTERVAL_MS, 500L).coerceAtLeast(100L),
            repeatCount = prefs.getInt(KEY_REPEAT_COUNT, 30).coerceAtLeast(1),
            infinite = prefs.getBoolean(KEY_INFINITE, false),
            markers = decodeMarkers(prefs.getString(KEY_OVERLAY_MARKERS, null)),
        )
    }

    private fun decodeMarkers(raw: String?): List<ScenarioMarker> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(";").mapNotNull { part ->
            val pieces = part.split(",")
            if (pieces.size != 3) return@mapNotNull null
            val id = pieces[0].toIntOrNull() ?: return@mapNotNull null
            val x = pieces[1].toIntOrNull() ?: return@mapNotNull null
            val y = pieces[2].toIntOrNull() ?: return@mapNotNull null
            ScenarioMarker(id, x + 43, y + 43)
        }.take(5)
    }
}
