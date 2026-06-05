# Changelog — ClickFlow Android

All notable changes to the ClickFlow Android project are documented here.
This project follows the ClickFlow step-based development model.

## [Unreleased]

### Step 71 — On-device OCR stub (OcrProvider interface + StubOcrProvider + OcrController)

Phase 2 continues: introduces the OCR abstraction layer following the proven
"brain first, I/O later" pattern. No ML Kit, no Tesseract, no Android imports
at the interface level. The real engine plugs in later by implementing `OcrProvider`.

- New `capture/OcrTextRegion.kt` — two data classes:
  - `OcrTextRegion(text, bounds: CaptureRegion, confidence)` — one recognized
    text region. Exposes `isUsable` (valid bounds + confidence > 0).
  - `OcrResult(regions, pageText, recognizedAtMs)` — full OCR pass result.
    Companion factories: `OcrResult.from(regions, nowMs)` (auto-builds `pageText`
    by joining region texts with a space) and `OcrResult.empty(nowMs)`.
- New `capture/OcrProvider.kt` — two declarations:
  - `interface OcrProvider { fun recognize(candidates: List<CaptureRegion>): OcrResult }`
    — the single-method contract for all OCR engines. No Android imports.
  - `class StubOcrProvider(injectedRegions, nowProvider)` — returns the injected
    list of `OcrTextRegion`s on every call; used in tests and simulation.
- New `capture/OcrController.kt` — orchestrates provider + search:
  - `recognize(candidates)` — delegates to provider, returns `OcrResult`.
  - `findText(query, result, caseSensitive)` — substring filter over regions.
  - `findExact(query, result, caseSensitive)` — equality filter.
  - `bestMatch(query, result, caseSensitive)` — highest-confidence `findText` hit.
    Empty query always returns empty / null. No Android imports.
- New `app/src/test/java/com/clickflow/android/capture/OcrControllerTest.kt`
  — 12 JVM tests: recognize all regions, pageText concatenation, findText
  case-insensitive (multiple hits), findText case-sensitive, findText empty
  query, findText no match, findExact case-insensitive, findExact case-sensitive
  no match, bestMatch highest confidence, bestMatch null, OcrResult.empty,
  OcrTextRegion.isUsable false for invalid bounds.
- `AppInfo.STEP` bumped to Step 71.

Safety / privacy invariants: OCR operates on injected regions only — no frame
bytes, no network, no tap; `SafetyGate.canRunRealTap()` unchanged (`false`).
ML Kit / Tesseract integration lands in a later step.

### Step 70 — Image-target scenario controller (preview/simulation, no tap)

- `ImageTargetResult` + `ImageTargetOutcome` (Matched / NoMatch / Error).
- `ImageTargetController.evaluate(templateId, candidates)` wiring `TemplateManager`
  + `TemplateMatcher`. 10 JVM tests. `AppInfo.STEP` → Step 70.

### Step 69 — Template matching engine

- `MatchResult` + `TemplateMatcher` (evaluate / evaluateBest / matchesOnly).
  8 JVM tests. `AppInfo.STEP` → Step 69.

### Step 68 — Template Manager

- `CaptureTemplate` + `TemplateManager`. 18 JVM tests. `AppInfo.STEP` → Step 68.

### Step 67 — Region Selector

- `CaptureRegion` + `RegionSelectorController`. JVM tests. `AppInfo.STEP` → Step 67.

### Steps 52–66 — Foundation through screen capture

See git history for full details.
