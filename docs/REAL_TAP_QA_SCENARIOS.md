# Real-Tap Prototype — QA Scenarios

**Scope.** This document enumerates the test scenarios for the Step 62/63
single-real-tap prototype. It is *not* a regression suite for the rest of
ClickFlow Android — those scenarios are covered by `ANDROID_SMOKE.md` and
`ANDROID_PRE_ALPHA_QA.md`. Everything below assumes a debug build of
`android-v0.1.0-prealpha` (or later) installed on a physical Android 7+ (API
24+) device.

**Hard invariants verified by every scenario below.**

1. `SafetyGate.canRunRealTap()` returns `false` (bulk/looped real taps stay
 blocked).
2. `RealTapController` is the sole caller of
 `ClickFlowAccessibilityService.performSingleTap`.
3. `performSingleTap` is the sole `dispatchGesture` call site in the codebase.
4. Safety review state, session state, and consent state are never persisted,
 never exported, never backed up.
5. Emergency Stop ends any active session and invalidates any pending consent.
6. The prototype is reachable ONLY from `AdvancedScreen → Real-tap prototype`.

---

## Group 1 — Gate behaviour (no real taps dispatched)

### QA-RT-01 — Default state, no review, no session
**Pre.** Fresh launch. Permissions not granted.
**Steps.**
1. Open Advanced → Real-tap prototype.
2. Observe the blocked-reasons list.

**Expected.** Four reasons listed:
- Safety review not completed.
- Accessibility service not enabled.
- Real-tap session not started.
- Per-tap consent not confirmed.
The "Request single real tap" button is disabled. No audit event for dispatch.

### QA-RT-02 — Review passed, service disabled
**Pre.** Toggle all 10 review items to passed.
**Steps.**
1. Tap "Start real-tap session".

**Expected.** Session start is REFUSED with the reason
"Accessibility service not enabled." No `realtap.session.started` audit event;
`safety.realTapBlocked` may be recorded.

### QA-RT-03 — Service enabled, review NOT passed
**Pre.** Enable ClickFlow accessibility service in system settings. Do NOT
complete the safety review.
**Steps.**
1. Open the prototype screen.
2. Try "Start real-tap session".

**Expected.** Refused with "Safety review not completed." Status badge shows
review pending.

### QA-RT-04 — Review + service, but no consent
**Pre.** Review passed, service enabled, session started.
**Steps.**
1. Wait without tapping "Request single real tap".
2. Observe.

**Expected.** Session is active; consent is **not** fresh; gate denies
dispatch if anything bypasses the UI. Blocked reason: "Per-tap consent not
confirmed."

---

## Group 2 — Consent lifecycle

### QA-RT-05 — Happy path
**Pre.** Review passed, service enabled, session active.
**Steps.**
1. Tap "Request single real tap" near the on-screen marker.
2. Read the consent dialog.
3. Tap "Confirm".

**Expected.**
- Audit: `realtap.consent.requested` → `realtap.consent.confirmed`
→ `realtap.dispatch.attempted` → `realtap.dispatch.success` (or
`dispatch.failure` if the OS rejects).
- The result chip on the prototype screen shows `DISPATCH_SUCCEEDED`
(or the failure outcome) within 1s.
- Exactly ONE tap is dispatched at the marker coordinates.
- Consent nonce is now invalid; tapping "Request single real tap" again
requires a NEW consent.

### QA-RT-06 — Consent TTL expiry (10s)
**Pre.** As QA-RT-05.
**Steps.**
1. Tap "Request single real tap".
2. Wait 11 seconds without confirming.
3. Try to confirm.

**Expected.** Confirm is refused. Audit: `realtap.consent.expired`. Result
chip shows `BLOCKED_CONSENT_EXPIRED` reasoning. No dispatch.

### QA-RT-07 — Consent reuse attempt
**Pre.** Just dispatched one tap successfully (QA-RT-05).
**Steps.**
1. Without re-requesting, attempt to confirm using any cached nonce
 (only possible via debug build / instrumentation).

