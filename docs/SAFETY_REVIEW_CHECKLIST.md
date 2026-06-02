# Safety Review Checklist — Single Real Tap

Before the first real tap of a session, the user must read and confirm all 10 items below. The pass state lives in memory only (`SafetyState.realTapSafetyReviewPassed`) and is reset on:

- App process death / cold start
- Emergency Stop
- Explicit "End real-tap session"
- Any change to overlay or accessibility permission grants
- Resetting the `RealTapSession`

The 10 items are rendered verbatim in the UI. Translations live in `res/values/strings.xml` and `res/values-ru/strings.xml`.

---

## The 10 items (EN)

1. **One tap at a time.** Confirming this review allows me to perform ONE real tap per explicit consent. Bulk, looped, and scenario-driven real taps remain disabled.
2. **Consent is single-use.** Each tap requires a fresh confirmation. A consent expires after 10 seconds and cannot be reused.
3. **The tap is real.** Unlike everything else in the app so far, this WILL dispatch a real input event through the Accessibility service.
4. **I am responsible for the target.** I will only request taps inside apps and contexts I own or have permission to interact with. I will not use this on banking, payment, government, or other sensitive UIs.
5. **No bypass of `SafetyGate`.** Even with this review passed, `SafetyGate` still blocks all bulk and scenario real-tap paths.
6. **Emergency Stop ends everything.** Pressing Emergency Stop immediately invalidates any pending consent and ends the real-tap session.
7. **Audit is permanent for this session.** Every real-tap request, dispatch, and outcome is recorded in the audit log.
8. **The session does not survive a restart.** Closing the app, force-stopping it, or rebooting ends the session and requires a fresh review.
9. **Revoking a permission ends the session.** Disabling overlay or accessibility immediately invalidates the session and any consent.
10. **This is a prototype.** Behaviour may change between versions. I will re-read this checklist whenever I update the app.

---

## The 10 items (RU)

1. **По одному тапу за раз.** Подтверждая эту проверку, я разрешаю выполнить ОДИН реальный тап на каждое явное подтверждение. Массовые, циклические и сценарные реальные тапы остаются отключёнными.
2. **Подтверждение одноразовое.** Каждый тап требует нового подтверждения. Подтверждение действует 10 секунд и не может быть использовано повторно.
3. **Тап реальный.** В отличие от всего остального в приложении до этого, ЭТО действительно отправит реальное событие ввода через службу специальных возможностей.
4. **Я отвечаю за цель.** Я буду запрашивать тапы только в приложениях и контекстах, которыми владею или для которых имею разрешение. Я не буду использовать это в банковских, платёжных, государственных и других чувствительных интерфейсах.
5. **Никакого обхода `SafetyGate`.** Даже с пройденной проверкой `SafetyGate` продолжает блокировать все массовые и сценарные пути.
6. **Аварийная остановка завершает всё.** Нажатие Emergency Stop немедленно аннулирует любое ожидающее подтверждение и завершает сессию.
7. **Аудит для этой сессии вечен.** Каждый запрос, отправка и результат реального тапа записываются в журнал аудита.
8. **Сессия не переживает перезапуск.** Закрытие приложения, принудительная остановка или перезагрузка завершают сессию и требуют новой проверки.
9. **Отзыв разрешения завершает сессию.** Отключение оверлея или специальных возможностей немедленно аннулирует сессию и любое подтверждение.
10. **Это прототип.** Поведение может меняться между версиями. Я буду перечитывать этот чек-лист при каждом обновлении.

---

## UX rules

- All 10 items render as scrollable, copy-selectable text.
- A single "I have read and understood every item" checkbox toggles the pass state. There are no per-item checkboxes — the contract is all-or-nothing.
- The pass state is reflected by `realTapSafetyReviewPassed` in `SafetyState` and mirrored to `DiagnosticsState`.
- The badge in `RealTapPrototypeScreen` shows `Safety review: PASSED (this session)` or `Safety review: required`.
- The "Start real-tap session" button is disabled while the review is not passed.

---

## Invariants

- The list above is the **single source of truth** for the review items. UI must render exactly this content (translated). Adding, removing, or rewording an item requires updating this document AND the string resources in the same commit.
- `realTapSafetyReviewPassed` is never persisted to disk, exported in backup, or transmitted off-device.
- A failed gate check or a dispatch failure does NOT reset the review pass state. Only the explicit reset triggers listed above do.
