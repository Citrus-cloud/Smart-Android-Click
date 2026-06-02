# Real-Tap Prototype — Fixes Log

A chronological, append-only record of every fix made against the
single-real-tap prototype after Step 62 landed. Each entry follows the same
schema:

- **Date / commit** — ISO date + short SHA.
- **Symptom** — what the user / QA saw.
- **Root cause** — the bug, in plain language.
- **Fix** — the exact code change.
- **Affected QA scenario(s)** — by ID from `REAL_TAP_QA_SCENARIOS.md`.
- **Invariant impact** — which of the 6 hard invariants this touched (none,
one, or more).

The point of this file is to make it impossible to silently regress a
safety-critical surface. Every fix that touches `RealTapController`,
`SafetyGate.canRunRealTapSingleProto`, the consent flow, or the
`performSingleTap` call site MUST land with an entry here in the same
commit.

---

## 2026-06-02 — Step 63 baseline

**Commit.** `7b251157` (AppInfo.STEP bump to Step 63).

**Symptom.** N/A — this is the starting line.

**Root cause.** N/A.

**Fix.** N/A. Step 63 is the first round of post-landing hardening for the
Step 62 prototype. It wires `RealTapController` into `ClickFlowViewModel`
(so the gated single-tap path is reachable end-to-end), lights up the live
`SafetyGate` state, expands audit granularity, enforces the marker-only
invariant in `confirmRealTap`, and adds a result chip + blocked-reasons
list to the prototype screen.

**Affected QA scenario(s).** All — this is the baseline against which all
17 scenarios in `REAL_TAP_QA_SCENARIOS.md` will be executed.

**Invariant impact.** None at the contract level. `canRunRealTap()` still
returns `false`. `performSingleTap` is still the sole `dispatchGesture`
call site. `RealTapController` is still the sole caller of
`performSingleTap`.

---

## Template — copy this for new entries

```
## YYYY-MM-DD — <short title>

**Commit.** `<short_sha>`.

**Symptom.** <what was observed>

**Root cause.** <plain-language bug>

**Fix.** <exact code change, file paths, brief diff summary>

**Affected QA scenario(s).** <QA-RT-XX, QA-RT-YY>

**Invariant impact.** <none | invariant 1/2/3/4/5/6, with explanation>
```
