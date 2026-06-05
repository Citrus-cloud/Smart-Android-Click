# Changelog — ClickFlow Android

All notable changes to the ClickFlow Android project are documented here.
This project follows the ClickFlow step-based development model.

## [Unreleased]

### Step 74 — Controlled tap session (rate/count limits, TTL, emergency stop)

Phase 3 begins: introduces the domain model and manager for a rate-limited
controlled real-tap session. Bulk real taps remain categorically forbidden;
this step adds the session shell that Step 75 will wire to a smart target.

- New `realtap/ControlledTapSession.kt` — three declarations:
  - `data class ControlledTapSession(sessionId, maxTaps, ttlMs, startedAtMs)` —
    mutable tap counter + terminated flag. `isActive(nowMs)`, `isExhausted()`,
    `recordTap(nowMs)` (returns false when inactive or exhausted), `terminate()`,
    `remainingTaps()`, `remainingTtlMs(nowMs)`.
  - `enum class ControlledTapBlockReason { SESSION_INACTIVE, SESSION_EXPIRED,
    SESSION_TERMINATED, TAP_LIMIT_REACHED, GATE_CLOSED }`.
  - `sealed class ControlledTapDispatchResult { Allowed, Blocked(reason) }`.
- New `realtap/ControlledTapSessionManager.kt` — manages one active session:
  - `startSession(sessionId, maxTaps 1–10, ttlMs 1_000–60_000)` — validates
    params, checks `SafetyGate.canRunControlledRealTapSession(sessionId)`, creates
    session. Returns `SessionResult.Ok` / `SessionResult.Error(reason)` with codes
    `already_active`, `invalid_params`, `gate_closed`.
  - `endSession()` — terminates and nulls the session.
  - `emergencyStop()` — delegates to `endSession()`.
  - `evaluateTap()` — checks session existence → not terminated → not expired →
    not exhausted → `SafetyGate.canRunRealTap()` (bulk, always false until
    Step 75). Returns `ControlledTapDispatchResult`.
  - `hasActiveSession()`, `session` read-only. Injected clock + gate.
- New `app/src/test/java/com/clickflow/android/realtap/ControlledTapSessionManagerTest.kt`
  — 14 JVM tests: no session initially, start → active, double-start error,
  invalid maxTaps, invalid TTL, endSession clears, evaluateTap no session →
  INACTIVE, evaluateTap active → GATE_CLOSED (bulk false), session expires after
  TTL, recordTap exhausts, remainingTaps decrements, remainingTtlMs decrements,
  emergencyStop terminates, evaluateTap after terminate → INACTIVE.
- `AppInfo.STEP` bumped to Step 74.

Safety invariants: `SafetyGate.canRunRealTap()` returns `false` unconditionally;
`evaluateTap()` always reaches the bulk-gate check and returns `GATE_CLOSED`
until Step 75 wires the smart-target dispatch path. No pixels, no tap, no I/O.

### Step 73 — Visual scenario builder + presets
- `ScenarioPreset` + `BuiltInPresets` + `VisualScenarioBuilder`. 13 JVM tests.

### Step 72 — Text-target controller
- `TextTargetResult` + `TextTargetOutcome` + `TextTargetController`. 11 JVM tests.

### Step 71 — On-device OCR stub
- `OcrProvider` interface + `StubOcrProvider` + `OcrController`. 12 JVM tests.

### Steps 52–70 — Foundation through image-target controller
See git history.
