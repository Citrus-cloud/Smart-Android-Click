package com.clickflow.android.profiles

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists profiles as JSON in INTERNAL app storage (`filesDir/profiles.json`).
 *
 * No permissions, no external storage. Never crashes on corrupted JSON: falls back to the default
 * profile and flags [corruptedStorageRecovered]. Always keeps exactly one active profile, and the
 * default profile (stable id) always exists.
 */
class ProfileRepository(private val storageFile: File) {

    @Volatile var corruptedStorageRecovered: Boolean = false; private set
    @Volatile var storageReady: Boolean = false; private set

    private var seq: Long = 0
    private fun nowMillis(): Long = System.currentTimeMillis()
    fun nextId(): String = "profile_${nowMillis()}_${++seq}"

    companion object {
        const val FILE_NAME = "profiles.json"

        fun defaultProfile(now: Long): Profile = Profile(
            id = ProfileDefaults.DEFAULT_PROFILE_ID,
            name = ProfileDefaults.DEFAULT_NAME,
            description = ProfileDefaults.DEFAULT_DESCRIPTION,
            createdAt = now,
            updatedAt = now,
            isActive = true,
        )
    }

    fun loadProfiles(): List<Profile> {
        corruptedStorageRecovered = false
        val result = runCatching {
            if (!storageFile.exists()) return@runCatching seedDefault()
            val text = storageFile.readText()
            if (text.isBlank()) return@runCatching seedDefault()
            val parsed = parse(text)
            if (parsed.isEmpty()) seedDefault() else ensureInvariants(parsed)
        }.getOrElse {
            corruptedStorageRecovered = true
            seedDefault()
        }
        storageReady = true
        return result
    }

    fun saveProfiles(profiles: List<Profile>) {
        runCatching {
            val arr = JSONArray()
            profiles.forEach { arr.put(toJson(it)) }
            val root = JSONObject().put("version", 1).put("profiles", arr)
            val tmp = File(storageFile.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(storageFile)) { storageFile.writeText(root.toString()); tmp.delete() }
            storageReady = true
        }
    }

    private fun seedDefault(): List<Profile> {
        val list = listOf(defaultProfile(nowMillis()))
        saveProfiles(list)
        return list
    }

    fun createProfile(input: ProfileInput, current: List<Profile>): List<Profile> {
        val now = nowMillis()
        val p = Profile(
            id = nextId(),
            name = input.name.trim(),
            description = input.description.trim(),
            createdAt = now, updatedAt = now,
            isActive = current.isEmpty(),
        )
        val updated = ensureInvariants(current + p)
        saveProfiles(updated)
        return updated
    }

    fun updateProfile(id: String, input: ProfileInput, current: List<Profile>): List<Profile> {
        val now = nowMillis()
        val updated = current.map {
            if (it.id == id) it.copy(name = input.name.trim(), description = input.description.trim(), updatedAt = now) else it
        }
        saveProfiles(updated)
        return updated
    }

    /** Removes a profile. Caller must enforce delete rules before calling. */
    fun deleteProfile(id: String, current: List<Profile>): List<Profile> {
        val remaining = current.filterNot { it.id == id }
        if (remaining.isEmpty()) return seedDefault()
        val fixed = ensureInvariants(remaining)
        saveProfiles(fixed)
        return fixed
    }

    fun setActiveProfile(id: String, current: List<Profile>): List<Profile> {
        if (current.none { it.id == id }) return current
        val updated = current.map { it.copy(isActive = it.id == id) }
        saveProfiles(updated)
        return updated
    }

    fun getActiveProfile(current: List<Profile>): Profile? =
        current.firstOrNull { it.isActive } ?: current.firstOrNull()

    fun resetProfiles(): List<Profile> = seedDefault()

    /** Replaces all profiles (used by backup import). Enforces invariants and persists. */
    fun replaceAll(profiles: List<Profile>): List<Profile> {
        val fixed = ensureInvariants(if (profiles.isEmpty()) listOf(defaultProfile(nowMillis())) else profiles)
        saveProfiles(fixed)
        return fixed
    }

    // ---- JSON --------------------------------------------------------------

    private fun parse(text: String): List<Profile> {
        val root = JSONObject(text)
        val arr = root.getJSONArray("profiles")
        val out = ArrayList<Profile>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Profile(
                    id = o.optString("id").ifBlank { nextId() },
                    name = o.optString("name").ifBlank { "Profile" }.take(ProfileDefaults.MAX_NAME_LEN),
                    description = o.optString("description").take(ProfileDefaults.MAX_DESCRIPTION_LEN),
                    createdAt = o.optLong("createdAt", nowMillis()),
                    updatedAt = o.optLong("updatedAt", nowMillis()),
                    isActive = o.optBoolean("isActive", false),
                ),
            )
        }
        return out
    }

    private fun toJson(p: Profile): JSONObject = JSONObject()
        .put("id", p.id).put("name", p.name).put("description", p.description)
        .put("createdAt", p.createdAt).put("updatedAt", p.updatedAt).put("isActive", p.isActive)

    /** Guarantees the default profile exists and exactly one profile is active. */
    private fun ensureInvariants(list: List<Profile>): List<Profile> {
        val now = nowMillis()
        val withDefault = if (list.none { it.id == ProfileDefaults.DEFAULT_PROFILE_ID })
            listOf(defaultProfile(now).copy(isActive = false)) + list else list
        val activeIndex = withDefault.indexOfFirst { it.isActive }.let { if (it < 0) 0 else it }
        return withDefault.mapIndexed { i, p -> p.copy(isActive = i == activeIndex) }
    }
}
