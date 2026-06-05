# Changelog — ClickFlow Android

All notable changes to the ClickFlow Android project are documented here.
This project follows the ClickFlow step-based development model.

## [Unreleased]

### Step 70 — Image-target scenario controller (preview/simulation, no tap)

Phase 2 continues: wires the Step 68 `TemplateManager` and Step 69 `TemplateMatcher`
into a single `ImageTargetController` that runs an end-to-end image-target lookup
and returns a typed outcome with a highlighted region. Pure Kotlin, no bitmaps,
no Android APIs, no tap dispatch.

- New `capture/ImageTargetResult.kt` — two types:
  - `ImageTargetResult(templateId, matched, confidence, highlight?, evaluatedAtMs, errorReason?)`
    — flat data class carrying the decision outcome.
  - `sealed class ImageTargetOutcome { Matched(result), NoMatch(result), Error(result) }` —
    typed wrapper; `Error.reason` shortcut exposes the stable reason code
    (`template_not_found`).
- New `capture/ImageTargetController.kt` — wires `TemplateManager` + `TemplateMatcher`:
  - `evaluate(templateId, candidates)` — looks up the template, calls
    `TemplateMatcher.evaluateBest`, maps the `MatchResult` to a typed
    `ImageTargetOutcome`. Returns `Error` when the template is not registered;
    `Matched` / `NoMatch` otherwise. No Android imports.
- Updated `capture/TemplateMatcher.kt` — added public `nowMs()` helper so
  `ImageTargetController` can stamp error results with the same injected clock.
- New `app/src/test/java/com/clickflow/android/capture/ImageTargetControllerTest.kt`
  — 10 JVM tests: match above threshold, score below threshold, exactly at threshold,
  empty candidates → NoMatch, unknown templateId → Error, best candidate picked from
  multiple, `evaluatedAtMs` from `nowProvider`, raw score > 1 clamped, negative score
  clamped → NoMatch, Error has null highlight.
- `AppInfo.STEP` bumped to Step 70.

Safety / privacy invariants: the controller operates on injected float scores only —
no pixels, no capture, no tap; `SafetyGate.canRunRealTap()` unchanged (`false`).
The real image-similarity provider (OpenCV / ML Kit) that will supply the
`MatchCandidate` list lands in a later step.

### Step 69 — Template matching engine (decision layer, no pixels, no tap)

Phase 2 continues: a pure-Kotlin decision layer that takes raw candidate scores
(floats) from a future matcher and decides whether each score constitutes a
"match" for a given `CaptureTemplate`, with confidence normalization and
highlighting helpers. No bitmaps, no Android APIs, no tap dispatch.

- New `capture/MatchResult.kt` — two data classes:
  - `MatchCandidate(location: CaptureRegion, rawScore: Float)` — one candidate
    position + its raw similarity score.
  - `MatchResult(templateId, confidence, matched, location?, evaluatedAtMs)` —
    the decision outcome; `confidence` is clamped to `[0, 1]`, `matched` is
    `confidence ≥ template.matchThreshold`. Exposes a `.highlight` property
    (returns `location` when matched, `null` otherwise) and a companion
    `noMatch(templateId, evaluatedAtMs)` factory.
- New `capture/TemplateMatcher.kt` — pure-Kotlin decision layer with injected
  `nowProvider: () -> Long`:
  - `evaluate(template, candidate)` — scores one candidate against one template;
    clamps raw score to `[0, 1]`, compares to `matchThreshold`, returns a
    `MatchResult`.
  - `evaluateBest(template, candidates)` — picks the candidate with the highest
    raw score; returns `MatchResult.noMatch(...)` for an empty list.
  - `matchesOnly(evaluations)` — filters to matched results, sorted by confidence
    descending. No Android imports.
- New `app/src/test/java/com/clickflow/android/capture/TemplateMatcherTest.kt`
  — 8 JVM tests: match above threshold, no match below threshold, match exactly
  at threshold, confidence clamped to [0,1], `evaluateBest` picks highest score,
  `evaluateBest` empty list → noMatch, `evaluatedAtMs` from injected provider,
  `matchesOnly` filters and sorts.
- `AppInfo.STEP` bumped to Step 69.

Safety / privacy invariants: the matcher operates on raw float scores only — no
pixels are held, no capture runs, no tap is dispatched; `SafetyGate.canRunRealTap()`
is untouched and still returns `false`. The actual image-similarity computation
(OpenCV / ML Kit) and the on-screen highlight overlay land in Step 70.

### Step 68 — Template Manager (in-memory registry of capture templates)

Phase 2 continues: models WHAT the matching engine (Step 69+) will look for —
named "templates" with their parameters — without storing any pixels, touching
disk, or running capture / analysis. Pure Kotlin logic + a full JVM unit suite,
following the proven Step 66 Part 1 / Step 67 "brain first, I/O later" pattern.

