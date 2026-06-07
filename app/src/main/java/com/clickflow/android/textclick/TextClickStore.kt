package com.clickflow.android.textclick

import android.content.Context

private const val PREFS_NAME = "clickflow_text_click"
private const val KEY_QUERY = "query"
private const val KEY_QUERIES = "queries"
private const val KEY_CONTAINS = "contains"
private const val KEY_IGNORE_CASE = "ignore_case"
private const val KEY_CONTINUOUS = "continuous"
private const val KEY_INTERVAL_MS = "interval_ms"
private const val KEY_REPEAT_COUNT = "repeat_count"
private const val KEY_INFINITE = "infinite"

private const val DEFAULT_QUERY = "OK"
private const val QUERY_SEPARATOR = "\n"
private const val MAX_QUERIES = 10
private const val MIN_INTERVAL_MS = 300L
private const val MAX_INTERVAL_MS = 600000L
private const val DEFAULT_INTERVAL_MS = 1100L
private const val MIN_REPEAT_COUNT = 1
private const val MAX_REPEAT_COUNT = 100000
private const val DEFAULT_REPEAT_COUNT = 50

data class TextClickConfig(
    val queries: List<String> = listOf(DEFAULT_QUERY),
    val contains: Boolean = true,
    val ignoreCase: Boolean = true,
    val continuous: Boolean = false,
    val intervalMs: Long = DEFAULT_INTERVAL_MS,
    val repeatCount: Int = DEFAULT_REPEAT_COUNT,
    val infinite: Boolean = false,
)

object TextClickStore {
    fun load(context: Context): TextClickConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedQueries = prefs.getString(KEY_QUERIES, null)
        val queries = if (storedQueries != null) {
            storedQueries.split(QUERY_SEPARATOR)
        } else {
            // Migrate the old single-query setting into the new list.
            listOf(prefs.getString(KEY_QUERY, DEFAULT_QUERY) ?: DEFAULT_QUERY)
        }
        return TextClickConfig(
            queries = queries,
            contains = prefs.getBoolean(KEY_CONTAINS, true),
            ignoreCase = prefs.getBoolean(KEY_IGNORE_CASE, true),
            continuous = prefs.getBoolean(KEY_CONTINUOUS, false),
            intervalMs = prefs.getLong(KEY_INTERVAL_MS, DEFAULT_INTERVAL_MS),
            repeatCount = prefs.getInt(KEY_REPEAT_COUNT, DEFAULT_REPEAT_COUNT),
            infinite = prefs.getBoolean(KEY_INFINITE, false),
        ).normalized()
    }

    fun save(context: Context, config: TextClickConfig) {
        val safe = config.normalized()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_QUERIES, safe.queries.joinToString(QUERY_SEPARATOR))
            .putString(KEY_QUERY, safe.queries.first()) // keep the legacy key in sync
            .putBoolean(KEY_CONTAINS, safe.contains)
            .putBoolean(KEY_IGNORE_CASE, safe.ignoreCase)
            .putBoolean(KEY_CONTINUOUS, safe.continuous)
            .putLong(KEY_INTERVAL_MS, safe.intervalMs)
            .putInt(KEY_REPEAT_COUNT, safe.repeatCount)
            .putBoolean(KEY_INFINITE, safe.infinite)
            .apply()
    }
}

fun TextClickConfig.normalized(): TextClickConfig {
    val cleaned = queries
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(MAX_QUERIES)
        .map { it.take(200) }
    return copy(
        queries = cleaned.ifEmpty { listOf(DEFAULT_QUERY) },
        intervalMs = intervalMs.coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS),
        repeatCount = repeatCount.coerceIn(MIN_REPEAT_COUNT, MAX_REPEAT_COUNT),
    )
}
