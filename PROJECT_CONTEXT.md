<PLACEHOLDER_EXISTING_CONTENT>

## Step 61
Read-only permissions surface. `PermissionsManager`, `Screen.PERMISSIONS`.

## Step 62
Real Tap Prototype — UI skeleton, dispatch blocked. Six prototype ViewModel APIs.

## Step 63
Live SafetyGate flags + ViewModel wiring. `RealTapDispatchResult`, `safetyGateReasons`.

## Step 64
RealTapController wired, granular audit, marker invariant, duplicate SafetyState fix.

## Step 65
First JVM test suite: SafetyGate, RealTapController, RealTapSession, RealTapSafetyReview.

## Step 66 (Part 1)
Pure-Kotlin screen-capture lifecycle: `ScreenCaptureState` + `ScreenCaptureController` + tests.

## Step 66 (Part 2)
Real MediaProjection: `ScreenCaptureRepository`, `ScreenCaptureService`, `ScreenCaptureActivity`, manifest.

## Step 67
`CaptureRegion` (normalized [0,1]) + `RegionSelectorController` (EMPTY/DRAGGING/SELECTED) + tests.

## Step 68
`CaptureTemplate` + `TemplateManager` (@Synchronized, sequential ids, Result.Ok/Error) + 18 JVM tests.

## Step 69
`MatchResult` + `TemplateMatcher` (evaluate/evaluateBest/matchesOnly, injected nowProvider) + 8 JVM tests.

## Step 70
`ImageTargetResult` + `ImageTargetOutcome` (Matched/NoMatch/Error) + `ImageTargetController` + 10 JVM tests.

## Step 71
`OcrTextRegion` + `OcrResult` (from/empty factories) + `interface OcrProvider` +
`StubOcrProvider` + `OcrController` (findText/findExact/bestMatch) + 12 JVM tests.

## Step 72
`TextTargetResult` + `TextTargetOutcome` (Matched/NoMatch/Error) +
`TextTargetController` (validate → OCR → bestMatch → typed outcome) + 11 JVM tests.

## Step 73

Visual scenario builder + presets — domain layer for assembling visual scenarios.

- `scenario/ScenarioPreset.kt`:
  - `enum PresetActionType { TAP, WAIT, NOTE }`.
  - `data class PresetAction(type, label, x, y, durationMs, note)` + `isValid`.
  - `data class ScenarioPreset(id, name, description, actions)` + `isValid`
    (id/name non-blank, name ≤ 60, description ≤ 200, 1–20 valid actions).
  - `object BuiltInPresets { TAP_CENTER, TAP_AND_WAIT, ALL }`.
- `scenario/VisualScenarioBuilder.kt`:
  - Mutable ordered action list (max 20).
  - `add`, `update`, `remove`, `move`, `clear`, `applyPreset`, `appendPreset`.
  - All mutations return `BuilderResult.Ok` / `BuilderResult.Error(reason)`.
  - Reason codes: `invalid_action`, `too_many_actions`, `invalid_index`, `invalid_preset`.
  - No Android imports.
- Test: `VisualScenarioBuilderTest.kt` — 13 JVM tests.
- `AppInfo.STEP` → Step 73.
- Invariants: pure in-memory data — no capture, no tap, no persistence;
  `SafetyGate.canRunRealTap()` unchanged (`false`).