**Expected.** Audit: `realtap.consent.reused`. Outcome:
`BLOCKED_CONSENT_REUSED`. No dispatch.

### QA-RT-08 — Cancel
**Pre.** Consent dialog open.
**Steps.**
1. Tap "Cancel".

**Expected.** Dialog dismissed. Audit: `realtap.consent.cancelled`. Session
remains active; nonce invalidated.

---

## Group 3 — Session lifecycle

### QA-RT-09 — Manual end session
**Pre.** Session active.
**Steps.**
1. Tap "End session".

**Expected.** Session ends. Audit: `realtap.session.ended`. Any pending
consent invalidated. Returning to the prototype screen shows session NOT
active.

### QA-RT-10 — Emergency Stop interrupts session
**Pre.** Session active, consent dialog open with 4s remaining.
**Steps.**
1. Trigger Emergency Stop (from Home, or by hardware key combo if wired).

**Expected.** Session immediately ended; consent invalidated; consent dialog
auto-dismissed. Audit: `realtap.session.ended` with
metadata `reason = emergency_stop`. Returning to prototype shows session
not active and consent expired.

### QA-RT-11 — Backgrounding the app
**Pre.** Session active, consent NOT yet requested.
**Steps.**
1. Press Home, wait 30s, return.

**Expected.** Session may still be active in-memory; if the process was
killed by the OS, session is gone (in-memory only). Either way, NO consent
survives.

---

## Group 4 — Safety review

### QA-RT-12 — Toggle each item
**Steps.**
1. Open prototype screen.
2. Toggle each of the 10 review items, then untoggle one.

**Expected.** `allPassed` flips false the moment any item is unchecked.
Session cannot be started until ALL 10 are checked again.

### QA-RT-13 — Process death resets review
**Pre.** Review passed, session NOT started.
**Steps.**
1. Force-stop the app from system settings.
2. Relaunch.

**Expected.** Review state is reset to all-unchecked. NOTHING about the
review or session was persisted to disk.

---

## Group 5 — Marker-only invariant

### QA-RT-14 — Marker-only dispatch
**Pre.** Marker placed at known coordinates.
**Steps.**
1. Drag marker to a new location.
2. Open prototype, run the happy-path scenario.

**Expected.** The tap is dispatched at the marker's exact coordinates.
There is no API path to dispatch at arbitrary coordinates — `confirmRealTap`
in the ViewModel reads from the marker position only.

### QA-RT-15 — Bulk/looped surface remains dead
**Pre.** Any state.
**Steps.**
1. Inspect SafetyCenter on the device.
2. Verify the "Real taps (bulk / looped / scheduled)" row reads
 `disabled (canRunRealTap=false)`.
3. Inspect AuditLog after running QA-RT-05.

**Expected.** No audit event of type
`realtap.dispatch.*` ever exists OUTSIDE a confirmed single-tap consent
flow. Only ONE `realtap.dispatch.success` per consent.

---

## Group 6 — Diagnostics + audit observability

### QA-RT-16 — Diagnostics mirror
**Pre.** Mid-session.
**Steps.**
1. Open Diagnostics.

**Expected.** All six new fields render and update live:
`realTapSessionActive`, `realTapSafetyReviewPassed`, `realTapConsentFresh`,
`realTapDispatchedCount`, `realTapLastOutcome`, `realTapLastEventAtMs`.

### QA-RT-17 — Audit export contains real-tap events
**Pre.** Completed at least one scenario per Group 1–3.
**Steps.**
1. Open Audit Log → Share.

**Expected.** Exported text includes the new `realtap.*` event types with
human-readable lines (EN and RU); no PII; no screenshots; no nonce values
in cleartext.

---

## Notes

- Every scenario must be re-run after any change to `RealTapController`,
`SafetyGate.canRunRealTapSingleProto`, or the consent flow in the
ViewModel.
- This document grows with each fix landed in
`docs/REAL_TAP_FIXES_LOG.md`. If a scenario uncovers a regression, write
it up there with the failing case + the fix.
