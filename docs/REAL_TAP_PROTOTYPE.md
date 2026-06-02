# Single Real Tap — Prototype (Step 62)

> Status: **Prototype**. Behind a per-session Safety Review and a per-tap consent.  
> Scope: **Exactly one real tap per explicit consent**. Bulk / looped / scenario-driven real taps remain hard-disabled by `SafetyGate`.

---

## Why this exists

For Steps 55–61 the app has been simulation-only:

- `SafetyGate.canRunRealTap()` always returns `false`.
- `DiagnosticsState.simulationOnly = true`, `realTapsEnabled = false`.
- The Accessibility service is declared but performs no automation.

Step 62 introduces a **narrow, audited path** that allows one real tap at a time, gated by:

1. The user passing a 10-item Safety Review (per session).
2. An explicit per-tap consent dialog with a 10-second TTL and a single-use nonce.
3. `SafetyGate.canRunRealTapSingleProto(...)` returning `true` for THIS specific request.
4. The `ClickFlowAccessibilityService` being bound, alive, and capable of `dispatchGesture`.

Even with all four green, **only one** `dispatchGesture` call is issued. There is no loop, no repeat, no batching.

---

## Architecture

```
UI (RealTapPrototypeScreen)
│
▼
ClickFlowViewModel.requestSingleRealTap(x, y)
│
▼
RealTapController.dispatch(RealTapRequest)
│
├── 1. SafetyGate.canRunRealTapSingleProto(state, request) -> Boolean
│       (reads SafetyState: review passed, session active, consent fresh, API >= 24)
│
├── 2. ClickFlowAccessibilityService.liveInstance ?: return BLOCKED_NO_SERVICE
│
├── 3. service.performSingleTap(x, y) -> dispatchGesture(...)
│
├── 4. AuditEvent.realtap.* emitted (request, dispatched, success/fail)
│
└── 5. DiagnosticsState mirrors updated (counts, last outcome)
```

Bulk path (`canRunRealTap()`) and scenario runner continue to return `false`. They share no code with the single-tap path.

---

## Files (Step 62)

| Layer | File | Role |
|---|---|---|
| Model | `realtap/RealTapRequest.kt` | Immutable request: target (x,y), nonce, createdAt |
| Model | `realtap/RealTapResult.kt` + `RealTapOutcome` enum | Outcome of one attempt |
| Model | `realtap/RealTapSession.kt` | In-memory session token (not persisted) |
| Model | `realtap/RealTapSafetyReview.kt` | 10 review items + pass state |
| Safety | `safety/SafetyState.kt` | +3 fields: `realTapSafetyReviewPassed`, `realTapSessionActive`, `realTapConsentFresh` |
| Safety | `safety/SafetyGate.kt` | +`canRunRealTapSingleProto()` and `getSingleProtoBlockedReasons()` |
| Service | `permissions/ClickFlowAccessibilityService.kt` | `performSingleTap()` + `liveInstance` singleton |
| Controller | `realtap/RealTapController.kt` | Gate → service → audit → mirror |
| Audit | `audit/AuditEvent.kt` | +14 `realtap.*` constants |
| Diagnostics | `diagnostics/DiagnosticsState.kt` + `DiagnosticsManager.kt` | +6 prototype mirror fields |
| Strings | `res/values/strings.xml`, `res/values-ru/strings.xml` | +24 keys each |
| UI | `ui/RealTapPrototypeScreen.kt` | Review + session + tap UX |

---

## Invariants (must hold across the codebase)

1. **Bulk stays blocked.** `SafetyGate.canRunRealTap()` continues to return `false`. The scenario runner never calls the real-tap path.
2. **Single dispatch entry.** `ClickFlowAccessibilityService.performSingleTap()` is the ONLY place that calls `dispatchGesture`. No other class invokes it directly.
3. **All four flags required.** `canRunRealTapSingleProto` returns `true` only when ALL of these are true: review passed, session active, consent fresh (TTL 10s, nonce unused), API ≥ 24, service bound (`liveInstance != null`).
4. **Consent is single-use.** Once a nonce is consumed by `RealTapController`, it is invalidated regardless of outcome. A second tap requires a new consent dialog.
5. **Session lives in memory only.** `RealTapSession` is not serialized, not backed up, not exported. Cold start = no session.
6. **Emergency Stop ends the session.** Pressing Emergency Stop sets `realTapSessionActive = false` and invalidates any pending consent.
7. **Audit always.** Every request, dispatch, and outcome emits an `AuditEvent.realtap.*` event, success or failure. Audit log is the source of truth for "did a real tap happen".
8. **No IPC for the service.** `ClickFlowAccessibilityService.liveInstance` is a process-local singleton. No bound service, no AIDL, no broadcasts.

---

## Out of scope for Step 62

- Multi-tap, gestures, swipes, long-press.
- Scheduled or scenario-driven real taps.
- Persisting `realTapSafetyReviewPassed` across sessions.
- Cross-process real-tap requests.
- Replacing the simulation runner with the real-tap path.

These remain blocked by `SafetyGate.canRunRealTap() == false` and the absence of any code path that calls `RealTapController` from the scenario runner.

---

## See also

- `docs/SAFETY_REVIEW_CHECKLIST.md` — the 10 review items, verbatim.
- `docs/CONSENT_FLOW.md` — the per-tap consent UX and TTL/nonce contract.
- `PROJECT_CONTEXT.md` — Step 62 section.
