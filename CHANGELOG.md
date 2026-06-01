# Changelog — ClickFlow Android

All notable changes to the ClickFlow Android project are documented here.
This project follows the ClickFlow step-based development model.

## [Unreleased]

### Step 57 — Android Pre-alpha Build/QA and Release Prep

- **validated the APK build**: `./gradlew assembleDebug` succeeds with JDK 17 + Android SDK 34,
  producing a debug-signed `app-debug.apk` (no release signing);
- minor cleanups (no new runtime features): `versionName` → `0.1.0-prealpha` (Android branch, distinct
  from desktop), `AppInfo.VERSION_NAME` updated, `Divider` → `HorizontalDivider`, removed an unused
  variable in `BackupManager`;
- added pre-alpha release docs: `ANDROID_PRE_ALPHA_RELEASE_NOTES.md`,
  `ANDROID_PRE_ALPHA_RELEASE_DRAFT.md` (GitHub release body), `ANDROID_PRE_ALPHA_TAG_PLAN.md`,
  `ANDROID_BUILD_TROUBLESHOOTING.md` (with a verified headless toolchain recipe);
- updated APK build guide / QA / checklist / smoke / README / PROJECT_CONTEXT;
- re-confirmed safety: 0 manifest permissions, 0 providers, no real taps, simulation-only;
- note: AndroidX injects a self-scoped `*.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` at build time
  (targetSdk 34) — not a declared/dangerous permission; the source manifest declares none.

### Step 56 — Android Backup Import/Export and Pre-alpha QA Prep

- added `backup/` package: `BackupModels` (`ImportStrategy`, `BackupPreview`, parse/validation/import
  results), `BackupManager` (create/parse/validate/preview/import + merge), `BackupValidator`;
- **backup JSON** = `{schema, version, createdAt, appVersion, profiles[], scenarios[], metadata}`;
  excludes the audit log; no screenshots/base64/private paths;
- **export**: share backup JSON as text via `ACTION_SEND` (no permissions, no FileProvider);
- **import**: paste JSON → validate + preview → merge; strategies `MERGE_RENAME_CONFLICTS`,
  `MERGE_KEEP_EXISTING`, `REPLACE_ALL_REQUIRE_CONFIRMATION` (gated by explicit confirmation);
- conflict handling: new ids for id clashes, `(Imported)` suffix for name clashes, unknown
  `profileId` reassigned to default; invalid items skipped with warnings; existing data never
  overwritten silently;
- `ProfileManager`/`ScenarioManager` gained `applyImported`; repositories gained `replaceAll`;
- `ClickFlowViewModel`: backup state (json text, preview, import result, replace-all confirm),
  `createBackupJson` / `shareBackupJson` / `validateBackupJson` / `importBackup` / `clearBackupImportState`;
  profiles + scenarios reload after import;
- backup audit events (`backup.export.*`, `backup.import.*`) — counts/warnings only, never the JSON;
- UI: **Backup** screen (status, export, paste+validate+preview, strategy import, result, clear);
  Diagnostics shows backup status; Safety Center reports backup text-only / no audit log / no
  permissions;
- docs `ANDROID_BACKUP_EXPORT.md`, `ANDROID_BACKUP_IMPORT.md`, `ANDROID_PRE_ALPHA_QA.md`,
  `ANDROID_PRE_ALPHA_RELEASE_CHECKLIST.md` + updates;
- RU/EN strings; no real taps, no permissions, no external storage.

### Step 55 — Android Audit Persistence, Export, and Profiles Foundation

- **Persistent audit log**: `AuditLogManager` now stores events as JSON Lines in
  `filesDir/audit-log.jsonl` (bounded 1000, newest-first, temp+rename writes); added
  `loadAuditEvents` / `appendEvent` / `getEvents` / `clearEvents` / `exportAsText` /
  `getAuditSummary` / `recoverCorruptedAuditLog`; corrupted file recovers to empty (+ warning event),
  bad lines skipped; metadata sanitized (no screenshots/base64, length caps);
- **Audit export**: share plain text via `ACTION_SEND` (no permissions, no FileProvider); Audit Log
  screen gains Share + summary + storage status;
- **Profiles foundation**: new `profiles/` package — `Profile`, `ProfileRepository`
  (`filesDir/profiles.json`, default profile, corrupted fallback, one active), `ProfileManager`
  (CRUD + delete rules via `DeleteResult`); validation (name ≤ 80, description ≤ 300);
- **Scenarios bound to profiles**: `Scenario.profileId`; repository reads/writes it and migrates
  legacy scenarios to the default profile (`storageMigrated`); `ScenarioManager` gains
  `scenariosForProfile` / `countForProfile` / `getActiveScenarioForProfile`; create assigns the
  active profile;
- delete rules: cannot delete the last/active profile or a profile with scenarios;
- `ClickFlowViewModel`: profile + audit state, `shareAuditLog()`, `auditSummary()`, profile form,
  profile-scoped active scenario; Diagnostics extended (profiles/audit storage);
- UI: Profiles screen + Profile form; Home/Scenarios show the active profile and its scenarios;
  Audit Log screen with summary/share; Safety Center reports profiles local + audit persistent +
  export share-text + real taps disabled;
- RU/EN strings; docs `ANDROID_PROFILES.md`, `ANDROID_AUDIT_PERSISTENCE.md`, `ANDROID_EXPORT_MODEL.md`
  + updates to README/PROJECT_CONTEXT/audit/storage/UI/safety/MVP/smoke;
