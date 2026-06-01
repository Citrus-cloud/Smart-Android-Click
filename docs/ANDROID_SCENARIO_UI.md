# Android Scenario UI — ClickFlow Android (Step 53)

## Scenarios screen

- Title "Scenarios / Сценарии".
- List of scenario cards. Each card shows: name, type, `x/y`, `repeatCount`, `intervalMs`, and an
  **ACTIVE** badge for the active scenario.
- Per-card actions: **Select**, **Edit**, **Delete**, **Run simulation**.
- Screen actions: **Create scenario**, **Reset to defaults**, **Back**.
- Empty state: message + **Create default scenario** button.

## Scenario form

Reached via Create or Edit. Fields:

- name
- x, y (numeric)
- repeatCount (numeric)
- intervalMs (numeric)

Buttons: **Save**, **Cancel**.

Inline validation (errors shown under each field):

- name required
- x ≥ 0 and y ≥ 0
- repeatCount in 1..1000
- intervalMs ≥ 100

On **Save**: create (new) or update (existing), then return to the Scenarios screen. Simulation is
**not** started automatically. **Cancel** discards edits and returns.

## Active scenario

Exactly one scenario is active. **Select** sets the active scenario; deleting the active one promotes
another. The active scenario drives the Home screen and "Start simulation".

## Main screen integration

Home shows: active scenario name + settings summary, simulation status, progress bar with
`current/total (percent)`, last simulated-tap log line, and buttons: **Start simulation**, **Stop**,
**Emergency Stop**, plus navigation to **Scenarios**, **Safety Center**, **Diagnostics**.

"Start simulation" runs the **active** scenario. If there is no active scenario, a message is shown
instead of crashing.

## Validation

Centralized in `ScenarioValidator`. The form, `ScenarioManager`, and the repository all use the same
rules, so invalid scenarios cannot be saved or persisted.

## Action editor (Step 54)

Tapping **Open** on a scenario opens the **Scenario Detail** screen — the multi-step editor:

- Header: scenario name + summary (`actions=N, repeat=R, interval=I ms`), plus **Edit scenario**
  (metadata form) and **Select** (make active).
- Ordered **action cards**, each showing its index, type, and a summary, with per-card actions:
  **Edit**, **Delete**, **Up**, **Down**.
- Add buttons: **Add tap action**, **Add wait action**, **Add note action** (each opens the Action
  form pre-set to that type).
- **Run simulation** runs this scenario; **Back** returns to the list.

### Action form

Fields depend on the action type:

- **Simulated tap**: X, Y (numeric), Label (optional).
- **Wait**: Duration ms (numeric).
- **Note**: message.

Inline validation: x/y ≥ 0; duration ≥ 100 ms; note message non-blank (≤ 300 chars). Save returns to
the detail screen; Cancel discards. The metadata form (name / repeat / interval) is reached via
**Create scenario** or **Edit scenario**; creating a scenario opens its detail so actions can be
added immediately.

### Audit Log screen

Reached from Home. Lists audit events (severity · type · message · timestamp), shows the event count,
and offers **Clear**. See `ANDROID_AUDIT_LOG.md`.

## Simulation run

`SimulationEngine` runs the active scenario for `repeatCount` steps, waiting `intervalMs` between
steps, updating progress and emitting a localized "Simulated tap at x,y" log line. **No real taps.**
Statuses: `idle / running / completed / stopped / emergency_stopped / error`. **Stop** → `stopped`;
**Emergency Stop** cancels immediately → `emergency_stopped`.
