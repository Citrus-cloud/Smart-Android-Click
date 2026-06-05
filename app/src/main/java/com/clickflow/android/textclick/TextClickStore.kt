package com.clickflow.android.textclick

import android.content.Context

private const val PREFS_NAME = "clickflow_text_click"
private const val KEY_QUERY = "query"
private const val KEY_CONTAINS = "contains"
private const val KEY_IGNORE_CASE = "ignore_case"
private const val KEY_CONTINUOUS = "continuous"

data class TextClickConfig(
    val query: String = "OK",
    val contains: Boolean = true,
    val ignoreCase: Boolean = true,
    val continuous: Boolean = false,
)

object TextClickStore {
    fun load(context: Context): TextClickConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return TextClickConfig(
            query = prefs.getString(KEY_QUERY, "OK") ?: "OK",
            contains = prefs.getBoolean(KEY_CONTAINS, true),
            ignoreCase = prefs.getBoolean(KEY_IGNORE_CASE, true),
            continuous = prefs.getBoolean(KEY_CONTINUOUS, false),
        )
    }

    fun save(context: Context, config: TextClickConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_QUERY, config.query)
            .putBoolean(KEY_CONTAINS, config.contains)
            .putBoolean(KEY_IGNORE_CASE, config.ignoreCase)
            .putBoolean(KEY_CONTINUOUS, config.continuous)
            .apply()
    }
}
