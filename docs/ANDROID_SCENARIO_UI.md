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

## Simulation run

`SimulationEngine` runs the active scenario for `repeatCount` steps, waiting `intervalMs` between
steps, updating progress and emitting a localized "Simulated tap at x,y" log line. **No real taps.**
Statuses: `idle / running / completed / stopped / emergency_stopped / error`. **Stop** → `stopped`;
**Emergency Stop** cancels immediately → `emergency_stopped`.
