package com.clickflow.android.scenario

import android.content.Context

/**
 * Premium gate for the Scenario feature (Phase 5).
 *
 * Scenario mode is positioned as a paid feature. For now the unlock state is a
 * simple persisted flag so the whole experience can be built and tested end to
 * end. [startPurchaseFlow] is the single hook a future billing integration
 * (Google Play Billing, etc.) should call once a purchase has been verified.
 */
object ScenarioPremium {

    private const val PREFS = "clickflow_premium"
    private const val KEY_UNLOCKED = "scenario_premium_unlocked"

    fun isUnlocked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_UNLOCKED, false)

    fun setUnlocked(context: Context, unlocked: Boolean) {
        prefs(context).edit().putBoolean(KEY_UNLOCKED, unlocked).apply()
    }

    /**
     * Future billing hook. A real implementation should launch the store purchase
     * flow and, only after the purchase is verified, call [setUnlocked] with true.
     * Until billing is wired up this simply unlocks locally for testing and reports
     * success through [onResult].
     */
    fun startPurchaseFlow(context: Context, onResult: (Boolean) -> Unit) {
        setUnlocked(context, true)
        onResult(true)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
