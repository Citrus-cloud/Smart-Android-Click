# Android Simple Clicker UX — ClickFlow Android (Step 59)

## Purpose

Make the app feel like a simple, beautiful mobile clicker. The home screen focuses on one task:
place a click point and press Start. All power-user features move into **Advanced**.

## User feedback (what we addressed)

- UI was too complex; too many buttons; unclear "where the clicker is".
- No obvious "simple clicker" mode; no draggable marker; coordinates shown instead of a visual point.
- Request: clean, minimal, calm, mobile-first; hide complexity under Advanced.

## New simple mode (default screen)

`Screen.HOME` is now the **Simple Clicker**:

1. Header — app name, a light "Simulation-only" badge, and "Drag the marker and press Start".
2. **Tap target** — a large rounded area containing a draggable circular marker (dot in the center).
3. **Quick settings** — Interval (− / +) and Count (− / +) steppers.
4. **Start** — the single most prominent button; plus Stop and Emergency Stop on one row.
5. **Status** — current status, progress bar, tap count, last simulated-tap log.
6. **Advanced** — opens the full menu.

## Clean minimal UI

- Material 3 with a calm indigo/neutral palette (see `Theme.kt`); soft rounded corners (20–24dp);
  light cards; even spacing; large finger-friendly buttons; short text.
- Dark/light themes both tuned for readability (no acid colors).

## Draggable marker

In-app circular marker dragged with the finger (`detectDragGestures`), clamped to the target area.
Position is normalized (0..1) and rendered via `offset`. See `ANDROID_MARKER_MODEL.md`.

## Quick settings

Interval (default 500 ms, min 100) and Count (default 10, min 1, max 1000) adjust the **Quick
clicker** scenario directly — no need to open the scenario editor.

## Advanced menu

`Screen.ADVANCED` lists: Scenarios, Profiles, Audit Log, Backup, Safety Center, Diagnostics, About.
The Simple Clicker screen no longer shows these directly. Back from any of them returns to Advanced;
Back from Advanced returns to the Simple Clicker.

## Simulation-only behavior

Start runs the **Quick clicker** scenario (one `SIMULATED_TAP` at the marker, `repeatCount`,
`intervalMs`) through the simulation engine. **No real taps.** `simulationOnly = true`,
`realTapsEnabled = false`, `canRunRealTap() = false`.

## What is not implemented yet

- No system overlay (no floating marker over other apps).
- No real taps / Accessibility Service / MediaProjection / overlay permission.

## Future system overlay plan

A future step may add a true floating overlay marker (requires `SYSTEM_ALERT_WINDOW` + explicit
consent) and, separately, real taps via an Accessibility Service — both strictly behind consent,
safety gates, and a go/no-go review. Not part of Step 59.
