package com.clickflow.android.profiles

/**
 * A profile groups scenarios into a workspace. Step 55 introduces profiles as a local-only
 * organizational layer; they carry no permissions and no real-input capability.
 */
data class Profile(
    val id: String,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = false,
)

/** Shared constants for profiles, referenced across packages. */
object ProfileDefaults {
    const val DEFAULT_PROFILE_ID = "profile_default"
    const val DEFAULT_NAME = "Default profile"
    const val DEFAULT_DESCRIPTION = "Simulation-only default workspace"
    const val MAX_NAME_LEN = 80
    const val MAX_DESCRIPTION_LEN = 300
}

/** Raw, unvalidated profile input from the create/edit form. */
data class ProfileInput(
    val name: String,
    val description: String,
)

/** Field-level validation errors for a profile. Null = valid. */
data class ProfileValidationErrors(
    val name: String? = null,
    val description: String? = null,
) {
    val hasErrors: Boolean get() = name != null || description != null
}

/**
 * Profile validation:
 *  - name not blank, max 80 chars
 *  - description max 300 chars
 */
object ProfileValidator {
    fun validate(input: ProfileInput): ProfileValidationErrors = ProfileValidationErrors(
        name = when {
            input.name.isBlank() -> "name_required"
            input.name.trim().length > ProfileDefaults.MAX_NAME_LEN -> "name_too_long"
            else -> null
        },
        description = if (input.description.trim().length > ProfileDefaults.MAX_DESCRIPTION_LEN)
            "description_too_long" else null,
    )

    fun isValid(input: ProfileInput): Boolean = !validate(input).hasErrors
}
