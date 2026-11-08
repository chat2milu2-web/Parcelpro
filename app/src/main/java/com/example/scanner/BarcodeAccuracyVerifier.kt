package com.example.scanner

/**
 * Accuracy-First Multi-Frame Barcode Verifier for Parcel Pro.
 *
 * Requirements:
 * - Expected format: EW112223445IN
 * - Strict pattern: ^[A-Z]{2}[0-9]{9}IN$ (Exactly 13 characters: 2 letters, 9 digits, IN suffix)
 * - Multi-Frame Verification: Must detect the exact same normalized barcode in at least
 *   5 consecutive frames before confirmation.
 * - Any frame with a different value, format error, or empty detection resets the counter.
 * - Never guesses or silently corrects characters.
 */
class BarcodeAccuracyVerifier(
    val requiredConsecutiveFrames: Int = 5
) {
    companion object {
        val STRICT_BARCODE_REGEX = Regex("^[A-Z]{2}[0-9]{9}IN$")
        const val EXACT_LENGTH = 13
    }

    enum class Status {
        IDLE,
        VERIFYING,
        VERIFIED,
        FORMAT_ERROR
    }

    data class State(
        val candidateCode: String? = null,
        val consecutiveFrames: Int = 0,
        val requiredFrames: Int = 5,
        val status: Status = Status.IDLE,
        val verifiedCode: String? = null,
        val invalidNotice: String? = null,
        val progressRatio: Float = 0f
    )

    private var currentCandidate: String? = null
    private var consecutiveCount: Int = 0
    private var lastState: State = State(requiredFrames = requiredConsecutiveFrames)

    /**
     * Checks if a string satisfies the strict 13-character Indian Post barcode format.
     */
    fun isValidFormat(rawCode: String?): Boolean {
        if (rawCode.isNullOrBlank()) return false
        val normalized = rawCode.trim().uppercase()
        return normalized.length == EXACT_LENGTH && STRICT_BARCODE_REGEX.matches(normalized)
    }

    /**
     * Process a raw barcode detected in a CameraX frame.
     *
     * @param detectedRawValue The raw text detected by Google ML Kit Barcode Scanner, or null/blank if no barcode in frame.
     * @return State representing current verification progress (e.g. 1/5, 2/5 ... 5/5, Verified, or Error).
     */
    @Synchronized
    fun processFrame(detectedRawValue: String?): State {
        if (detectedRawValue.isNullOrBlank()) {
            // No barcode detected in this frame.
            // Reset consecutive counter to ensure continuous verified frames.
            consecutiveCount = 0
            currentCandidate = null
            lastState = State(
                candidateCode = null,
                consecutiveFrames = 0,
                requiredFrames = requiredConsecutiveFrames,
                status = Status.IDLE,
                verifiedCode = null,
                invalidNotice = null,
                progressRatio = 0f
            )
            return lastState
        }

        val normalized = detectedRawValue.trim().uppercase()

        // 1. Strict Format Validation
        if (normalized.length != EXACT_LENGTH || !STRICT_BARCODE_REGEX.matches(normalized)) {
            consecutiveCount = 0
            currentCandidate = null
            val reason = when {
                normalized.length != EXACT_LENGTH -> "Wrong length: ${normalized.length}/$EXACT_LENGTH chars ('$normalized')"
                !normalized.endsWith("IN") -> "Must end with 'IN' suffix ('$normalized')"
                else -> "Expected 2 letters + 9 digits + IN ('$normalized')"
            }
            lastState = State(
                candidateCode = normalized,
                consecutiveFrames = 0,
                requiredFrames = requiredConsecutiveFrames,
                status = Status.FORMAT_ERROR,
                verifiedCode = null,
                invalidNotice = "Format Mismatch: $reason. Expected format like EW112223445IN.",
                progressRatio = 0f
            )
            return lastState
        }

        // 2. Multi-Frame Consecutive Matching
        if (normalized == currentCandidate) {
            consecutiveCount++
        } else {
            // Value changed! Reset to 1 for the new candidate
            currentCandidate = normalized
            consecutiveCount = 1
        }

        val isFullyVerified = consecutiveCount >= requiredConsecutiveFrames
        val ratio = (consecutiveCount.toFloat() / requiredConsecutiveFrames.toFloat()).coerceIn(0f, 1f)

        lastState = State(
            candidateCode = currentCandidate,
            consecutiveFrames = consecutiveCount.coerceAtMost(requiredConsecutiveFrames),
            requiredFrames = requiredConsecutiveFrames,
            status = if (isFullyVerified) Status.VERIFIED else Status.VERIFYING,
            verifiedCode = if (isFullyVerified) currentCandidate else null,
            invalidNotice = null,
            progressRatio = ratio
        )

        return lastState
    }

    /**
     * Resets the verifier state (e.g. after a parcel has been saved or scanner opened).
     */
    @Synchronized
    fun reset() {
        currentCandidate = null
        consecutiveCount = 0
        lastState = State(requiredFrames = requiredConsecutiveFrames)
    }
}
