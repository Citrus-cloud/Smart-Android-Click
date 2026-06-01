# ClickFlow Android

Native Android foundation for **ClickFlow** — the cross-platform click-automation project.
This is a **separate native Android application** (Kotlin + Jetpack Compose), **not** an Electron
port and **not** a runtime copy of the desktop code.

> **Status: Step 53 — Scenario UI + Local Storage. Simulation-only. No real taps.**

## Relation to desktop ClickFlow

The desktop app ([Citrus-cloud/Mine](https://github.com/Citrus-cloud/Mine)) reached
`v1.0.0-alpha.1`: Electron app, click simulation/dry-run, a hard-gated real-coordinate-click
alpha, image/text/OCR simulation, Visual Builder, Scenario Presets, Safety Center, Audit Logs,
Diagnostics — and **no** real image/text clicks, **no** keyboard automation, **no** prohibited
automation.

ClickFlow Android reuses the **product concepts and safety philosophy** of the desktop app, but is
an independent native codebase. Desktop code is **not** bundled or executed on Android.

## Current status (Step 53)

Builds on the Step 52 foundation with a real scenario layer:

- Full scenario CRUD UI: list, create, edit, delete, select active.
- **Local persistence** — scenarios stored as JSON in internal app storage; restored on restart.
- Inline form validation (name, coordinates, repeat count, interval).
- Simulation runs the active scenario with **progress** (steps, percent, dry-run log).
- Diagnostics shows scenario count, active scenario, storage state, corruption recovery.
- Safety Center reports simulation-only + local-storage + real taps disabled.
- RU/EN localization for the scenario layer.

Foundation from Step 52 (Kotlin + Jetpack Compose app shell, Safety gate/state/center, Diagnostics)
remains intact.

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
