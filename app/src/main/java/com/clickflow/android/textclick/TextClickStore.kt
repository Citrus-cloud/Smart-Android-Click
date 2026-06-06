package com.clickflow.android.textclick

import android.content.Context

private const val PREFS_NAME = "clickflow_text_click"
private const val KEY_QUERY = "query"
private const val KEY_CONTAINS = "contains"
private const val KEY_IGNORE_CASE = "ignore_case"
private const val KEY_CONTINUOUS = "continuous"
private const val KEY_INTERVAL_MS = "interval_ms"
private const val KEY_REPEAT_COUNT = "repeat_count"
private const val KEY_INFINITE = "infinite"

data class TextClickConfig(
    val query: String = "OK",
    val contains: Boolean = true,
    val ignoreCase: Boolean = true,
    val continuous: Boolean = false,
    val intervalMs: Long = 1100L,
    val repeatCount: Int = 50,
    val infinite: Boolean = false,
)

object TextClickStore {
    fun load(context: Context): TextClickConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return TextClickConfig(
            query = prefs.getString(KEY_QUERY, "OK") ?: "OK",
            contains = prefs.getBoolean(KEY_CONTAINS, true),
            ignoreCase = prefs.getBoolean(KEY_IGNORE_CASE, true),
            continuous = prefs.getBoolean(KEY_CONTINUOUS, false),
            intervalMs = prefs.getLong(KEY_INTERVAL_MS, 1100L),
            repeatCount = prefs.getInt(KEY_REPEAT_COUNT, 50),
            infinite = prefs.getBoolean(KEY_INFINITE, false),
        )
    }

    fun save(context: Context, config: TextClickConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_QUERY, config.query)
            .putBoolean(KEY_CONTAINS, config.contains)
            .putBoolean(KEY_IGNORE_CASE, config.ignoreCase)
            .putBoolean(KEY_CONTINUOUS, config.continuous)
            .putLong(KEY_INTERVAL_MS, config.intervalMs.coerceIn(300L, 600000L))
            .putInt(KEY_REPEAT_COUNT, config.repeatCount.coerceIn(1, 100000))
            .putBoolean(KEY_INFINITE, config.infinite)
            .apply()
    }
}
