# Changelog — ClickFlow Android

All notable changes to the ClickFlow Android project are documented here.
This project follows the ClickFlow step-based development model.

## [Unreleased]

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
