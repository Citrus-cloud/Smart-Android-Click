# ClickFlow Android — Pre-alpha Release Notes

## Summary

ClickFlow Android is the native Android branch of ClickFlow. This **pre-alpha** is a **simulation-only**
app: it lets you author and run multi-step "click" scenarios in a dry-run engine that performs **no
real taps**. It is a debug build for early testing and feedback.

## Current status

- Version: **0.1.0-prealpha** (debug, debug-signed).
- Package: `com.clickflow.android` (debug applicationId `com.clickflow.android.debug`).
- minSdk 26 / targetSdk 34. Kotlin + Jetpack Compose (Material 3).
- Builds cleanly with JDK 17 + Android SDK 34 (`./gradlew assembleDebug`).

## Included features

- **Simulation-only engine** — runs scenarios as dry-runs with progress; no real input.
- **Scenarios** — create/edit/delete/select; persisted locally.
- **Multi-step actions** — `SIMULATED_TAP`, `WAIT`, `NOTE`; reorderable.
- **Profiles** — local workspaces grouping scenarios; default profile; one active.
- **Audit log** — persistent (internal `audit-log.jsonl`), with summary; share as text.
- **Backup import/export** — profiles + scenarios as JSON text (validate, preview, merge).
- **Safety Center / Diagnostics** — transparent status surfaces.
- **RU/EN** localization.

## Simulation-only model

`simulationOnly = true`, `realTapsEnabled = false`, `canRunRealTap() = false`,
`attemptRealTap() = false`. The engine only updates status/log/progress and emits audit events.

## Scenarios / Multi-step actions

Each scenario has a name, `repeatCount`, `intervalMs`, and an ordered list of actions. The engine
runs `repeatCount × actions` in order; `WAIT` delays, `SIMULATED_TAP`/`NOTE` only log.

## Profiles

Scenarios are grouped by `profileId`. Switching the active profile changes the visible scenarios.
Default profile always exists; profiles with scenarios / the active / the last profile cannot be
deleted.

## Audit log

Persistent JSON Lines, bounded (1000), newest-first, corrupted-file fallback. Exportable as plain
text via the share sheet.

## Backup import/export

Backup JSON (`schema: clickflow-android-backup`, v1) contains profiles + scenarios only. Export via
share-text; import via pasted text with validation, preview, and merge strategies. **Backups exclude
the audit log.**

## Safety model

No real taps; no permissions; no providers; no external storage; no Accessibility/MediaProjection/
overlay. See `ANDROID_SAFETY_MODEL.md`.

## What is intentionally NOT included

- Real taps / Accessibility Service / `dispatchGesture`.
- MediaProjection / screen capture.
- Overlay (`SYSTEM_ALERT_WINDOW`), root, background/hidden automation.
- INTERNET or storage permissions; FileProvider; document picker.
- Any prohibited automation (captcha/anti-bot/ad-click/banking, spyware/keyloggers).

## Build / install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

API 26+ device/emulator. See `APK_BUILD_GUIDE.md` and `ANDROID_BUILD_TROUBLESHOOTING.md`.

## Known limitations

- Simulation only — no real device automation of any kind.
- Debug build only; no release signing, no auto-update, no Play distribution.
- Backup is text paste/share only (no file picker yet).

## Feedback

Please report issues via the repository's issue tracker, including device, Android version, and steps
to reproduce. No personal data is collected by the app.
