# Android Audit Log — ClickFlow Android (Step 54)

## Purpose

Provide a transparent, in-app record of what the simulation did and of lifecycle/safety events, so
behavior is auditable. Reinforces the safety-first posture: every simulated action is logged.

## Event format

`AuditEvent`:

```
id: String
timestamp: Long          // System.currentTimeMillis()
type: String             // dotted type, see below
severity: AuditSeverity  // INFO | WARNING | ERROR | SAFETY
scenarioId: String?
actionId: String?
message: String          // app-generated, localized where applicable
metadata: Map<String,String>
```

## Event types

- `scenario.started`
- `scenario.completed`
- `scenario.stopped`
- `scenario.emergencyStopped` (SAFETY)
- `action.simulatedTap`
- `action.wait`
- `action.note`
- `validation.failed`
- `storage.recovered`
- `storage.migrated`
- `safety.realTapBlocked` (SAFETY)

## In-memory vs persistence

Step 54 kept the log in memory. **Step 55 made it persistent**: events are stored as JSON Lines in
`filesDir/audit-log.jsonl` (bounded 1000, newest-first, corrupted fallback), and the log is exportable
via the Android share sheet (plain text). See `ANDROID_AUDIT_PERSISTENCE.md` and
`ANDROID_EXPORT_MODEL.md`.

## Privacy rules

- **No screenshots, no base64, no captured screen content** — there is nothing of the sort to log.
- **No personal data, no credentials, no network identifiers.**
- Only app-generated, non-sensitive text (action descriptions, lifecycle, validation, safety).
- No permissions required; nothing leaves the device.

## Safety events

- `scenario.emergencyStopped` is logged at SAFETY severity whenever Emergency Stop fires.
- `safety.realTapBlocked` is logged if the defensive `attemptRealTapBlocked()` chokepoint is ever
  invoked. There is no UI path to enable real taps; `SafetyGate.attemptRealTap()` always returns
  `false`.