- no real taps, no permissions, no external storage.

### Step 54 — Android Multi-step Scenarios and Simulation Audit Log

- expanded `Scenario` to schema **v2**: ordered `actions: List<ScenarioAction>`, `version=2`,
  `MULTI_STEP_SIMULATION` type; `ScenarioSettings` reduced to `repeatCount`/`intervalMs`;
- added action types `SIMULATED_TAP` / `WAIT` / `NOTE` (`ScenarioAction`, `ScenarioActionType`) with
  per-type validation (x/y ≥ 0; duration ≥ 100 ms; note ≤ 300 chars);
- `ScenarioRepository`: backward-compatible **v1→v2 migration** (old `x/y` → one `SIMULATED_TAP`
  action), `storageMigrated` flag, action-list persistence, multi-step default scenario, corrupted
  fallback;
- `ScenarioManager`: action CRUD — `addAction` / `updateAction` / `deleteAction` / `moveAction` /
  `duplicateAction`;
- `SimulationEngine`: executes `repeatCount × actions` in order with progress
  (`currentActionIndex` / `currentRepeatIndex` / percent); WAIT delays; emits audit events; statuses
  include `error`; still no real input;
- added `audit/` package: `AuditEvent`, `AuditSeverity`, `AuditType`, `AuditLogManager` (in-memory,
  bounded, `StateFlow`, `exportText()`);
- `ClickFlowViewModel`: scenario detail + action form state, audit events, run history, run indices,
  defensive `attemptRealTapBlocked()` (audits `safety.realTapBlocked`);
- UI: Scenario Detail (action list + add/edit/delete/move + run), Action form (per-type fields with
  inline validation), Audit Log screen (list + clear), Home progress (current action/repeat/percent);
- Diagnostics: actions count, current action/repeat, audit count + last type, storage migrated;
  Safety Center: multi-step simulation-only, audit log enabled, no permissions, real taps disabled;
- RU/EN strings (75/75); docs `ANDROID_MULTI_STEP_SCENARIOS.md` + `ANDROID_AUDIT_LOG.md`; updated
  README, PROJECT_CONTEXT, scenario storage/UI docs, MVP checklist, safety model, smoke script;
- no real taps implemented.

### Step 53 — Android Scenario UI and Local Storage

- expanded `Scenario` with `ScenarioSettings` (x, y, repeatCount, intervalMs) and `isActive`;
- added `ScenarioValidator` (name required; x/y ≥ 0; repeatCount 1..1000; intervalMs ≥ 100);
- added `ScenarioRepository` — JSON persistence in internal storage with corrupted-storage fallback
  to a default scenario (`corruptedStorageRecovered`); no permissions, no external storage;
- reworked `ScenarioManager` to back onto the repository and expose a `StateFlow`;
- reworked `SimulationEngine` to run the active scenario with steps/interval/progress and a
  localized dry-run log line; added `error` status; still no real input;
- expanded `ClickFlowViewModel` (AndroidViewModel) with scenario list, active selection, form state,
  validation errors, storage flags, and scenario run controls;
- added Compose UI: Scenarios list (with empty state), create/edit form with inline validation, and
  Home integration showing active scenario + progress;
- updated Diagnostics (scenarios count, active scenario, storage state) and Safety Center
  (scenarios simulation-only, local storage used, real taps disabled);
- added RU/EN strings for the scenario layer;
- added docs `ANDROID_SCENARIO_STORAGE.md` and `ANDROID_SCENARIO_UI.md`; updated README,
  PROJECT_CONTEXT, MVP checklist, safety model;
- no real taps implemented.

### Step 52 — Android Project Foundation

- created Android project structure;
- added simulation-only app shell;
- added basic UI;
- added scenario simulation foundation;
- added Safety Center foundation;
- added Diagnostics foundation;
- added Android safety/product docs;
- no real taps implemented.

Details:

- Initialized a native **Kotlin + Jetpack Compose** Android project (package `com.clickflow.android`,
  app name "ClickFlow Android", minSdk 26 / targetSdk 34, versionName `1.0.0-alpha.1`).
- Configured Gradle (Kotlin DSL) with AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.06.00, and the
  Gradle 8.7 wrapper.
- Added single-activity Compose UI: Home (status badge, Start/Stop/Emergency Stop, navigation),
  Scenarios, Safety Center, Diagnostics.
- Added simulation-only scenario model (`Scenario`, `ScenarioManager`) supporting
  `simple_tap_simulation`, and `SimulationEngine` with `startSimulation` / `stopSimulation` /
  `emergencyStop` / `getStatus` and an `idle / running / completed / stopped / emergency_stopped`
  status machine.
- Added safety foundation (`SafetyState`, `SafetyGate`, `SafetyCenter`):
  `simulationOnly = true`, `realTapsEnabled = false`, `canRunRealTap()` returns false, blocked
  reasons surfaced in UI.
- Added diagnostics foundation (`DiagnosticsState`, `DiagnosticsManager`).
- Added RU/EN string resources.
- Added documentation: product plan, safety model, architecture, permissions plan, accessibility
  plan, MediaProjection plan, MVP checklist, APK build guide; plus a smoke-test script.
- **No real taps implemented.** No Accessibility Service runtime, no MediaProjection capture, no
  overlay logic, no runtime permission requests, no prohibited automation.
