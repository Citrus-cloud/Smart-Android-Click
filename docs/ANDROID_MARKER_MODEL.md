# Android Marker Model — ClickFlow Android (Step 59)

## Marker purpose

The marker is the visual click point the user drags on the Simple Clicker screen. It replaces typing
coordinates: you move a circle, and Start simulates taps at that point.

## In-app marker

The marker is rendered **inside the app's own UI** (a Compose `Box` in the target area). It is **not**
a system overlay: it cannot appear over other apps, and the app requests no `SYSTEM_ALERT_WINDOW`
permission.

## Coordinate mapping

- The marker position is stored as **normalized fractions** `fx, fy ∈ [0, 1]` in the ViewModel.
- Rendering: pixel center = `(fx * areaWidth, fy * areaHeight)`; the marker is offset by its radius.
- Dragging adds `drag.dx/areaWidth` and `drag.dy/areaHeight` to the fractions, clamped to `[0, 1]`.
- For persistence, fractions are scaled to integers `0..1000` (`round(f * 1000)`).

## Storage

The marker is persisted in the **Quick clicker** scenario's single `SIMULATED_TAP` action
(`x`, `y` in the `0..1000` space), in internal storage (`scenarios.json`). It is written on drag end
and on Start, so the position **survives app restarts**. Interval/count live in that scenario's
settings.

## Limitations

- In-app only; not over other apps.
- Coordinates are an abstract `0..1000` space, not real device pixels (intentional — simulation only).
- No real taps are performed at the marker.

## Future overlay marker

A real floating overlay marker would require `SYSTEM_ALERT_WINDOW` and explicit user consent, and
real taps would require an Accessibility Service. Both are out of scope here and would ship only
behind consent + safety gates + a go/no-go review.
