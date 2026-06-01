package com.clickflow.android.backup

import com.clickflow.android.profiles.Profile
import com.clickflow.android.profiles.ProfileInput
import com.clickflow.android.profiles.ProfileValidator
import com.clickflow.android.scenarios.ActionInput
import com.clickflow.android.scenarios.Scenario
import com.clickflow.android.scenarios.ScenarioValidator

/** Validates parsed backup items against the same rules used by the live app. */
object BackupValidator {

    fun isProfileValid(p: Profile): Boolean =
        ProfileValidator.isValid(ProfileInput(name = p.name, description = p.description))

    fun isScenarioValid(s: Scenario): Boolean {
        if (s.name.isBlank()) return false
        if (s.actions.isEmpty()) return false
        if (s.settings.repeatCount !in ScenarioValidator.MIN_REPEAT..ScenarioValidator.MAX_REPEAT) return false
        if (s.settings.intervalMs < ScenarioValidator.MIN_INTERVAL_MS) return false
        return s.actions.all { a ->
            ScenarioValidator.isActionValid(
                ActionInput(
                    type = a.type, x = a.x, y = a.y,
                    durationMs = a.durationMs, message = a.message, label = a.label,
                ),
            )
        }
    }

    /**
     * Validates a successful parse: schema-level checks plus per-item validity.
     * Invalid individual items are not fatal (they will be skipped on import); they surface as
     * warnings. The backup is invalid only if it has no profiles or no valid scenarios at all.
     */
    fun validate(parse: BackupParseResult.Success): BackupValidationResult {
        val errors = ArrayList<String>()
        val warnings = ArrayList<String>(parse.warnings)

        if (parse.profiles.isEmpty()) errors.add("no_profiles")
        val invalidProfiles = parse.profiles.count { !isProfileValid(it) }
        val invalidScenarios = parse.scenarios.count { !isScenarioValid(it) }
        if (invalidProfiles > 0) warnings.add("invalid_profiles:$invalidProfiles")
        if (invalidScenarios > 0) warnings.add("invalid_scenarios:$invalidScenarios")
        if (parse.scenarios.isNotEmpty() && parse.scenarios.all { !isScenarioValid(it) })
            warnings.add("all_scenarios_invalid")

        return BackupValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
        )
    }
}