- New `capture/CaptureTemplate.kt` — immutable metadata for one reference target:
`id`, `name`, the normalized `CaptureRegion` it was taken from, reference
`widthPx` / `heightPx` (metadata only — no pixels held), a `matchThreshold`
clamped to `[0.1, 1.0]`, and an injected `createdAtMs`. Exposes `isValid`,
`aspectRatio`, and copy-helpers `withName` / `withThreshold` / `withRegion`.
- New `capture/TemplateManager.kt` — `@Synchronized` in-memory registry keyed by
stable sequential ids (`tpl-1`, `tpl-2`, …). `add` trims + validates the name
(non-empty, ≤ 60 chars, case-insensitive unique), the region (`isValid`) and the
size (> 0), clamps the threshold and clamps the region to the unit square;
`rename`, `setThreshold`, `setRegion`, `remove`, `clear`, plus read-only `list` /
`count` / `isEmpty` / `get` / `contains`. Every mutation returns a
`Result.Ok(template)` / `Result.Error(reason)` with stable reason codes
(`empty_name`, `name_too_long`, `invalid_region`, `invalid_size`,
`duplicate_name`, `not_found`). No Android imports.
- New `app/src/test/java/com/clickflow/android/capture/TemplateManagerTest.kt` —
JVM tests: empty start, add / trim / validate (empty name, bad size, invalid
region, duplicate name, threshold clamp), sequential unique ids, rename (success,
duplicate rejection, rename-to-own-name, not-found), threshold clamp, region
update, remove, and clear.
- `AppInfo.STEP` bumped to Step 68.

Safety / privacy invariants: a template is metadata + geometry only — it
references no pixels, performs no capture and no analysis, and is never persisted
by this step; `SafetyGate.canRunRealTap()` is untouched and still returns `false`.
The capture of an actual reference bitmap, on-device matching, and the management
UI land in later steps (Step 69+).

### Step 67 — Region Selector (normalized CaptureRegion + selection state machine)

Phase 2 continues: lets the user mark WHERE on a captured frame a future match
should look, without capturing or analyzing anything. Pure geometry + a
framework-free selection state machine, fully unit-tested on the JVM (the proven
Step 66 Part 1 pattern).

- New `capture/CaptureRegion.kt` — resolution-independent rectangle in normalized
[0, 1] fractions (left / top / right / bottom). Exposes `width` / `height` /
`area` / `centerX` / `centerY`, `isValid`, `contains(x, y)`, `clampedToUnit()`,
`toPixels(w, h)`, plus companion `FULL` and `fromCorners(...)` which orders the
edges so a drag in any direction yields a valid rectangle.
- New `capture/RegionSelectorController.kt` — `@Synchronized` state machine over
`RegionSelectorState { phase (EMPTY / DRAGGING / SELECTED), region, error }`:
`beginSelection` → DRAGGING point-rect at the clamped anchor; `updateSelection`
rebuilds the rectangle from anchor → current corner; `commitSelection` rejects a
sub-2% region (`region_too_small`, stays DRAGGING) else → SELECTED; `setRegion`
selects a preset / full-frame region (clamped); `clear` → EMPTY. No Android
imports.
- New `app/src/test/java/com/clickflow/android/capture/RegionSelectorControllerTest.kt`
— initial empty, begin → dragging point-rect, drag-direction normalization,
update-without-begin rejected, commit-too-small stays dragging, commit-valid
selects + `contains` checks, `setRegion` clamp + too-small rejection, clear
resets, and `CaptureRegion` geometry / `toPixels` math.
- `AppInfo.STEP` bumped to Step 67.

Safety / privacy invariants: a region is pure geometry — it references no pixels,
performs no capture and no analysis, and is never persisted by this step;
`SafetyGate.canRunRealTap()` is untouched and still returns `false`. The Compose
drag-to-select overlay over the captured frame and its wiring into the capture
screen land in a later UI pass.

### Step 66 (Part 2) — Real MediaProjection screen capture (service + consent activity + UI)

Completes Step 66: drives the Part 1 `ScreenCaptureController` from a real
MediaProjection pipeline. Capture only — a single frame is read for its
dimensions in memory and immediately discarded; nothing is written to disk,
exported, or analyzed.

