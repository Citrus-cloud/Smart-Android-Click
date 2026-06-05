# Changelog — ClickFlow Android

All notable changes to the ClickFlow Android project are documented here.
This project follows the ClickFlow step-based development model.

## [Unreleased]

### Step 73 — Visual scenario builder + presets (domain layer)

Phase 2 closes: introduces the builder-pattern domain layer for assembling
visual scenarios from typed actions and built-in presets. No UI, no persistence,
pure Kotlin + JVM tests.

- New `scenario/ScenarioPreset.kt` — three declarations:
  - `enum class PresetActionType { TAP, WAIT, NOTE }`.
  - `data class PresetAction(type, label, x, y, durationMs, note)` — one step;
    `isValid` enforces type-specific constraints (TAP: x/y in [0,1]; WAIT:
    durationMs ≥ 100; NOTE: non-blank, ≤ 300 chars; label ≤ 60).
  - `data class ScenarioPreset(id, name, description, actions)` — named template;
    `isValid` checks id/name non-blank, name ≤ 60, description ≤ 200, 1–20
    actions, all actions valid.
  - `object BuiltInPresets` — `TAP_CENTER` (single centre tap) and
    `TAP_AND_WAIT` (centre tap + 500 ms wait); `ALL` list for enumeration.
- New `scenario/VisualScenarioBuilder.kt` — mutable ordered action list
  (max 20) with CRUD + preset support; all mutations return
  `BuilderResult.Ok` / `BuilderResult.Error(reason)` with stable reason codes
  (`invalid_action`, `too_many_actions`, `invalid_index`, `invalid_preset`):
  - `add(action)`, `update(index, action)`, `remove(index)`, `move(from, to)`,
    `clear()`.
  - `applyPreset(preset)` — replaces all actions with preset's actions.
  - `appendPreset(preset)` — appends preset's actions to existing list.
  - Read-only `actions`, `count`, `isEmpty`. No Android imports.
- New `app/src/test/java/com/clickflow/android/scenario/VisualScenarioBuilderTest.kt`
  — 13 JVM tests: starts empty, add valid, add invalid (→ error), add → 20 limit,
  update replaces, update out-of-bounds, remove at index, move reorders, clear,
  applyPreset replaces all, appendPreset appends, appendPreset limit exceeded,
  BuiltInPresets all valid.
- `AppInfo.STEP` bumped to Step 73.

Safety / privacy invariants: builder is pure in-memory data — no capture, no tap,
no persistence; `SafetyGate.canRunRealTap()` unchanged (`false`).

### Step 72 — Text-target scenario controller

- `TextTargetResult` + `TextTargetOutcome` + `TextTargetController`. 11 JVM tests.

### Step 71 — On-device OCR stub

- `OcrTextRegion` + `OcrResult` + `OcrProvider` + `StubOcrProvider` +
  `OcrController`. 12 JVM tests.

### Steps 52–70 — Foundation through image-target controller

See git history for full details.
