# ClickFlow Android

Native Android foundation for **ClickFlow** — the cross-platform click-automation project.
This is a **separate native Android application** (Kotlin + Jetpack Compose), **not** an Electron
port and **not** a runtime copy of the desktop code.

> **Status: Step 58 — Pre-alpha GitHub release PUBLISHED. Simulation-only. No real taps.**
>
> Release: **[ClickFlow Android Pre-alpha](https://github.com/Citrus-cloud/Smart-Android-Click/releases/tag/android-v0.1.0-prealpha)**
> · tag `android-v0.1.0-prealpha` (pre-release) · asset `app-debug.apk` (debug-signed) · versionName `0.1.0-prealpha`.

## Relation to desktop ClickFlow

The desktop app ([Citrus-cloud/Mine](https://github.com/Citrus-cloud/Mine)) reached
`v1.0.0-alpha.1`: Electron app, click simulation/dry-run, a hard-gated real-coordinate-click
alpha, image/text/OCR simulation, Visual Builder, Scenario Presets, Safety Center, Audit Logs,
Diagnostics — and **no** real image/text clicks, **no** keyboard automation, **no** prohibited
automation.

ClickFlow Android reuses the **product concepts and safety philosophy** of the desktop app, but is
an independent native codebase. Desktop code is **not** bundled or executed on Android.

## Current status (Step 57)

Pre-alpha **build/QA + release prep** — the debug APK now **builds cleanly**:

- Verified `./gradlew assembleDebug` with **JDK 17 + Android SDK 34** → `app-debug.apk` (debug-signed).
- Minor cleanups only (no new runtime features): version → `0.1.0-prealpha`, `Divider` →
  `HorizontalDivider`, removed an unused variable.
- Pre-alpha **release notes**, **GitHub release draft**, **tag plan**, and **build troubleshooting**
  docs added (with a verified headless toolchain recipe).
- Re-confirmed: **0 permissions, 0 providers, no real taps, simulation-only.**

Earlier work — Steps 52–56 (foundation; scenario CRUD + storage; multi-step + audit; profiles +
persistent/exportable audit; backup import/export) — remains intact.

## Build

```bash
./gradlew assembleDebug          # Windows: gradlew.bat assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk  (debug-signed, versionName 0.1.0-prealpha)
```

Requires JDK 17 + Android SDK (`platforms;android-34`, `build-tools;34.0.0`). See
[docs/ANDROID_BUILD_TROUBLESHOOTING.md](docs/ANDROID_BUILD_TROUBLESHOOTING.md).

**Still not implemented (deferred to future safety-reviewed steps):** real taps, Accessibility
Service automation, `dispatchGesture`, MediaProjection capture, overlay logic, runtime permission
requests.

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
| versionName | 1.0.0-alpha.1 |
| Stack | Kotlin, Gradle (Kotlin DSL), Jetpack Compose, Material 3 |

## Documentation

- [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md)
- [CHANGELOG.md](CHANGELOG.md)
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

- **No real taps yet** — `realTapsEnabled = false`, `simulationOnly = true`, hard-coded.
- No hidden/background automation; no captcha/anti-bot bypass; no ad-click, banking, or payment
  automation; no spyware/keyloggers/input hooks; no root-only features.
- Future real taps will require Accessibility Service **plus** explicit user consent **plus** safety
  gates **plus** an emergency stop. See [docs/ANDROID_SAFETY_MODEL.md](docs/ANDROID_SAFETY_MODEL.md).

## License

MIT (aligned with the desktop ClickFlow project).
