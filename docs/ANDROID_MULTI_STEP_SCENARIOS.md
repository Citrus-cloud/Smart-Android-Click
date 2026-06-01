# Android Multi-step Scenarios — ClickFlow Android (Step 54)

## Purpose

Extend scenarios from a single simulated tap to an ordered list of simulation **actions**, executed
in sequence for a configurable number of repeats. Still **simulation only** — no real input.

## Current status

Implemented in Step 54. Scenarios are schema **version 2**. The simulation engine walks the action
list, emits progress and audit events, and never performs a real tap.

## Scenario v2 model

```
Scenario
  id: String
  name: String
  type: ScenarioType            // SIMPLE_TAP_SIMULATION | MULTI_STEP_SIMULATION
  settings: ScenarioSettings    // repeatCount, intervalMs
  actions: List<ScenarioAction> // ordered steps
  createdAt / updatedAt: Long
  isActive: Boolean
  version: Int = 2
```

## Action types

`ScenarioAction(id, type, x?, y?, durationMs?, message?, label?)`, `ScenarioActionType`:

| Type | Fields | Behavior (simulation) |
|---|---|---|
| `SIMULATED_TAP` | x, y, label? | Logs "Simulated tap at x,y" + audit event. **No real tap.** |
| `WAIT` | durationMs | `delay(durationMs)` + logs "Waited N ms". |
| `NOTE` | message | Logs the note message. |

Validation: name not blank; actions not empty; repeatCount 1..1000; intervalMs ≥ 100;
SIMULATED_TAP x/y ≥ 0; WAIT durationMs ≥ 100; NOTE message not blank, ≤ 300 chars.

## Backward compatibility

A Step 53 (v1) scenario stored a single tap as top-level `x/y`. On load, `ScenarioRepository`
migrates it to a single `SIMULATED_TAP` action (preserving `repeatCount`/`intervalMs`), upgrades the
record to version 2, persists it, and flags `storageMigrated` (surfaced in Diagnostics + an audit
`storage.migrated` event). Nested-vs-top-level settings are both read.

## Simulation engine flow

```
for repeat in 1..repeatCount:
  for action in actions:
    delay(intervalMs)
    log + audit (simulated_tap | wait | note)
    if WAIT: delay(durationMs)
    update progress (currentStep, currentActionIndex, currentRepeatIndex, percent)
total steps = repeatCount * actions.size
```

Statuses: `idle / running / completed / stopped / emergency_stopped / error`. Stop → `stopped`;
Emergency Stop cancels the coroutine immediately → `emergency_stopped` (audit SAFETY event).

## What remains disabled

Real taps, Accessibility Service, `dispatchGesture`, MediaProjection, overlay, runtime permissions,
and all prohibited automation. `canRunRealTap()` returns `false`; the manifest declares no
permissions.
