# ClickFlow Android

Native Android foundation for **ClickFlow** — the cross-platform click-automation project.
This is a **separate native Android application** (Kotlin + Jetpack Compose), **not** an Electron
port and **not** a runtime copy of the desktop code.

> **Status: Step 62 — Single real-tap prototype (gated, audited, per-tap consent). In progress.**
>
> The domain layer (controller, gate extension, session/review/consent models, audit constants,
> diagnostics mirror, EN+RU strings) and the design docs are committed.
> ViewModel APIs, the `RealTapPrototypeScreen` composable, navigation routing, and the SafetyCenter
> prototype item are still pending. Bulk, looped, and scenario-driven real taps remain hard-disabled
> by `SafetyGate.canRunRealTap() == false`. The single-tap path is gated behind a per-session
> Safety Review, a per-tap consent (10s TTL, single-use nonce), API ≥ 24, and a bound
> Accessibility Service.
>
> The latest published APK is still the Step 60 build at the pre-release
> [`android-v0.1.0-prealpha`](https://github.com/Citrus-cloud/Smart-Android-Click/releases/tag/android-v0.1.0-prealpha)
> (Simple Clicker UX, simulation-only).

## Relation to desktop ClickFlow

The desktop app ([Citrus-cloud/Mine](https://github.com/Citrus-cloud/Mine)) reached
`v1.0.0-alpha.1`: Electron app, click simulation/dry-run, a hard-gated real-coordinate-click
alpha, image/text/OCR simulation, Visual Builder, Scenario Presets, Safety Center, Audit Logs,
Diagnostics — and **no** real image/text clicks, **no** keyboard automation, **no** prohibited
automation.

ClickFlow Android reuses the **product concepts and safety philosophy** of the desktop app, but is
an independent native codebase. Desktop code is **not** bundled or executed on Android.

## Current status (Step 62 in progress; Steps 52–61 intact)

**Step 62 — Single real-tap prototype (in progress):**

- introduces a **narrow, audited path** for *exactly one real tap per explicit consent* via
the Step 61 `ClickFlowAccessibilityService`;
- bulk / looped / scenario-driven real taps remain blocked (`SafetyGate.canRunRealTap()` still
returns `false`); the scenario runner is untouched;
- four independent gates required for a single tap: Safety Review passed (per session, in-memory
only) + session active + consent fresh (10s TTL, single-use nonce) + API ≥ 24 with bound service;
- every state transition emits a `realtap.*` audit event; review/session/consent state is never
persisted, never exported, never backed up;
- Emergency Stop ends the session and invalidates any pending consent.

See [docs/REAL_TAP_PROTOTYPE.md](docs/REAL_TAP_PROTOTYPE.md),
[docs/SAFETY_REVIEW_CHECKLIST.md](docs/SAFETY_REVIEW_CHECKLIST.md), and
[docs/CONSENT_FLOW.md](docs/CONSENT_FLOW.md).

**Step 61 — Permissions skeleton (overlay + accessibility service, no automation):**

- read-only `PermissionsManager` (overlay via `Settings.canDrawOverlays()`, accessibility via
`Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`);
- declared `ClickFlowAccessibilityService` as a **no-op** (no event subscriptions, no
`dispatchGesture` until Step 62, no window content reads);
- declared `SYSTEM_ALERT_WINDOW` in the manifest (opt-in via system settings); no autostart, no
`<receiver>`, no scheduling.

**Earlier steps (52–60) remain intact:** project foundation, scenario CRUD + storage,
multi-step + audit log, profiles + persistent/exportable audit, backup import/export,
pre-alpha build + published release, Simple Clicker UX with draggable marker.

## Build

```bash
./gradlew assembleDebug          # Windows: gradlew.bat assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk  (debug-signed, versionName 0.1.0-prealpha)
```

Requires JDK 17 + Android SDK (`platforms;android-34`, `build-tools;34.0.0`). See
[docs/ANDROID_BUILD_TROUBLESHOOTING.md](docs/ANDROID_BUILD_TROUBLESHOOTING.md).

**Still not implemented (deferred to future safety-reviewed steps):** real input via the
scenario runner (bulk / loop / scheduled), MediaProjection capture, system overlay rendering,
release signing.

## Build the debug APK

Requires JDK 17 and the Android SDK (compileSdk 34, build-tools, platform-34).

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

The APK is **debug-signed only** — never release-signed. See
[docs/APK_BUILD_GUIDE.md](docs/APK_BUILD_GUIDE.md).

## Run

Install the debug APK on a device/emulator (API 26+):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then: launch → **Start simulation** → **Stop** → **Emergency Stop** → open **Safety Center** and
**Diagnostics**. See [scripts/android-smoke.md](scripts/android-smoke.md).

## App identity

| | |
|---|---|
| App name | ClickFlow Android |
| Package | `com.clickflow.android` |
| minSdk | 26 |
| targetSdk / compileSdk | 34 |
| versionName | 0.1.0-prealpha |
| versionCode | 1 |
| Stack | Kotlin, Gradle (Kotlin DSL), Jetpack Compose, Material 3 |

## Documentation

- [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md)
- [CHANGELOG.md](CHANGELOG.md)
- [docs/REAL_TAP_PROTOTYPE.md](docs/REAL_TAP_PROTOTYPE.md) — Step 62 architecture + invariants
- [docs/SAFETY_REVIEW_CHECKLIST.md](docs/SAFETY_REVIEW_CHECKLIST.md) — the 10-item review (EN + RU)
- [docs/CONSENT_FLOW.md](docs/CONSENT_FLOW.md) — per-tap consent lifecycle, TTL/nonce contract
- [docs/ANDROID_MANUAL_RELEASE_ASSET_UPDATE.md](docs/ANDROID_MANUAL_RELEASE_ASSET_UPDATE.md) — Step 60 release-asset refresh guide
- [docs/ANDROID_SIMPLE_CLICKER_UX.md](docs/ANDROID_SIMPLE_CLICKER_UX.md)
- [docs/ANDROID_MARKER_MODEL.md](docs/ANDROID_MARKER_MODEL.md)
- [docs/ANDROID_PRE_ALPHA_RELEASE_NOTES.md](docs/ANDROID_PRE_ALPHA_RELEASE_NOTES.md)
- [docs/ANDROID_PRE_ALPHA_RELEASE_DRAFT.md](docs/ANDROID_PRE_ALPHA_RELEASE_DRAFT.md)
- [docs/ANDROID_PRE_ALPHA_TAG_PLAN.md](docs/ANDROID_PRE_ALPHA_TAG_PLAN.md)
- [docs/ANDROID_BUILD_TROUBLESHOOTING.md](docs/ANDROID_BUILD_TROUBLESHOOTING.md)
- [docs/ANDROID_BACKUP_EXPORT.md](docs/ANDROID_BACKUP_EXPORT.md)
- [docs/ANDROID_BACKUP_IMPORT.md](docs/ANDROID_BACKUP_IMPORT.md)
- [docs/ANDROID_PRE_ALPHA_QA.md](docs/ANDROID_PRE_ALPHA_QA.md)
- [docs/ANDROID_PRE_ALPHA_RELEASE_CHECKLIST.md](docs/ANDROID_PRE_ALPHA_RELEASE_CHECKLIST.md)
- [docs/ANDROID_PROFILES.md](docs/ANDROID_PROFILES.md)
- [docs/ANDROID_AUDIT_PERSISTENCE.md](docs/ANDROID_AUDIT_PERSISTENCE.md)
- [docs/ANDROID_EXPORT_MODEL.md](docs/ANDROID_EXPORT_MODEL.md)
- [docs/ANDROID_MULTI_STEP_SCENARIOS.md](docs/ANDROID_MULTI_STEP_SCENARIOS.md)
- [docs/ANDROID_AUDIT_LOG.md](docs/ANDROID_AUDIT_LOG.md)
- [docs/ANDROID_SCENARIO_STORAGE.md](docs/ANDROID_SCENARIO_STORAGE.md)
- [docs/ANDROID_SCENARIO_UI.md](docs/ANDROID_SCENARIO_UI.md)
- [docs/ANDROID_PRODUCT_PLAN.md](docs/ANDROID_PRODUCT_PLAN.md)
- [docs/ANDROID_SAFETY_MODEL.md](docs/ANDROID_SAFETY_MODEL.md)
- [docs/ANDROID_ARCHITECTURE.md](docs/ANDROID_ARCHITECTURE.md)
- [docs/ANDROID_PERMISSIONS_PLAN.md](docs/ANDROID_PERMISSIONS_PLAN.md)
- [docs/ANDROID_ACCESSIBILITY_PLAN.md](docs/ANDROID_ACCESSIBILITY_PLAN.md)
- [docs/ANDROID_MEDIAPROJECTION_PLAN.md](docs/ANDROID_MEDIAPROJECTION_PLAN.md)
- [docs/ANDROID_MVP_CHECKLIST.md](docs/ANDROID_MVP_CHECKLIST.md)
- [docs/APK_BUILD_GUIDE.md](docs/APK_BUILD_GUIDE.md)

## Safety model (summary)

- **Bulk real taps remain disabled** — `SafetyGate.canRunRealTap()` returns `false` (hard-coded).
The scenario runner has no access to any real-input path.
- **The single-tap prototype** (Step 62) is gated behind four independent checks: per-session
Safety Review, active session, fresh single-use consent (10s TTL), and a bound accessibility
service on API ≥ 24. Every transition is audited.
- No hidden/background automation; no captcha/anti-bot bypass; no ad-click, banking, or payment
automation; no spyware/keyloggers/input hooks; no root-only features.
- See [docs/ANDROID_SAFETY_MODEL.md](docs/ANDROID_SAFETY_MODEL.md),
[docs/REAL_TAP_PROTOTYPE.md](docs/REAL_TAP_PROTOTYPE.md),
[docs/SAFETY_REVIEW_CHECKLIST.md](docs/SAFETY_REVIEW_CHECKLIST.md),
[docs/CONSENT_FLOW.md](docs/CONSENT_FLOW.md).

## License

MIT (aligned with the desktop ClickFlow project).
