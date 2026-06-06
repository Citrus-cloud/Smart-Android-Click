# ClickFlow Android — умный автокликер

ClickFlow — минималистичный умный автокликер для Android без root. Приложение выполняет пользовательские нажатия по меткам, по найденной картинке, по найденному тексту и по пользовательским сценариям.

Текущая ветка `chore/b1-b2-cleanup` очищена от старого simulation/prototype-кода и подготовлена к локальной проверке перед релизным hardening/merge.

## Статус

- Версия: `0.2.0-beta` (`versionCode = 2`).
- Платформа: Android 8.0+ (`minSdk 26`), target/compile SDK 34.
- UI: Jetpack Compose + Material 3.
- OCR: ML Kit Text Recognition.
- Текущий статус: beta cleanup + release preparation.

## Что умеет приложение

- Overlay-метки поверх других приложений.
- Реальные тапы через Accessibility Service.
- Клик по картинке/иконке на экране.
- Клик по тексту через OCR.
- Пользовательские сценарии из шагов:
  - `MARKER` — тап по координатам;
  - `PHOTO` — поиск изображения и тап;
  - `TEXT` — поиск текста и тап;
  - `WAIT` — пауза.
- Повторы, интервалы, бесконечный режим.
- Плавающие кнопки остановки.
- Локальное сохранение настроек, шаблонов и сценариев.

## Разрешения

### Accessibility

Главное разрешение приложения. Используется только после ручного включения пользователем в настройках Android.

ClickFlow использует Accessibility для:

- выполнения пользовательских тапов;
- запуска пользовательских сценариев;
- получения временного снимка экрана через Accessibility screenshot API на Android 11+ для локального поиска картинки/текста.

ClickFlow не использует Accessibility для скрытого сбора данных, чтения паролей, платёжных данных или личных сообщений.

### Overlay

Нужно для отображения плавающих меток, панели прогресса и кнопок остановки поверх других приложений.

### MediaProjection

В текущем релизном манифесте отдельный MediaProjection-preview поток убран. Фото/текст-поиск работает через Accessibility screenshot API на Android 11+.

## Сборка

```bash
./gradlew clean testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
./gradlew assembleRelease --no-daemon
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Перед merge обязательно проверить `assembleRelease`, потому что в release включены R8/minify и shrink resources.

## Архитектура

Основные пакеты:

- `overlay/` — плавающие overlay-метки и сервис `FloatingTapperOverlayService`.
- `permissions/` — `ClickFlowAccessibilityService`: реальные тапы и screenshot API Android 11+.
- `imageclick/` — клик по изображению: шаблоны, сервис, экран настройки, `BitmapTemplateMatcher`.
- `textclick/` — клик по тексту: настройки, сервис, OCR через ML Kit.
- `scenario/` — пользовательские сценарии:
  - `ScenarioModel.kt` — модель сценариев;
  - `ScenarioLibraryStore.kt` — JSON-хранилище сценариев;
  - `ScenarioActivity.kt` — UI конструктора;
  - `ScenarioEngineService.kt` — исполнение сценариев;
  - `ScenarioPremium.kt` — тестовый premium-gate.
- `core/` — `ClickFlowViewModel`, `AppInfo`.
- `ui/` — основной Compose UI и навигация.

Удалены старые неподключённые слои: `scenarios/`, `realtap/`, `profiles/`, `audit/`, `backup/`, `diagnostics/`, `safety/`, `capture/`.

## Что изменено в cleanup-ветке

### Чистка

Удалён старый simulation/prototype-код, не участвующий в текущем пользовательском потоке.

### Image matching

`BitmapTemplateMatcher` усилен:

- coarse-to-fine поиск;
- плотное уточнение позиции вокруг лучшего кандидата;
- адаптивная сетка семплов;
- диапазон масштаба по умолчанию `0.8–1.2`;
- шаг масштаба `0.05`.

### Хранилища

Добавлена нормализация и безопасная миграция:

- `TextClickStore`;
- `ImageClickTemplateStore`;
- `ScenarioLibraryStore`.

### Release hardening

- `isMinifyEnabled = true` для release.
- `isShrinkResources = true`.
- Добавлены ProGuard/R8-правила.
- Android Backup выключен для локальных данных автоматизации.
- Манифест сокращён до реально используемых разрешений/компонентов.
- Обновлены Privacy Policy и Data Safety drafts.

## Документы

- [Инструкция для тестирования](docs/TESTING_GUIDE_RU.md)
- [Релизный чеклист](docs/RELEASE_CHECKLIST_RU.md)
- [План мерджа](docs/MERGE_PLAN_RU.md)
- [План доведения до магазина](docs/STORE_READY_PLAN_RU.md)
- [Черновик Privacy Policy](docs/PRIVACY_POLICY_DRAFT_RU.md)
- [Черновик Google Play Data Safety](docs/GOOGLE_PLAY_DATA_SAFETY_RU.md)
- [Черновик описания для магазина](docs/STORE_LISTING_DRAFT_RU.md)

## Перед merge в `main`

1. Собрать локально:

```bash
./gradlew clean testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
./gradlew assembleRelease --no-daemon
```

2. Проверить на телефоне:

- запуск приложения;
- Accessibility;
- Overlay;
- overlay-метки;
- клик по картинке на Android 11+;
- клик по тексту на Android 11+;
- сценарии `MARKER`, `PHOTO`, `TEXT`, `WAIT`;
- остановку всех режимов;
- сохранение настроек после перезапуска.

3. Только после успешной локальной сборки и smoke-test — merge PR в `main`.
