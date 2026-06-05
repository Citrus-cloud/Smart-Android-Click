# Changelog — ClickFlow Android

All notable changes to the ClickFlow Android project are documented here.
This project follows the ClickFlow step-based development model.

## [Unreleased]

### Step 66 (Part 1) — Screen-capture lifecycle controller (pure Kotlin)

First slice of Phase 2 ("move the brain to Android"). Introduces the
framework-free state machine that the real MediaProjection pipeline (Part 2)
will drive. No Android APIs here, so it runs under `./gradlew testDebugUnitTest`
alongside the Step 65 suite.

- New `app/src/main/java/com/clickflow/android/capture/`:
- `ScreenCaptureState.kt` — `CapturePermission` (NOT_REQUESTED / GRANTED /
DENIED), `CaptureStatus` (IDLE / AWAITING_PERMISSION / CAPTURING / FRAME_READY /
STOPPED / ERROR), and `CaptureFrame` (metadata only: width, height,
capturedAtMs — holds no pixels). The state carries the invariant flags
`inMemoryOnly = true`, `persistedToDisk = false`, `analysisPerformed = false`.
- `ScreenCaptureController.kt` — `@Synchronized` state machine:
`requestPermission` → AWAITING_PERMISSION; `onPermissionResult(granted)` →
GRANTED or DENIED + error; `startCapture` requires GRANTED; `onFrameCaptured`
records frame metadata only (rejects a non-CAPTURING state and non-positive
dimensions); `stop` drops the in-memory frame; `onError`; `reset` preserves a
previously granted permission. Holds no Android dependencies.
- New `app/src/test/java/com/clickflow/android/capture/ScreenCaptureControllerTest.kt`
— initial state, permission request/deny, start-requires-permission,
granted→capture→in-memory frame (asserts the three privacy invariants),
frame-requires-capturing, invalid-dimension rejection, stop-drops-frame,
reset-preserves-permission, and the error transition.
- `AppInfo.STEP` bumped to Step 66 (Part 1).
- Deferred to Step 66 Part 2: the real `ScreenCaptureService`
(MediaProjection + ImageReader + VirtualDisplay capturing a single frame to
memory), the Activity consent flow (`createScreenCaptureIntent`), the
foreground-service manifest entries (`FOREGROUND_SERVICE` /
`FOREGROUND_SERVICE_MEDIA_PROJECTION` / `POST_NOTIFICATIONS`), and the capture
screen UI.

Safety / privacy invariants: a captured frame lives only in RAM and is never
written to disk; this step captures only — no OCR / template matching runs;
`SafetyGate.canRunRealTap()` is untouched and still returns `false`.

### Step 65 — RealTap JVM unit-test suite

First automated test coverage for the real-tap prototype. Pure-JVM JUnit 4 (no
Robolectric, no Android framework) so the suite runs under
`./gradlew testDebugUnitTest`.

- New `app/src/test/java/com/clickflow/android/`:
- `safety/SafetyGateTest.kt` — bulk `canRunRealTap()` is always false even with
all four prototype flags live; `canRunRealTapSingleProto()` requires all four;
`resetPrototypeFlags()` clears review/session/consent but preserves the
system-derived accessibility flag; `canRunControlledRealTapSession(sid)` mirrors
the single-proto gate and `getControlledSessionBlockedReasons(sid)` carries the
session id; blocked-reason lists shrink as flags are set.
- `realtap/RealTapControllerTest.kt` — `evaluate(...)` returns
`BLOCKED_INVALID_CONSENT` for a null marker, `BLOCKED_NO_SERVICE` when the
accessibility service is unbound, `BLOCKED_BY_GATE` when bound but the gate is
closed, and `ALLOWED` only when every flag is live; each branch emits the
matching granular `realtap.*` audit type; the bulk gate stays false throughout.
The audit log is constructed with no storage file so the test never touches
`org.json` / disk.
- `realtap/RealTapSessionTest.kt` — consent freshness within `CONSENT_TTL_MS`,
expiry just past the TTL, single-use consumption (nonce cleared), and
consume-when-stale being a no-op.
- `realtap/RealTapSafetyReviewTest.kt` — empty review not passed; acknowledge
all 10 items + `markPassed` passes; acknowledging an unknown key throws;
revoking an item after passing clears `passed`; `reset()` returns `EMPTY`.
- `AppInfo.STEP` bumped to Step 65.
- Deferred (need instrumentation / Robolectric): ViewModel StateFlow transitions
and the `confirmRealTap` message-emission path. The domain logic they delegate to
(`RealTapController`, `SafetyGate`, `RealTapSession`, `RealTapSafetyReview`) is
covered here.

