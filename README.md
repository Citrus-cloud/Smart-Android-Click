# ClickFlow Android

ClickFlow — минималистичный умный автокликер для Android.

## Возможности

- Обычная тапалка по одной или нескольким меткам.
- Плавающие overlay-метки поверх других приложений.
- Сохранение настроек и позиций меток.
- Таймер, повторы, бесконечный режим.
- Аварийная остановка.
- Клик по картинке/иконке.
- Клик по тексту через OCR.
- Простой сценарий по сохранённым overlay-меткам.

## Разрешения

ClickFlow использует системные разрешения Android:

- Accessibility — для реальных тапов.
- Overlay — для меток поверх экрана.
- MediaProjection — для поиска картинки и текста на экране.

Внутренние лишние согласия и демо-чеклисты убраны из основного пользовательского потока.

## Как собрать

```bash
./gradlew testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Документация

- [Инструкция для тестирования](docs/TESTING_GUIDE_RU.md)
- [План доведения до магазина](docs/STORE_READY_PLAN_RU.md)
- [Черновик Privacy Policy](docs/PRIVACY_POLICY_DRAFT_RU.md)
