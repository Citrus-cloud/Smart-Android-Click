<PLACEHOLDER_EXISTING_CONTENT>

## Step 61
Read-only permissions. `PermissionsManager`, `Screen.PERMISSIONS`.

## Step 62
Real Tap Prototype — UI skeleton. Six prototype ViewModel APIs.

## Step 63
Live SafetyGate flags + ViewModel wiring. `RealTapDispatchResult`.

## Step 64
RealTapController wired, granular audit, marker invariant, build fix.

## Step 65
JVM test suite: SafetyGate, RealTapController, RealTapSession, RealTapSafetyReview.

## Step 66 (Part 1)
Screen-capture lifecycle: `ScreenCaptureState` + `ScreenCaptureController` + tests.

## Step 66 (Part 2)
Real MediaProjection: `ScreenCaptureRepository`, `ScreenCaptureService`, `ScreenCaptureActivity`.

## Step 67
`CaptureRegion` + `RegionSelectorController` (EMPTY/DRAGGING/SELECTED) + tests.

## Step 68
`CaptureTemplate` + `TemplateManager` + 18 JVM tests.

## Step 69
`MatchResult` + `TemplateMatcher` (evaluate/evaluateBest/matchesOnly) + 8 JVM tests.

## Step 70
`ImageTargetResult` + `ImageTargetOutcome` + `ImageTargetController` + 10 JVM tests.

## Step 71
`OcrTextRegion` + `OcrResult` + `interface OcrProvider` + `StubOcrProvider` + `OcrController` + 12 JVM tests.

## Step 72
`TextTargetResult` + `TextTargetOutcome` + `TextTargetController` + 11 JVM tests.

## Step 73
`ScenarioPreset` + `PresetAction` + `BuiltInPresets` + `VisualScenarioBuilder` + 13 JVM tests.

## Step 74

Controlled tap session — Phase 3 domain model.

- `realtap/ControlledTapSession.kt`:
  - `data class ControlledTapSession(sessionId, maxTaps, ttlMs, startedAtMs)`: mutable
    tap counter + terminated flag. `isActive(nowMs)`, `isExhausted()`, `recordTap(nowMs)`,
    `terminate()`, `remainingTaps()`, `remainingTtlMs(nowMs)`.
  - `enum ControlledTapBlockReason { SESSION_INACTIVE, SESSION_EXPIRED, SESSION_TERMINATED,
    TAP_LIMIT_REACHED, GATE_CLOSED }`.
  - `sealed ControlledTapDispatchResult { Allowed, Blocked(reason) }`.
- `realtap/ControlledTapSessionManager.kt`:
  - `startSession(sessionId, maxTaps 1–10, ttlMs 1_000–60_000)`: validates params →
    `gate.canRunControlledRealTapSession` → creates session.
    Result codes: `already_active`, `invalid_params`, `gate_closed`.
  - `endSession()`, `emergencyStop()`, `evaluateTap()` (checks: inactive → terminated
    → expired → exhausted → bulk gate), `hasActiveSession()`, `session`.
  - Injected gate + nowProvider.
- Test: `ControlledTapSessionManagerTest.kt` — 14 JVM tests.
- `AppInfo.STEP` → Step 74.
- Invariants: `SafetyGate.canRunRealTap()` = `false`; `evaluateTap()` always returns
  GATE_CLOSED until Step 75. No tap, no I/O.
