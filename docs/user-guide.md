# ClickFlow Android — User Guide

> Version: 1.0.0 · Step 82

---

## 🇷🇺 Что такое ClickFlow Android?

**ClickFlow Android** — умный автокликер для Android.
Вместо фиксированных координат приложение определяет,
**куда нажать**, анализируя скриншот: через шаблонное сопоставление (изображение)
или распознавание текста (OCR).

---

## Архитектура

```
Захват экрана (MediaProjection)
        ↓
Селектор региона → Менеджер шаблонов → TemplateMatcher
        ↓                         ↓
ImageTargetController        OcrController → TextTargetController
        ↓                         ↓
         └──────────────────┘
                  ↓
     SmartTargetTapController
                  ↓
  ControlledTapSessionManager
                  ↓
       SafetyGate (4 флага)
                  ↓
     Аудит + Emergency Stop
```

---

## Быстрый старт

### 1. Установка

1. Скачайте `ClickFlow.apk` со страницы [Releases](https://github.com/Citrus-cloud/Smart-Android-Click/releases)
2. Перед установкой включите:
   **Настройки → Безопасность → Установка неизвестных приложений**
3. Откройте APK и установите

### 2. Создайте шаблон (клик по изображению)

1. Откройте вкладку **Шаблоны**
2. Нажмите **+** — выделите регион вокруг кнопки, в которую хотите нажимать
3. Дайте шаблону название и сохраните

### 3. Создайте текстовую цель (клик по тексту)

1. Откройте вкладку **Текстовые цели**
2. Введите видимый текст кнопки (например: `ОК`)
3. Сохраните

### 4. Постройте сценарий

1. Вкладка **Сценарий**
2. Добавьте шаги: **TAP** (шаблон или текст), **WAIT**, **NOTE**
3. Используйте пресет: **TAP_CENTER** или **TAP_AND_WAIT**

### 5. Запустить сценарий

1. **Safety Center** — все 4 чека должны быть зелёными ✅
2. Нажмите **Дать согласие** — открывается 10-секундное окно
3. Нажмите **Запустить сценарий**
4. Для немедленной остановки нажмите **Аварийная остановка**

---

## Модель безопасности

| Проверка | Что значит |
|----------|-------------|
| Review passed | Одноразовое подтверждение обзора |
| Consent fresh | Явное согласие на следующее действие (TTL 10 с) |
| Session active | Сессия с лимитом нажатий открыта |
| Accessibility | Служба специальных возможностей разрешена |

---

## Решение проблем

**Действие заблокировано — SESSION_GATE_CLOSED**
Балк реального нажатия закрыт в pre-alpha. Функция появится в одном из следующих обновлений.

**Шаблон не найден**
PUбедитесь, что цель видна на экране. Перезахватите шаблон, порог выбрать область потеснее.

**OCR не находит текст**
Проверьте орфографию. Если знак смешанный, отключите Case Sensitive.

---

## English Summary

ClickFlow Android is a smart auto-clicker for Android. It finds click targets
automatically via image template matching or OCR text recognition.

**Quick start:** Install APK → create template or text target → build a scenario →
check Safety Center (all 4 green) → Give consent → Run.

Emergency Stop halts all actions instantly at any time.
