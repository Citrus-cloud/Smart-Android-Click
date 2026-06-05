# ClickFlow Android

Native Android foundation for **ClickFlow** — the cross-platform click-automation project.

---

## 🇷🇺 Краткое описание

**Где мы сейчас:**

- ✅ **Сделано:** Шаги 52–75.
- 🔄 **Только что сделали:** **Шаг 75** — `SmartTargetTapController`:
  5-шаговая проверка (request → session → gate → consent → drift)
  + `recordConsent` / `dispatch`. 12 JVM-тестов.
- ➡️ **Следующий шаг:** **Шаг 76** — аудит + Emergency Stop для
  smart-сессий.

---

## Status

> **Phase 3 — Step 75 done, Step 76 next.**
>
> **Just landed — Step 75:** `SmartTargetTapRequest` + `SmartTargetTapResult` +
> `SmartTargetTapController` (5-check dispatch chain, 10s consent TTL, 2%
> coordinate-drift check, recordTap on success) + 12 JVM tests.
>
> **Next — Step 76:** audit events + Emergency Stop wiring for smart tap sessions.

## Roadmap

### Phase 3 (74–76) — 🔄 in progress
- **74 ✅** · **75 ✅** · **76 ➡️** (audit + emergency stop)

### Phase 4 (77–79) · Phase 5 (80–84)

## Done so far (Steps 52–75)

- **52–73** — foundation through visual builder.
- **74** — controlled tap session (14 tests).
- **75** — smart-target tap controller (12 tests).

## Build & Test

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Safety model

`SafetyGate.canRunRealTap()` = `false`. `dispatch()` returns SESSION_GATE_CLOSED
until Step 76. Bulk/background taps never allowed.

## License

MIT.
