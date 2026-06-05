# ClickFlow Android

Native Android foundation for **ClickFlow** — the cross-platform click-automation project.

---

## 🇷🇺 Краткое описание

**Что это.** ClickFlow Android — умный автокликер для Android.

**Где мы сейчас:**

- ✅ **Сделано:** Шаги 52–74.
- 🔄 **Только что сделали:** **Шаг 74** — `ControlledTapSession` + `ControlledTapSessionManager`
  (лимит maxTaps 1–10, TTL 1–60 с, проверка gate, Emergency Stop). 14 JVM-тестов.
- ➡️ **Следующий шаг:** **Шаг 75** — умная цель → единичный реальный тап с
  явным согласием.

---

## Status

> **Phase 3 — real taps on Android. Step 74 done, Step 75 next.**
>
> **Just landed — Step 74:** `ControlledTapSession` (maxTaps 1–10, TTL, recordTap,
> remainingTaps, remainingTtlMs, terminate) + `ControlledTapSessionManager`
> (startSession / endSession / emergencyStop / evaluateTap) + 14 JVM tests.
> `evaluateTap()` returns `GATE_CLOSED` (bulk gate always false) until Step 75.
>
> **Next — Step 75:** wire a smart target (image or text) into a single real tap
> with explicit consent inside a controlled session.

## Roadmap (Steps 64–84)

### Phase 1 (64–65) ✅ · Phase 2 (66–73) ✅

### Phase 3 — real taps on Android (Steps 74–76) — 🔄 in progress

- **Step 74 ✅** — Controlled tap session domain model + manager.
- **Step 75 ➡️** — Smart target → single real tap with explicit consent.
- **Step 76** — Audit + emergency stop for smart sessions.

### Phase 4 (77–79) · Phase 5 (80–84)

## Done so far (Steps 52–74)

- **52–67** — foundation through region selector.
- **68–70** — template manager / matching engine / image-target controller.
- **71–73** — OCR stub / text-target controller / visual builder + presets.
- **74** — controlled tap session (14 tests).

## Build & Test

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Safety model

`SafetyGate.canRunRealTap()` = `false`. Phase 3 introduces controlled sessions
gated by four independent safety checks; bulk/background taps never allowed.

## License

MIT.
