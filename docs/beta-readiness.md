# Beta Readiness Checklist — ClickFlow Android v1.0.0-beta.1

All items must be checked before tagging `v1.0.0-beta.1`.

## Автоматические тесты (JVM)

- [ ] `./gradlew testDebugUnitTest` — все тесты проходят.
- [ ] `TemplateManagerTest` — 18/18.
- [ ] `TemplateMatcherTest` — 8/8.
- [ ] `ImageTargetControllerTest` — 10/10.
- [ ] `OcrControllerTest` — 12/12.
- [ ] `TextTargetControllerTest` — 11/11.
- [ ] `VisualScenarioBuilderTest` — 13/13.
- [ ] `ControlledTapSessionManagerTest` — 14/14.
- [ ] `SmartTargetTapControllerTest` — 12/12.
- [ ] `SmartSessionAuditTest` — 13/13.
- [ ] **Итого: 124+ JVM-тестов проходят.**

## Документация

- [ ] `docs/user-guide.md` — прочитана и актуальна.
- [ ] `docs/e2e-qa-scenarios.md` — все 10 сценариев проверены вручную.
- [ ] `docs/parity-matrix.md` — все ⚠️-пункты разрешены или зафиксированы.
- [ ] `docs/qa-checklist-android.md` — все пункты выполнены.

## Безопасность (ручное тестирование на устройстве)

- [ ] Emergency Stop протестирован в живой сессии.
- [ ] TTL согласия истекает через 10 с (Safety Gate).
- [ ] Safety Center отображает все 4 флага.
- [ ] Строки отображаются на русском языке (локаль `ru`).
- [ ] Строки отображаются на английском языке (локаль `en`).

## Релиз

- [ ] `AppInfo.VERSION_NAME` = `"1.0.0-beta.1"`.
- [ ] APK собирается без ошибок.
- [ ] Git тег `v1.0.0-beta.1` запушен.
- [ ] GitHub Release с APK создан.
