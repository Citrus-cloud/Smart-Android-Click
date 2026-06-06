# ClickFlow — план мерджа ветки `chore/b1-b2-cleanup`

Этот документ фиксирует, что было сделано в ветке и что нужно проверить перед merge в `main`.

## 1. Что изменено

### Чистка кода

Удалён старый неподключённый слой симуляции и прототипов:

- `scenarios/` — старый simulation engine;
- `realtap/` — старый real-tap prototype layer;
- `profiles/`;
- `audit/`;
- `backup/`;
- `diagnostics/`;
- `safety/`;
- осиротевший `core/ClickFlowConstants.kt`.

Оставлен рабочий пользовательский поток:

- `overlay/` — overlay-метки;
- `permissions/` — Accessibility service;
- `imageclick/` — клик по изображению;
- `textclick/` — клик по тексту;
- `scenario/` — реальные пользовательские сценарии;
- `capture/` — screen capture / OCR / target controllers;
- `core/`, `ui/` — основной UI и навигация.

### Алгоритм поиска картинки

`BitmapTemplateMatcher` усилен:

- двухфазный поиск: грубый scan → точное уточнение;
- адаптивная плотность семплов;
- диапазон масштаба по умолчанию `0.8–1.2`;
- шаг масштаба `0.05`.

### Хранилища и миграции

Добавлена нормализация и чистка:

- `TextClickStore`;
- `ImageClickTemplateStore`;
- `ScenarioLibraryStore`.

### Релизная подготовка

- Включён R8/minify для release.
- Включён resource shrinking.
- Добавлены базовые ProGuard/R8-правила.
- Android Backup выключен для локальных данных автоматизации.
- Обновлены Accessibility/store-facing тексты.
- Обновлены privacy/store документы.

## 2. Что обязательно проверить локально

```bash
./gradlew clean testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
./gradlew assembleRelease --no-daemon
```

Если падает release — сначала смотреть R8/ProGuard warning/error.

## 3. Что обязательно проверить на телефоне

- запуск приложения;
- Accessibility включается и остаётся включённым;
- Overlay включается;
- overlay-метка отображается поверх стороннего приложения;
- тап по метке работает;
- клик по картинке работает;
- клик по тексту работает;
- сценарии запускаются;
- шаги `MARKER`, `PHOTO`, `TEXT`, `WAIT` выполняются;
- кнопки Stop останавливают режимы;
- настройки/шаблоны/сценарии сохраняются после перезапуска.

## 4. Условия для merge

Мержить в `main` только если:

- debug-сборка проходит;
- unit-тесты проходят;
- release-сборка проходит;
- smoke-test на телефоне пройден;
- нет критичных runtime crash;
- PR просмотрен и принят.

## 5. После merge

Следующие отдельные ветки лучше делать отдельно:

1. UI-полировка сценариев.
2. Store assets: иконка, скриншоты, описания.
3. Google Play Billing, если премиум будет платным.
4. CI/CD, когда будет токен с `workflows` scope.
