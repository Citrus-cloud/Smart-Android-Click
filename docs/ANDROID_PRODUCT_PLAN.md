# Android Product Plan — ClickFlow Android

## Goal

Bring ClickFlow's safety-first click-automation concept to Android as a standalone native app,
introducing capabilities incrementally and only behind explicit consent and safety gates. Step 52
establishes the foundation; real automation is deliberately deferred.

## Android MVP scope

The Android MVP (across multiple future steps) aims for:

- Author and manage simulation scenarios.
- A safety center that transparently reports what the app can and cannot do.
- Diagnostics for support and trust.
- Eventually: real taps via Accessibility Service, gated behind consent + safety review.
- Eventually: optional screen analysis via MediaProjection, gated behind consent.

## What is included in Step 52

- Native Kotlin + Jetpack Compose app shell.
- Navigation across Home, Scenarios, Safety Center, Diagnostics.
- Simulation-only scenario model (`simple_tap_simulation`).
- Simulation engine with an explicit status machine.
- Safety foundation with real taps hard-disabled.
- Diagnostics foundation.
- RU/EN localization.
- Documentation and a debug APK build path.

## What is NOT included in Step 52

- Real tap execution.
- Accessibility Service automation.
- MediaProjection screen capture.
- Overlay permission runtime logic.
- Runtime permission requests.
- Any prohibited automation.

## Future real tap architecture

Real taps will be implemented through an Android **AccessibilityService** using
`dispatchGesture(...)`, never through injected input events or root. Activation will require:

1. The user manually enabling the Accessibility Service in system settings.
2. An explicit in-app consent screen describing exactly what will happen.
3. Safety gates (`canRunRealTap()`), feature flags, and an always-available Emergency Stop.
4. A completed safety review (go/no-go), mirroring the desktop approach.

See [ANDROID_ACCESSIBILITY_PLAN.md](ANDROID_ACCESSIBILITY_PLAN.md).

## Safety model

See [ANDROID_SAFETY_MODEL.md](ANDROID_SAFETY_MODEL.md). Core principle: simulation-only by default;
every real-world capability is opt-in, consented, gated, reversible, and interruptible.

## Relation to desktop ClickFlow

Shares product philosophy and safety posture with the Electron desktop app
([Citrus-cloud/Mine](https://github.com/Citrus-cloud/Mine)); shares no runtime code. The Android app
is a separate native codebase.

## APK release plan

- Step 52: debug APK only (`assembleDebug`), debug-signed.
- Future: release signing and store distribution only after functionality + safety review.
