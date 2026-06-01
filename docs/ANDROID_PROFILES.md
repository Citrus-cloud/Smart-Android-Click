# Android Profiles — ClickFlow Android (Step 55)

## Purpose

Profiles are local **workspaces** that group scenarios. They let a user separate sets of simulation
scenarios (e.g. "Testing", "Demos"). Profiles are organizational only — they carry no permissions
and no real-input capability.

## Model

```
Profile(id, name, description, createdAt, updatedAt, isActive)
```

A stable **default profile** (`id = profile_default`, "Default profile") always exists, and exactly
one profile is active at any time.

## Storage

- JSON in INTERNAL storage: `filesDir/profiles.json`.
- No permissions, no external storage.
- Corrupted/missing file → fallback to the default profile (`corruptedProfileStorageRecovered`).
- Invariants enforced on every load/save: default profile present, exactly one active.

## Validation

- name: required, max 80 chars.
- description: max 300 chars.

## Binding scenarios to profiles

Each `Scenario` has a `profileId`. The Scenarios screen and Home show only the **active profile's**
scenarios; creating a scenario assigns the active profile. Switching the active profile changes the
visible scenario set and ensures an active scenario exists within that profile.

### Migration

Step 54 scenarios had no `profileId`. On load they are assigned the **default profile**
(`profile_default`) and `storageMigrated` is set (audited as `storage.migrated`).

## Delete rules (Step 55)

A profile **cannot** be deleted if it is:

- the **last** profile, or
- the **active** profile, or
- a profile that still **has scenarios**.

Blocked attempts surface a localized message; nothing is deleted silently. (Moving scenarios between
profiles / reassigning on delete is deferred to a future step.)

## ProfileManager API

`load`, `getProfiles`, `getActiveProfile`, `createProfile`, `updateProfile`, `deleteProfile`
(returns `DeleteResult.Success` / `Blocked(reasonKey)`), `setActiveProfile`, `resetProfiles`.

## Not implemented / future

- Import/export of profiles + their scenarios (planned — see `ANDROID_EXPORT_MODEL.md`).
- Reassigning scenarios to another profile on delete.
- No real taps, no permissions — unchanged.
