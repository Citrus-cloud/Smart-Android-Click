# Android Accessibility Plan — ClickFlow Android

> **Not implemented in Step 52.** This is a forward-looking design. No `AccessibilityService` exists
> in the codebase yet, and no real taps are performed.

## Why Accessibility Service

On non-rooted Android, the supported, non-hacky way to perform programmatic taps is an
`AccessibilityService` dispatching gestures. ClickFlow Android will use this — never input injection,
instrumentation hacks, or root.

## Tap execution (future)

- Implement an `AccessibilityService` and use `dispatchGesture(GestureDescription, callback, handler)`
  to perform taps/swipes at coordinates defined by a scenario.
- Map `simple_tap_simulation` scenarios onto real gestures **only** after the safety conditions below.

## Gesture dispatch (future)

- Single tap, then multi-tap/repeat, then simple paths (swipes).
- Each dispatch checks `SafetyGate.canRunRealTap()` immediately before executing.

## User consent (future)

- The user must manually enable the service in **Settings → Accessibility** (the app cannot self-enable).
- An in-app consent screen will state exactly what the app will do, where, and how to stop it.
- Consent is revocable at any time by disabling the service.

## Restrictions (future)

- Real taps gated by `SafetyGate`, feature flags, and a completed safety review for that step.
- Emergency Stop must immediately cancel any in-flight gesture and disable further dispatch.
- Audit logging of real-tap sessions.

## Disallowed use cases (always)

- Captcha/anti-bot bypass.
- Ad-click automation / fraud.
- Banking, payment, or protected app automation.
- Hidden/background taps without explicit consent.
- Anything in [ANDROID_SAFETY_MODEL.md](ANDROID_SAFETY_MODEL.md)'s prohibited list.

## Step 52 status

- No `AccessibilityService` class.
- No `BIND_ACCESSIBILITY_SERVICE` declaration.
- No `dispatchGesture` calls.
- `canRunRealTap()` returns `false`.
