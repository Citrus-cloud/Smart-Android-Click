<PLACEHOLDER_EXISTING_CONTENT>

## Step 61 — Permissions skeleton (overlay + accessibility, no automation)

**Status:** complete (UI wiring landed alongside Step 62).

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

**UI integration (landed alongside Step 62):**

- `ClickFlowViewModel`: instantiates `PermissionsManager`, exposes `PermissionStatus` state, wires `refreshPermissions()`, has `Screen.PERMISSIONS`, passes status into `SafetyCenter` / `DiagnosticsManager`.
- `Screens.kt`: `PermissionsScreen` composable (intro text + 2 cards for overlay/accessibility, status badge, "Open settings" + "Refresh" buttons, safety footer); "Permissions" entry button in `AdvancedScreen`; routed via the navigation `when`.

**What did NOT change (intentionally):**

- `safety/SafetyGate.kt` — untouched. `canRunRealTap()` still hard-coded to `false`.
- No real `dispatchGesture()` call from Step 61 itself. (The Step 62 prototype adds the SOLE call site, behind four independent gates.)
- No INTERNET, MediaProjection, READ/WRITE_EXTERNAL_STORAGE permissions.
- No overlay window is actually drawn. The strings reference a floating-marker preview that is *planned* — it is not yet rendered.
- The `<service>` is declared `exported="true"` (required for accessibility services) but is functionally inert (Step 61). Step 62 adds `performSingleTap` as a single-shot dispatch site invoked only by `RealTapController` behind the gates.

**Safety invariants (preserved after Step 61 + Step 62):**

- `SafetyGate.canRunRealTap()` = `false` (hard-coded).
- `SafetyCenter` exposes ZERO controls to enable bulk/looped real input.
- Accessibility service still subscribes to no events and still reads no window content.
- Overlay permission unlocks only an in-app *visual* marker — never a tap.

## Step 62 — Single real-tap prototype (gated, audited, per-tap consent)

**Status:** UI wiring landed (domain + strings + docs + ViewModel + RealTapPrototypeScreen + Screens.kt routing committed). Remaining: SafetyCenter prototype item bump + `AppInfo.STEP` bump + smoke verification.

**Goal:** introduce a **narrow, fully-audited path** that allows the user to perform **exactly one real tap per explicit consent** through the existing (Step 61) `ClickFlowAccessibilityService`. Bulk, looped, and scenario-driven real taps remain hard-disabled by `SafetyGate`. This is the first step in the project where `dispatchGesture` is wired at all — it is wired behind four independent gates.

**Why this is safe:**

1. `SafetyGate.canRunRealTap()` still returns `false`. The scenario runner, all bulk paths, all scheduled paths, all import/replay paths are untouched.
2. A separate, NEW method `SafetyGate.canRunRealTapSingleProto(state, request)` controls the single-tap path. It returns `true` only when ALL of these hold simultaneously: (a) the per-session Safety Review is passed, (b) a real-tap session is active, (c) a fresh per-tap consent exists (nonce valid, TTL ≤ 10s, not consumed), (d) API ≥ 24 and the accessibility service is currently bound.
3. `RealTapController` is the ONLY caller of `ClickFlowAccessibilityService.performSingleTap`, and `performSingleTap` is the ONLY place in the codebase that invokes `dispatchGesture`. There is no other entry point.
4. The Safety Review pass state lives in memory only — never persisted, never backed up, never exported. Cold start = no session.
5. Emergency Stop immediately ends the session and invalidates any pending consent.
6. Every transition (review pass, session start/end, consent request/confirm/cancel/expire, dispatch attempt/block/success/failure) emits a `realtap.*` audit event.

**What landed — domain layer (earlier commits):**

