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

## Build environment note

Building requires JDK 17 and the Android SDK (`compileSdk 34`, `build-tools`, `platform-34`). On a
machine without these, install Android Studio (bundles the SDK) or the command-line SDK tools, then
run the Gradle command above. See [APK_BUILD_GUIDE.md](APK_BUILD_GUIDE.md).
