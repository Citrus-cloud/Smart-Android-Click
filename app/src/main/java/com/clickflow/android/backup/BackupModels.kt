package com.clickflow.android.backup

import com.clickflow.android.profiles.Profile
import com.clickflow.android.scenarios.Scenario

/** How an imported backup is merged with current data. */
enum class ImportStrategy {
    /** Keep existing items; skip imported items that conflict by id/name. */
    MERGE_KEEP_EXISTING,
    /** Import everything, assigning fresh ids and renaming name conflicts with "(Imported)". */
    MERGE_RENAME_CONFLICTS,
    /** Replace all current data with the backup. Requires explicit confirmation. */
    REPLACE_ALL_REQUIRE_CONFIRMATION,
}

/** Lightweight preview of a backup, shown before importing. */
data class BackupPreview(
    val valid: Boolean,
    val profilesCount: Int = 0,
    val scenariosCount: Int = 0,
    val invalidItemsCount: Int = 0,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val appVersion: String? = null,
    val createdAt: Long? = null,
)

/** Result of parsing backup JSON into typed objects. */
sealed class BackupParseResult {
    data class Success(
        val profiles: List<Profile>,
        val scenarios: List<Scenario>,
        val appVersion: String?,
        val createdAt: Long?,
        val warnings: List<String>,
        val invalidItems: Int,
    ) : BackupParseResult()

    data class Failure(val error: String) : BackupParseResult()
}

/** Result of validating a parsed backup. */
data class BackupValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

/** Outcome of an import, including the merged lists ready to persist. */
data class BackupImportResult(
    val success: Boolean,
    val importedProfiles: Int = 0,
    val importedScenarios: Int = 0,
    val skippedItems: Int = 0,
    val warnings: List<String> = emptyList(),
    val error: String? = null,
    val mergedProfiles: List<Profile> = emptyList(),
    val mergedScenarios: List<Scenario> = emptyList(),
)
