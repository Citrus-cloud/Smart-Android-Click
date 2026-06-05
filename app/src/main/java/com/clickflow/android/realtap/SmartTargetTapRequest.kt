package com.clickflow.android.realtap

import com.clickflow.android.capture.CaptureRegion

/**
 * Step 75 — A request to perform a single real tap at a smart-target location.
 *
 * Produced by wiring an image-target or text-target result into the controlled
 * tap session. The tap coordinates are derived from the highlight region's centre.
 *
 * @param sessionId      The controlling [ControlledTapSession] id.
 * @param targetType     What kind of smart target produced this request.
 * @param highlightRegion The matched region from image/text analysis.
 * @param requestedAtMs  Wall-clock ms when the request was created.
 */
data class SmartTargetTapRequest(
    val sessionId: String,
    val targetType: SmartTargetType,
    val highlightRegion: CaptureRegion,
    val requestedAtMs: Long
) {
    /** Normalized tap X = horizontal centre of the highlight region. */
    val tapX: Float get() = highlightRegion.centerX

    /** Normalized tap Y = vertical centre of the highlight region. */
    val tapY: Float get() = highlightRegion.centerY

    /** True when the highlight region is geometrically valid. */
    val isValid: Boolean get() = highlightRegion.isValid
}

/** Which analysis produced the smart-target tap request. */
enum class SmartTargetType { IMAGE_TARGET, TEXT_TARGET }

/**
 * Outcome of a smart-target tap dispatch attempt.
 * The DISPATCHED case records that a tap was requested but does not
 * confirm hardware delivery (that is Step 76's concern).
 */
sealed class SmartTargetTapResult {
    /** Tap was admitted by the session and the safety gate (Step 75 marks it allowed). */
    data class Dispatched(val request: SmartTargetTapRequest, val tapNumber: Int) : SmartTargetTapResult()

    /** Tap was blocked before reaching the dispatch path. */
    data class Blocked(
        val request: SmartTargetTapRequest?,
        val reason: SmartTargetBlockReason
    ) : SmartTargetTapResult()
}

/** Why a smart-target tap was blocked. */
enum class SmartTargetBlockReason {
    INVALID_REQUEST,       // highlight region invalid or request null
    NO_ACTIVE_SESSION,     // no controlled session open
    SESSION_GATE_CLOSED,   // ControlledTapSessionManager.evaluateTap blocked
    CONSENT_MISSING,       // no valid consent recorded for this request
    CONSENT_EXPIRED,       // consent TTL elapsed
    MARKER_DRIFT           // tap coordinates differ from consented coordinates
}
