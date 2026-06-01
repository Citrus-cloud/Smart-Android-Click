# Android MediaProjection Plan — ClickFlow Android

> **Not implemented in Step 52.** No screen capture occurs. This is a forward-looking design.

## Purpose (future)

Optional on-device screen analysis to support future image_click / text_click features
(template matching, OCR), mirroring the desktop concept — but on Android via MediaProjection.

## MediaProjection flow (future)

1. User taps a clearly-labeled "Enable screen analysis" action.
2. App calls `MediaProjectionManager.createScreenCaptureIntent()`.
3. The **system** shows its own capture-consent dialog (cannot be bypassed or auto-accepted).
4. On grant, capture runs only for the consented session and stops when the user stops it.

## User consent (future)

- System consent dialog is mandatory and per-session.
- In-app explanation precedes the request.
- A persistent indicator while capture is active.

## Screenshot privacy (future)

- **No disk save by default.** Frames are processed in memory and discarded.
- No upload, no network transmission of captured content.
- Any optional save would itself be explicit, opt-in, and clearly indicated.

## Future image/text matching (future)

- On-device only. Template matching / OCR run locally.
- Results feed scenario logic only after the same safety gating as real taps.

## Step 52 status

- No MediaProjection usage.
- No screen capture code.
- No related permissions requested.
- `mediaProjectionEnabled = false`.
