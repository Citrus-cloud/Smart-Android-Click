<PLACEHOLDER_EXISTING_CONTENT>

## Step 61

Read-only permissions surface for the Real Tap prototype.

- Added `PermissionsManager` that reports overlay-permission and AccessibilityService status without granting anything.
- Added `Screen.PERMISSIONS` and a Permissions screen that surfaces live status and deep-links into the system settings via `openOverlaySettings()` / `openAccessibilitySettings()`.
- `SafetyCenter` now accepts a live `PermissionStatus` snapshot; the legacy zero-arg constructor still works for pre-Step-61 callers.
- `SafetyGate.canRunRealTap()` remains hard-coded `false`. Granting either permission does NOT enable real input.
- `Diagnostics` reports `overlayEnabled` + `accessibilityEnabled`.

## Step 62

Real Tap Prototype — UI skeleton only, dispatch still blocked.

- Added `Screen.REAL_TAP_PROTOTYPE` and `RealTapPrototypeScreen.kt` (self-contained Compose screen).
- ViewModel adds:
- `RealTapSessionState { INACTIVE, ACTIVE }`
- `RealTapConsent(x, y, requestedAtMs, expiresAtMs)` — 10s window.
- `SafetyReviewState` — 10-item checklist; `allPassed` gates session start.
- APIs: `toggleSafetyReviewItem`, `startRealTapSession`, `endRealTapSession`, `requestRealTap`, `confirmRealTap`, `cancelRealTap`.
- `SafetyGate` introduces `SafetyState` snapshot + `canRunRealTapSingleProto()` (returns true only when all four prototype flags are simultaneously satisfied). Bulk `canRunRealTap()` still false.
- Every prototype transition emits a `SAFETY_REAL_TAP_BLOCKED` audit event (session start/end, consent requested/expired/confirmed/cancelled, dispatch blocked).
- `emergencyStop()` tears down any active session + pending consent and records the termination.
- Bulk real taps remain categorically forbidden.

## Step 63

Make the SafetyGate prototype flags **live** and wire the ViewModel + UI to them end-to-end. Bulk real-tap dispatch is still impossible; this step closes the per-flag plumbing gap that Step 62 left as compile-time defaults.

**Status:** Landed (engineering scope) + 1 follow-up deferred to Step 64.

### What landed

- **`SafetyGate` live mutators** (`safety/SafetyGate.kt`)
- Four narrow per-flag mutators (one flag per call, synchronized):
  - `updateReviewPassed(passed: Boolean)`
  - `updateAccessibility(enabled: Boolean)`
  - `updateSession(active: Boolean)`
  - `updateConsentFresh(fresh: Boolean)`
- `resetPrototypeFlags()` — single-call safe baseline used by emergency stop + session end. Simulation-only remains true.
- `canRunRealTap()` (bulk) still returns `false` unconditionally — unchanged and not touched by this step.
- `canRunRealTapSingleProto()` now reads the LIVE state instead of compile-time defaults.
- `getSingleProtoBlockedReasons()` — diagnostic list of which gating flags are currently missing (human-readable, one entry per missing flag). Used by the UI chip and Diagnostics.

- **`ClickFlowViewModel` wiring** (`core/ClickFlowViewModel.kt`)
- Each of the six real-tap prototype APIs now drives the correct `SafetyGate` mutator on every transition:
  - `toggleSafetyReviewItem` → `gate.updateReviewPassed(safetyReview.allPassed)` after the toggle.
  - `startRealTapSession` → `gate.updateSession(true)` on success.
  - `endRealTapSession` → `gate.updateSession(false)` + `gate.updateConsentFresh(false)`.
  - `requestRealTap` → `gate.updateConsentFresh(true)`.
  - `confirmRealTap` → consults `gate.canRunRealTapSingleProto()`; clears consent + calls `gate.updateConsentFresh(false)` regardless of outcome.
  - `cancelRealTap` → `gate.updateConsentFresh(false)`.
- `refreshPermissions()` now also pushes `accessibilityServiceEnabled` into the gate, so the live-state truth follows system settings without the user having to leave and re-enter the screen.
- `emergencyStop()` calls `gate.resetPrototypeFlags()` in addition to its prior teardown work.
- New read-only state stream `safetyGateReasons: StateFlow<List<String>>` exposes `gate.getSingleProtoBlockedReasons()` to the UI; recomputed after every mutator call.
- New `RealTapDispatchResult` enum: `DISPATCHED`, `BLOCKED_BY_GATE`, `BLOCKED_NO_SERVICE`, `BLOCKED_INVALID_CONSENT`, `DISPATCH_CANCELLED`, `DISPATCH_FAILED`. Surfaced via `lastDispatchResult: StateFlow<RealTapDispatchResult?>`. Today every confirmation produces `BLOCKED_BY_GATE` because `canRunRealTap()` is still false at the dispatch layer; the enum exists so the UI can show the actual reason and so future wiring (Step 64) can return the other values without a state model change.

- **UI — `RealTapPrototypeScreen.kt`**
- New result chip directly under the consent controls. Renders the localized label for the most recent `RealTapDispatchResult` (or hides itself when null). Color coded — DISPATCHED green, every BLOCKED_* / DISPATCH_FAILED red, DISPATCH_CANCELLED neutral.
- New collapsible "Why blocked" list beneath the chip, driven by `safetyGateReasons`. Shows one bullet per missing prototype flag. Empty list collapses the section entirely.
- The existing safety-review checklist, session start/end button, consent request/confirm/cancel buttons, bulk-blocked notice, emergency-stop button, and back button are unchanged in placement and behavior.

