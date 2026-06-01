package com.clickflow.android.backup

import com.clickflow.android.core.AppInfo
import com.clickflow.android.profiles.Profile
import com.clickflow.android.profiles.ProfileDefaults
import com.clickflow.android.scenarios.Scenario
import com.clickflow.android.scenarios.ScenarioAction
import com.clickflow.android.scenarios.ScenarioActionType
import com.clickflow.android.scenarios.ScenarioSettings
import com.clickflow.android.scenarios.ScenarioType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Creates, parses, validates, previews, and imports ClickFlow Android backups.
 *
 * A backup is a JSON-text document holding profiles + scenarios only. It NEVER includes the audit
 * log, screenshots, base64, or private paths. Export/import are text-based (no files, no
 * permissions); this class is pure (no Android/IO deps beyond org.json).
 */
class BackupManager {

    companion object {
        const val SCHEMA = "clickflow-android-backup"
        const val VERSION = 1
    }

    private var seq: Long = 0
    private fun newId(prefix: String): String = "${prefix}_imp_${System.currentTimeMillis()}_${++seq}"

    // ---- create ------------------------------------------------------------

    /** Builds backup JSON from current profiles + scenarios. Excludes the audit log by default. */
    fun createBackup(profiles: List<Profile>, scenarios: List<Scenario>): String {
        val root = JSONObject()
        root.put("schema", SCHEMA)
        root.put("version", VERSION)
        root.put("createdAt", System.currentTimeMillis())
        root.put("appVersion", AppInfo.VERSION_NAME)
        val pArr = JSONArray(); profiles.forEach { pArr.put(profileToJson(it)) }
        val sArr = JSONArray(); scenarios.forEach { sArr.put(scenarioToJson(it)) }
        root.put("profiles", pArr)
        root.put("scenarios", sArr)
        root.put("metadata", JSONObject().put("simulationOnly", true).put("containsAuditLog", false))
        return root.toString(2)
    }

    // ---- parse -------------------------------------------------------------

    fun parseBackup(json: String): BackupParseResult {
        if (json.isBlank()) return BackupParseResult.Failure("empty")
        return runCatching {
            val root = JSONObject(json)
            if (root.optString("schema") != SCHEMA) return BackupParseResult.Failure("bad_schema")
            val warnings = ArrayList<String>()
            var invalid = 0

            val profiles = ArrayList<Profile>()
            root.optJSONArray("profiles")?.let { arr ->
                for (i in 0 until arr.length()) {
                    runCatching { profiles.add(profileFromJson(arr.getJSONObject(i))) }
                        .onFailure { invalid++; warnings.add("unparsable_profile") }
                }
            }
            val scenarios = ArrayList<Scenario>()
            root.optJSONArray("scenarios")?.let { arr ->
                for (i in 0 until arr.length()) {
                    runCatching { scenarios.add(scenarioFromJson(arr.getJSONObject(i))) }
                        .onFailure { invalid++; warnings.add("unparsable_scenario") }
                }
            }
            BackupParseResult.Success(
                profiles = profiles,
                scenarios = scenarios,
                appVersion = root.optString("appVersion").ifBlank { null },
                createdAt = if (root.has("createdAt")) root.optLong("createdAt") else null,
                warnings = warnings,
                invalidItems = invalid,
            )
        }.getOrElse { BackupParseResult.Failure("bad_json") }
    }

    fun validateBackup(parse: BackupParseResult): BackupValidationResult = when (parse) {
        is BackupParseResult.Failure -> BackupValidationResult(false, errors = listOf(parse.error))
        is BackupParseResult.Success -> BackupValidator.validate(parse)
    }

    fun previewBackup(json: String): BackupPreview {
        return when (val parse = parseBackup(json)) {
            is BackupParseResult.Failure -> BackupPreview(valid = false, errors = listOf(parse.error))
            is BackupParseResult.Success -> {
                val v = BackupValidator.validate(parse)
                val invalid = parse.invalidItems +
                    parse.profiles.count { !BackupValidator.isProfileValid(it) } +
                    parse.scenarios.count { !BackupValidator.isScenarioValid(it) }
                BackupPreview(
                    valid = v.valid,
                    profilesCount = parse.profiles.size,
                    scenariosCount = parse.scenarios.size,
                    invalidItemsCount = invalid,
                    warnings = v.warnings,
                    errors = v.errors,
                    appVersion = parse.appVersion,
                    createdAt = parse.createdAt,
                )
            }
        }
    }

    // ---- import / merge ----------------------------------------------------

