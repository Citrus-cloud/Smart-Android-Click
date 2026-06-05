# ClickFlow Android

Native Android foundation for **ClickFlow** — the cross-platform click-automation project.
This is a **separate native Android application** (Kotlin + Jetpack Compose), **not** an Electron
port and **not** a runtime copy of the desktop code.

---

## 🇷🇺 Краткое описание (по-русски)

**Что это.** ClickFlow Android — это умный автокликер для Android. Цель — приложение,
которое умеет само определять по изображению на экране (скриншоту) или по распознанным
кнопкам/тексту, куда нужно нажать, и выполнять это безопасно и под полным контролем
пользователя.

**Для чего.** Автоматизация рутинных повторяющихся нажатий, которые пользователь иначе
делал бы вручную. Всё строится вокруг безопасности: никаких скрытых действий, никакого
обхода капчи/антиботов, никаких кликов по рекламе, банкингу или платежам.

**Какие функции должно выполнять после релиза (v1.0.0):**

1. Простой кликер — нажатия по точке/точкам с интервалом и повторами.
2. Сценарии — многошаговые последовательности действий (тап / пауза / заметка) с профилями.
3. **Умные цели** — находить куда нажать по шаблону-картинке (template matching) или по тексту
   (OCR на устройстве) — анализ скриншота, без отправки данных наружу.
4. Реальные нажатия — только под явным согласием, с аудитом и кнопкой аварийной остановки;
   массовые/фоновые нажатия запрещены.
5. Безопасность и прозрачность — Safety Center, журнал аудита, диагностика, резервное копирование.

**Где мы сейчас (коротко):**

- ✅ **Сделано:** Шаги 52–66 (фундамент, сценарии, профили, аудит, бэкап, релиз pre-alpha,
  простой кликер, прототип реального тапа + тесты, захват экрана через MediaProjection).
- 🔄 **Только что сделали:** **Шаг 67** — выбор области (Region Selector): нормализованная
  геометрия `CaptureRegion` + чистый `RegionSelectorController` (выделение прямоугольника
  на кадре) с юнит-тестами. Только геометрия — без захвата, без анализа, ничего не сохраняется.
- ➡️ **Следующий шаг (предпочтительный):** **Шаг 68** — менеджер шаблонов: реестр целевых
  изображений-эталонов и их параметров (порог совпадения, область) для последующего поиска.

Полный план завершения проекта (шаги 64–84) ведётся в Notion. Ниже — подробности на английском.

---

## Status

