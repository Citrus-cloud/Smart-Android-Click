# ClickFlow Android

Native Android foundation for **ClickFlow** — the cross-platform click-automation project.

---

## 🇷🇺 Краткое описание (по-русски)

**Что это.** ClickFlow Android — умный автокликер для Android.

**Где мы сейчас:**

- ✅ **Сделано:** Шаги 52–72.
- 🔄 **Только что сделали:** **Шаг 72** — `TextTargetController`: OCR → `bestMatch` →
  `TextTargetOutcome` (Matched/NoMatch/Error) с `highlight`. 11 JVM-тестов.
- ➡️ **Следующий шаг:** **Шаг 73** — визуальный строитель сценариев +
  пресеты (модель + валидация, без UI пока).

---

## Status

> **Phase 2 — in progress. Step 72 done, Step 73 next.**
>
> **Just landed — Step 72:** `TextTargetResult` + `TextTargetOutcome` +
> `TextTargetController` (validate → OCR → bestMatch → typed outcome) + 11 JVM tests.
>
> **Next — Step 73:** Visual scenario builder + presets model (domain layer only).

## Roadmap (Steps 64–84)

### Phase 2 — «the brain» on Android (Steps 66–73) — 🔄 in progress

- Step 66 ✅ · Step 67 ✅ · Step 68 ✅ · Step 69 ✅ · Step 70 ✅ · Step 71 ✅ · **Step 72 ✅**
- **Step 73 ➡️** — Visual scenario builder + presets.

### Phase 3 (Steps 74–76) · Phase 4 (Steps 77–79) · Phase 5 (Steps 80–84)

## Done so far (Steps 52–72)

- **52–67** — foundation through region selector.
- **68** — template manager (18 tests). **69** — matching engine (8 tests).
- **70** — image-target controller (10 tests). **71** — OCR stub (12 tests).
- **72** — text-target controller (11 tests).

## Build & Test

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Safety model

`SafetyGate.canRunRealTap()` = `false`. Phase 2 is preview-only.

## License

MIT.