- **Strings** — added EN + RU keys for the six dispatch-result labels (`real_tap_result_dispatched`, `real_tap_result_blocked_by_gate`, `real_tap_result_blocked_no_service`, `real_tap_result_blocked_invalid_consent`, `real_tap_result_dispatch_cancelled`, `real_tap_result_dispatch_failed`) and the section header `real_tap_why_blocked`.

- **Docs**
- `docs/REAL_TAP_QA_SCENARIOS.md` — exhaustive manual-QA matrix for the prototype (every reachable combination of the four prototype flags + every dispatch-result branch).
- `docs/REAL_TAP_FIXES_LOG.md` — running log of fixes applied during Step 62/63 hardening (build-break recovery, missing string keys, truncation-safe edit procedure).
- `AppInfo.STEP` bumped to 63.

### Deferred to Step 64

- Extracting a standalone `RealTapController` class from the ViewModel. The Step 63 spec called for one; the practical landing keeps the logic inline because the ViewModel has the StateFlow plumbing wired through the rest of the app and a controller extraction needs its own constructor + injection plan. Functionally equivalent — all six APIs do the same work, just on the ViewModel directly.
- Granular `realtap.*` `AuditType` enum entries (`realtap.dispatch_attempt`, `realtap.dispatch_blocked`, `realtap.dispatch_success`, `realtap.dispatch_failed`). Today every transition reuses `SAFETY_REAL_TAP_BLOCKED` with a descriptive message — semantically correct but coarse. Step 64 will widen the enum and migrate the call sites.
- Marker-only invariant inside `confirmRealTap` (reject `(x,y)` not matching the current marker within live composition bounds). Today the consent payload IS the marker snapshot at request time, so the surface for drift is small; the explicit reject is still worth adding.

### Why this shape

- Each flag flip is exactly one mutator call → audit-trail reasoning stays trivial.
- `resetPrototypeFlags` exists so emergency stop has one chokepoint and cannot accidentally leave the gate in a partially-armed state.
- The chip + reasons list make the gate's verdict legible to the user without exposing the underlying boolean snapshot.
- Bulk real-tap dispatch (`canRunRealTap()`) remains `false` and is not touched by this step. Any future change to that surface requires its own safety review.

## Step 64

Wire `RealTapController` end-to-end into the ViewModel, emit granular `realtap.*` audit events, enforce the marker-only invariant, and fix a build-breaking duplicate `SafetyState`. This closes all three items deferred from Step 63.

- **Build fix:** `safety/SafetyGate.kt` previously declared an inline `data class SafetyState` in the same package as `safety/SafetyState.kt` — a redeclaration that broke compilation. The inline copy was removed; the standalone superset in `SafetyState.kt` is the single source of truth.
- **ViewModel:** the six prototype APIs (`toggleSafetyReviewItem`, `startRealTapSession`, `endRealTapSession`, `requestRealTap`, `confirmRealTap`, `cancelRealTap`) + `emergencyStop` now route every audit record through `RealTapController(gate, auditLog)` using the granular `AuditType.REAL_TAP_*` constants instead of the generic `SAFETY_REAL_TAP_BLOCKED`. The dispatch decision is delegated to `RealTapController.evaluate(...)` and mapped onto the existing `RealTapDispatchResult` for the UI chip (`ALLOWED`→`DISPATCHED`, plus the three BLOCKED_* values).
- **Marker invariant:** `confirmRealTap` recomputes the live marker `(x,y)` and rejects the tap as `BLOCKED_INVALID_CONSENT` (granular consent-declined, reason `marker_drift`) when the marker has moved since consent was requested. Consent expiry and the missing-consent case are also recorded through the controller.
- **SafetyGate:** added `canRunControlledRealTapSession(sessionId)` (identical preconditions to `canRunRealTapSingleProto`) and `getControlledSessionBlockedReasons(sessionId)`. Bulk `canRunRealTap()` still returns `false` unconditionally.
- **No new strings:** the UI reuses the existing `real_tap_audit_*` message keys.
- `AppInfo.STEP` bumped to 64.

## Step 65

First automated test coverage for the real-tap prototype — pure-JVM JUnit 4, no Android framework or Robolectric, runnable via `./gradlew testDebugUnitTest`.

- New `app/src/test/java/com/clickflow/android/`:
  - `safety/SafetyGateTest.kt`
  - `realtap/RealTapControllerTest.kt`
  - `realtap/RealTapSessionTest.kt`
  - `realtap/RealTapSafetyReviewTest.kt`
- The controller tests construct `AuditLogManager()` with no storage file, so the JSONL / `org.json` persistence path is never exercised and the tests stay framework-free.
- Covered: bulk gate always closed; single-proto / controlled-session gating; `resetPrototypeFlags` semantics; controller decision matrix (invalid consent / no service / gate-blocked / allowed) with matching granular audit types; consent TTL + single-use nonce; the 10-item safety-review lifecycle.
- Deferred (need instrumentation / Robolectric): ViewModel StateFlow transitions and the `confirmRealTap` message-emission path. The domain logic they delegate to is covered here.
- `AppInfo.STEP` bumped to 65.
