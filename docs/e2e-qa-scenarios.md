# E2E QA Scenarios — ClickFlow Android (Step 81)

End-to-end test scenarios for ClickFlow Android.
All scenarios must pass before public beta (Step 83).

---

## Scenario 1 — Safety gate lifecycle

1. Launch app — `SafetyGate.canRunRealTap()` = `false`.
2. Enable all 4 flags via `SafetyGate`: `updateReviewPassed(true)`,
   `updateAccessibility(true)`, `updateSession(true)`, `updateConsentFresh(true)`.
3. `canRunControlledRealTapSession(id)` returns `true`.
4. Call `updateConsentFresh(false)` — gate closes.
5. Emergency stop: `SmartSessionEmergencyStop.execute()` —
   session terminates, consent cleared, audit event recorded.

**Expected:** Gate state matches expectations at every step.

---

## Scenario 2 — Controlled tap session lifecycle

1. Open gate (all 4 flags true).
2. `startSession("s1", maxTaps=3, ttlMs=10_000)` → `SessionResult.Ok`.
3. `hasActiveSession()` = `true`.
4. `evaluateTap()` → `GATE_CLOSED` (bulk `canRunRealTap()` = `false`).
5. `endSession()` → `hasActiveSession()` = `false`.

---

## Scenario 3 — Smart target tap request

1. Create `CaptureRegion(0.3f, 0.3f, 0.7f, 0.7f)`.
2. Create `SmartTargetTapRequest("s1", IMAGE_TARGET, region, nowMs)`.
3. `tapX` = 0.5, `tapY` = 0.5, `isValid` = true.
4. `SmartTargetTapController.recordConsent(request)`.
5. `dispatch(request)` → `SESSION_GATE_CLOSED` (bulk gate still false).

---

## Scenario 4 — Audit log

1. Create `SmartSessionAuditLog(maxEvents=10)`.
2. Record 10 events → `count` = 10.
3. Record 11th event → oldest discarded, `count` = 10.
4. `eventsOfType(TAP_BLOCKED)` returns only TAP_BLOCKED entries.
5. `exportText()` non-empty.
6. `clear()` → `count` = 0.

---

## Scenario 5 — Emergency stop

1. Open gate, start session, record consent.
2. Call `SmartSessionEmergencyStop.execute("test")`.
3. `sessionManager.hasActiveSession()` = `false`.
4. `tapController.consent` = `null`.
5. `auditLog.eventsOfType(SESSION_EMERGENCY_STOPPED).size` = 1.
6. `events.first().detail` = `"test"`.

---

## Scenario 6 — TTL expiry

1. Start session `ttlMs=5000`, advance clock by 6000 ms.
2. `session.isActive(now)` = `false`.
3. `hasActiveSession()` = `false`.

---

## Scenario 7 — Template matching pipeline (unit)

1. Add template via `TemplateManager.add(template)`.
2. Create `MatchResult` with score 0.92.
3. `ImageTargetController.evaluate(templateId, listOf(result))`
   → `ImageTargetOutcome.Matched`.
4. Remove template → `evaluate` → `ImageTargetOutcome.Error("template_not_found")`.

---

## Scenario 8 — OCR pipeline (unit)

1. Create `StubOcrProvider` with injected regions.
2. `OcrController.findText("hello")` → returns matching `OcrTextRegion`.
3. `OcrController.findExact("Hello", caseSensitive=true)` → no match.
4. `OcrController.bestMatch("helo")` → returns closest region.

---

## Scenario 9 — Visual builder + presets

1. `VisualScenarioBuilder.add(PresetAction(TAP, ...))` → `Ok`.
2. Add 20 actions → 21st returns `Error("too_many_actions")`.
3. `applyPreset(BuiltInPresets.TAP_AND_WAIT)` → replaces all, 2 actions.
4. `BuiltInPresets.ALL` → both presets valid.

---

## Scenario 10 — String resources (localization)

1. Device locale = `ru` → `R.string.emergency_stop_button` = "Аварийная остановка".
2. Device locale = `en` → `R.string.emergency_stop_button` = "Emergency Stop".
3. All keys present in both `values/strings.xml` and `values-ru/strings.xml`.
