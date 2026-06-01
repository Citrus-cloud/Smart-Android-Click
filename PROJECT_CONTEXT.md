# PROJECT_CONTEXT — ClickFlow Android

## What this is

The native Android branch of ClickFlow. **Android repo started** at **Step 52**.
It is a standalone Kotlin/Compose application — explicitly **not** Electron and **not** a runtime
copy of the desktop ClickFlow code.

## Current state (Step 52)

- **simulation-only Android foundation**;
- **no Accessibility real taps yet**;
- **no MediaProjection yet**;
- **no real taps**;
- a **future branch will add Android Accessibility Service after safety review**.

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
