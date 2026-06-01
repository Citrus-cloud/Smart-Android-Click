# Android MVP Checklist — ClickFlow Android (Step 52)

Acceptance checklist for the Step 52 foundation. Verify on a device/emulator (API 26+) after
building the debug APK.

- [ ] **project builds** — `./gradlew assembleDebug` succeeds (JDK 17 + Android SDK required).
- [ ] **app launches** — ClickFlow Android opens to the Home screen.
- [ ] **simulation starts/stops** — "Start simulation" sets status `running`; "Stop" sets `stopped`.
- [ ] **emergency stop works** — "Emergency Stop" sets status `emergency_stopped`.
- [ ] **safety center opens** — shows simulation-only enabled, real taps not implemented, etc.
- [ ] **diagnostics opens** — shows version, `simulationOnly=true`, `realTapsEnabled=false`, etc.
- [ ] **real taps disabled** — no UI control enables real taps; `canRunRealTap()` is false.
- [ ] **docs updated** — README, PROJECT_CONTEXT, CHANGELOG, and `docs/` present and current.

## Step 53 — Scenario UI + Local Storage

- [ ] **create scenario** — form validates and saves a new scenario.
- [ ] **edit scenario** — existing scenario can be edited and saved.
- [ ] **delete scenario** — scenario can be removed; active one is re-promoted.
- [ ] **select active scenario** — Select sets the active scenario (ACTIVE badge shown).
- [ ] **persistence after restart** — scenarios survive an app restart (loaded from internal JSON).
- [ ] **simulation progress** — running the active scenario shows steps/percent and a dry-run log.
- [ ] **corrupted storage safe** — a corrupted `scenarios.json` falls back to default without crash.

## Step 54 — Multi-step Scenarios + Audit Log

- [ ] **create multi-step scenario** — new scenario opens its detail editor.
- [ ] **add tap / wait / note actions** — each type can be added via its form.
- [ ] **edit action** — an existing action can be edited and saved.
- [ ] **move action up/down** — reordering persists.
- [ ] **delete action** — removing works; scenario never becomes empty.
- [ ] **persistence after restart** — multi-step scenarios survive an app restart.
- [ ] **v1 migration** — a Step 53 scenario loads as a v2 single `SIMULATED_TAP` action.
- [ ] **run shows progress** — current repeat / current action / percent update during a run.
- [ ] **audit log** — events appear; Clear empties the log.
- [ ] **emergency stop** — cancels immediately and logs a SAFETY audit event.

## Step 55 — Profiles + Audit Persistence + Export

- [ ] **profiles screen** — opens and lists profiles with scenario counts.
- [ ] **create / edit profile** — form validates (name ≤ 80, description ≤ 300) and saves.
- [ ] **select active profile** — switches the visible scenario set.
- [ ] **scenarios filtered by profile** — Home/Scenarios show only the active profile's scenarios.
- [ ] **delete rules** — cannot delete the last/active profile or one with scenarios (message shown).
- [ ] **migration** — legacy scenarios load under the default profile.
- [ ] **audit persists** — events survive an app restart (`audit-log.jsonl`).
- [ ] **audit summary** — severity counts + storage status shown.
- [ ] **audit clear** — empties the persisted log.
- [ ] **audit share** — share-sheet opens with the text report (no permissions).

## Step 56 — Backup Import/Export + Pre-alpha QA

- [ ] **backup screen** — opens and shows profile/scenario counts + simulation-only note.
- [ ] **export backup** — share-sheet opens with backup JSON text.
- [ ] **paste + validate** — preview shows profiles/scenarios/invalid counts; Import gated on valid.
- [ ] **import merge/rename** — imported items added; conflicts renamed; existing data preserved.
- [ ] **invalid items skipped** — warnings reported; nothing crashes.
- [ ] **replace-all** — only after explicit confirmation.
- [ ] **reload after import** — profiles + scenarios refresh.
- [ ] **backup excludes audit log** — `containsAuditLog: false`; no screenshots/base64.
- [ ] **diagnostics** — backup status/timestamps shown; no permissions, no external storage.

## Step 59 — Simple Clicker UX + Draggable Marker

- [ ] **opens to Simple Clicker** — clean home with tap-target + Start.
- [ ] **draggable marker** — circular marker moves with the finger, clamped to the area.
- [ ] **marker persists** — position survives an app restart.
- [ ] **quick settings** — Interval / Count steppers change values on the home screen.
- [ ] **Start uses marker** — runs simulation at the marker; progress + tap count shown.
- [ ] **Stop / Emergency Stop** — work from the home screen.
- [ ] **Advanced menu** — opens Scenarios / Profiles / Audit / Backup / Safety / Diagnostics / About.
- [ ] **complex screens still work** — reachable via Advanced; Back returns to Advanced.
- [ ] **no permissions / no real taps** — marker is in-app only; simulation-only.

## Build environment note

Building requires JDK 17 and the Android SDK (`compileSdk 34`, `build-tools`, `platform-34`). On a
machine without these, install Android Studio (bundles the SDK) or the command-line SDK tools, then
run the Gradle command above. See [APK_BUILD_GUIDE.md](APK_BUILD_GUIDE.md).
