# Changelog — ClickFlow Android

All notable changes to the ClickFlow Android project are documented here.
This project follows the ClickFlow step-based development model.

## [Unreleased]

### Step 72 — Text-target scenario controller (preview/simulation, no tap)

Phase 2 continues: wires the Step 71 `OcrController` into a single
`TextTargetController.evaluate` call that runs OCR, finds the best text
match, and returns a typed `TextTargetOutcome` with a highlighted region.
Pure Kotlin, no Android imports, no tap dispatch.

- New `capture/TextTargetResult.kt` — two types:
  - `TextTargetResult(query, matched, highlight?, matchedText?, confidence,
    evaluatedAtMs, errorReason?)` — flat data class with the decision outcome.
  - `sealed class TextTargetOutcome { Matched(result), NoMatch(result),
    Error(result) }` — typed wrapper; `Error.reason` exposes stable code
    (`empty_query`).
- New `capture/TextTargetController.kt` — wires `OcrController` + injected clock:
  - `evaluate(query, candidates, caseSensitive)` — validates query (non-blank),
    calls `OcrController.recognize(candidates)`, then `OcrController.bestMatch`;
    returns `Error` for blank query, `Matched` / `NoMatch` otherwise. Confidence
    clamped to `[0, 1]`. No Android imports.
- New `app/src/test/java/com/clickflow/android/capture/TextTargetControllerTest.kt`
  — 11 JVM tests: query found → Matched, highlight is highest-confidence bounds,
  query not found → NoMatch, empty query → Error, blank query → Error,
  case-insensitive by default, case-sensitive miss, case-sensitive hit,
  `evaluatedAtMs` from provider, query preserved in result, NoMatch null
  highlight + zero confidence.
- `AppInfo.STEP` bumped to Step 72.

Safety / privacy invariants: result is a preview only — no tap, no pixels stored,
no network; `SafetyGate.canRunRealTap()` unchanged (`false`).

### Step 71 — On-device OCR stub

- `OcrTextRegion` + `OcrResult` + `interface OcrProvider` + `StubOcrProvider` +
  `OcrController` (findText / findExact / bestMatch). 12 JVM tests.
  `AppInfo.STEP` → Step 71.

### Step 70 — Image-target controller

- `ImageTargetResult` + `ImageTargetOutcome` + `ImageTargetController`. 10 JVM tests.
  `AppInfo.STEP` → Step 70.

### Steps 52–69 — Foundation through template matching engine

See git history for full details.