### Step 64 — RealTapController wired end-to-end + granular audit + marker invariant

Completes the three items deferred from Step 63 and fixes a build-breaking
duplicate `SafetyState` declaration.

- **Build fix — duplicate `SafetyState` removed**: `safety/SafetyGate.kt` carried
an inline `data class SafetyState` in the same package as the standalone
`safety/SafetyState.kt`, which is a redeclaration and broke compilation. The
inline copy was deleted; the canonical `safety/SafetyState.kt` (superset of the
five prototype flags) is now the single definition. `SafetyGate` is unchanged
behaviorally — it still reads/writes the same flags via `copy(...)` and
`STEP_62_DEFAULT`.
- **`RealTapController` wired into `ClickFlowViewModel`**: the six prototype APIs
(`toggleSafetyReviewItem`, `startRealTapSession`, `endRealTapSession`,
`requestRealTap`, `confirmRealTap`, `cancelRealTap`) plus `emergencyStop` now
route every audit record through `RealTapController(gate, auditLog)` using the
granular `AuditType.REAL_TAP_*` constants instead of the generic
`SAFETY_REAL_TAP_BLOCKED` event. The controller holds no state of its own.
- **Granular `realtap.*` audit events**: session start/end, consent
requested/given/declined, safety-review passed/failed, and the dispatch decision
now use the granular `AuditType.REAL_TAP_*` constants. The dispatch decision is
delegated to `RealTapController.evaluate(...)`, which maps onto the existing
`RealTapDispatchResult` for the UI chip (`ALLOWED`→`DISPATCHED`,
`BLOCKED_BY_GATE`, `BLOCKED_NO_SERVICE`, `BLOCKED_INVALID_CONSENT`). The legacy
bulk chokepoint `attemptRealTapBlocked()` still records `SAFETY_REAL_TAP_BLOCKED`.
- **Marker-only invariant in `confirmRealTap`**: the consent payload is the
marker snapshot taken at request time. Confirmation now recomputes the live
marker `(x,y)` and rejects the tap as `BLOCKED_INVALID_CONSENT` (granular
consent-declined, reason `marker_drift`) if the marker has moved. Consent expiry
and the missing-consent case are also recorded via the controller.
- **SafetyGate additions**: `canRunControlledRealTapSession(sessionId)` (identical
preconditions to `canRunRealTapSingleProto`) and
`getControlledSessionBlockedReasons(sessionId)`. Bulk `canRunRealTap()` still
returns `false` unconditionally.
- **No new string keys**: the UI reuses the existing audit-mirror message keys
(`real_tap_audit_session_started` / `_session_ended` / `_consent_requested` /
`_consent_confirmed` / `_consent_expired` / `_dispatch_blocked`).
- `AppInfo.STEP` bumped to Step 64.

Safety invariants preserved: `SafetyGate.canRunRealTap()` returns `false`
unconditionally; bulk / looped / scenario-driven paths untouched; the controller
is pure orchestration (no Android APIs, no StateFlows) and unit-testable; review
+ session + consent state remain process-local and are never persisted, exported,
or backed up.

### Step 63 — Real-tap prototype hardening (in progress)

- **AppInfo bump**: `core/AppInfo.kt` `STEP` updated to
`Step 63 — Real-tap prototype hardening (controller wiring, granular audit, marker invariant, live SafetyGate state)`
(commit `7b251157`).
- **QA scenarios doc landed**: new `docs/REAL_TAP_QA_SCENARIOS.md` — 17 scenarios across 6 groups
covering happy path, gate failures (review/session/consent/service/API), consent edge cases
(TTL expiry, single-use nonce, double-confirm, cancel mid-countdown), session lifecycle
(cold start, Emergency Stop, end-session-with-pending-consent), marker invariant
(only-marker-target, off-screen rejection, multi-marker disambiguation), dispatch failure
modes (service unbound mid-flight, gesture rejection, dispatch race), and audit completeness
(every transition emits exactly one `realtap.*` event) (commit `b300df2a`).
- **Fixes log landed**: new `docs/REAL_TAP_FIXES_LOG.md` — template-driven log for each
scenario fix (symptom, root cause, file(s) changed, commit, verification, audit-event
coverage) plus a baseline entry documenting the Step 62 → 63 starting state
(commit `49574d31`).
- **SafetyGate live state landed**: `safety/SafetyGate.kt` extended with four narrow per-flag
mutators (`updateReviewPassed`, `updateAccessibility`, `updateSession`, `updateConsentFresh`)
and a `resetPrototypeFlags()` chokepoint wired for Emergency Stop / session end. State is
process-local, never persisted, never exported. `canRunRealTap()` still returns `false`
unconditionally; the bulk contract is untouched. The mutators are `@Synchronized` and only
flip one flag per call — no bulk "set everything" entry point — so audit reasoning stays
one-call-per-transition (commit `bdc84bd4`).

