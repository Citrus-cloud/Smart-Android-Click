# Android Pre-alpha Post-release Checklist — ClickFlow Android

Verify the published pre-release end-to-end on a real device/emulator (API 26+).

Release: **ClickFlow Android Pre-alpha** · tag `android-v0.1.0-prealpha` (pre-release) ·
https://github.com/Citrus-cloud/Smart-Android-Click/releases/tag/android-v0.1.0-prealpha
APK: `app-debug.apk` (15,761,368 bytes, debug-signed).

## Steps

- [ ] **download APK** from the release assets.
- [ ] **install APK** — `adb install -r app-debug.apk` (or open on device).
- [ ] **launch app** — opens to Home; active profile + scenario shown.
- [ ] **verify no permissions requested** — no runtime permission prompts; Settings → App → Permissions shows none.
- [ ] **run simulation** — Start; progress shows repeat/action/percent; no real taps occur.
- [ ] **open Profiles** — list + active profile; create/select works.
- [ ] **open Scenarios** — scenarios filtered by active profile; multi-step editor works.
- [ ] **open Audit Log** — events present; persists across restart; share as text works.
- [ ] **export backup** — share sheet opens with backup JSON text.
- [ ] **import backup** — paste JSON → Validate → preview → merge; no silent overwrite.
- [ ] **verify real taps disabled** — Safety Center / Diagnostics show real taps off; `canRunRealTap() = false`.

## Step 60 — updated APK (Simple Clicker UX)

- [ ] **download the updated APK** from the release assets (re-download; asset was replaced).
- [ ] **Simple Clicker opens first** — clean home with tap-target + Start.
- [ ] **drag marker** — the circle moves with the finger and stays in bounds.
- [ ] **change interval/count** — steppers update values on the home screen.
- [ ] **Start simulation** — progress + tap count advance; no real taps.
- [ ] **Stop** — status `stopped`.
- [ ] **Emergency Stop** — status `emergency_stopped` immediately.
- [ ] **open Advanced** — Scenarios/Profiles/Audit/Backup/Safety/Diagnostics/About reachable.
- [ ] **verify no permissions** — no prompts; Settings shows none.
- [ ] **verify no real taps** — Safety Center / Diagnostics confirm simulation-only.

## Sign-off

- [ ] All boxes checked on at least one device.
- [ ] No crashes; no permission prompts; no real input performed.
- [ ] If issues found, file them and note them on the release.
