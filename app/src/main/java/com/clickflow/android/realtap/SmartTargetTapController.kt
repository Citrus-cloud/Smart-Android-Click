package com.clickflow.android.realtap

/**
 * Step 75 — Smart-target tap controller.
 *
 * Wires a [ControlledTapSessionManager] and a single-use consent record into
 * a tap-admission flow that:
 *  1. Validates the [SmartTargetTapRequest] (non-null, valid region).
 *  2. Checks that an active controlled session exists.
 *  3. Evaluates the session tap quota and bulk-gate via
 *     [ControlledTapSessionManager.evaluateTap].
 *  4. Validates the consent record (present, not expired, coordinates match).
 *  5. On all checks passing, records the tap in the session and returns
 *     [SmartTargetTapResult.Dispatched].
 *
 * This step does NOT call `dispatchGesture` — that is Step 76's concern.
 * All state is process-local; nothing is persisted. No Android imports.
 *
 * @param sessionManager  Controlled-session owner.
 * @param nowProvider     Injected clock.
 */
class SmartTargetTapController(
    private val sessionManager: ControlledTapSessionManager,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    /** Consent TTL in ms. */
    val CONSENT_TTL_MS: Long = 10_000L

    private var _consent: SmartTargetConsent? = null

    /** The pending consent, if any. */
    val consent: SmartTargetConsent? get() = _consent

    /**
     * Record consent for the given [request].
     * Overwrites any prior pending consent.
     */
    fun recordConsent(request: SmartTargetTapRequest) {
        _consent = SmartTargetConsent(
            request = request,
            recordedAtMs = nowProvider()
        )
    }

    /** Clear any pending consent. */
    fun clearConsent() { _consent = null }

    /**
     * Attempt to dispatch the tap for [request].
     *
     * All checks must pass for [SmartTargetTapResult.Dispatched] to be returned.
     * On any failure the consent is preserved (not consumed) except on
     * CONSENT_EXPIRED and MARKER_DRIFT where it is cleared.
     */
    fun dispatch(request: SmartTargetTapRequest?): SmartTargetTapResult {
        // 1. Validate request
        if (request == null || !request.isValid) {
            return SmartTargetTapResult.Blocked(request, SmartTargetBlockReason.INVALID_REQUEST)
        }

        // 2. Active session?
        if (!sessionManager.hasActiveSession()) {
            return SmartTargetTapResult.Blocked(request, SmartTargetBlockReason.NO_ACTIVE_SESSION)
        }

        // 3. Session quota + bulk gate
        val tapEval = sessionManager.evaluateTap()
        if (tapEval is ControlledTapDispatchResult.Blocked) {
            return SmartTargetTapResult.Blocked(request, SmartTargetBlockReason.SESSION_GATE_CLOSED)
        }

        // 4. Consent present?
        val c = _consent
            ?: return SmartTargetTapResult.Blocked(request, SmartTargetBlockReason.CONSENT_MISSING)

        // 4a. Consent expired?
        val now = nowProvider()
        if (now - c.recordedAtMs >= CONSENT_TTL_MS) {
            clearConsent()
            return SmartTargetTapResult.Blocked(request, SmartTargetBlockReason.CONSENT_EXPIRED)
        }

        // 4b. Marker drift (tap coordinates must match consented coordinates within tolerance)
        val dx = kotlin.math.abs(request.tapX - c.request.tapX)
        val dy = kotlin.math.abs(request.tapY - c.request.tapY)
        if (dx > COORD_TOLERANCE || dy > COORD_TOLERANCE) {
            clearConsent()
            return SmartTargetTapResult.Blocked(request, SmartTargetBlockReason.MARKER_DRIFT)
        }

        // 5. All checks passed — record tap in session, consume consent
        val session = sessionManager.session!!
        val tapNumber = session.tapsDispatched + 1
        session.recordTap(now)
        clearConsent()

        return SmartTargetTapResult.Dispatched(request, tapNumber)
    }

    companion object {
        /** Normalized coordinate drift tolerance (2% of screen width/height). */
        const val COORD_TOLERANCE = 0.02f
    }
}

/**
 * A recorded consent for one smart-target tap.
 *
 * @param request       The original tap request that was consented to.
 * @param recordedAtMs  Wall-clock ms when consent was recorded.
 */
data class SmartTargetConsent(
    val request: SmartTargetTapRequest,
    val recordedAtMs: Long
)