**Pending for Step 63 (next phase):**

- `core/ClickFlowViewModel.kt` rewrite: wire `RealTapController` end-to-end into the existing
six prototype APIs (`startRealTapSession` / `endRealTapSession` / `requestRealTap` /
`confirmRealTap` / `cancelRealTap` / `toggleSafetyReviewItem`); drive the new SafetyGate
mutators on every transition; emit granular `realtap.*` audit events for every state change
including the dispatch attempt/block/success/failure quadrant; enforce the marker-only
invariant inside `confirmRealTap` (reject any (x, y) that does not match the current marker
position within the live composition bounds).
- `ui/RealTapPrototypeScreen.kt`: add the result chip (DISPATCHED / BLOCKED_BY_GATE /
BLOCKED_NO_SERVICE / BLOCKED_INVALID_CONSENT / DISPATCH_CANCELLED / DISPATCH_FAILED) and a
human-readable blocked-reasons list driven by `SafetyGate.getSingleProtoBlockedReasons()`.
- `PROJECT_CONTEXT.md`: add the `## Step 63` section finalizing the above once landed.

Safety invariants preserved: `SafetyGate.canRunRealTap()` returns `false`; bulk / looped /
scenario-driven paths untouched; review + session + consent state remain process-local and
never persisted / exported / backed up; SafetyGate state is mutable in-process only and
resets to a known-safe baseline on Emergency Stop and process death.

### Step 62 — Single real-tap prototype (gated, audited, per-tap consent)

