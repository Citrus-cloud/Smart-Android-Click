# Android Architecture — ClickFlow Android

## App layers

```
┌─────────────────────────────────────────────┐
│ UI (Jetpack Compose)                          │
│   ClickFlowApp → Home / Scenarios /           │
│   SafetyCenter / Diagnostics screens          │
├─────────────────────────────────────────────┤
│ State holder                                  │
│   ClickFlowViewModel (navigation + wiring)    │
├─────────────────────────────────────────────┤
│ Domain                                        │
│   SimulationEngine   ScenarioManager          │
│   SafetyGate/State   DiagnosticsManager       │
│   SafetyCenter                                │
├─────────────────────────────────────────────┤
│ Future (NOT in Step 52)                       │
│   AccessibilityService   MediaProjection      │
└─────────────────────────────────────────────┘
```

## UI

Single `ComponentActivity` (`MainActivity`) hosting Compose. `ClickFlowApp` switches between four
screens based on `ClickFlowViewModel.screen`. Material 3 theming via `ClickFlowTheme`. Strings are
externalized for RU/EN.

## Scenario manager

`ScenarioManager` holds scenarios in memory (no persistence in Step 52) and seeds two
`simple_tap_simulation` presets. `Scenario` carries `id`, `name`, `type`, `settings`, `createdAt`,
`updatedAt`.

## Simulation engine

`SimulationEngine` exposes `startSimulation`, `stopSimulation`, `emergencyStop`, `getStatus`, plus a
`StateFlow<SimulationStatus>` for the UI. It performs **no real input** — it only advances a status
machine: `idle → running → completed / stopped / emergency_stopped`. It consults `SafetyGate` before
running.

## Safety gates

`SafetyGate` is the single authority for permissions. `canRunSimulation()` is true;
`canRunRealTap()` is hard `false`; `getBlockedReasons()` feeds the Safety Center. `SafetyState`
holds the capability flags (`simulationOnly = true`, `realTapsEnabled = false`, etc.).

## Future Accessibility Service

A future `AccessibilityService` will perform real taps via `dispatchGesture`, gated by consent +
`SafetyGate` + feature flags + Emergency Stop. Not present in Step 52. See
[ANDROID_ACCESSIBILITY_PLAN.md](ANDROID_ACCESSIBILITY_PLAN.md).

## Future MediaProjection

A future capture module will use MediaProjection for optional on-device image/text analysis, gated
by consent, with no disk persistence by default. Not present in Step 52. See
[ANDROID_MEDIAPROJECTION_PLAN.md](ANDROID_MEDIAPROJECTION_PLAN.md).

## Diagnostics

`DiagnosticsManager` builds a read-only `DiagnosticsState` snapshot (version, simulation flags,
planned capabilities, active scenario, last run status) for the Diagnostics screen.
