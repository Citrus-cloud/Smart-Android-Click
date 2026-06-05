# ClickFlow Android

Native Android foundation for **ClickFlow** — the cross-platform click-automation project.
This is a **separate native Android application** (Kotlin + Jetpack Compose), **not** an Electron
port and **not** a runtime copy of the desktop code.

---

## 🇷🇺 Краткое описание (по-русски)

**Что это.** ClickFlow Android — умный автокликер для Android.

**Где мы сейчас:**

- ✅ **Сделано:** Шаги 52–71.
- 🔄 **Только что сделали:** **Шаг 71** — OCR-абстракция: `OcrTextRegion` + `OcrResult` +
  `interface OcrProvider` + `StubOcrProvider` + `OcrController` (findText / findExact /
  bestMatch). 12 JVM-тестов. ML Kit ещё не подключён.
- ➡️ **Следующий шаг:** **Шаг 72** — сценарий «Text target»: `TextTargetController`
  (запрос → OCR → `bestMatch` → `TextTargetOutcome`).

---

## Status

> **Phase 2 («the brain» on Android) — in progress. Current: Step 71 done, Step 72 next.**
>
> **Just landed — Step 71 (On-device OCR stub):** `OcrTextRegion` / `OcrResult` /
> `OcrProvider` interface / `StubOcrProvider` / `OcrController` with 12 JVM tests.
> Pure abstraction — no ML Kit, no frame bytes, no tap.
>
> **Next — Step 72 (Text-target scenario controller):** wires `OcrController` into a
> `TextTargetController` that returns a typed `TextTargetOutcome` with a highlighted
> bounds region (preview only, no tap).
>
> Bulk real taps remain hard-disabled by `SafetyGate.canRunRealTap() == false`.

## What this app is (and does)

ClickFlow Android is a **smart auto-clicker**: find *where* to tap by image template
matching or on-device OCR, then tap with explicit consent and full audit.

## Roadmap (Steps 64–84)

### Phase 2 — «the brain» on Android (Steps 66–73) — 🔄 in progress

- **Step 66** — Screen capture. ✅
- **Step 67** — Region selector. ✅
- **Step 68** — Template manager. ✅
- **Step 69** — Template matching engine. ✅
- **Step 70** — Image-target controller. ✅
- **Step 71** — On-device OCR stub. ✅
- **Step 72** — ➡️ next. Text-target scenario controller.
- **Step 73** — Visual scenario builder + presets.

### Phase 3 — real taps (Steps 74–76)
### Phase 4 — desktop smart click (Steps 77–79)
### Phase 5 — finish (Steps 80–84)

## Done so far (Steps 52–71)

- **52–67** — foundation through region selector.
- **68** — template manager (18 JVM tests).
- **69** — template matching engine (8 JVM tests).
- **70** — image-target controller (10 JVM tests).
- **71** — OCR stub: `OcrProvider` interface + `StubOcrProvider` + `OcrController` (12 JVM tests).

## Build & Test

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Safety model

`SafetyGate.canRunRealTap()` = `false` (hard-coded). Phase 2 is preview-only.

## License

MIT.