- New `app/src/main/java/com/clickflow/android/capture/`:
- `ScreenCaptureRepository.kt` — process-local `object` that wraps a single
`ScreenCaptureController` and republishes its state as a
`StateFlow<ScreenCaptureState>` so Compose can observe it. Holds no pixels and
no Android Context; bridges the service (producer) and the UI (consumer).
- `ScreenCaptureService.kt` — foreground `Service`
(`foregroundServiceType=mediaProjection`). Receives the consent `resultCode` +
data Intent, builds `MediaProjection` → `ImageReader` → `VirtualDisplay`,
captures exactly one frame, reads width/height only, mirrors it via
`ScreenCaptureRepository.onFrameCaptured(...)`, then tears the whole pipeline
down. Registers a `MediaProjection.Callback` before creating the virtual
display (Android 14+ requirement) and posts a low-importance ongoing
notification while active. Never writes a frame to disk.
- `ScreenCaptureActivity.kt` — dedicated `ComponentActivity` owning the consent
flow via `registerForActivityResult(StartActivityForResult)` +
`MediaProjectionManager.createScreenCaptureIntent()`. On consent it starts the
foreground service; on denial it records `onPermissionResult(false)`. Compose
UI observes the repository state and shows status / permission / frame
dimensions + the three privacy-invariant flags, plus Start / Stop / Reset /
Back.
- `AndroidManifest.xml` — added `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_MEDIA_PROJECTION`, `POST_NOTIFICATIONS`; declared
`ScreenCaptureActivity` (`exported=false`) and `ScreenCaptureService`
(`exported=false`, `foregroundServiceType=mediaProjection`). Still no `INTERNET`
or storage permissions.
- `ui/Screens.kt` — `AdvancedScreen` gains a screen-capture entry that launches
`ScreenCaptureActivity`.
- `AppInfo.STEP` bumped to Step 66 (Part 2).

Safety / privacy invariants: a captured frame lives only in RAM and is never
written to disk, exported, or analyzed (no OCR / template matching in this
step); `SafetyGate.canRunRealTap()` is untouched and still returns `false`.

### Step 66 (Part 1) — Screen-capture lifecycle controller (pure Kotlin)

First slice of Phase 2 ("move the brain to Android"). Introduces the
framework-free state machine that the real MediaProjection pipeline (Part 2)
will drive. No Android APIs here, so it runs under `./gradlew testDebugUnitTest`
alongside the Step 65 suite.

- New `app/src/main/java/com/clickflow/android/capture/`:
- `ScreenCaptureState.kt` — `CapturePermission` (NOT_REQUESTED / GRANTED /
DENIED), `CaptureStatus` (IDLE / AWAITING_PERMISSION / CAPTURING / FRAME_READY /
STOPPED / ERROR), and `CaptureFrame` (metadata only: width, height,
capturedAtMs — holds no pixels). The state carries the invariant flags
`inMemoryOnly = true`, `persistedToDisk = false`, `analysisPerformed = false`.
- `ScreenCaptureController.kt` — `@Synchronized` state machine:
`requestPermission` → AWAITING_PERMISSION; `onPermissionResult(granted)` →
GRANTED or DENIED + error; `startCapture` requires GRANTED; `onFrameCaptured`
records frame metadata only (rejects a non-CAPTURING state and non-positive
dimensions); `stop` drops the in-memory frame; `onError`; `reset` preserves a
previously granted permission. Holds no Android dependencies.
- New `app/src/test/java/com/clickflow/android/capture/ScreenCaptureControllerTest.kt`
— initial state, permission request/deny, start-requires-permission,
granted→capture→in-memory frame (asserts the three privacy invariants),
frame-requires-capturing, invalid-dimension rejection, stop-drops-frame,
reset-preserves-permission, and the error transition.
- `AppInfo.STEP` bumped to Step 66 (Part 1).
- Deferred to Step 66 Part 2: the real `ScreenCaptureService`
(MediaProjection + ImageReader + VirtualDisplay capturing a single frame to
memory), the Activity consent flow (`createScreenCaptureIntent`), the
foreground-service manifest entries (`FOREGROUND_SERVICE` /
`FOREGROUND_SERVICE_MEDIA_PROJECTION` / `POST_NOTIFICATIONS`), and the capture
screen UI.

Safety / privacy invariants: a captured frame lives only in RAM and is never
written to disk; this step captures only — no OCR / template matching runs;
`SafetyGate.canRunRealTap()` is untouched and still returns `false`.

### Step 65 — RealTap JVM unit-test suite

First automated test coverage for the real-tap prototype. Pure-JVM JUnit 4 (no
Robolectric, no Android framework) so the suite runs under
`./gradlew testDebugUnitTest`.

- New `app/src/test/java/com/clickflow/android/`:
- `safety/SafetyGateTest.kt`
- `realtap/RealTapControllerTest.kt`
- `realtap/RealTapSessionTest.kt`
- `realtap/RealTapSafetyReviewTest.kt`
- `AppInfo.STEP` bumped to Step 65.

### Step 64 — RealTapController wired end-to-end + granular audit + marker invariant

- Build fix, controller wiring, granular audit, marker invariant.
- `AppInfo.STEP` bumped to Step 64.

### Steps 52–63 — Foundation through real-tap prototype hardening

See git history for full details.
