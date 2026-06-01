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

## Step 54 — multi-step + audit checks

9. **Open a scenario** — Scenarios → **Open** → Scenario Detail shows the action list.
10. **Add actions** — add a tap (X/Y), a wait (duration), and a note (message); each validates.
11. **Reorder / edit / delete** — Up/Down reorders; Edit changes an action; Delete removes one.
12. **Run multi-step** — **Run simulation**; Home shows current repeat / current action / percent.
13. **Audit Log** — open from Home; verify `scenario.started`, `action.*`, `scenario.completed`.
14. **Emergency Stop mid-run** — start a long run, hit Emergency Stop; status `emergency_stopped`
    and a `scenario.emergencyStopped` SAFETY event appears.
15. **Clear audit log** — Clear empties the list.
16. **Migration** — install over a Step 53 build (or seed a v1 `scenarios.json`); the old tap loads
    as a single `SIMULATED_TAP` action and Diagnostics shows `storageMigrated=true`.

## Step 55 — profiles + audit persistence/export checks

17. **Profiles** — Home → Profiles; list shows the default profile with a scenario count.
18. **Create / edit profile** — create "Testing"; edit it; validation rejects blank/long name.
19. **Select profile** — switch active profile; Home/Scenarios show only that profile's scenarios.
20. **Create scenario in profile** — new scenario is bound to the active profile.
21. **Delete rules** — deleting the active/last profile or one with scenarios is blocked with a message.
22. **Migration** — install over a Step 54 build; old scenarios appear under the default profile.
23. **Audit persists** — generate events, kill + relaunch the app; events are still present.
24. **Audit summary** — Audit Log shows severity counts + storage status.
25. **Audit share** — Share opens the system share sheet with the text report.
26. **Audit clear** — Clear empties the persisted log (stays empty after restart).

## Step 56 — backup import/export checks

27. **Backup screen** — Home → Backup; status shows profile/scenario counts + simulation-only note.
28. **Export backup** — Export opens the share sheet with backup JSON text.
29. **Round-trip import** — copy the exported JSON, paste into Import, **Validate** → preview valid.
30. **Merge & rename** — Import (merge & rename); imported profile gets `(Imported)`; counts update.
31. **No silent overwrite** — existing profiles/scenarios remain; conflicts get new ids.
32. **Invalid JSON** — paste garbage → Validate shows invalid; Import stays disabled.
33. **Replace-all** — toggle confirm, then Replace all; data replaced (default profile ensured).
34. **Backup excludes audit log** — exported JSON has `"containsAuditLog": false` and no audit data.

## Pass criteria
- App builds, installs, launches.
- Start / Stop / Emergency Stop transition status correctly.
- Multi-step scenarios: actions add/edit/delete/move/run with progress.
- Profiles: create/edit/select/delete with rules; scenarios filtered by active profile.
- Audit log persists across restarts, summarizes, shares as text, and clears.
- Backup: export shares text; import validates, previews, merges without overwriting; replace-all
  requires confirmation; backups exclude the audit log.
- Safety Center and Diagnostics open and report simulation-only / real taps disabled.
- No real taps occur anywhere; no permissions requested.
