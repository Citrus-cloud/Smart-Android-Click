# ClickFlow Android

Native Android foundation for **ClickFlow** — the cross-platform click-automation project.

---

## 🇷🇺 Краткое описание (по-русски)

**Что это.** ClickFlow Android — умный автокматический кликер для Android.

**Где мы сейчас:**

- ✅ **Сделано:** Шаги 52–73.
- 🔄 **Только что сделали:** **Шаг 73** — `ScenarioPreset` + `BuiltInPresets` +
  `VisualScenarioBuilder` (add/update/remove/move/applyPreset/appendPreset,
  max 20 действий). 13 JVM-тестов. Phase 2 — ЗАВЕРШЕНА.
- ➡️ **Следующий шаг:** **Шаг 74** — Phase 3: управляемая сессия реальных
  тапов (`ControlledTapSession`): лимиты, TTL, аварийная остановка.

---

## Status

> **Phase 2 — ✅ DONE (Steps 66–73). Phase 3 starting — Step 74 next.**
>
> **Just landed — Step 73 (Visual scenario builder + presets):**
> `ScenarioPreset` / `PresetAction` / `BuiltInPresets` +
> `VisualScenarioBuilder` (CRUD + applyPreset/appendPreset, max 20) + 13 JVM tests.
> Pure domain layer — no UI, no persistence, no tap.
>
> **Next — Step 74 (Controlled tap session):** `ControlledTapSession` domain model
> gated by `canRunControlledRealTapSession()` with rate/count limits, TTL, and
> Emergency Stop support.

## Roadmap (Steps 64–84)

### Phase 1 — Android debt (64–65) — ✅
### Phase 2 — «the brain» (66–73) — ✅

- 66 ✅ · 67 ✅ · 68 ✅ · 69 ✅ · 70 ✅ · 71 ✅ · 72 ✅ · **73 ✅**

### Phase 3 — real taps on Android (Steps 74–76) — 🔄 starting

- **Step 74 ➡️** — Controlled tap session (rate/count limits, TTL, emergency stop).
- **Step 75** — Smart target → single real tap with explicit consent.
- **Step 76** — Audit + emergency stop for smart sessions.

### Phase 4 — desktop smart click (77–79) · Phase 5 — finish (80–84)

## Done so far (Steps 52–73)

- **52–67** — foundation through region selector.
- **68** — template manager (18 tests). **69** — matching engine (8 tests).
- **70** — image-target controller (10 tests). **71** — OCR stub (12 tests).
- **72** — text-target controller (11 tests). **73** — visual builder + presets (13 tests).

## Build & Test

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Safety model

`SafetyGate.canRunRealTap()` = `false`. Phase 2 is preview-only. Phase 3 introduces
controlled real taps behind four independent safety gates.

## License

MIT.
