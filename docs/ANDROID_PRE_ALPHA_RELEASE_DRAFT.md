# GitHub Release draft — ClickFlow Android Pre-alpha

> Paste this as the GitHub Release body. Suggested tag: `android-v0.1.0-prealpha`. Mark as
> **pre-release**. Attach `app-debug.apk`.

---

**Title:** ClickFlow Android Pre-alpha

## Summary

First public **pre-alpha** of ClickFlow Android — a native Kotlin/Compose app that authors and runs
**simulation-only** multi-step "click" scenarios. **No real taps. No permissions.** Debug build for
early testing and feedback.

## Highlights

- Simulation-only scenario engine with live progress.
- Multi-step actions: simulated tap, wait, note.
- Local profiles (workspaces) grouping scenarios.
- Persistent audit log + text export.
- Backup import/export (profiles + scenarios) as JSON text.
- RU/EN UI.

## Safety model

- `simulationOnly = true`, `realTapsEnabled = false`, `canRunRealTap() = false`.
- **0 manifest permissions, 0 content providers.**
- No Accessibility Service, no MediaProjection, no overlay, no external storage.
- No captcha/anti-bot/ad-click/banking automation; no spyware/keyloggers.

## What works

- Create/edit/run scenarios; reorder multi-step actions.
- Profiles: create/edit/select; scenarios filtered by active profile.
- Audit log: persists across restarts; share as text; clear.
- Backup: export (share text); import (paste → validate → preview → merge).

## What is disabled

- Real input of any kind (taps/gestures), screen capture, overlays, root.
- Network and storage permissions; file pickers.

## APK install

```bash
adb install -r app-debug.apk
```

Requires an Android device/emulator on **API 26+**. The APK is **debug-signed** (not for production).

## Testing

See `docs/ANDROID_PRE_ALPHA_QA.md` and `scripts/android-smoke.md` for the manual test pass.

## Known limitations

- Simulation only; debug build; no release signing / auto-update / Play distribution.
- Backup is text paste/share only (no file picker yet).

## Feedback

Open an issue with device, Android version, and reproduction steps. The app collects no personal data.

## Security note

This is a safety-first, simulation-only pre-alpha. It cannot perform real taps and requests no
permissions. Real automation, if ever added, will be gated behind explicit consent, safety gates, and
a documented go/no-go review.