- `audit/AuditEvent.kt` — added 14 `realtap.*` constants: `realtap.review.passed`, `realtap.review.reset`, `realtap.session.started`, `realtap.session.ended`, `realtap.consent.requested`, `realtap.consent.confirmed`, `realtap.consent.cancelled`, `realtap.consent.expired`, `realtap.consent.reused`, `realtap.dispatch.attempted`, `realtap.dispatch.blocked`, `realtap.dispatch.success`, `realtap.dispatch.failure`, `realtap.service.unavailable`. Combined with the 19 pre-existing constants, `AuditType` now exposes 33 constants in total.
- `realtap/RealTapRequest.kt` — immutable request (`targetX`, `targetY`, `nonce`, `createdAtMs`).
- `realtap/RealTapResult.kt` + `RealTapOutcome` — outcome enum (`DISPATCHED`, `BLOCKED_BY_GATE`, `BLOCKED_NO_SERVICE`, `BLOCKED_INVALID_CONSENT`, `DISPATCH_CANCELLED`, `DISPATCH_FAILED`).
- `realtap/RealTapSession.kt` — in-memory session token (id, startedAt, reviewSnapshotId). Not serialized.
- `realtap/RealTapSafetyReview.kt` — the 10-item review (verbatim in `docs/SAFETY_REVIEW_CHECKLIST.md`) + `passed: Boolean` + `passedAtMs: Long?`.
- `safety/SafetyState.kt` — extended with `realTapSafetyReviewPassed`, `realTapSessionActive`, `realTapConsentFresh` (all default `false`). Existing fields (`simulationOnly`, `realTapsEnabled`, blocked reasons) untouched.
- `safety/SafetyGate.kt` — adds `canRunRealTapSingleProto(state: SafetyState, request: RealTapRequest): Boolean` and `getSingleProtoBlockedReasons(state, request): List<String>`. `canRunRealTap()` continues to return `false` unconditionally.
- `permissions/ClickFlowAccessibilityService.kt` — added `companion object { var liveInstance: ClickFlowAccessibilityService? }` set in `onServiceConnected` / cleared in `onUnbind`, and `fun performSingleTap(x: Float, y: Float, onResult: (Boolean) -> Unit)` that builds a single `GestureDescription` (one stroke, ~50 ms) and calls `dispatchGesture`. Still no event subscriptions, still no window content reads.
- `realtap/RealTapController.kt` — orchestrates: `beginConsent(target)`, `confirmConsent(nonce)`, `cancelConsent(reason)`, `dispatch(request)`. Validates gate → service alive → consent fresh+unconsumed → dispatch → audit → mirror to diagnostics. Single-use nonce, 10-second TTL.
- `diagnostics/DiagnosticsState.kt` — added 6 prototype-mirror fields: `realTapSessionActive`, `realTapSafetyReviewPassed`, `realTapConsentFresh`, `realTapDispatchedCount`, `realTapLastOutcome`, `realTapLastEventAtMs`. Total fields: 11 (5 originals + 6 real-tap).
- `diagnostics/DiagnosticsManager.kt` — accepts and propagates the 6 new fields. Defaults remain safe (all `false` / 0 / null).
- `res/values/strings.xml` + `res/values-ru/strings.xml` — added 22 new keys for the prototype (EN + RU): `real_tap_prototype*`, `real_tap_safety_review*`, `real_tap_start_session` / `real_tap_end_session`, `real_tap_session_active` / `real_tap_session_inactive`, `real_tap_request_tap`, `real_tap_consent_title` / `real_tap_consent_body` / `real_tap_consent_confirm` / `real_tap_consent_cancel`, `real_tap_blocked_by_gate`, `real_tap_dispatch_succeeded` / `real_tap_dispatch_failed`, `real_tap_accessibility_required`, `real_tap_api_too_low`, `real_tap_bulk_still_blocked`, `real_tap_emergency_stop_note`, plus 5 audit-mirror message keys (`real_tap_audit_session_started`, `real_tap_audit_session_ended`, `real_tap_audit_consent_requested`, `real_tap_audit_consent_expired`, `real_tap_audit_dispatch_blocked`).
- `docs/REAL_TAP_PROTOTYPE.md` — architecture, file map, invariants, out-of-scope list.
- `docs/SAFETY_REVIEW_CHECKLIST.md` — the 10 review items, verbatim, EN + RU, plus UX rules and invariants.
- `docs/CONSENT_FLOW.md` — per-tap consent lifecycle, TTL/nonce contract, audit events, defence-in-depth re-checks.

