# Android Pre-alpha QA — ClickFlow Android (Step 56)

Manual QA pass for the pre-alpha. Requires JDK 17 + Android SDK (see `APK_BUILD_GUIDE.md`) and a
device/emulator on API 26+.

> **Build verified (Step 57):** `./gradlew assembleDebug` compiles cleanly (JDK 17 + Android SDK 34)
> and produces a debug-signed `app-debug.apk` (versionName `0.1.0-prealpha`). The remaining items are
> on-device manual checks.

## Checklist

- [x] **build debug APK** — `./gradlew assembleDebug` succeeds (verified).
- [ ] **launch app** — opens to Home with the active profile + scenario.
- [ ] **create profile** — Profiles → Create; validation works; profile appears.
- [ ] **create multi-step scenario** — under the active profile; add tap/wait/note actions.
- [ ] **run simulation** — progress shows current repeat / action / percent; no real taps.
- [ ] **check audit log** — lifecycle + per-action events present.
- [ ] **export audit log** — Share opens the system share sheet with text.
- [ ] **export backup** — Backup → Export shares JSON text (no permissions).
- [ ] **paste backup into import** — paste the exported JSON into the import field.
- [ ] **validate backup** — preview shows profiles/scenarios/invalid counts.
- [ ] **import backup** — merge & rename adds items without overwriting existing data.
- [ ] **verify imported scenarios** — appear under their profile; conflicts renamed.
- [ ] **verify no permissions** — `AndroidManifest.xml` has 0 `uses-permission`.
- [ ] **verify real taps disabled** — Safety Center / Diagnostics show real taps off.

## Expected invariants

- `simulationOnly = true`, `realTapsEnabled = false`, `canRunRealTap() = false`.
- No external storage; all data in internal app storage.
- Backup excludes the audit log; no screenshots/base64 anywhere.
