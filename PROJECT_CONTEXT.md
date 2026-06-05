<PLACEHOLDER_EXISTING_CONTENT>

## Step 61
Read-only permissions. `PermissionsManager`, `Screen.PERMISSIONS`.

## Step 62
Real Tap Prototype — six prototype ViewModel APIs, SafetyState.

## Step 63
Live SafetyGate flags, `RealTapDispatchResult`, `safetyGateReasons`.

## Step 64
RealTapController wired, granular audit, marker invariant, build fix.

## Step 65
JVM tests: SafetyGate, RealTapController, RealTapSession, RealTapSafetyReview.

## Step 66 (Part 1)
`ScreenCaptureState` + `ScreenCaptureController` + tests.

## Step 66 (Part 2)
`ScreenCaptureRepository`, `ScreenCaptureService`, `ScreenCaptureActivity`, manifest.

## Step 67
`CaptureRegion` + `RegionSelectorController` + tests.

## Step 68
`CaptureTemplate` + `TemplateManager` + 18 JVM tests.

## Step 69
`MatchResult` + `TemplateMatcher` + 8 JVM tests.

## Step 70
`ImageTargetResult` + `ImageTargetOutcome` + `ImageTargetController` + 10 JVM tests.

## Step 71
`OcrTextRegion` + `OcrResult` + `OcrProvider` + `StubOcrProvider` + `OcrController` + 12 JVM tests.

## Step 72
`TextTargetResult` + `TextTargetOutcome` + `TextTargetController` + 11 JVM tests.

## Step 73
`ScenarioPreset` + `BuiltInPresets` + `VisualScenarioBuilder` + 13 JVM tests.

## Step 74
`ControlledTapSession` + `ControlledTapSessionManager` + 14 JVM tests.

## Step 75

Smart target → single real tap with explicit consent.

- `realtap/SmartTargetTapRequest.kt`:
  - `data class SmartTargetTapRequest(sessionId, targetType, highlightRegion, requestedAtMs)`.
    `tapX` / `tapY` = `highlightRegion.centerX/Y`. `isValid` = region valid.
  - `enum SmartTargetType { IMAGE_TARGET, TEXT_TARGET }`.
  - `sealed SmartTargetTapResult { Dispatched(request, tapNumber), Blocked(request?, reason) }`.
  - `enum SmartTargetBlockReason { INVALID_REQUEST, NO_ACTIVE_SESSION, SESSION_GATE_CLOSED,
    CONSENT_MISSING, CONSENT_EXPIRED, MARKER_DRIFT }`.
- `realtap/SmartTargetTapController.kt`:
  - `recordConsent(request)`, `clearConsent()`, `consent: SmartTargetConsent?`.
  - `dispatch(request?)`: 5-check chain (validate → session → evaluateTap → consent present
    → consent fresh + coords match) → `recordTap` + `clearConsent` → `Dispatched`.
  - `CONSENT_TTL_MS = 10_000L`, `COORD_TOLERANCE = 0.02f`.
  - `data class SmartTargetConsent(request, recordedAtMs)`.
- Test: `SmartTargetTapControllerTest.kt` — 12 JVM tests.
- `AppInfo.STEP` → Step 75.
- Invariants: `canRunRealTap()` = `false`; `evaluateTap()` → GATE_CLOSED; `dispatch()` →
  SESSION_GATE_CLOSED until Step 76. No `dispatchGesture`, no I/O.
