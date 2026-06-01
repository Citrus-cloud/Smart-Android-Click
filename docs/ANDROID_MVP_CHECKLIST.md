# Android MVP Checklist — ClickFlow Android (Step 52)

Acceptance checklist for the Step 52 foundation. Verify on a device/emulator (API 26+) after
building the debug APK.

- [ ] **project builds** — `./gradlew assembleDebug` succeeds (JDK 17 + Android SDK required).
- [ ] **app launches** — ClickFlow Android opens to the Home screen.
- [ ] **simulation starts/stops** — "Start simulation" sets status `running`; "Stop" sets `stopped`.
- [ ] **emergency stop works** — "Emergency Stop" sets status `emergency_stopped`.
- [ ] **safety center opens** — shows simulation-only enabled, real taps not implemented, etc.
- [ ] **diagnostics opens** — shows version, `simulationOnly=true`, `realTapsEnabled=false`, etc.
- [ ] **real taps disabled** — no UI control enables real taps; `canRunRealTap()` is false.
- [ ] **docs updated** — README, PROJECT_CONTEXT, CHANGELOG, and `docs/` present and current.

## Build environment note

Building requires JDK 17 and the Android SDK (`compileSdk 34`, `build-tools`, `platform-34`). On a
machine without these, install Android Studio (bundles the SDK) or the command-line SDK tools, then
run the Gradle command above. See [APK_BUILD_GUIDE.md](APK_BUILD_GUIDE.md).
