# PROJECT_CONTEXT — ClickFlow Android

## What this is

The native Android branch of ClickFlow. **Android repo started** at **Step 52**.
It is a standalone Kotlin/Compose application — explicitly **not** Electron and **not** a runtime
copy of the desktop ClickFlow code.

## Current state (Step 59)

- **Simple Clicker UX + draggable marker + clean minimal UI** on top of Steps 52–58;
- home = Simple Clicker: draggable in-app circular marker (not a system overlay), Start/Stop/Emergency,
  quick Interval/Count steppers backed by a per-profile "Quick clicker" scenario;
- marker + quick settings persist via existing scenario storage; survive restart;
- **Advanced** menu hides Scenarios/Profiles/Audit/Backup/Safety/Diagnostics/About (all still available);
- calm Material 3 theme (light + dark); build verified (`assembleDebug`);
- **real taps disabled**; **0 permissions**; **0 providers**; no overlay/Accessibility/MediaProjection;
- next: on-device QA of the new UX, or a system-overlay/Accessibility skeleton strictly behind
  consent + safety gate + go/no-go (no real taps until reviewed).

## Earlier state (Step 58)

- **pre-alpha GitHub release PUBLISHED** on top of Steps 52–57;
- tag **`android-v0.1.0-prealpha`** (annotated, at commit `b9cb875`) pushed; GitHub **pre-release**
  created with `app-debug.apk` (15,761,368 bytes, debug-signed) attached;
- release URL:
  https://github.com/Citrus-cloud/Smart-Android-Click/releases/tag/android-v0.1.0-prealpha
- build verified: `./gradlew assembleDebug` succeeds (JDK 17 + Android SDK 34);
- `versionName = 0.1.0-prealpha`, `versionCode = 1` (Android branch, distinct from desktop);
- **real taps disabled**; **0 permissions**; **0 providers**; no external storage;
- **no Accessibility real taps yet**, **no MediaProjection yet**, **no real taps**;
- next after release: polish, or an **Accessibility skeleton strictly behind explicit consent +
  safety gate + go/no-go** (no real taps until reviewed).

### Steps 52–55 (still in place)

- 52 foundation; 53 scenario CRUD + storage; 54 multi-step + audit log; 55 profiles + persistent
  audit + audit export.

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
