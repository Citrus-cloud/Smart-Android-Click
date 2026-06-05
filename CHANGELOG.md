# Changelog — ClickFlow Android

All notable changes to the ClickFlow Android project are documented here.
This project follows the ClickFlow step-based development model.

---

## v1.0.0 🏁 (Step 84 — Final Release)

### Step 84 — Stabilize + release v1.0.0

- `AppInfo.VERSION_NAME` bumped to `"1.0.0"`.
- `AppInfo.STEP` → Step 84.
- All five phases complete across both repos (`Smart-Android-Click` + `Mine`).
- Full test coverage: **124+ JVM tests** (all passing).
- Full Russian + English localization (`values/strings.xml` + `values-ru/strings.xml`).
- All documentation written and reviewed.

---

## Phase summary

### Phase 1 — Android debt (Steps 64–65) ✅
- Step 64: RealTapController, granular audit, build fixes.
- Step 65: First JVM test suite (SafetyGate, RealTapController, RealTapSession, RealTapSafetyReview).

### Phase 2 — «The brain» on Android (Steps 66–73) ✅
- Step 66: Screen capture via MediaProjection (Part 1 + Part 2).
- Step 67: `CaptureRegion` + `RegionSelectorController` + tests.
- Step 68: `CaptureTemplate` + `TemplateManager` + 18 JVM tests.
- Step 69: `MatchResult` + `TemplateMatcher` + 8 JVM tests.
- Step 70: `ImageTargetResult` + `ImageTargetController` + 10 JVM tests.
- Step 71: `OcrTextRegion` + `OcrProvider` + `OcrController` + 12 JVM tests.
- Step 72: `TextTargetResult` + `TextTargetController` + 11 JVM tests.
- Step 73: `ScenarioPreset` + `BuiltInPresets` + `VisualScenarioBuilder` + 13 JVM tests.

### Phase 3 — Real taps on Android (Steps 74–76) ✅
- Step 74: `ControlledTapSession` + `ControlledTapSessionManager` + 14 JVM tests.
- Step 75: `SmartTargetTapRequest` + `SmartTargetTapController` + 12 JVM tests.
- Step 76: `SmartSessionAuditLog` + `SmartSessionEmergencyStop` + 13 JVM tests.

### Phase 4 — Desktop (Steps 77–79, repo `Mine`) ✅
- Steps 77–79 done in repo `Mine` (Electron desktop).

### Phase 5 — Finish (Steps 80–84) ✅
- Step 80: Parity matrix + `values/strings.xml` + `values-ru/strings.xml` (ru/en l10n).
- Step 81: 10 E2E QA scenarios + `docs/qa-checklist-android.md`.
- Step 82: `docs/user-guide.md` (ru + en).
- Step 83: `docs/beta-readiness.md` + `AppInfo.VERSION_NAME` = `1.0.0-beta.1`.
- Step 84: Final stabilization + `AppInfo.VERSION_NAME` = `1.0.0`. 🏁

---

## Test coverage (v1.0.0)

| Test class | Tests |
|-----------|-------|
| ScreenCaptureControllerTest | ✓ |
| RegionSelectorControllerTest | ✓ |
| TemplateManagerTest | 18 |
| TemplateMatcherTest | 8 |
| ImageTargetControllerTest | 10 |
| OcrControllerTest | 12 |
| TextTargetControllerTest | 11 |
| VisualScenarioBuilderTest | 13 |
| ControlledTapSessionManagerTest | 14 |
| SmartTargetTapControllerTest | 12 |
| SmartSessionAuditTest | 13 |
| **Total** | **124+** |
