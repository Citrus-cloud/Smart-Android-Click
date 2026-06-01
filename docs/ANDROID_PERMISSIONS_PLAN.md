# Android Permissions Plan — ClickFlow Android

**Step 52 requests NO runtime permissions and declares none in the manifest.** This document
describes permissions that *future* steps may introduce, each only with explicit user consent.

## Future permissions (not requested in Step 52)

### Accessibility Service
- **Mechanism:** `BIND_ACCESSIBILITY_SERVICE` + a declared `AccessibilityService`.
- **Purpose:** perform real taps via `dispatchGesture`.
- **Consent:** the user must manually enable the service in system Accessibility settings; the app
  cannot enable it programmatically. Preceded by an in-app explanation + safety gate.

### MediaProjection
- **Mechanism:** `MediaProjectionManager.createScreenCaptureIntent()` (runtime user grant).
- **Purpose:** optional screen analysis for image/text matching.
- **Consent:** system shows its own capture-consent dialog each session; no auto-grant possible.

### Overlay (only if needed)
- **Permission:** `SYSTEM_ALERT_WINDOW`.
- **Purpose:** a floating control/emergency-stop button, **only if** a concrete need is justified.
- **Consent:** user grants via Settings; the app must function without it.

### Notification
- **Permission:** `POST_NOTIFICATIONS` (Android 13+).
- **Purpose:** foreground-service status / run notifications, if a foreground service is added.

### Foreground service (only if needed)
- **Permission:** `FOREGROUND_SERVICE` (+ a typed subtype if required).
- **Purpose:** keep a consented automation session alive with a visible notification.

## Principles

- Request the **minimum** necessary, **only when** a feature that needs it is actually used.
- Always pair a request with an in-app explanation and a safety gate.
- Never request permissions associated with prohibited use cases.
- The app must degrade gracefully (stay simulation-capable) if permissions are denied.