- introduced a narrow, fully-audited path for **exactly one real tap per explicit consent** through
the Step 61 `ClickFlowAccessibilityService`; bulk, looped, and scenario-driven real taps remain
hard-disabled by `SafetyGate.canRunRealTap()` (still returns `false`);
- `realtap/` package: `RealTapRequest`, `RealTapResult` + `RealTapOutcome` (6 outcomes),
`RealTapSession` (in-memory only — never persisted/exported/backed up),
`RealTapSafetyReview` (10-item review, in-memory pass state), `RealTapController`
(gate → service → audit → diagnostics mirror; single-use nonce, 10s consent TTL,
defence-in-depth re-checks before every dispatch);
- `safety/SafetyState.kt` extended with `realTapSafetyReviewPassed` /
`realTapSessionActive` / `realTapConsentFresh` (defaults `false`);
- `safety/SafetyGate.kt`: NEW `canRunRealTapSingleProto(state, request)` + `getSingleProtoBlockedReasons(...)`;
requires ALL of {review passed, session active, consent fresh + nonce valid + TTL ≤ 10s,
API ≥ 24, accessibility service bound}; `canRunRealTap()` still returns `false`;
- `permissions/ClickFlowAccessibilityService.kt`: added `liveInstance` singleton (set in
`onServiceConnected`, cleared in `onUnbind`) and `performSingleTap(x, y, onResult)` —
the ONLY `dispatchGesture` call site in the codebase; still no event subscriptions, no
window content reads;
- `audit/AuditEvent.kt`: 14 new `realtap.*` constants (`review.passed/reset`,
`session.started/ended`, `consent.requested/confirmed/cancelled/expired/reused`,
`dispatch.attempted/blocked/success/failure`, `service.unavailable`); `AuditType`
total = 33;
- `diagnostics/DiagnosticsState.kt` + `DiagnosticsManager.kt`: +6 mirror fields
(`realTapSessionActive`, `realTapSafetyReviewPassed`, `realTapConsentFresh`,
`realTapDispatchedCount`, `realTapLastOutcome`, `realTapLastEventAtMs`);
- `res/values/strings.xml` + `res/values-ru/strings.xml`: 22 new keys each (EN + RU) for the
prototype UX — review, session, consent dialog, blocked reasons, emergency-stop note, plus
5 audit-mirror message keys (`real_tap_audit_session_started`, `real_tap_audit_session_ended`,
`real_tap_audit_consent_requested`, `real_tap_audit_consent_expired`,
`real_tap_audit_dispatch_blocked`);
- **UI wiring landed:** `ClickFlowViewModel` adds `Screen.REAL_TAP_PROTOTYPE`,
`SafetyReviewState` (10-item checklist + `itemsLocalized` + `allPassed` + `toggle`),
`RealTapSessionState`, `RealTapConsent` (10s expiry), six new `StateFlow`s
(`safetyReview`, `realTapSession`, `realTapConsent`, plus three one-shot audit-mirror
message flows), and six prototype APIs (`startRealTapSession`, `endRealTapSession`,
`requestRealTap`, `confirmRealTap`, `cancelRealTap`, `toggleSafetyReviewItem`); all
transitions audit-logged; `emergencyStop()` tears down the active session and invalidates
any pending consent;
- `ui/RealTapPrototypeScreen.kt`: self-contained composable (own `RealTapScaffold` +
`RealTapMessageLine` helpers, no dependency on `Screens.kt` private helpers) — header,
status badge, scrollable 10-item review block, session controls, "Request single real tap"
button, consent dialog with countdown, blocked-reason list driven by
`SafetyGate.getSingleProtoBlockedReasons`, and the permanent "Bulk and looped real taps
remain blocked by SafetyGate" footer;
- `ui/Screens.kt`: `Screen.REAL_TAP_PROTOTYPE` routed in the navigation `when`;
`AdvancedScreen` gains a `NavButton(stringResource(R.string.btn_real_tap_prototype))`
entry; `MessageLine` extended with the five audit-mirror keys;
- **Safety / metadata polish landed:** `safety/SafetyCenter.kt` extended — `items()` now
accepts three optional read-only parameters (`realTapSafetyReviewPassed`,
`realTapSessionActive`, `realTapConsentFresh`, all default `false` for compat); reports
three new line items (Safety Review, session, consent), a "Single real-tap prototype"
summary line (status: `gated — Safety Review + active session + fresh consent (10s TTL) + API ≥ 24`),
and a permanent "Bulk / looped / scenario real taps — blocked by SafetyGate (prototype
does not unlock these)" reminder; the legacy "Real taps" row was renamed to "Real taps
(bulk / looped / scheduled)"; the Accessibility Service line was updated to reflect the
Step 62 single-tap dispatch surface; `core/AppInfo.kt`: `STEP` bumped to
`Step 62 — Single real-tap prototype (UI wiring landed; gated by SafetyGate + per-tap consent)`;
- new docs: `docs/REAL_TAP_PROTOTYPE.md` (architecture, file map, invariants, out-of-scope),
`docs/SAFETY_REVIEW_CHECKLIST.md` (the 10 items verbatim, EN + RU, UX rules),
`docs/CONSENT_FLOW.md` (per-tap consent lifecycle, TTL/nonce contract, audit events,
defence-in-depth);
- pending in Step 62 (release tasks, code phase complete): smoke verification, CI green on
`main`, release-guide refresh, APK rebuild and asset replacement on the
`android-v0.1.0-prealpha` pre-release;
- invariants: scenario runner untouched; `canRunRealTap()` still `false`;
`performSingleTap` is the sole `dispatchGesture` call site; `RealTapController` is the sole
caller of `performSingleTap`; review + session + consent state never persisted, never
exported, never backed up; Emergency Stop ends the session and invalidates any pending consent;
the prototype UI is reachable only from `AdvancedScreen` — no shortcut from Home / Simple
Clicker / Scenario Runner; SafetyCenter surfaces prototype state read-only and reiterates
that bulk / looped / scenario real taps remain blocked.

### Step 61 — Permissions skeleton (overlay + accessibility service, no automation)

