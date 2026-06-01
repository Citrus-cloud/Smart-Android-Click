# Android Export Model — ClickFlow Android (Step 55)

## Purpose

Let users get their audit log out of the app safely, without any storage permissions or file
providers.

## Chosen approach: share plain text (no files)

Step 55 uses the Android **share sheet** with a text payload:

- `Intent.ACTION_SEND`, `type = "text/plain"`.
- `EXTRA_TEXT = AuditLogManager.exportAsText()`; `EXTRA_SUBJECT = "ClickFlow Android Audit Log"`.
- Launched via `Intent.createChooser(...)` with `FLAG_ACTIVITY_NEW_TASK` (started from app context).

This requires **no permissions** and **no FileProvider** (avoids touching manifest `<provider>`).
If the chooser cannot start, the UI shows a clear error (`audit_log_export_failed`).

## Why not FileProvider / file export (yet)

A `FileProvider` would not need `uses-permission`, but it adds a manifest `<provider>` and an XML
paths config. To keep Step 55 minimal and unambiguous, file export is **deferred**; text share covers
the need today.

## What is exported

- A header summary (`events`, per-severity counts) plus one line per event:
  `[timestamp] SEVERITY type message`.

## What is NOT exported

- No screenshots, no base64, no images.
- No private file paths.
- No personal data, credentials, or device identifiers.
- No scenario/profile JSON (scenario/profile import-export is planned for a future step).

## Future import/export

- Export scenarios + profiles as JSON (share text or, if justified, a `FileProvider` URI).
- Import with strict validation and the same corrupted-input fallback used for storage.
- Optional on-disk audit export file. All deferred; none added in Step 55.
