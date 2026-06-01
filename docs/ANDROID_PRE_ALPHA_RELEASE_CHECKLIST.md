# Android Pre-alpha Release Checklist — ClickFlow Android

Gate before tagging an Android pre-alpha (`1.0.0-alpha.x`). Debug builds only; no release signing yet.

## Build & install

- [ ] **Gradle build** — `./gradlew assembleDebug` succeeds (JDK 17 + Android SDK).
- [ ] **APK install** — `adb install -r app/build/outputs/apk/debug/app-debug.apk` on API 26+.
- [ ] **smoke test** — run `scripts/android-smoke.md` end to end.

## Data layers

- [ ] **scenario storage** — scenarios persist across restart (`scenarios.json`).
- [ ] **profile storage** — profiles persist; default profile always present (`profiles.json`).
- [ ] **audit persistence** — audit log survives restart (`audit-log.jsonl`).
- [ ] **backup export/import** — export shares text; import validates, previews, merges.

## UX & localization

- [ ] **RU/EN UI** — strings resolve in both locales (parity verified).

## Safety

- [ ] **no permissions** — `AndroidManifest.xml` has 0 `uses-permission`, 0 `<provider>`.
- [ ] **no real taps** — no `dispatchGesture`/Accessibility runtime/MediaProjection/overlay;
      `canRunRealTap()` = false.
- [ ] **no external storage** — all storage internal; backup/audit export is share-text only.

## Docs

- [ ] **README/CHANGELOG updated** — current step reflected; docs present under `docs/`.

## Known limitations (pre-alpha)

- Simulation only — no real input of any kind.
- No code signing, no auto-update, no Play distribution.
- Backup is text-paste only (no file picker / FileProvider yet).
