# Android Scenario Storage — ClickFlow Android

## Purpose

Persist user-authored simulation scenarios locally so they survive app restarts, without any
network, permissions, or external storage.

## Current status (Step 53)

Implemented. Scenarios are stored as JSON in internal app storage and restored on launch. Storage is
**simulation data only** — it cannot and does not enable real taps.

## Scenario model

```
Scenario
  id: String
  name: String
  type: ScenarioType           // SIMPLE_TAP_SIMULATION
  settings: ScenarioSettings   // x, y, repeatCount, intervalMs
  createdAt: Long
  updatedAt: Long
  isActive: Boolean
```

Validation (see `ScenarioValidator`): name not blank; `x >= 0`; `y >= 0`;
`repeatCount in 1..1000`; `intervalMs >= 100`.

## Storage location

- **Internal app storage**: `context.filesDir/scenarios.json`.
- Format: `{ "version": 1, "scenarios": [ ... ] }`, parsed with `org.json` (bundled with Android —
  no serialization plugin).
- Writes are temp-file + rename for resilience against partial writes.
- **No external storage. No permissions.**

## Corrupted storage fallback

`ScenarioRepository.loadScenarios()` never throws. If the file is missing, empty, malformed, or
unreadable, it falls back to a single **default scenario** and sets
`corruptedStorageRecovered = true` (surfaced in Diagnostics). The app never crashes on bad storage.

Default scenario:

| field | value |
|---|---|
| name | Demo simulation tap |
| x | 500 |
| y | 800 |
| repeatCount | 5 |
| intervalMs | 500 |

Per-field hardening: unknown `type` → `SIMPLE_TAP_SIMULATION`; out-of-range values are coerced into
valid ranges on read; exactly one scenario is forced active.

## What is NOT stored

- No personal data, no credentials.
- No real-input configuration (there is none).
- Nothing on external/shared storage.
- No analytics or network state.

## Simulation-only behavior

Stored `x/y/repeatCount/intervalMs` drive only the dry-run engine (status, progress, and a localized
"Simulated tap at x,y" log line). They are never sent to any real input API.

## Future import/export

A later step may add JSON import/export of scenarios (share/back up presets), still without
permissions where possible (e.g. Storage Access Framework). Not implemented in Step 53.
