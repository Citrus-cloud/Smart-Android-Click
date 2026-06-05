<PLACEHOLDER_EXISTING_CONTENT>

## Step 61

Read-only permissions surface for the Real Tap prototype.

- Added `PermissionsManager` that reports overlay-permission and AccessibilityService status without granting anything.
- Added `Screen.PERMISSIONS` and a Permissions screen that surfaces live status and deep-links into the system settings via `openOverlaySettings()` / `openAccessibilitySettings()`.
- `SafetyCenter` now accepts a live `PermissionStatus` snapshot; the legacy zero-arg constructor still works for pre-Step-61 callers.
- `SafetyGate.canRunRealTap()` remains hard-coded `false`. Granting either permission does NOT enable real input.
- `Diagnostics` reports `overlayEnabled` + `accessibilityEnabled`.

## Step 62

Real Tap Prototype — UI skeleton only, dispatch still blocked.

- Added `Screen.REAL_TAP_PROTOTYPE` and `RealTapPrototypeScreen.kt` (self-contained Compose screen).
- ViewModel adds `RealTapSessionState`, `RealTapConsent`, `SafetyReviewState`, six prototype APIs.
- `SafetyGate` introduces `SafetyState` + `canRunRealTapSingleProto()`. Bulk `canRunRealTap()` still false.
- Every prototype transition emits a `SAFETY_REAL_TAP_BLOCKED` audit event.
- Bulk real taps remain categorically forbidden.

## Step 63

Make SafetyGate prototype flags live and wire ViewModel + UI end-to-end.

- `SafetyGate` live mutators (`updateReviewPassed`, `updateAccessibility`, `updateSession`, `updateConsentFresh`) + `resetPrototypeFlags()`.
- ViewModel wired; `RealTapDispatchResult` enum; `safetyGateReasons` StateFlow.
- UI: result chip + collapsible "Why blocked" list.
- Docs: `REAL_TAP_QA_SCENARIOS.md`, `REAL_TAP_FIXES_LOG.md`.

## Step 64

Wire `RealTapController` end-to-end, emit granular `realtap.*` audit events, enforce marker-only invariant, fix build-breaking duplicate `SafetyState`.

- Build fix: removed inline `SafetyState` duplicate from `SafetyGate.kt`.
- ViewModel: six prototype APIs route through `RealTapController(gate, auditLog)` with granular `AuditType.REAL_TAP_*` constants.
- Marker invariant in `confirmRealTap`.
- `SafetyGate`: added `canRunControlledRealTapSession` + `getControlledSessionBlockedReasons`.
- `AppInfo.STEP` bumped to 64.

## Step 65

First automated test coverage for the real-tap prototype — pure-JVM JUnit 4.

- New tests: `safety/SafetyGateTest.kt`, `realtap/RealTapControllerTest.kt`, `realtap/RealTapSessionTest.kt`, `realtap/RealTapSafetyReviewTest.kt`.
- `AppInfo.STEP` bumped to 65.

## Step 66 (Part 1)

Phase 2 kickoff — pure, testable screen-capture lifecycle controller.

- New `capture/`: `ScreenCaptureState.kt` + `ScreenCaptureController.kt` (framework-free state machine).
- Test: `ScreenCaptureControllerTest.kt`.
- Invariants: frame is RAM-only, capture only, no analysis, `SafetyGate.canRunRealTap()` unchanged.

## Step 66 (Part 2)

Real MediaProjection pipeline.

- New `capture/`: `ScreenCaptureRepository.kt`, `ScreenCaptureService.kt`, `ScreenCaptureActivity.kt`.
- Manifest: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `POST_NOTIFICATIONS`.
- `ui/Screens.kt`: Advanced entry for screen capture.
- `AppInfo.STEP` → Step 66 (Part 2).

## Step 67

Region Selector — pure geometry + selection state machine.

- `capture/CaptureRegion.kt` — normalized [0,1] rectangle.
- `capture/RegionSelectorController.kt` — EMPTY / DRAGGING / SELECTED state machine.
- Test: `RegionSelectorControllerTest.kt`.
- `AppInfo.STEP` → Step 67.

## Step 68

Template Manager — in-memory registry of named capture templates.

- `capture/CaptureTemplate.kt` — immutable metadata (id, name, region, widthPx, heightPx, matchThreshold, createdAtMs).
- `capture/TemplateManager.kt` — `@Synchronized` registry, sequential ids, `Result.Ok` / `Result.Error`.
- Test: `TemplateManagerTest.kt` (18 JVM tests).
- `AppInfo.STEP` → Step 68.

## Step 69

Template Matching Engine — pure-Kotlin decision layer (float scores, no bitmaps).

- `capture/MatchResult.kt` — `MatchCandidate(location, rawScore)` + `MatchResult(templateId, confidence, matched, location?, evaluatedAtMs)` with `.highlight` and `noMatch` factory.
- `capture/TemplateMatcher.kt` — `evaluate` / `evaluateBest` / `matchesOnly`, injected `nowProvider`.
- Test: `TemplateMatcherTest.kt` (8 JVM tests).
- `AppInfo.STEP` → Step 69.
- Invariants: float-score decisions only — no pixels, no capture, no tap; `SafetyGate.canRunRealTap()` unchanged.

## Step 70

Image-target scenario controller — wires `TemplateManager` + `TemplateMatcher` into a typed lookup with a highlighted region (preview/simulation, no tap).

- `capture/ImageTargetResult.kt` — `ImageTargetResult(templateId, matched, confidence, highlight?, evaluatedAtMs, errorReason?)` + `sealed class ImageTargetOutcome { Matched, NoMatch, Error }`.
- `capture/ImageTargetController.kt` — `evaluate(templateId, candidates)`: looks up template, calls `TemplateMatcher.evaluateBest`, returns typed `ImageTargetOutcome`. Error code: `template_not_found`.
- `capture/TemplateMatcher.kt` — added public `nowMs()` helper for error-result stamping.
- Test: `ImageTargetControllerTest.kt` (10 JVM tests).
- `AppInfo.STEP` → Step 70.
- Invariants: injected float scores only — no pixels, no tap; `SafetyGate.canRunRealTap()` unchanged (`false`). Real image-similarity provider lands in a later step.
