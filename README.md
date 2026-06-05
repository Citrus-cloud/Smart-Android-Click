# ClickFlow Android

Native Android smart auto-clicker — finds and taps targets by **image matching** or **OCR text**,
with a four-check safety gate and full audit log.

[![version](https://img.shields.io/badge/version-1.0.0-green)](#)
[![tests](https://img.shields.io/badge/JVM%20tests-124%2B-brightgreen)](#)
[![lang](https://img.shields.io/badge/lang-Kotlin-orange)](#)

---

## 🇷🇺 Краткое описание

**ClickFlow Android v1.0.0** — финальный релиз 🏁

Все 5 фаз (84 шага) завершены. **124+ JVM-тестов**.

---

## 📥 Скачать APK

Перейдите на [**Releases**](https://github.com/Citrus-cloud/Smart-Android-Click/releases) →
скачайте `ClickFlow-v1.0.0.apk` → установите на телефон.

> ⚠️ Перед установкой включите **Настройки → Безопасность → Установка неизвестных приложений**.

---

## ✨ Что умеет приложение

| Функция | Описание |
|---------|----------|
| 🔍 Шаблонное сопоставление | Нажатие по скриншоту кнопки |
| 📝 OCR (распознавание текста) | Нажатие по видимому тексту |
| 🛠️ Строитель сценариев | TAP / WAIT / NOTE + пресеты |
| 🔒 Safety Gate | 4 независимых проверки безопасности |
| 🛡️ Emergency Stop | Мгновенная остановка всех действий |
| 📝 Аудит сессий | Журнал 200 событий |
| 🌐 Локализация | Русский + Английский |

---

## 🔧 Сборка из исходников

### Требования
- JDK 17
- Android SDK API 34
- Android Studio (рекомендуется) или command-line tools

### Сборка
```bash
git clone https://github.com/Citrus-cloud/Smart-Android-Click.git
cd Smart-Android-Click
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Тесты
```bash
./gradlew testDebugUnitTest
```

---

## 📁 Структура проекта

```
app/src/main/java/com/clickflow/android/
├── capture/          ← MediaProjection, TemplateMatcher, OCR, RegionSelector
├── core/             ← AppInfo
├── realtap/          ← SafetyGate, Session, SmartTargetTap, Audit
└── scenario/         ← VisualScenarioBuilder, ScenarioPreset

docs/
├── user-guide.md
├── e2e-qa-scenarios.md
├── qa-checklist-android.md
├── beta-readiness.md
└── parity-matrix.md
```

---

## 📚 Документация

- [📖 Руководство пользователя](docs/user-guide.md)
- [✅ E2E QA сценарии](docs/e2e-qa-scenarios.md)
- [📋 QA-чеклист](docs/qa-checklist-android.md)
- [📈 Матрица паритета](docs/parity-matrix.md)
- [🔄 CHANGELOG](CHANGELOG.md)

---

## 🛡️ Модель безопасности

`SafetyGate.canRunRealTap()` = `false` в этой сборке.
Phase 3 (Шаги 74–76) вводит session + consent + audit;
реальные нажатия через `dispatchGesture` будут добавлены в следующем обновлении.

## Лицензия

MIT
