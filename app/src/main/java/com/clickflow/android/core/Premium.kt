package com.clickflow.android.core

import android.content.Context

/**
 * Central place for the multi-target ("multitap") limits and the premium gate.
 *
 * Billing is not wired up yet, so [isPremiumUnlocked] is just a local flag. The UI shows
 * a lock on targets beyond the free limit but cannot actually sell anything. A hidden dev
 * toggle ([setPremiumUnlocked]) lets us unlock all targets for testing.
 */
object Premium {
    private const val PREFS_NAME = "clickflow_premium"
    private const val KEY_UNLOCKED = "premium_unlocked"

    /** How many simultaneous targets a free user may run for photo/text multitap. */
    const val FREE_TARGET_LIMIT = 2

    /** How many simultaneous targets a premium user may run for photo/text multitap. */
    const val PREMIUM_TARGET_LIMIT = 10

    /** Floating markers are never gated; this is just a generous safety cap. */
    const val MARKER_LIMIT = 50

    fun isPremiumUnlocked(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_UNLOCKED, false)

    fun setPremiumUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_UNLOCKED, unlocked).apply()
    }

    /** Max simultaneous photo/text targets for the current premium state. */
    fun targetLimit(context: Context): Int =
        if (isPremiumUnlocked(context)) PREMIUM_TARGET_LIMIT else FREE_TARGET_LIMIT
}
