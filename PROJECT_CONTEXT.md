# PROJECT_CONTEXT — ClickFlow Android

## What this is

The native Android branch of ClickFlow. **Android repo started** at **Step 52**.
It is a standalone Kotlin/Compose application — explicitly **not** Electron and **not** a runtime
copy of the desktop ClickFlow code.

## Current state (Step 55)

- **profiles + persistent/exportable audit log** on top of Steps 52–54;
- **profiles** group scenarios into local workspaces (default profile, one active); scenarios bound
  to `profileId`; UI shows the active profile's scenarios;
- **audit log persisted** to `filesDir/audit-log.jsonl` (bounded, corrupted fallback) with summary;
- **audit export** via share-text only (no permissions, no FileProvider);
- migration assigns legacy scenarios to the default profile;
- **real taps disabled**; **no permissions**; no external storage;
- **no Accessibility real taps yet**, **no MediaProjection yet**, **no real taps**;
- a **future branch will add Android Accessibility Service after safety review**.

### Steps 52–54 (still in place)

- Step 52: simulation-only foundation. Step 53: scenario CRUD + local storage.
- Step 54: multi-step scenarios (`SIMULATED_TAP`/`WAIT`/`NOTE`, schema v2) + in-memory audit log.

### Steps 52–53 (still in place)

- Step 52: simulation-only foundation (app shell, engine, safety gate/state/center, diagnostics).
- Step 53: scenario CRUD + local JSON persistence in internal storage with corrupted-storage
  fallback.

### Step 52 (foundation, still in place)

- simulation-only Android foundation: app shell, simulation engine, safety gate/state/center,
  diagnostics, RU/EN localization.

## Scope captured at Step 52

Included:

- Android app shell (Kotlin + Jetpack Compose, Material 3).
- Single-activity navigation: Home, Scenarios, Safety Center, Diagnostics.
- Simulation engine with a status machine (idle/running/completed/stopped/emergency_stopped).
- Scenario model limited to `simple_tap_simulation`.
- Safety gate/state/center with real taps hard-disabled.
- Diagnostics assembler.
- RU/EN localization.
- Safety + product documentation.
- Debug APK build configuration and Gradle wrapper.

Explicitly excluded (deferred to later, safety-reviewed steps):

- Real tap execution.
- Accessibility Service automation / gesture dispatch.
- MediaProjection screen capture.
- Overlay permission runtime logic.
- Runtime permission requests of any kind.
- Any prohibited automation (captcha/anti-bot bypass, ad-click fraud, banking/payment apps,
  hidden/background control, spyware/keyloggers, root-only features).

## Relation to desktop ClickFlow

Desktop ClickFlow ([Citrus-cloud/Mine](https://github.com/Citrus-cloud/Mine)) is an Electron app at
`v1.0.0-alpha.1` with simulation-first behavior and a hard-gated real-coordinate-click alpha. The
Android project mirrors its **safety-first product philosophy** but shares **no runtime code**.

## Architecture summary

```
UI (Compose)
  → ClickFlowViewModel (state holder / navigation)
      → SimulationEngine (status only, no input)  ← SafetyGate (canRunRealTap = false)
      → ScenarioManager (in-memory presets)
      → DiagnosticsManager (read-only snapshot)
      → SafetyCenter (read-only status)
```

See [docs/ANDROID_ARCHITECTURE.md](docs/ANDROID_ARCHITECTURE.md).

## APK release plan

- Step 52 ships **debug builds only** (`./gradlew assembleDebug`), debug-signed.
- Release signing, code signing, and store distribution are out of scope until functionality and a
  safety review justify them.
