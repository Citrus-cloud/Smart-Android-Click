# Changelog — ClickFlow Android

All notable changes to the ClickFlow Android project are documented here.
This project follows the ClickFlow step-based development model.

## [Unreleased]

### Step 76 — Audit + Emergency Stop for smart tap sessions

Phase 3 closes: in-memory bounded audit log + atomic Emergency-Stop coordinator.
All state is process-local, no Android imports, no real tap dispatched.

- New `realtap/SmartSessionAudit.kt` — three declarations:
  - `data class SmartSessionAuditEvent(type, sessionId?, detail, recordedAtMs)`.
  - `enum class SmartSessionAuditType { SESSION_STARTED, SESSION_ENDED,
    SESSION_EXPIRED, SESSION_EMERGENCY_STOPPED, TAP_CONSENT_RECORDED,
    TAP_CONSENT_CLEARED, TAP_DISPATCHED, TAP_BLOCKED, GATE_CHECKED,
    INVARIANT_VIOLATION }`.
  - `class SmartSessionAuditLog(maxEvents 200, nowProvider)`: `record(type,sid,detail)`
    (bounds-checked), `eventsOfType(type)`, `clear()`, `exportText()`,
    `events: List`, `count: Int`.
  - `class SmartSessionEmergencyStop(sessionManager, tapController, auditLog)`:
    `execute(detail)` — clearConsent → emergencyStop → record SESSION_EMERGENCY_STOPPED.
- New test `realtap/SmartSessionAuditTest.kt` — 13 JVM tests:
  starts empty, record adds, timestamp from provider, eventsOfType filters,
  max-events discards oldest, clear empties, exportText non-empty / empty,
  emergencyStop clears consent, terminates session, records audit event with
  sessionId + detail, no-session still records (null sessionId),
  multiple accumulate.
- `AppInfo.STEP` bumped to Step 76.

Phase 3 (Steps 74–76) complete. Phase 4 begins with Step 77 in repo `Mine` (desktop).

### Step 75 — Smart target → single real tap
- `SmartTargetTapRequest` + `SmartTargetTapController`. 12 JVM tests.

### Step 74 — Controlled tap session
- `ControlledTapSession` + `ControlledTapSessionManager`. 14 JVM tests.

### Steps 52–73 — Foundation through visual builder
See git history.
