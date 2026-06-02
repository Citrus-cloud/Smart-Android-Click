package com.clickflow.android.realtap

import android.os.Build
import com.clickflow.android.permissions.ClickFlowAccessibilityService
import com.clickflow.android.safety.SafetyGate

/**
* Step 62 — orchestrates a single real-tap request end-to-end.
*
* Responsibilities (in strict order):
*   1. Ask [SafetyGate.canRunRealTapSingleProto]. If false -> BLOCKED_BY_GATE,
*      with a precise list of which sub-conditions failed.
*   2. Verify a session is active and consent is fresh.
*   3. Verify the AccessibilityService is currently bound
*      ([ClickFlowAccessibilityService.isConnected]).
*   4. Consume the per-tap consent atomically (it cannot be reused).
*   5. Call [ClickFlowAccessibilityService.performSingleTap] for ONE tap.
*   6. Return a [RealTapResult] with the outcome.
*
* The controller does NOT itself decide whether the safety review has been
* passed or whether to ask for consent — that is the UI/ViewModel's job. It
* only reads the resulting flags via [SafetyGate].
*
* The controller holds no state of its own; it is safe to instantiate per call.
*/
class RealTapController(
   private val gate: SafetyGate,
   private val clock: () -> Long = System::currentTimeMillis,
   /** Hook for tests — production passes the real service singleton. */
   private val serviceProvider: () -> ClickFlowAccessibilityService? =
       { ClickFlowAccessibilityService.liveInstance },
) {

   /**
    * Execute a single real-tap request. Synchronous; the result captures the
    * dispatch decision but the gesture itself is best-effort async at the OS
    * level — DISPATCH_SUCCEEDED only means the system accepted the gesture.
    */
   fun dispatch(request: RealTapRequest): RealTapResult {
       // 1. API level check — dispatchGesture is API 24+.
       if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
           return RealTapResult(
               outcome = RealTapOutcome.BLOCKED_API_TOO_LOW,
               request = request,
               happenedAtMs = clock(),
               blockedReasons = listOf("Device API ${Build.VERSION.SDK_INT} is below 24."),
           )
       }

       // 2. Safety gate — composite check.
       if (!gate.canRunRealTapSingleProto()) {
           return RealTapResult(
               outcome = RealTapOutcome.BLOCKED_BY_GATE,
               request = request,
               happenedAtMs = clock(),
               blockedReasons = gate.getSingleProtoBlockedReasons(),
           )
       }

       // 3. Accessibility service must be live in this process.
       val service = serviceProvider() ?: return RealTapResult(
           outcome = RealTapOutcome.BLOCKED_ACCESSIBILITY_DISABLED,
           request = request,
           happenedAtMs = clock(),
           blockedReasons = listOf("AccessibilityService is not currently connected."),
       )

       // 4. Dispatch — exactly one stroke.
       val accepted = service.performSingleTap(
           x = request.x,
           y = request.y,
           durationMs = request.durationMs,
       )

       return RealTapResult(
           outcome = if (accepted) RealTapOutcome.DISPATCH_SUCCEEDED
           else RealTapOutcome.DISPATCH_FAILED,
           request = request,
           happenedAtMs = clock(),
       )
   }
}
