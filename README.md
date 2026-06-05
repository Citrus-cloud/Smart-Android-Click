# ClickFlow Android

Native Android foundation for **ClickFlow** — the cross-platform click-automation project.
This is a **separate native Android application** (Kotlin + Jetpack Compose), **not** an Electron
port and **not** a runtime copy of the desktop code.

---

## 🇷🇺 Краткое описание (по-русски)

**Что это.** ClickFlow Android — умный автокликер для Android.

**Где мы сейчас:**

- ✅ **Сделано:** Шаги 52–70.
- 🔄 **Только что сделали:** **Шаг 70** — `ImageTargetController`: запросит шаблон из
  `TemplateManager`, запускает `TemplateMatcher.evaluateBest`, возвращает
  `ImageTargetOutcome` (Matched / NoMatch / Error) с `highlight`. 10 JVM-тестов.
- ➡️ **Следующий шаг:** **Шаг 71** — on-device OCR: интерфейс + stub для
  распознавания текста на устройстве (без реального ML пока).

---

## Status

> **Phase 2 («the brain» on Android) — in progress. Current: Step 70 done, Step 71 next.**
>
> **Just landed — Step 70 (Image-target controller):** `ImageTargetResult` +
> `ImageTargetOutcome` (sealed: Matched / NoMatch / Error) + `ImageTargetController`
> (`evaluate(templateId, candidates)` wiring `TemplateManager` → `TemplateMatcher` →
> typed outcome with `highlight`). 10 JVM tests. Pure float-score preview — no bitmaps,
> no tap.
>
> **Next — Step 71 (On-device OCR stub):** `OcrResult` + `OcrProvider` interface +
> `StubOcrProvider` (returns synthetic text regions), JVM tests. Brain-first, no ML yet.
>
> Bulk real taps remain hard-disabled by `SafetyGate.canRunRealTap() == false`.

## What this app is (and does)

ClickFlow Android is a **smart auto-clicker**: an automation tool that can decide *where* to tap
by looking at the screen — either by matching a reference image (template) or by reading text
(on-device OCR) — and then perform that tap **only with the user's explicit, audited consent**.

## Roadmap (Steps 64–84)

### Phase 1 — Android debt (Steps 64–65) — ✅ done

### Phase 2 — «the brain» on Android (Steps 66–73) — 🔄 in progress

- **Step 66** — Screen capture (MediaProjection). ✅ done.
- **Step 67** — Region selector. ✅ done.
- **Step 68** — Template manager. ✅ done.
- **Step 69** — Template matching engine. ✅ done.
- **Step 70** — Image-target controller. ✅ done.
- **Step 71** — ➡️ next. On-device OCR (interface + stub, no real ML yet).
- **Step 72** — «Text target» scenario type (simulated).
- **Step 73** — Visual scenario builder + presets.

### Phase 3 — real taps on Android (Steps 74–76)

### Phase 4 — desktop smart click (Steps 77–79, repo `Mine`)

### Phase 5 — finish (Steps 80–84)

## Done so far (Steps 52–70)

- **52–67** — foundation, scenarios, profiles, audit, backup, release, simple clicker,
  real-tap prototype + tests, screen capture, region selector.
- **68** — template manager (`CaptureTemplate` + `TemplateManager` + 18 JVM tests).
- **69** — template matching engine (`MatchResult` + `TemplateMatcher` + 8 JVM tests).
- **70** — image-target controller (`ImageTargetResult` + `ImageTargetOutcome` + `ImageTargetController` + 10 JVM tests).

## Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## App identity

| | |
|---|---|
| Package | `com.clickflow.android` |
| minSdk | 26 |
| targetSdk / compileSdk | 34 |
| versionName | 0.1.0-prealpha |
| Stack | Kotlin, Jetpack Compose, Material 3 |

## Safety model (summary)

`SafetyGate.canRunRealTap()` returns `false` (hard-coded). Phase 2 is preview-only.
No hidden automation; no captcha/anti-bot; no ad-click, banking, or payment automation.

## License

MIT.
