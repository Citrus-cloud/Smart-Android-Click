# QA Checklist — ClickFlow Android v1.0.0

## Automated (JVM)

- [ ] `./gradlew testDebugUnitTest` — all tests pass.
- [ ] `ScreenCaptureControllerTest` — passes.
- [ ] `RegionSelectorControllerTest` — passes.
- [ ] `TemplateManagerTest` (18 tests) — passes.
- [ ] `TemplateMatcherTest` (8 tests) — passes.
- [ ] `ImageTargetControllerTest` (10 tests) — passes.
- [ ] `OcrControllerTest` (12 tests) — passes.
- [ ] `TextTargetControllerTest` (11 tests) — passes.
- [ ] `VisualScenarioBuilderTest` (13 tests) — passes.
- [ ] `ControlledTapSessionManagerTest` (14 tests) — passes.
- [ ] `SmartTargetTapControllerTest` (12 tests) — passes.
- [ ] `SmartSessionAuditTest` (13 tests) — passes.
- [ ] **Total: 124+ JVM tests pass.**

## Manual (device)

- [ ] App installs from APK without errors (Android 8+).
- [ ] App launches, no crashes on startup.
- [ ] Safety Center UI shows all 4 gate checks.
- [ ] Emergency Stop button visible and responsive.
- [ ] String resources display in Russian on ru-locale device.
- [ ] String resources display in English on en-locale device.
- [ ] Screen capture permission dialog appears.
- [ ] Region selector draws region correctly.
- [ ] Template add/edit/delete works.
- [ ] Scenario builder: add TAP / WAIT / NOTE, reorder, delete.
- [ ] Preset TAP_CENTER and TAP_AND_WAIT load without error.
- [ ] Audit log records events (verify via debug UI or logcat).
- [ ] Emergency stop terminates active session and clears consent.

## Release

- [ ] `AppInfo.VERSION_NAME` = `"1.0.0"`.
- [ ] APK builds via `./gradlew assembleDebug` without errors.
- [ ] `AppInfo.STEP` reflects final step.
- [ ] Git tag `v1.0.0` pushed.
- [ ] GitHub Release created with APK attached.
