# Android Smoke Test — ClickFlow Android (Step 52)

Manual smoke test to confirm the foundation works end-to-end.

## Preconditions
- JDK 17 + Android SDK installed (see `docs/APK_BUILD_GUIDE.md`).
- A device or emulator on API 26+.

## Steps

1. **Build debug APK**
   ```bash
   ./gradlew assembleDebug
   ```
   Expect: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk`.

2. **Install manually**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Launch** — open "ClickFlow Android". Home screen shows the title and the
   "Simulation-only foundation" badge.

4. **Start simulation** — tap **Start simulation**. Status shows `running`.

5. **Stop** — tap **Stop**. Status shows `stopped`.

6. **Emergency Stop** — tap **Emergency Stop**. Status shows `emergency_stopped`.

7. **Open Safety Center** — tap **Safety Center**. Verify:
   - Simulation-only: enabled
   - Real taps: not implemented
   - Accessibility Service: planned
   - MediaProjection: planned
   - Overlay: planned
   - Emergency Stop: available
   - Audit logs: planned
   - Prohibited automation: blocked

8. **Open Diagnostics** — tap **Diagnostics**. Verify `simulationOnly=true`,
   `realTapsEnabled=false`, version `1.0.0-alpha.1`.

## Pass criteria
- App builds, installs, launches.
- Start / Stop / Emergency Stop transition status correctly.
- Safety Center and Diagnostics open and report simulation-only / real taps disabled.
- No real taps occur anywhere.
