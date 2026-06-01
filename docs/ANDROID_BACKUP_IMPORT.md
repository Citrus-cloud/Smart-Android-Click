# Android Backup Import — ClickFlow Android (Step 56)

## Purpose

Restore profiles + scenarios from a pasted backup JSON, safely: validate first, preview, then merge
without silently destroying existing data.

## Flow

1. Open **Backup** → paste JSON into the text field.
2. **Validate** → `BackupManager.previewBackup(json)` returns a `BackupPreview`
   (valid, profilesCount, scenariosCount, invalidItemsCount, warnings, appVersion, createdAt).
3. Import is **disabled until validation passes**.
4. Choose a strategy and import. Profiles + scenarios reload from storage afterward.

Import never runs automatically on paste. No file picker, no document provider, no permissions.

## Validation

- Schema must be `clickflow-android-backup`; JSON must parse; backup must contain at least one
  profile (else rejected).
- Each item is validated with the same rules as the live app (`BackupValidator`). Invalid items are
  **skipped with a warning**, not imported.

## Import strategies

| Strategy | Behavior |
|---|---|
| `MERGE_RENAME_CONFLICTS` (primary) | Adds imported profiles with fresh ids; name collisions get a `(Imported)` suffix; scenario id collisions get fresh ids; `profileId` remapped to the imported/known profile, else the default profile. |
| `MERGE_KEEP_EXISTING` | Keeps existing data; imported items conflicting by id/name are skipped (with warnings). |
| `REPLACE_ALL_REQUIRE_CONFIRMATION` | Replaces all current profiles+scenarios with the backup. **Requires explicit confirmation** (a toggle in the UI); blocked otherwise. |

## Conflict handling (details)

- **Profile id conflict** → new id generated.
- **Profile name conflict** → name + " (Imported)".
- **Scenario id conflict** → new id generated.
- **Scenario references a missing profileId** → assigned to the default profile.
- **Active profile is not changed** by merge; imported profiles are never active by default.
- **No profiles in backup** → rejected.

## After import

- Profiles and scenarios are persisted (`applyImported`) and reloaded.
- Result card shows imported profiles, imported scenarios, skipped items, warnings.
- Audit events: `backup.import.validationStarted`, `backup.import.validationFailed`,
  `backup.import.completed`, `backup.import.skippedInvalidItem`,
  `backup.import.replaceAllRequested`, `backup.import.replaceAllConfirmed` (counts only — never the
  JSON body).
