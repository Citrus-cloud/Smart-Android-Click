package com.clickflow.android.realtap

import com.clickflow.android.safety.SafetyGate
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ControlledTapSessionManagerTest {

    private var nowMs = 0L
    private lateinit var gate: SafetyGate
    private lateinit var manager: ControlledTapSessionManager

    @Before
    fun setUp() {
        nowMs = 1_000L
        gate = SafetyGate()
        // Enable all single-proto flags so canRunControlledRealTapSession returns true
        gate.updateReviewPassed(true)
        gate.updateAccessibility(true)
        gate.updateSession(true)
        gate.updateConsentFresh(true)
        manager = ControlledTapSessionManager(gate) { nowMs }
    }

    // 1. No active session initially
    @Test
    fun noActiveSession_initially() {
        assertFalse(manager.hasActiveSession())
        assertNull(manager.session)
    }

    // 2. Start session → active
    @Test
    fun startSession_becomesActive() {
        val result = manager.startSession("s1", maxTaps = 3, ttlMs = 10_000L)
        assertTrue(result is ControlledTapSessionManager.SessionResult.Ok)
        assertTrue(manager.hasActiveSession())
    }

    // 3. Cannot start second session while one is active
    @Test
    fun startSession_alreadyActive_returnsError() {
        manager.startSession("s1")
        val result = manager.startSession("s2")
        assertTrue(result is ControlledTapSessionManager.SessionResult.Error)
        assertEquals("already_active", (result as ControlledTapSessionManager.SessionResult.Error).reason)
    }

    // 4. Invalid maxTaps returns error
    @Test
    fun startSession_invalidMaxTaps_returnsError() {
        val result = manager.startSession("s1", maxTaps = 0)
        assertEquals("invalid_params", (result as ControlledTapSessionManager.SessionResult.Error).reason)
    }

    // 5. Invalid TTL returns error
    @Test
    fun startSession_invalidTtl_returnsError() {
        val result = manager.startSession("s1", ttlMs = 500L)
        assertEquals("invalid_params", (result as ControlledTapSessionManager.SessionResult.Error).reason)
    }

    // 6. End session clears active session
    @Test
    fun endSession_clearsSession() {
        manager.startSession("s1")
        manager.endSession()
        assertFalse(manager.hasActiveSession())
        assertNull(manager.session)
    }

    // 7. evaluateTap with no session → SESSION_INACTIVE
    @Test
    fun evaluateTap_noSession_blocked_inactive() {
        val result = manager.evaluateTap()
        assertTrue(result is ControlledTapDispatchResult.Blocked)
        assertEquals(
            ControlledTapBlockReason.SESSION_INACTIVE,
            (result as ControlledTapDispatchResult.Blocked).reason
        )
    }

    // 8. evaluateTap within active session → GATE_CLOSED (bulk gate is false)
    @Test
    fun evaluateTap_activeSession_gateClosedByBulk() {
        manager.startSession("s1", maxTaps = 3, ttlMs = 10_000L)
        val result = manager.evaluateTap()
        // Bulk canRunRealTap() always false → GATE_CLOSED
        assertTrue(result is ControlledTapDispatchResult.Blocked)
        assertEquals(
            ControlledTapBlockReason.GATE_CLOSED,
            (result as ControlledTapDispatchResult.Blocked).reason
        )
    }

    // 9. Session expires after TTL
    @Test
    fun session_expiredAfterTtl_blockedExpired() {
        manager.startSession("s1", maxTaps = 3, ttlMs = 5_000L)
        nowMs += 6_000L // advance past TTL
        assertFalse(manager.hasActiveSession())
    }

    // 10. recordTap increments count; exhausted after maxTaps
    @Test
    fun recordTap_exhaustsSession() {
        manager.startSession("s1", maxTaps = 2, ttlMs = 10_000L)
        val s = manager.session!!
        assertTrue(s.recordTap(nowMs))
        assertTrue(s.recordTap(nowMs))
        assertTrue(s.isExhausted())
        assertFalse(s.recordTap(nowMs)) // 3rd tap rejected
    }

    // 11. remainingTaps decrements correctly
    @Test
    fun remainingTaps_decrementsOnRecord() {
        manager.startSession("s1", maxTaps = 3, ttlMs = 10_000L)
        val s = manager.session!!
        assertEquals(3, s.remainingTaps())
        s.recordTap(nowMs)
        assertEquals(2, s.remainingTaps())
    }

    // 12. remainingTtlMs decrements with time
    @Test
    fun remainingTtlMs_decrements() {
        manager.startSession("s1", maxTaps = 1, ttlMs = 10_000L)
        val s = manager.session!!
        nowMs += 3_000L
        assertEquals(7_000L, s.remainingTtlMs(nowMs))
    }

    // 13. emergencyStop terminates session
    @Test
    fun emergencyStop_terminatesSession() {
        manager.startSession("s1")
        manager.emergencyStop()
        assertFalse(manager.hasActiveSession())
    }

    // 14. evaluateTap after terminate → SESSION_INACTIVE (session nulled by endSession)
    @Test
    fun evaluateTap_afterTerminate_inactive() {
        manager.startSession("s1")
        manager.endSession()
        val result = manager.evaluateTap()
        assertEquals(
            ControlledTapBlockReason.SESSION_INACTIVE,
            (result as ControlledTapDispatchResult.Blocked).reason
        )
    }
}
