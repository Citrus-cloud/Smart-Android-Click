# Per-Tap Consent Flow — Single Real Tap

Every real tap requires its own consent. Consent is **single-use**, has a **10-second TTL**, and is bound by a **nonce**. This document is the contract.

---

## Lifecycle

```
[User has session active + review passed]
         │
         │  taps "Request single real tap"
         ▼
ViewModel.requestSingleRealTap(x, y)
         │
         ▼
RealTapController.beginConsent(target=(x,y))
         │
         │  generates nonce (UUID v4), createdAt = now
         │  stores PendingConsent in memory
         │  sets SafetyState.realTapConsentFresh = true
         ▼
UI shows ConsentDialog
         │
         │  body: "ONE real tap at (x, y). Consent expires in 10 seconds."
         │  buttons: [Cancel] [Confirm tap]
         ▼
┌────────┴────────┐
│                 │
[Confirm]          [Cancel / timeout / Emergency Stop]
│                 │
▼                 ▼
RealTapController.dispatch(request)   RealTapController.invalidateConsent(reason)
│                                  │
│  checks ALL of:                  │  audit: realtap.consent.cancelled
│   - consent.nonce matches        │  state: realTapConsentFresh = false
│   - now - createdAt <= 10000 ms  │
│   - review passed                │
│   - session active               │
│   - API >= 24, service bound     │
│                                  │
│  marks consent CONSUMED          │
│  (regardless of outcome)         │
│                                  │
▼                                  │
service.performSingleTap(x, y)        │
│                                  │
▼                                  │
audit: realtap.dispatch.success      │
or realtap.dispatch.failure        │
│                                  │
▼                                  │
DiagnosticsState mirrors updated      │
```

---

## The contract

### 1. TTL

- Consent is valid for **exactly 10 seconds** from `createdAt`.
- The check is `(now - createdAt) <= 10_000 ms`. Equal to 10000 is still valid; strictly greater is expired.
- Expiry is checked **server-side in `RealTapController`**, not just by the UI countdown. A delayed confirm tap that arrives past 10s is rejected with `realtap.consent.expired`.

### 2. Nonce

- Generated as a random UUID (version 4) at consent creation.
- Stored alongside the consent in memory only.
- Required as part of the dispatch call. A mismatched or missing nonce returns `BLOCKED_INVALID_CONSENT`.
- Marked CONSUMED on first successful dispatch attempt — even if the dispatch itself fails. A second dispatch with the same nonce is rejected with `realtap.consent.reused`.

### 3. Scope

- A consent is bound to a specific (x, y) target captured at the moment "Request" was tapped. The user cannot move the marker after the dialog opens and have the new position used. If the target needs to change, the consent must be cancelled and a fresh one requested.

### 4. Invalidation triggers

A pending consent is invalidated (`realTapConsentFresh = false`) by any of:

- The user pressing Cancel.
- The 10-second TTL elapsing.
- Emergency Stop being pressed.
- The session ending for any reason.
- A revocation of overlay or accessibility permission.
- The app going to background AND not returning within the TTL window.
- Any audit-emitting reset on `SafetyState`.

### 5. Audit events

Every state transition emits a `realtap.*` audit event:

| Event constant | When |
|---|---|
| `realtap.consent.requested` | Consent dialog opened |
| `realtap.consent.confirmed` | User pressed Confirm before expiry |
| `realtap.consent.cancelled` | User pressed Cancel |
| `realtap.consent.expired` | TTL elapsed without confirmation |
| `realtap.consent.reused` | Same nonce presented twice (should be impossible from UI; defence in depth) |
| `realtap.dispatch.attempted` | Controller began the gated dispatch |
| `realtap.dispatch.blocked` | Gate returned false (reason recorded) |
| `realtap.dispatch.success` | `dispatchGesture` reported onCompleted |
| `realtap.dispatch.failure` | `dispatchGesture` reported onCancelled or threw |
| `realtap.session.started` | Session began (after review pass) |
| `realtap.session.ended` | Session ended (any reason) |
| `realtap.review.passed` | Safety Review acknowledged |
| `realtap.review.reset` | Safety Review reset |

---

## UI requirements

- The dialog MUST display the exact target `(x, y)` and a live "expires in Ns" countdown.
- The Confirm button MUST be disabled while the countdown is ≤ 0.
- The Cancel button MUST always be enabled.
- Tapping anywhere outside the dialog cancels (same as pressing Cancel).
- Emergency Stop reachable from any screen also cancels.

---

## Defence in depth

Even though the UI prevents most edge cases, `RealTapController` validates every consent **server-side**:

- Re-checks `SafetyGate.canRunRealTapSingleProto` immediately before `service.performSingleTap`.
- Re-checks the consent has not been consumed.
- Re-checks the TTL (in case of delays between UI confirm and controller execution).

If any check fails, the controller emits `realtap.dispatch.blocked` with the reason and returns. No tap is performed.

---

## Out of scope

- Long-lived consent grants ("trust this app for 5 minutes").
- Per-app consent persistence.
- Biometric or PIN confirmation on the consent dialog.

These may be revisited in future steps, but Step 62 is intentionally minimal: one tap, one consent, fresh every time.
