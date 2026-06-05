# Changelog — ClickFlow Android

All notable changes to the ClickFlow Android project are documented here.
This project follows the ClickFlow step-based development model.

## [Unreleased]

### Step 75 — Smart target → single real tap with explicit consent

Phase 3 continues: wires an image-target or text-target highlight region into
a single-tap admission flow backed by the Step 74 `ControlledTapSessionManager`
and a single-use consent record. Does NOT call `dispatchGesture` (that is
Step 76); all state is process-local, no Android imports.

- New `realtap/SmartTargetTapRequest.kt` — three declarations:
  - `data class SmartTargetTapRequest(sessionId, targetType, highlightRegion,
    requestedAtMs)` — tap coordinates derived from `highlightRegion.centerX/Y`;
    `isValid` = region is valid.
  - `enum class SmartTargetType { IMAGE_TARGET, TEXT_TARGET }`.
  - `sealed class SmartTargetTapResult { Dispatched(request, tapNumber),
    Blocked(request?, reason) }` + `enum SmartTargetBlockReason { INVALID_REQUEST,
    NO_ACTIVE_SESSION, SESSION_GATE_CLOSED, CONSENT_MISSING, CONSENT_EXPIRED,
    MARKER_DRIFT }`.
- New `realtap/SmartTargetTapController.kt` — orchestrates session + consent:
  - `recordConsent(request)` — stamps consent at `nowProvider()`.
  - `clearConsent()`.
  - `dispatch(request?)` — five-check chain:
    1. Validate request (non-null, valid region) → `INVALID_REQUEST`.
    2. Active controlled session → `NO_ACTIVE_SESSION`.
    3. `sessionManager.evaluateTap()` → `SESSION_GATE_CLOSED`.
    4. Consent present → `CONSENT_MISSING`; not expired (10s TTL) →
       `CONSENT_EXPIRED` (clears consent); coordinates match within 2% →
       `MARKER_DRIFT` (clears consent).
    5. All passed → `session.recordTap(now)`, clears consent, returns
       `Dispatched(request, tapNumber)`.
  - `COORD_TOLERANCE = 0.02f`; `CONSENT_TTL_MS = 10_000L`.
  - `data class SmartTargetConsent(request, recordedAtMs)`.
- New test `realtap/SmartTargetTapControllerTest.kt` — 12 JVM tests:
  null request → INVALID_REQUEST, invalid region → INVALID_REQUEST,
  no session → NO_ACTIVE_SESSION, bulk gate closed → SESSION_GATE_CLOSED,
  consent missing (field check), recordConsent stores, clearConsent nulls,
  consent expiry math, tapX/Y from centre, isValid true/false, consent timestamp.
- `AppInfo.STEP` bumped to Step 75.

Safety invariants: `SafetyGate.canRunRealTap()` = `false`; `evaluateTap()`
still returns `GATE_CLOSED`; `dispatch()` therefore always returns
`SESSION_GATE_CLOSED` until Step 76 opens the single-tap path.
No `dispatchGesture`, no pixels, no I/O.

### Step 74 — Controlled tap session
- `ControlledTapSession` + `ControlledTapSessionManager`. 14 JVM tests.

### Steps 52–73 — Foundation through visual builder
See git history.
