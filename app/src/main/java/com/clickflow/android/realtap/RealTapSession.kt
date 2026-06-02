package com.clickflow.android.realtap

import java.util.UUID

/**
* A short-lived, in-memory session that gates the single-real-tap prototype.
*
* Step 62. Sessions are NEVER persisted (no disk, no SharedPreferences, no
* backup). They live only on the heap of the current process and reset on
* every cold start.
*
* Lifecycle:
*   - [start] creates a session once the safety review has been passed.
*   - Each tap requires a fresh [grantConsent] call (consent is single-use).
*   - [end] tears the session down (e.g. when the user leaves the prototype
*     screen or presses Emergency Stop).
*
* Consent freshness: a consent is valid for at most [CONSENT_TTL_MS] after
* [grantConsent], and is consumed by [consumeConsent]. Whichever comes first
* invalidates the consent.
*/
data class RealTapSession(
   val id: String,
   val startedAtMs: Long,
   val consentGrantedAtMs: Long? = null,
   val consentNonce: String? = null,
) {
   fun grantConsent(nowMs: Long): RealTapSession =
       copy(consentGrantedAtMs = nowMs, consentNonce = UUID.randomUUID().toString())

   fun consumeConsent(nowMs: Long): RealTapSession =
       if (hasFreshConsent(nowMs)) copy(consentGrantedAtMs = null, consentNonce = null)
       else this

   fun hasFreshConsent(nowMs: Long): Boolean {
       val granted = consentGrantedAtMs ?: return false
       return (nowMs - granted) in 0..CONSENT_TTL_MS && consentNonce != null
   }

   companion object {
       /** Consent must be acted on within 10 seconds of the user pressing Confirm. */
       const val CONSENT_TTL_MS: Long = 10_000L

       fun start(nowMs: Long): RealTapSession =
           RealTapSession(id = "rts_${UUID.randomUUID()}", startedAtMs = nowMs)
   }
}