    fun importBackup(
        json: String,
        strategy: ImportStrategy,
        currentProfiles: List<Profile>,
        currentScenarios: List<Scenario>,
        replaceConfirmed: Boolean = false,
    ): BackupImportResult {
        val parse = parseBackup(json)
        if (parse is BackupParseResult.Failure)
            return BackupImportResult(success = false, error = parse.error)
        parse as BackupParseResult.Success
        val validation = BackupValidator.validate(parse)
        if (!validation.valid)
            return BackupImportResult(success = false, error = "invalid_backup", warnings = validation.warnings)

        val warnings = ArrayList(validation.warnings)
        val validProfiles = parse.profiles.filter { BackupValidator.isProfileValid(it) }
        val validScenarios = parse.scenarios.filter { BackupValidator.isScenarioValid(it) }
        val skipped = (parse.profiles.size - validProfiles.size) + (parse.scenarios.size - validScenarios.size)

        return when (strategy) {
            ImportStrategy.REPLACE_ALL_REQUIRE_CONFIRMATION -> {
                if (!replaceConfirmed)
                    return BackupImportResult(success = false, error = "replace_not_confirmed")
                val profiles = ensureProfileInvariants(validProfiles.map { it.copy() })
                val pids = profiles.map { it.id }.toSet()
                val scenarios = validScenarios.map {
                    if (it.profileId in pids) it else it.copy(profileId = ProfileDefaults.DEFAULT_PROFILE_ID)
                }
                BackupImportResult(true, profiles.size, scenarios.size, skipped, warnings, null, profiles, scenarios)
            }

            ImportStrategy.MERGE_KEEP_EXISTING -> {
                val existingIds = currentProfiles.map { it.id }.toSet()
                val existingSids = currentScenarios.map { it.id }.toSet()
                val idMap = HashMap<String, String>()
                val addProfiles = ArrayList<Profile>()
                for (p in validProfiles) {
                    val sameName = currentProfiles.firstOrNull { it.name.equals(p.name, true) }
                    when {
                        p.id in existingIds -> idMap[p.id] = p.id
                        sameName != null -> { idMap[p.id] = sameName.id; warnings.add("profile_kept_existing:${p.name}") }
                        else -> { val np = p.copy(isActive = false); addProfiles.add(np); idMap[p.id] = np.id }
                    }
                }
                val addScenarios = ArrayList<Scenario>()
                var skip = skipped
                for (s in validScenarios) {
                    if (s.id in existingSids) { skip++; warnings.add("scenario_kept_existing:${s.name}"); continue }
                    addScenarios.add(remapScenario(s, idMap, currentProfiles, addProfiles, renameId = false))
                }
                BackupImportResult(true, addProfiles.size, addScenarios.size, skip, warnings, null,
                    currentProfiles + addProfiles, currentScenarios + addScenarios)
            }

            ImportStrategy.MERGE_RENAME_CONFLICTS -> {
                val existingIds = currentProfiles.map { it.id }.toMutableSet()
                val existingNames = currentProfiles.map { it.name.lowercase() }.toMutableSet()
                val idMap = HashMap<String, String>()
                val addProfiles = ArrayList<Profile>()
                for (p in validProfiles) {
                    val newPid = if (p.id in existingIds || currentProfiles.any { it.id == p.id }) newId("profile") else p.id
                    var name = p.name
                    if (name.lowercase() in existingNames) name = "$name (Imported)"
                    val np = p.copy(id = newPid, name = name, isActive = false)
                    addProfiles.add(np); idMap[p.id] = newPid
                    existingIds.add(newPid); existingNames.add(name.lowercase())
                }
                val existingSids = currentScenarios.map { it.id }.toMutableSet()
                val addScenarios = ArrayList<Scenario>()
                for (s in validScenarios) {
                    val rebased = remapScenario(s, idMap, currentProfiles, addProfiles, renameId = s.id in existingSids)
                    existingSids.add(rebased.id)
                    addScenarios.add(rebased)
                }
                BackupImportResult(true, addProfiles.size, addScenarios.size, skipped, warnings, null,
                    currentProfiles + addProfiles, currentScenarios + addScenarios)
            }
        }
    }

    /** Remaps a scenario's id (if needed) and its profileId to a known profile (else default). */
    private fun remapScenario(
        s: Scenario,
        idMap: Map<String, String>,
        currentProfiles: List<Profile>,
        addedProfiles: List<Profile>,
        renameId: Boolean,
    ): Scenario {
        val mappedPid = idMap[s.profileId] ?: s.profileId
        val known = currentProfiles.any { it.id == mappedPid } || addedProfiles.any { it.id == mappedPid }
        val finalPid = if (known) mappedPid else ProfileDefaults.DEFAULT_PROFILE_ID
        return s.copy(
            id = if (renameId) newId("scn") else s.id,
            profileId = finalPid,
            isActive = false,
        )
    }

