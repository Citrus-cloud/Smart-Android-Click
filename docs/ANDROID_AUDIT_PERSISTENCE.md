# Android Audit Persistence — ClickFlow Android (Step 55)

## Purpose

Make the audit log durable across app restarts while keeping it private, bounded, and crash-safe.

## Storage

- **JSON Lines** in INTERNAL storage: `filesDir/audit-log.jsonl` (one event per line, newest first).
- No permissions, no external storage.
- Bounded to **1000** events; oldest are dropped when the cap is exceeded.
- Writes use temp-file + rename for resilience.

## API (`AuditLogManager`)

- `loadAuditEvents()` — load on startup; never throws.
- `appendEvent(event)` / `log(type, severity, message, …)` — add + persist.
- `getEvents()` / `events: StateFlow` — current events (newest first).
- `clearEvents()` — empty + persist.
- `exportAsText()` — plain-text report (header summary + lines).
- `getAuditSummary()` — counts by severity + storage flags.
- `recoverCorruptedAuditLog()` — wipe to a clean state.

## Corrupted-file handling

- Unreadable file → recover to an empty log, set `corruptedAuditRecovered`, and write a
  `storage.recovered` warning event.
- A single malformed JSONL line is **skipped**; the rest of the log still loads.

## Audit summary

`AuditSummary(totalEvents, infoCount, warningCount, errorCount, safetyCount, lastEventType,
storageReady, corruptedAuditRecovered)` — shown on the Audit Log screen and in Diagnostics.

## Privacy / sanitization

- No screenshots, no base64, no captured screen content.
- Metadata keys containing `screenshot/image/base64/bitmap/pixels` are dropped; values are
  length-capped (200 chars); messages capped (500 chars).
- Nothing leaves the device except via the explicit user-initiated text share (see
  `ANDROID_EXPORT_MODEL.md`).