> **Phase 2 («the brain» on Android) — in progress. Current: Step 67 done, Step 68 next.**
>
> Phase 1 (Android debt: Steps 64–65) and Step 66 (screen capture, both parts) are complete.
> Phase 2 gives the app «eyes» (screen capture → region → template matching → OCR) so it can
> decide *where* to tap. All of Phase 2 is **simulation / preview only**: the app shows what it
> *would* tap, it does not tap.
>
> **Just landed — Step 67 (Region Selector):** `capture/CaptureRegion.kt` (a resolution-independent
> normalized rectangle) + `capture/RegionSelectorController.kt` (a framework-free begin / update /
> commit / setRegion / clear state machine) with a full JVM JUnit 4 suite
> (`RegionSelectorControllerTest.kt`). It records *where* on a captured frame a later match should
> look — pure geometry, no capture, no analysis, nothing persisted.
>
> **Next — Step 68 (Template Manager):** a pure, testable registry of target reference images
> (id / name / dimensions / match threshold / optional region) before any bitmap or disk I/O — same
> «brain first, I/O later» pattern used for capture.
>
> Bulk, looped, and scenario-driven real taps remain hard-disabled by
> `SafetyGate.canRunRealTap() == false`. The latest published APK is still the Step 60 build at
> [`android-v0.1.0-prealpha`](https://github.com/Citrus-cloud/Smart-Android-Click/releases/tag/android-v0.1.0-prealpha)
> (Simple Clicker UX, simulation-only).

## What this app is (and does)

ClickFlow Android is a **smart auto-clicker**: an automation tool that can decide *where* to tap
by looking at the screen — either by matching a reference image (template) or by reading text
(on-device OCR) — and then perform that tap **only with the user's explicit, audited consent**.

**Functions the released app (v1.0.0) is intended to provide:**

- **Simple Clicker** — tap one or more points at a chosen interval and repeat count.
- **Scenarios** — ordered multi-step sequences (simulated tap / wait / note), organized by profiles.
- **Smart targets** — find *where* to tap from a screenshot:
  - **Image target** (template matching with a confidence score), and
  - **Text target** (on-device OCR), with the match highlighted before any action.
- **Real taps** — a single real tap per explicit consent, fully gated and audited; later a
  rate-limited controlled session. **Bulk / looped / background real taps are never allowed.**
- **Safety & transparency** — Safety Center, persistent + exportable audit log, Diagnostics,
  backup import/export, and an always-available Emergency Stop.

Everything visual (capture, template matching, OCR) runs **on-device**; frames are held in memory
and never persisted or exported.

## Relation to desktop ClickFlow

The desktop app ([Citrus-cloud/Mine](https://github.com/Citrus-cloud/Mine)) reached
`v1.0.0-alpha.1`: Electron app, click simulation/dry-run, a hard-gated real-coordinate-click
alpha, image/text/OCR simulation, Visual Builder, Scenario Presets, Safety Center, Audit Logs,
Diagnostics — and **no** prohibited automation. ClickFlow Android reuses the **product concepts
and safety philosophy**, but is an independent native codebase. Desktop code is **not** bundled
or executed on Android.

## Roadmap (Steps 64–84)

The completion plan is tracked in Notion and grouped into 5 phases. Numbering continues globally.

### Phase 1 — Android debt (Steps 64–65) — ✅ done

- **Step 64** — clean `RealTapController` wiring end-to-end, granular `realtap.*` audit events,
  marker-only invariant in `confirmRealTap`, and a build fix (duplicate `SafetyState` removed).
- **Step 65** — first automated test coverage: pure-JVM JUnit 4 suites for `SafetyGate`,
  `RealTapController`, `RealTapSession`, and `RealTapSafetyReview`.

### Phase 2 — «the brain» on Android (Steps 66–73) — 🔄 in progress (simulation / preview only)

- **Step 66 — Screen capture (MediaProjection). ✅ done.** One-time permission, a single frame
  held in memory (never to disk), capture only — no analysis.
  - **Part 1 — ✅ done:** pure-Kotlin capture lifecycle controller + state + JVM tests.
  - **Part 2 — ✅ done:** real `ScreenCaptureService`, Activity consent, capture UI, manifest perms.
- **Step 67 — Region selector. ✅ done.** Normalized `CaptureRegion` geometry + framework-free
  `RegionSelectorController` (draw a rectangle on the frame) + JVM tests. Geometry only.
- **Step 68 — ➡️ next.** Template manager (registry of target reference images + match params).
- **Step 69** — Template-matching engine + confidence + highlight (show only, no tap).
- **Step 70** — «Image target» scenario type (find → point, simulated).
- **Step 71** — On-device OCR (ML Kit on-device or Tesseract; manual / per-session).
- **Step 72** — «Text target» scenario type (simulated).
- **Step 73** — Visual scenario builder + presets.

### Phase 3 — real taps on Android (Steps 74–76)

- **Step 74** — controlled tap session under `canRunControlledRealTapSession()` (rate / count
  limits, consent TTL, emergency stop).
- **Step 75** — link a smart target → a single real tap with explicit consent (bulk still forbidden).
- **Step 76** — audit + emergency stop for smart sessions.

### Phase 4 — desktop smart click (Steps 77–79, repo `Mine`)

- **Step 77** — real-input safety review.
- **Step 78** — enable real `image_click` / `text_click` under the gate (one action per confirm).
- **Step 79** — QA + publish `v1.0.0-alpha.1`.

### Phase 5 — finish (Steps 80–84)

- **Step 80** — desktop↔android parity + RU/EN localization.
- **Step 81** — end-to-end QA + invariant checks.
- **Step 82** — user docs.
- **Step 83** — public beta (both platforms).
- **Step 84** — stabilize + release `v1.0.0`.

**Cross-cutting invariants (never violated):** no captcha / anti-bot / ad-click / banking /
protected-app automation; no hidden background input; real input only with explicit consent +
audit + emergency stop.

## Done so far (Steps 52–67, intact)

- **52** — project foundation: Kotlin + Compose shell, simulation engine, Safety Center,
  Diagnostics, docs.
- **53** — scenario UI + local JSON storage (with corrupted-storage fallback).
- **54** — multi-step scenarios + in-memory audit log.
- **55** — persistent + exportable audit log; profiles foundation; scenarios bound to profiles.
- **56** — backup import/export (text JSON, merge strategies, no permissions).
- **57** — pre-alpha build/QA + release prep.
- **58** — published pre-release `android-v0.1.0-prealpha` (debug APK).
- **59** — Simple Clicker UX + draggable in-app marker + clean Material 3 theme.
- **60** — refreshed the pre-release APK with the Simple Clicker UX.
- **61** — permissions skeleton (overlay + no-op accessibility service; no automation).
- **62** — single real-tap prototype (gated, audited, per-tap consent; bulk still blocked).
- **63** — real-tap prototype hardening (live SafetyGate state, granular audit groundwork).
- **64** — `RealTapController` wired end-to-end + granular audit + marker invariant + build fix.
- **65** — pure-JVM unit-test suite for the safety / real-tap domain.
- **66** — screen capture via MediaProjection: pure lifecycle controller (Part 1) + real foreground
  `ScreenCaptureService`, consent Activity, and capture UI (Part 2). In-memory frame only.
- **67** — region selector: normalized `CaptureRegion` geometry + `RegionSelectorController` + tests.

## Build

```bash
./gradlew assembleDebug          # Windows: gradlew.bat assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk  (debug-signed, versionName 0.1.0-prealpha)
```

Requires JDK 17 + Android SDK (`platforms;android-34`, `build-tools;34.0.0`). See
[docs/ANDROID_BUILD_TROUBLESHOOTING.md](docs/ANDROID_BUILD_TROUBLESHOOTING.md) and
[docs/APK_BUILD_GUIDE.md](docs/APK_BUILD_GUIDE.md). The APK is **debug-signed only** — never
release-signed.

### Run unit tests

```bash
./gradlew testDebugUnitTest      # pure-JVM JUnit 4 (no Robolectric / no device)
```

Covers `safety/`, `realtap/`, and `capture/` (screen-capture lifecycle + region selector).

## Run

Install the debug APK on a device/emulator (API 26+):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then: launch → **Start simulation** → **Stop** → **Emergency Stop** → open **Safety Center** and
**Diagnostics**. See [scripts/android-smoke.md](scripts/android-smoke.md).

## App identity

| | |
|---|---|
| App name | ClickFlow Android |
| Package | `com.clickflow.android` |
| minSdk | 26 |
| targetSdk / compileSdk | 34 |
| versionName | 0.1.0-prealpha |
| versionCode | 1 |
| Stack | Kotlin, Gradle (Kotlin DSL), Jetpack Compose, Material 3 |

## Documentation

- [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) — step-by-step engineering context
- [CHANGELOG.md](CHANGELOG.md) — detailed changelog per step
- [docs/REAL_TAP_PROTOTYPE.md](docs/REAL_TAP_PROTOTYPE.md) — single real-tap architecture + invariants
- [docs/SAFETY_REVIEW_CHECKLIST.md](docs/SAFETY_REVIEW_CHECKLIST.md) — the 10-item review (EN + RU)
- [docs/CONSENT_FLOW.md](docs/CONSENT_FLOW.md) — per-tap consent lifecycle, TTL/nonce contract
- [docs/REAL_TAP_QA_SCENARIOS.md](docs/REAL_TAP_QA_SCENARIOS.md) — real-tap QA scenarios
- [docs/REAL_TAP_FIXES_LOG.md](docs/REAL_TAP_FIXES_LOG.md) — per-scenario fix log
- [docs/ANDROID_SAFETY_MODEL.md](docs/ANDROID_SAFETY_MODEL.md) — safety model
- [docs/ANDROID_ARCHITECTURE.md](docs/ANDROID_ARCHITECTURE.md) — architecture
- [docs/ANDROID_MEDIAPROJECTION_PLAN.md](docs/ANDROID_MEDIAPROJECTION_PLAN.md) — screen-capture plan (Step 66)
- [docs/ANDROID_ACCESSIBILITY_PLAN.md](docs/ANDROID_ACCESSIBILITY_PLAN.md) — accessibility plan
- [docs/ANDROID_PRODUCT_PLAN.md](docs/ANDROID_PRODUCT_PLAN.md) — product plan
- [docs/ANDROID_SIMPLE_CLICKER_UX.md](docs/ANDROID_SIMPLE_CLICKER_UX.md) · [docs/ANDROID_MARKER_MODEL.md](docs/ANDROID_MARKER_MODEL.md)
- [docs/ANDROID_PROFILES.md](docs/ANDROID_PROFILES.md) · [docs/ANDROID_AUDIT_PERSISTENCE.md](docs/ANDROID_AUDIT_PERSISTENCE.md) · [docs/ANDROID_EXPORT_MODEL.md](docs/ANDROID_EXPORT_MODEL.md)
- [docs/ANDROID_MULTI_STEP_SCENARIOS.md](docs/ANDROID_MULTI_STEP_SCENARIOS.md) · [docs/ANDROID_AUDIT_LOG.md](docs/ANDROID_AUDIT_LOG.md)
- [docs/ANDROID_SCENARIO_STORAGE.md](docs/ANDROID_SCENARIO_STORAGE.md) · [docs/ANDROID_SCENARIO_UI.md](docs/ANDROID_SCENARIO_UI.md)
- [docs/ANDROID_BACKUP_EXPORT.md](docs/ANDROID_BACKUP_EXPORT.md) · [docs/ANDROID_BACKUP_IMPORT.md](docs/ANDROID_BACKUP_IMPORT.md)
- [docs/ANDROID_PERMISSIONS_PLAN.md](docs/ANDROID_PERMISSIONS_PLAN.md) · [docs/ANDROID_MVP_CHECKLIST.md](docs/ANDROID_MVP_CHECKLIST.md)
- [docs/ANDROID_BUILD_TROUBLESHOOTING.md](docs/ANDROID_BUILD_TROUBLESHOOTING.md) · [docs/APK_BUILD_GUIDE.md](docs/APK_BUILD_GUIDE.md)
- [docs/ANDROID_PRE_ALPHA_RELEASE_NOTES.md](docs/ANDROID_PRE_ALPHA_RELEASE_NOTES.md) · [docs/ANDROID_PRE_ALPHA_RELEASE_DRAFT.md](docs/ANDROID_PRE_ALPHA_RELEASE_DRAFT.md) · [docs/ANDROID_PRE_ALPHA_TAG_PLAN.md](docs/ANDROID_PRE_ALPHA_TAG_PLAN.md)
- [docs/ANDROID_PRE_ALPHA_QA.md](docs/ANDROID_PRE_ALPHA_QA.md) · [docs/ANDROID_PRE_ALPHA_RELEASE_CHECKLIST.md](docs/ANDROID_PRE_ALPHA_RELEASE_CHECKLIST.md) · [docs/ANDROID_MANUAL_RELEASE_ASSET_UPDATE.md](docs/ANDROID_MANUAL_RELEASE_ASSET_UPDATE.md)

## Safety model (summary)

- **Bulk real taps are disabled** — `SafetyGate.canRunRealTap()` returns `false` (hard-coded).
  The scenario runner has no access to any real-input path.
- **The single-tap prototype** is gated behind four independent checks: per-session Safety Review,
  active session, fresh single-use consent (10s TTL), and a bound accessibility service on
  API ≥ 24. Every transition is audited via granular `realtap.*` events.
- **Phase 2 (screen capture / smart targets) is preview-only:** captured frames live in memory,
  are never written to disk or exported, and Step 66 performs **capture only — no analysis**.
- No hidden/background automation; no captcha/anti-bot bypass; no ad-click, banking, or payment
  automation; no spyware/keyloggers/input hooks; no root-only features.
- See [docs/ANDROID_SAFETY_MODEL.md](docs/ANDROID_SAFETY_MODEL.md),
  [docs/REAL_TAP_PROTOTYPE.md](docs/REAL_TAP_PROTOTYPE.md),
  [docs/SAFETY_REVIEW_CHECKLIST.md](docs/SAFETY_REVIEW_CHECKLIST.md),
  [docs/CONSENT_FLOW.md](docs/CONSENT_FLOW.md).

## License

MIT (aligned with the desktop ClickFlow project).
