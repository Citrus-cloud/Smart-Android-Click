# Android Backup Export — ClickFlow Android (Step 56)

## Purpose

Let a user export their **profiles + scenarios** as a single JSON-text document they can save
anywhere (notes, cloud, chat), without storage permissions or file providers.

## Backup JSON schema

```json
{
  "schema": "clickflow-android-backup",
  "version": 1,
  "createdAt": 1710000000000,
  "appVersion": "1.0.0-alpha.1",
  "profiles": [ /* Profile objects */ ],
  "scenarios": [ /* Scenario objects (with profileId, actions, settings) */ ],
  "metadata": { "simulationOnly": true, "containsAuditLog": false }
}
```

## What is exported

- All **profiles** (id, name, description, timestamps, isActive).
- All **scenarios** (multi-step actions, settings, `profileId`).

## What is NOT exported

- **No audit log** (`containsAuditLog: false`).
- No screenshots, no base64, no images.
- No private file paths, no device identifiers, no credentials.

## How export/share works

`BackupManager.createBackup(profiles, scenarios)` builds the JSON. The Backup screen's **Export**
button shares it as plain text:

- `Intent.ACTION_SEND`, `type = "text/plain"`, `EXTRA_TEXT = <backup JSON>`.
- `Intent.createChooser(...)` + `FLAG_ACTIVITY_NEW_TASK`, started from app context.
- **No FileProvider, no `<provider>`, no permissions.**

Failures show a clear message (`backup_export_failed`). Audit events: `backup.export.requested`,
`backup.export.shared`, `backup.export.failed` (counts only — never the JSON body).
