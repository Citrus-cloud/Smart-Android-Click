# Android Safety Model — ClickFlow Android

ClickFlow Android is **safety-first**. This document is binding for Step 52 and a contract for
future steps.

## Step 52 guarantees

- **No real taps yet.** `realTapsEnabled = false` and `simulationOnly = true` are hard-coded.
  `SafetyGate.canRunRealTap()` returns `false` unconditionally, and no real-tap code path exists.
- **No hidden automation.** Nothing runs without the user pressing a button in the foreground UI.
- **No background control without explicit user consent.**
- **Emergency Stop is always available** and immediately halts any simulation.

## Storage (Step 53)

- Scenario storage is **local internal JSON only** — no permissions, no external storage, no network.
- **Scenario storage does not enable real taps.** Stored coordinates/intervals drive only the
  simulation engine; there is no code path from storage to real input.
- Corrupted storage falls back to a default scenario and never crashes the app.

## Audit log (Step 54)

- An in-memory audit log records lifecycle, per-action, validation, storage, and safety events.
- **The audit log does not enable real taps** — it only records simulation/dry-run activity.
- It stores no screenshots, no base64, no captured screen content, and no personal data.
- Emergency Stop logs a `scenario.emergencyStopped` SAFETY event; any hypothetical real-tap attempt
  routes through `SafetyGate.attemptRealTap()` (always false) and logs `safety.realTapBlocked`.

## Audit persistence, export & profiles (Step 55)

- The audit log is **persisted** to internal storage only (`filesDir/audit-log.jsonl`); it never
  leaves the device on its own.
- **Export is share-text only** (`ACTION_SEND`, `text/plain`) — no storage permission, no FileProvider.
  The exported text contains no screenshots, no base64, no private paths, no personal data.
- **Profiles are local only** (`filesDir/profiles.json`); they group scenarios and grant no
  capabilities. None of this enables real taps.

## Backup import/export (Step 56)

- Backup export/import is **text only** (`ACTION_SEND` share / pasted JSON) — no file picker, no
  FileProvider, no storage permissions.
- Backups **exclude the audit log** by default, and contain no screenshots, base64, or private paths.
- Import **validates before applying** and never overwrites existing data silently; `REPLACE_ALL`
  requires explicit confirmation. None of this enables real taps.

## Multi-step scenarios (Step 54)

- Multi-step scenarios are **simulation only**. Each `SIMULATED_TAP` action logs and updates progress
  but performs **no real input**; `WAIT` only delays; `NOTE` only logs.

## Categorically prohibited (now and in the future)

- Captcha solving / bypass.
- Anti-bot system evasion.
- Advertising-click automation / ad fraud.
- Banking, payment, or other protected/financial app automation.
- Hidden or disguised clicks.
- Background control without explicit, informed user consent.
- Spyware, keyloggers, or input hooks.
- Malicious or excessive permissions.
- Root-only functionality.

## Future real taps — required conditions

Real taps will only ever be enabled when **all** of the following hold:

1. **Accessibility Service** is implemented and manually enabled by the user in system settings.
2. **Explicit in-app consent** is given on a screen that describes the exact behavior.
3. **Safety gates** (`SafetyGate`), feature flags, and audit logging are in place.
4. **Emergency Stop** is wired and verified.
5. A documented **safety review (go/no-go)** is completed for the step that introduces it.

Until every condition is met, real taps remain disabled in code, not just in configuration.

## Layered enforcement

- **State** (`SafetyState`): single source of truth for capability flags.
- **Gate** (`SafetyGate`): the only place that answers "is this allowed?"; real taps always denied.
- **Center** (`SafetyCenter`): read-only UI surface; intentionally exposes **no** control to enable
  real taps.

## Emergency Stop

`SimulationEngine.emergencyStop()` transitions to `emergency_stopped` and clears any active run.
It is always reachable from the Home screen. Future real-tap work must keep Emergency Stop as a
top-level, always-available control.
