<PLACEHOLDER_EXISTING_CONTENT>

## Step 61 — Permissions skeleton (overlay + accessibility, no automation)

**Status:** in progress.

**Goal:** introduce the *plumbing* needed to later show a system overlay marker and react to accessibility events, WITHOUT enabling any real input. SafetyGate.canRunRealTap() remains hard-coded to false; nothing in this step adds, weakens, or bypasses that gate.

**What landed (domain layer, services, manifest, strings):**

- `permissions/PermissionStatus.kt` — pure data class (`overlayGranted`, `accessibilityEnabled`, `lastUpdatedAt`) + `EMPTY` constant.
- `permissions/PermissionsManager.kt` — read-only detector. Uses `Settings.canDrawOverlays()` for SYSTEM_ALERT_WINDOW and parses `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` for the accessibility service flag. Exposes `refresh()` + intents to open the matching system settings screens. Does not enable anything itself.
- `permissions/ClickFlowAccessibilityService.kt` — `AccessibilityService` subclass. **No-op skeleton.** No event subscriptions, no gesture dispatch, no window content reads. Class exists only so the user has something to enable in Settings.
- `res/xml/accessibility_service_config.xml` — minimal config (`accessibilityEventTypes="0"`, `canRetrieveWindowContent="false"`, no `canPerformGestures` capability). No gestures, no event subscriptions.
- `AndroidManifest.xml` — `SYSTEM_ALERT_WINDOW` declared (opt-in by user via system settings). `ClickFlowAccessibilityService` declared with `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"` and `accessibility_service_config` meta-data. **No autostart, no `<receiver>`, no scheduling.**
- `res/values/strings.xml` + `res/values-ru/strings.xml` — new keys for permissions, accessibility service label/summary/description, and floating-marker UX (English + Russian).
- `safety/SafetyCenter.kt` — extended constructor accepts `PermissionStatus`. Items now reflect live overlay/accessibility status. Real-tap row still reads `disabled (canRunRealTap=false)`.
- `diagnostics/DiagnosticsManager.kt` — `overlayEnabled` and `accessibilityEnabled` parameters (default `false`); `permissionsRequired` stays `false` because permissions remain opt-in.
- `core/AppInfo.kt` — `STEP` bumped to `Step 61 — Permissions skeleton`.

**What did NOT change (intentionally):**

- `safety/SafetyGate.kt` — untouched. `canRunRealTap()` still hard-coded to `false`.
- No real `dispatchGesture()` call anywhere. No `MotionEvent.obtain()` call anywhere. No `Instrumentation` call anywhere.
- No INTERNET, MediaProjection, READ/WRITE_EXTERNAL_STORAGE permissions.
- No overlay window is actually drawn. The strings reference a floating-marker preview that is *planned* — it is not yet rendered. UI work for this is pending.
- The `<service>` is declared `exported="true"` (required for accessibility services) but is functionally inert.

**Pending in Step 61 (UI integration):**

- `ClickFlowViewModel`: instantiate `PermissionsManager`, expose `PermissionStatus` state, wire `refreshPermissions()`, add `Screen.PERMISSIONS`, pass status into `SafetyCenter`/`DiagnosticsManager`.
- `Screens.kt`: new `PermissionsScreen` composable (intro text + 2 cards for overlay/accessibility, status badge, "Open settings" + "Refresh" buttons, safety footer); add "Permissions" entry button to `AdvancedScreen`; wire into the navigation `when`.
- (Optional, later) floating-marker preview composable behind `overlayGranted`.

**Safety invariants (unchanged after Step 61 lands fully):**

- `SafetyGate.canRunRealTap()` = `false` (hard-coded).
- `SafetyCenter` exposes ZERO controls to enable real input.
- Accessibility service is a no-op even when the user enables it in system settings.
- Overlay permission unlocks only an in-app *visual* marker — never a tap.
