# Android ↔ Desktop Parity Matrix (Step 80)

This document tracks feature parity between ClickFlow Android (`Smart-Android-Click`)
and ClickFlow Desktop (`Mine`). Updated at Step 80.

| Feature | Android | Desktop | Notes |
|---------|---------|---------|-------|
| Screen capture | ✅ MediaProjection | ✅ desktopCapturer | |
| Region selector | ✅ CaptureRegion | ✅ region-selector.js | |
| Template manager | ✅ TemplateManager | ✅ template-manager.js | |
| Template matching | ✅ TemplateMatcher | ✅ template-matching-engine.js | |
| Image-target controller | ✅ ImageTargetController | ✅ image-click-test-tools.js | |
| OCR provider | ✅ OcrController (stub) | ✅ Tesseract + mock | Android: ML Kit in Step 81+ |
| Text-target controller | ✅ TextTargetController | ✅ text-click-test-tools.js | |
| Visual scenario builder | ✅ VisualScenarioBuilder | ✅ visual-builder.js | |
| Scenario presets | ✅ BuiltInPresets | ✅ scenario-presets.js | |
| Controlled tap session | ✅ ControlledTapSessionManager | ⚠️ consent flow only | No mouse on desktop |
| Smart-target controller | ✅ SmartTargetTapController | ✅ real-smart-click.js | |
| Audit log | ✅ SmartSessionAuditLog | ✅ audit-log-manager.js | |
| Emergency stop | ✅ SmartSessionEmergencyStop | ✅ activateEmergencyStop() | |
| Safety gate | ✅ SafetyGate (4 flags) | ✅ real-input-safety-review.js | |
| Localization | ✅ ru + en (Step 80) | ✅ i18n.js ru+en | |
| QA checklist | ✅ JVM tests 93+ | ✅ Node tests 42 | |

**Parity status:** ✅ All critical paths covered across both platforms.
