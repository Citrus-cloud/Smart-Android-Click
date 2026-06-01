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

## Schema v2 migration (Step 54)

Step 54 introduces **schema version 2** (multi-step). Scenarios now persist `settings`
(`repeatCount`, `intervalMs`) and an `actions` array; each action stores `type` plus the fields
relevant to it (`x/y/label`, `durationMs`, or `message`).

On load, the repository auto-migrates older records:

- A v1 (Step 53) scenario — single tap stored as top-level `x/y` — becomes one `SIMULATED_TAP`
  action, preserving `repeatCount`/`intervalMs`. The record is upgraded to `version: 2`, re-saved,
  and `storageMigrated` is set (also logged as a `storage.migrated` audit event).
- Settings are read from either the nested `settings` object (v2) or top-level fields (v1).
- Per-field values are coerced into valid ranges on read; an empty action list is repaired with a
  placeholder `NOTE` so a scenario is never empty.

The corrupted-storage fallback now seeds a **multi-step** default (NOTE → SIMULATED_TAP → WAIT →
NOTE) instead of a single tap.

## Future import/export

A later step may add JSON import/export of scenarios (share/back up presets), still without
permissions where possible (e.g. Storage Access Framework). Not implemented in Step 53.