- introduced the plumbing to later show a system overlay marker and react to accessibility events
WITHOUT enabling any real input; `SafetyGate.canRunRealTap()` remains hard-coded to `false`;
- `permissions/PermissionStatus.kt` — pure data class
(`overlayGranted`, `accessibilityEnabled`, `lastUpdatedAt`) + `EMPTY` constant;
- `permissions/PermissionsManager.kt` — read-only detector using
`Settings.canDrawOverlays()` and parsing `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`;
exposes `refresh()` plus intents to open the matching system settings screens; does not enable
anything itself;
- `permissions/ClickFlowAccessibilityService.kt` — `AccessibilityService` subclass declared as a
**no-op skeleton**: no event subscriptions, no gesture dispatch, no window content reads;
- `res/xml/accessibility_service_config.xml` — minimal config (`accessibilityEventTypes="0"`,
`canRetrieveWindowContent="false"`, no `canPerformGestures`);
- `AndroidManifest.xml` — `SYSTEM_ALERT_WINDOW` declared (opt-in via system settings),
`ClickFlowAccessibilityService` declared with `BIND_ACCESSIBILITY_SERVICE` and config meta-data;
no autostart, no `<receiver>`, no scheduling;
- `safety/SafetyCenter.kt` — extended to accept `PermissionStatus`; items reflect live overlay /
accessibility status; real-tap row still reads `disabled (canRunRealTap=false)`;
- `diagnostics/DiagnosticsManager.kt` — `overlayEnabled` / `accessibilityEnabled` parameters
(default `false`); `permissionsRequired` stays `false` because permissions remain opt-in;
- new EN + RU strings for permissions, accessibility service label / summary / description, and
floating-marker UX;
- UI wiring landed alongside Step 62: `ClickFlowViewModel` exposes `PermissionStatus` state,
`refreshPermissions()`, `Screen.PERMISSIONS`; `PermissionsScreen` composable + Advanced entry
button + navigation routing;
- safety invariants: `SafetyGate.canRunRealTap()` = `false` (hard-coded); SafetyCenter exposes
zero controls to enable real input; accessibility service is a no-op even when enabled in
system settings; overlay permission unlocks only an in-app visual marker, never a tap.

### Step 60 — Updated Android Pre-alpha APK with Simple Clicker UX

- rebuilt the debug APK after Step 59 (Simple Clicker UX);
- updated the existing GitHub pre-release `android-v0.1.0-prealpha`: replaced the `app-debug.apk`
asset with the new build and refreshed the release body to call out the Simple Clicker UX;
- updated release notes/draft and post-release checklist;
- confirmed no permissions, no providers, no real taps (simulation-only) before publishing.

### Step 59 — Android Simple Clicker UX, Draggable Target Marker, and Clean Minimal UI

- **Simple Clicker** is now the home screen: a large rounded tap-target with a **draggable circular
marker** (in-app only — no system overlay), prominent **Start**, plus Stop / Emergency Stop, status
and progress;
- **quick settings** (Interval / Count steppers) edit a per-profile **"Quick clicker"** scenario
(single `SIMULATED_TAP`) directly — no scenario editor needed;
- the marker drag updates normalized fractions in the ViewModel and persists into the quick
scenario's tap action (scaled `0..1000`); position survives restart;
- **Advanced** menu added (`Screen.ADVANCED`) hosting Scenarios / Profiles / Audit Log / Backup /
Safety Center / Diagnostics / About; complex screens removed from the home screen but fully
available; their Back returns to Advanced; About screen added;
- **clean minimal theme**: calm Material 3 indigo/neutral palette (light + dark), soft rounded
corners, light cards;
- Diagnostics adds `simpleClickerReady` / markerX / markerY / quickIntervalMs / quickRepeatCount /
overlayEnabled=false / accessibilityEnabled=false; Safety Center states marker is in-app only,
Start runs simulation only, system overlay not implemented;
- existing scenarios / profiles / audit / backup are unchanged and reachable via Advanced;
- docs `ANDROID_SIMPLE_CLICKER_UX.md`, `ANDROID_MARKER_MODEL.md` + updates;
- RU/EN strings (167/167); build verified (`assembleDebug`); no real taps, no permissions, no overlay.

### Step 58 — Android Pre-alpha Release Published

- published GitHub **pre-release** `android-v0.1.0-prealpha` (title "ClickFlow Android Pre-alpha")
from commit `b9cb875`; body from `ANDROID_PRE_ALPHA_RELEASE_DRAFT.md`; marked pre-release;
- annotated tag `android-v0.1.0-prealpha` created and pushed;
- uploaded asset `app-debug.apk` (15,761,368 bytes, debug-signed);
- release URL:
https://github.com/Citrus-cloud/Smart-Android-Click/releases/tag/android-v0.1.0-prealpha
- re-ran final checks before publishing (build succeeds; 0 permissions; 0 providers; no real taps);
- added `ANDROID_PRE_ALPHA_POST_RELEASE_CHECKLIST.md`; updated README/PROJECT_CONTEXT.

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
app name "ClickFlow Android", minSdk 26 / versionName `1.0.0-alpha.1`).
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
