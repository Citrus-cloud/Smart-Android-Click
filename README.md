# ClickFlow Android

Native Android foundation for **ClickFlow** — the cross-platform click-automation project.

---

## 🇷🇺 Краткое описание

**Где мы сейчас:**

- ✅ **Сделано:** Шаги 52–76. **Phase 3 завершена.**
- 🔄 **Только что сделали:** **Шаг 76** — `SmartSessionAuditLog` (bounded, exportText)
  + `SmartSessionEmergencyStop` (clearConsent → stop → audit). 13 JVM-тестов.
- ➡️ **Следующий шаг:** **Шаг 77** — в репозитории `Mine` (Electron/desktop),
  safety review для реального ввода.

---

## Status

> **Phase 3 (Android real taps) — ✅ COMPLETE.**
>
> **Just landed — Step 76:** `SmartSessionAuditLog` (bounded 200 events,
> eventsOfType, clear, exportText) + `SmartSessionEmergencyStop`
> (clearConsent → emergencyStop → audit SESSION_EMERGENCY_STOPPED) + 13 JVM tests.
>
> **Next — Step 77 (repo Mine):** desktop real-input safety review.

## Roadmap

### Phase 1 (64–65) ✅ · Phase 2 (66–73) ✅ · Phase 3 (74–76) ✅

### Phase 4 — desktop smart click (Steps 77–79, repo `Mine`) — 🔄 starting

- **Step 77 ➡️** — real-input safety review (Electron).
- **Step 78** — real `image_click` / `text_click` under the gate.
- **Step 79** — QA + publish `v1.0.0-alpha.1`.

### Phase 5 (80–84)
- **80** parity + localization · **81** e2e QA · **82** user docs · **83** public beta · **84** `v1.0.0`.

## Done so far (Steps 52–76)

- **52–73** — foundation through visual builder.
- **74** controlled tap session · **75** smart-target controller · **76** audit + E-stop.

## Build & Test

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Safety model

`SafetyGate.canRunRealTap()` = `false`. All three Phase-3 layers (session,
consent, audit) are in place; actual `dispatchGesture` wiring is deferred.
Bulk/background taps never allowed.

## License

MIT.
