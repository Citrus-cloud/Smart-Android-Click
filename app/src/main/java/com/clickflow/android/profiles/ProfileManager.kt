package com.clickflow.android.profiles

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Result of a profile delete attempt: null id means success; otherwise an error key. */
sealed class DeleteResult {
    object Success : DeleteResult()
    data class Blocked(val reasonKey: String) : DeleteResult()
}

/**
 * Runtime source of truth for profiles. Backed by [ProfileRepository].
 *
 * Delete rules (Step 55): cannot delete the last profile, the active profile, or a profile that
 * still has scenarios.
 */
class ProfileManager(private val repository: ProfileRepository) {

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    val storageReady: Boolean get() = repository.storageReady
    val corruptedStorageRecovered: Boolean get() = repository.corruptedStorageRecovered

    fun load() { _profiles.value = repository.loadProfiles() }

    fun getProfiles(): List<Profile> = _profiles.value
    fun getActiveProfile(): Profile? = repository.getActiveProfile(_profiles.value)
    fun activeProfileId(): String = getActiveProfile()?.id ?: ProfileDefaults.DEFAULT_PROFILE_ID
    fun byId(id: String): Profile? = _profiles.value.firstOrNull { it.id == id }

    fun validate(input: ProfileInput): ProfileValidationErrors = ProfileValidator.validate(input)

    fun createProfile(input: ProfileInput): ProfileValidationErrors {
        val errors = ProfileValidator.validate(input)
        if (errors.hasErrors) return errors
        _profiles.value = repository.createProfile(input, _profiles.value)
        return errors
    }

    fun updateProfile(id: String, input: ProfileInput): ProfileValidationErrors {
        val errors = ProfileValidator.validate(input)
        if (errors.hasErrors) return errors
        _profiles.value = repository.updateProfile(id, input, _profiles.value)
        return errors
    }

    /** Applies delete rules. [scenarioCount] is the number of scenarios bound to the profile. */
    fun deleteProfile(id: String, scenarioCount: Int): DeleteResult {
        val list = _profiles.value
        if (list.size <= 1) return DeleteResult.Blocked("cannot_delete_last")
        if (byId(id)?.isActive == true) return DeleteResult.Blocked("cannot_delete_active")
        if (scenarioCount > 0) return DeleteResult.Blocked("cannot_delete_with_scenarios")
        _profiles.value = repository.deleteProfile(id, list)
        return DeleteResult.Success
    }

    fun setActiveProfile(id: String) { _profiles.value = repository.setActiveProfile(id, _profiles.value) }
    fun resetProfiles() { _profiles.value = repository.resetProfiles() }
}
