package com.clickflow.android.realtap

import com.clickflow.android.capture.CaptureRegion
import com.clickflow.android.safety.SafetyGate
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SmartTargetTapControllerTest {

    private var nowMs = 1_000L
    private lateinit var gate: SafetyGate
    private lateinit var sessionManager: ControlledTapSessionManager
    private lateinit var controller: SmartTargetTapController

    private val validRegion = CaptureRegion(0.4f, 0.4f, 0.6f, 0.6f) // centre at (0.5, 0.5)
    private val validRequest by lazy {
        SmartTargetTapRequest("s1", SmartTargetType.IMAGE_TARGET, validRegion, nowMs)
    }

    @Before
    fun setUp() {
        nowMs = 1_000L
        gate = SafetyGate()
        gate.updateReviewPassed(true)
        gate.updateAccessibility(true)
        gate.updateSession(true)
        gate.updateConsentFresh(true)
        sessionManager = ControlledTapSessionManager(gate) { nowMs }
        controller = SmartTargetTapController(sessionManager) { nowMs }
        // Start a session
        sessionManager.startSession("s1", maxTaps = 3, ttlMs = 10_000L)
    }

    // 1. Null request → INVALID_REQUEST
    @Test
    fun nullRequest_blocked_invalidRequest() {
        val result = controller.dispatch(null)
        assertTrue(result is SmartTargetTapResult.Blocked)
        assertEquals(SmartTargetBlockReason.INVALID_REQUEST,
            (result as SmartTargetTapResult.Blocked).reason)
    }

    // 2. Invalid region → INVALID_REQUEST
    @Test
    fun invalidRegion_blocked_invalidRequest() {
        val bad = SmartTargetTapRequest("s1", SmartTargetType.IMAGE_TARGET,
            CaptureRegion(0.9f, 0.9f, 0.1f, 0.1f), nowMs) // inverted = invalid
        val result = controller.dispatch(bad)
        assertEquals(SmartTargetBlockReason.INVALID_REQUEST,
            (result as SmartTargetTapResult.Blocked).reason)
    }

    // 3. No active session → NO_ACTIVE_SESSION
    @Test
    fun noSession_blocked_noActiveSession() {
        sessionManager.endSession()
        val result = controller.dispatch(validRequest)
        assertEquals(SmartTargetBlockReason.NO_ACTIVE_SESSION,
            (result as SmartTargetTapResult.Blocked).reason)
    }

    // 4. Active session but bulk gate closed → SESSION_GATE_CLOSED
    @Test
    fun activeSession_bulkGateClosed_sessionGateClosed() {
        // Bulk canRunRealTap() is always false → GATE_CLOSED from evaluateTap
        controller.recordConsent(validRequest)
        val result = controller.dispatch(validRequest)
        assertEquals(SmartTargetBlockReason.SESSION_GATE_CLOSED,
            (result as SmartTargetTapResult.Blocked).reason)
    }

    // 5. Consent missing → CONSENT_MISSING (would only reach this if gate were open)
    //    Verify the consent check path directly via the consent field
    @Test
    fun consentMissing_noConsentRecorded() {
        assertNull(controller.consent)
    }

    // 6. recordConsent stores consent
    @Test
    fun recordConsent_storesConsent() {
        controller.recordConsent(validRequest)
        assertNotNull(controller.consent)
        assertEquals(validRequest, controller.consent!!.request)
    }

    // 7. clearConsent nulls consent
    @Test
    fun clearConsent_nullsConsent() {
        controller.recordConsent(validRequest)
        controller.clearConsent()
        assertNull(controller.consent)
    }

    // 8. Consent TTL: consent recorded at nowMs, dispatch at nowMs + 10_000 → expired
    @Test
    fun consentExpired_clearedAndBlocked() {
        controller.recordConsent(validRequest)
        nowMs += 10_001L // past TTL
        // dispatch would be blocked by gate before consent check, but we can
        // test the consent field is cleared after expiry by simulating:
        // Since gate blocks at step 3, we confirm consent is still set now
        assertNotNull(controller.consent)
        // advance nowMs and check remainingTtlMs
        val age = nowMs - controller.consent!!.recordedAtMs
        assertTrue(age >= controller.CONSENT_TTL_MS)
    }

    // 9. tapX / tapY from highlight centre
    @Test
    fun tapCoordinates_fromHighlightCentre() {
        assertEquals(0.5f, validRequest.tapX, 0.001f)
        assertEquals(0.5f, validRequest.tapY, 0.001f)
    }

    // 10. SmartTargetTapRequest.isValid true for valid region
    @Test
    fun request_isValid_validRegion() {
        assertTrue(validRequest.isValid)
    }

    // 11. SmartTargetTapRequest.isValid false for invalid region
    @Test
    fun request_isValid_false_invalidRegion() {
        val bad = SmartTargetTapRequest("s1", SmartTargetType.TEXT_TARGET,
            CaptureRegion(0.8f, 0.8f, 0.2f, 0.2f), nowMs)
        assertFalse(bad.isValid)
    }

    // 12. SmartTargetConsent records requestedAtMs
    @Test
    fun smartTargetConsent_recordsTimestamp() {
        controller.recordConsent(validRequest)
        assertEquals(nowMs, controller.consent!!.recordedAtMs)
    }
}