**What landed — UI wiring (this turn):**

- `ClickFlowViewModel.kt` — added `Screen.REAL_TAP_PROTOTYPE`; new state types `RealTapSessionState` (`INACTIVE` / `ACTIVE`) and an in-memory `SafetyReviewState` (10-item checklist, `itemsLocalized()` + `allPassed()` + `toggle()`); new `RealTapConsent` data class (target + nonce + 10s expiry). Six `StateFlow`s now power the prototype UI (`safetyReview`, `realTapSession`, `realTapConsent`, plus three one-shot audit-mirror message flows). Six prototype APIs: `startRealTapSession()`, `endRealTapSession()`, `requestRealTap(x, y)`, `confirmRealTap(nonce)`, `cancelRealTap(reason)`, `toggleSafetyReviewItem(index)`. All transitions audit-logged. `emergencyStop()` now tears down the active session and invalidates any pending consent.
- `ui/RealTapPrototypeScreen.kt` — self-contained composable (its own `RealTapScaffold` + `RealTapMessageLine` helpers, no dependency on private helpers in `Screens.kt`). Renders: header / status badge ("Safety review: PASSED (this session)" or "required"), scrollable 10-item review block (toggle per item), session controls (Start / End session), "Request single real tap" button (disabled until review + session pass), consent dialog with countdown + Confirm/Cancel buttons, blocked-reason list driven by `SafetyGate.getSingleProtoBlockedReasons`, and a permanent "Bulk and looped real taps remain blocked by SafetyGate" footer.
- `ui/Screens.kt` — `Screen.REAL_TAP_PROTOTYPE` added to the navigation `when` (routes to `RealTapPrototypeScreen(vm)`); `AdvancedScreen` gains a `NavButton(stringResource(R.string.btn_real_tap_prototype))` entry; new `MessageLine` mappings for the five audit-mirror keys so prototype state transitions appear in the standard message toast.

**What did NOT change (intentionally):**

- `safety/SafetyGate.kt::canRunRealTap()` — still returns `false`. The scenario runner and all bulk paths remain simulation-only.
- `SimulationEngine` — untouched. It has no awareness of the real-tap path and cannot trigger it.
- No new manifest permissions. No `<receiver>` added. No autostart, no scheduling.
- No persistence of session / review / consent state. No backup field. No export field.
- No biometric or PIN gate on the consent dialog (out of scope for the prototype).

**Pending in Step 62 (still to land):**

- `safety/SafetyCenter.kt` — append a "Single real-tap prototype" item that reports session state, review state, and consent freshness. Add the "Bulk and looped real taps remain blocked by SafetyGate" reminder.
- `core/AppInfo.kt` — bump `STEP` to `Step 62 — Real tap prototype`.
- Smoke verification (`scripts/android-smoke.md`) + CI green + release-guide refresh + APK refresh for the pre-release. Until then, the published APK remains the Step 60 build.

**Invariants verified at end of Step 62 (with UI wiring):**

- `SafetyGate.canRunRealTap()` continues to return `false`.
- `ClickFlowAccessibilityService.performSingleTap` is the sole `dispatchGesture` call site.
- `RealTapController` is the sole caller of `performSingleTap`.
- The scenario runner has zero references to `RealTapController` / `performSingleTap` / `dispatchGesture`.
- The Safety Review and consent state are not persisted, not exported, not backed up.
- Every state transition emits a `realtap.*` audit event.
- The prototype UI is reachable only from `AdvancedScreen` (no shortcut from Home / Simple Clicker / Scenario Runner).

See `docs/REAL_TAP_PROTOTYPE.md`, `docs/SAFETY_REVIEW_CHECKLIST.md`, `docs/CONSENT_FLOW.md`.