    private fun ensureProfileInvariants(list: List<Profile>): List<Profile> {
        val now = System.currentTimeMillis()
        val withDefault = if (list.none { it.id == ProfileDefaults.DEFAULT_PROFILE_ID })
            listOf(
                Profile(
                    id = ProfileDefaults.DEFAULT_PROFILE_ID,
                    name = ProfileDefaults.DEFAULT_NAME,
                    description = ProfileDefaults.DEFAULT_DESCRIPTION,
                    createdAt = now, updatedAt = now, isActive = false,
                ),
            ) + list else list
        val activeIndex = withDefault.indexOfFirst { it.isActive }.let { if (it < 0) 0 else it }
        return withDefault.mapIndexed { i, p -> p.copy(isActive = i == activeIndex) }
    }

    // ---- JSON helpers (mirror repository formats) --------------------------

    private fun profileToJson(p: Profile): JSONObject = JSONObject()
        .put("id", p.id).put("name", p.name).put("description", p.description)
        .put("createdAt", p.createdAt).put("updatedAt", p.updatedAt).put("isActive", p.isActive)

    private fun profileFromJson(o: JSONObject): Profile {
        val now = System.currentTimeMillis()
        return Profile(
            id = o.optString("id").ifBlank { newId("profile") },
            name = o.optString("name").ifBlank { "Profile" }.take(ProfileDefaults.MAX_NAME_LEN),
            description = o.optString("description").take(ProfileDefaults.MAX_DESCRIPTION_LEN),
            createdAt = o.optLong("createdAt", now),
            updatedAt = o.optLong("updatedAt", now),
            isActive = o.optBoolean("isActive", false),
        )
    }

    private fun scenarioToJson(s: Scenario): JSONObject = JSONObject().apply {
        put("id", s.id); put("name", s.name); put("type", s.type.name); put("version", 2)
        put("settings", JSONObject().put("repeatCount", s.settings.repeatCount).put("intervalMs", s.settings.intervalMs))
        val acts = JSONArray()
        s.actions.forEach { a ->
            val ao = JSONObject().put("id", a.id).put("type", a.type.name)
            a.x?.let { ao.put("x", it) }; a.y?.let { ao.put("y", it) }
            a.durationMs?.let { ao.put("durationMs", it) }
            a.message?.let { ao.put("message", it) }; a.label?.let { ao.put("label", it) }
            acts.put(ao)
        }
        put("actions", acts)
        put("createdAt", s.createdAt); put("updatedAt", s.updatedAt)
        put("isActive", s.isActive); put("profileId", s.profileId)
    }

    private fun scenarioFromJson(o: JSONObject): Scenario {
        val now = System.currentTimeMillis()
        val settingsObj = o.optJSONObject("settings")
        val repeat = (settingsObj?.optInt("repeatCount") ?: o.optInt("repeatCount", 1))
        val interval = (settingsObj?.optLong("intervalMs") ?: o.optLong("intervalMs", 500L))
        val acts = ArrayList<ScenarioAction>()
        o.optJSONArray("actions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                val type = runCatching { ScenarioActionType.valueOf(a.optString("type")) }
                    .getOrDefault(ScenarioActionType.NOTE)
                acts.add(
                    ScenarioAction(
                        id = a.optString("id").ifBlank { newId("act") },
                        type = type,
                        x = if (a.has("x") && !a.isNull("x")) a.optInt("x") else null,
                        y = if (a.has("y") && !a.isNull("y")) a.optInt("y") else null,
                        durationMs = if (a.has("durationMs") && !a.isNull("durationMs")) a.optLong("durationMs") else null,
                        message = if (a.has("message") && !a.isNull("message")) a.optString("message") else null,
                        label = if (a.has("label") && !a.isNull("label")) a.optString("label") else null,
                    ),
                )
            }
        }
        return Scenario(
            id = o.optString("id").ifBlank { newId("scn") },
            name = o.optString("name").ifBlank { "Scenario" },
            type = runCatching { ScenarioType.valueOf(o.optString("type")) }.getOrDefault(ScenarioType.MULTI_STEP_SIMULATION),
            settings = ScenarioSettings(repeat, interval),
            actions = acts,
            createdAt = o.optLong("createdAt", now),
            updatedAt = o.optLong("updatedAt", now),
            isActive = o.optBoolean("isActive", false),
            version = 2,
            profileId = if (o.has("profileId") && !o.isNull("profileId")) o.optString("profileId").ifBlank { ProfileDefaults.DEFAULT_PROFILE_ID } else ProfileDefaults.DEFAULT_PROFILE_ID,
        )
    }
}
