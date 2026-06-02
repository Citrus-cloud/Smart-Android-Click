package com.clickflow.android.realtap

/**
* A 10-point safety checklist the user must explicitly accept before the
* single-real-tap prototype is unlocked for the current app session.
*
* Step 62. The review is in-memory only: it is NOT persisted to disk, NOT
* exported in backups, and resets when the process dies. This is intentional —
* we want the user to re-read the list every time they relaunch.
*/
data class RealTapSafetyReview(
   val itemsAcknowledged: Set<String> = emptySet(),
   val passedAtMs: Long? = null,
) {
   val passed: Boolean get() = passedAtMs != null && itemsAcknowledged.size >= REQUIRED_ITEMS.size

   fun acknowledge(itemKey: String): RealTapSafetyReview {
       require(itemKey in REQUIRED_ITEMS) { "Unknown safety review item: $itemKey" }
       return copy(itemsAcknowledged = itemsAcknowledged + itemKey)
   }

   fun revoke(itemKey: String): RealTapSafetyReview =
       copy(
           itemsAcknowledged = itemsAcknowledged - itemKey,
           passedAtMs = if (itemsAcknowledged.size - 1 < REQUIRED_ITEMS.size) null else passedAtMs,
       )

   fun markPassed(nowMs: Long): RealTapSafetyReview {
       check(itemsAcknowledged.containsAll(REQUIRED_ITEMS)) {
           "Cannot mark passed — missing items: ${REQUIRED_ITEMS - itemsAcknowledged}"
       }
       return copy(passedAtMs = nowMs)
   }

   fun reset(): RealTapSafetyReview = RealTapSafetyReview()

   companion object {
       /** Stable keys for the 10 required acknowledgements. Used as strings.xml ids. */
       val REQUIRED_ITEMS: List<String> = listOf(
           "realtap_review_single_tap_only",
           "realtap_review_no_repeat",
           "realtap_review_no_loop",
           "realtap_review_no_ocr",
           "realtap_review_no_screen_capture",
           "realtap_review_user_initiated",
           "realtap_review_emergency_stop",
           "realtap_review_session_in_memory",
           "realtap_review_no_background",
           "realtap_review_understand_risk",
       )

       val EMPTY = RealTapSafetyReview()
   }
}
