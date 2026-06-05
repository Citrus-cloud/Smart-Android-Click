package com.clickflow.android.realtap

import com.clickflow.android.capture.CaptureRegion
import com.clickflow.android.safety.SafetyGate
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SmartSessionAuditTest {

    private var nowMs = 1_000L
    private lateinit var auditLog: SmartSessionAuditLog
    private lateinit var gate: SafetyGate
    private lateinit var sessionManager: ControlledTapSessionManager
    private lateinit var tapController: SmartTargetTapController
    private lateinit var emergencyStop: SmartSessionEmergencyStop

    @Before
    fun setUp() {
        nowMs = 1_000L
        auditLog = SmartSessionAuditLog(maxEvents = 50) { nowMs }
        gate = SafetyGate()
        gate.updateReviewPassed(true)
        gate.updateAccessibility(true)
        gate.updateSession(true)
        gate.updateConsentFresh(true)
        sessionManager = ControlledTapSessionManager(gate) { nowMs }
        tapController = SmartTargetTapController(sessionManager) { nowMs }
        emergencyStop = SmartSessionEmergencyStop(sessionManager, tapController, auditLog)
    }

    // 1. Audit log starts empty
    @Test
    fun auditLog_startsEmpty() {
        assertEquals(0, auditLog.count)
        assertTrue(auditLog.events.isEmpty())
    }

    // 2. record() adds an event
    @Test
    fun record_addsEvent() {
        auditLog.record(SmartSessionAuditType.SESSION_STARTED, "s1", "test")
        assertEquals(1, auditLog.count)
        assertEquals(SmartSessionAuditType.SESSION_STARTED, auditLog.events.first().type)
    }

    // 3. recordedAtMs comes from nowProvider
    @Test
    fun record_timestampFromNowProvider() {
        auditLog.record(SmartSessionAuditType.TAP_DISPATCHED, "s1")
        assertEquals(nowMs, auditLog.events.first().recordedAtMs)
    }

    // 4. eventsOfType filters correctly
    @Test
    fun eventsOfType_filtersCorrectly() {
        auditLog.record(SmartSessionAuditType.SESSION_STARTED, "s1")
        auditLog.record(SmartSessionAuditType.TAP_DISPATCHED, "s1")
        auditLog.record(SmartSessionAuditType.SESSION_ENDED, "s1")
        val dispatches = auditLog.eventsOfType(SmartSessionAuditType.TAP_DISPATCHED)
        assertEquals(1, dispatches.size)
    }

    // 5. Log discards oldest when maxEvents exceeded
    @Test
    fun log_discardsOldest_whenMaxExceeded() {
        val log = SmartSessionAuditLog(maxEvents = 3) { nowMs }
        log.record(SmartSessionAuditType.SESSION_STARTED, "a")
        log.record(SmartSessionAuditType.TAP_DISPATCHED, "b")
        log.record(SmartSessionAuditType.SESSION_ENDED, "c")
        log.record(SmartSessionAuditType.TAP_BLOCKED, "d")
        assertEquals(3, log.count)
        // oldest (SESSION_STARTED) should be gone
        assertFalse(log.events.any { it.type == SmartSessionAuditType.SESSION_STARTED })
        assertTrue(log.events.any { it.type == SmartSessionAuditType.TAP_BLOCKED })
    }

    // 6. clear() empties log
    @Test
    fun clear_emptiesLog() {
        auditLog.record(SmartSessionAuditType.SESSION_STARTED)
        auditLog.clear()
        assertEquals(0, auditLog.count)
    }

    // 7. exportText returns non-empty string for non-empty log
    @Test
    fun exportText_nonEmpty() {
        auditLog.record(SmartSessionAuditType.TAP_BLOCKED, "s1", "gate_closed")
        val text = auditLog.exportText()
        assertTrue(text.contains("TAP_BLOCKED"))
        assertTrue(text.contains("gate_closed"))
    }

    // 8. EmergencyStop clears consent
    @Test
    fun emergencyStop_clearsConsent() {
        sessionManager.startSession("s1", maxTaps = 2, ttlMs = 10_000L)
        val region = CaptureRegion(0.4f, 0.4f, 0.6f, 0.6f)
        val req = SmartTargetTapRequest("s1", SmartTargetType.IMAGE_TARGET, region, nowMs)
        tapController.recordConsent(req)
        assertNotNull(tapController.consent)
        emergencyStop.execute()
        assertNull(tapController.consent)
    }

    // 9. EmergencyStop terminates session
    @Test
    fun emergencyStop_terminatesSession() {
        sessionManager.startSession("s1", maxTaps = 2, ttlMs = 10_000L)
        emergencyStop.execute()
        assertFalse(sessionManager.hasActiveSession())
    }

    // 10. EmergencyStop records SESSION_EMERGENCY_STOPPED audit event
    @Test
    fun emergencyStop_recordsAuditEvent() {
        sessionManager.startSession("s1", maxTaps = 2, ttlMs = 10_000L)
        emergencyStop.execute("test-stop")
        val events = auditLog.eventsOfType(SmartSessionAuditType.SESSION_EMERGENCY_STOPPED)
        assertEquals(1, events.size)
        assertEquals("s1", events.first().sessionId)
        assertEquals("test-stop", events.first().detail)
    }

    // 11. EmergencyStop with no active session — still records event
    @Test
    fun emergencyStop_noSession_stillRecordsEvent() {
        emergencyStop.execute("no-session")
        val events = auditLog.eventsOfType(SmartSessionAuditType.SESSION_EMERGENCY_STOPPED)
        assertEquals(1, events.size)
        assertNull(events.first().sessionId)
    }

    // 12. Multiple records accumulate
    @Test
    fun multipleRecords_accumulate() {
        repeat(5) { auditLog.record(SmartSessionAuditType.TAP_BLOCKED, detail = "it=$it") }
        assertEquals(5, auditLog.count)
    }

    // 13. exportText empty for empty log
    @Test
    fun exportText_emptyForEmptyLog() {
        assertEquals("", auditLog.exportText())
    }
}
