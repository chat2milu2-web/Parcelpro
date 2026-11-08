package com.example

import com.example.scanner.BarcodeAccuracyVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeAccuracyVerifierTest {

    private val verifier = BarcodeAccuracyVerifier(requiredConsecutiveFrames = 5)

    @Test
    fun testExact13CharRegexValidation() {
        // Valid 13-character Indian Post barcodes (2 letters + 9 digits + IN)
        assertTrue(verifier.isValidFormat("EW112223445IN"))
        assertTrue(verifier.isValidFormat("EM987654321IN"))
        assertTrue(verifier.isValidFormat("CP123456789IN"))
        assertTrue(verifier.isValidFormat("SP000000001IN"))
        assertTrue(verifier.isValidFormat("ew112223445in")) // lowercase normalized

        // Invalid formats
        assertFalse(verifier.isValidFormat("EW112223445I")) // 12 chars (missing N)
        assertFalse(verifier.isValidFormat("EW1122234451IN")) // 14 chars (extra digit)
        assertFalse(verifier.isValidFormat("1234567890123")) // No letters
        assertFalse(verifier.isValidFormat("EW112223445US")) // Non-IN suffix
        assertFalse(verifier.isValidFormat("E1122234451IN")) // 1 letter prefix
        assertFalse(verifier.isValidFormat("EWA12223445IN")) // 3 letter prefix
        assertFalse(verifier.isValidFormat(""))
        assertFalse(verifier.isValidFormat(null))
    }

    @Test
    fun testFiveConsecutiveFramesRequirement() {
        val validCode = "EW112223445IN"

        // Frame 1
        var state = verifier.processFrame(validCode)
        assertEquals(1, state.consecutiveFrames)
        assertEquals(BarcodeAccuracyVerifier.Status.VERIFYING, state.status)
        assertNull(state.verifiedCode)

        // Frame 2
        state = verifier.processFrame(validCode)
        assertEquals(2, state.consecutiveFrames)
        assertEquals(BarcodeAccuracyVerifier.Status.VERIFYING, state.status)
        assertNull(state.verifiedCode)

        // Frame 3
        state = verifier.processFrame(validCode)
        assertEquals(3, state.consecutiveFrames)
        assertEquals(BarcodeAccuracyVerifier.Status.VERIFYING, state.status)
        assertNull(state.verifiedCode)

        // Frame 4
        state = verifier.processFrame(validCode)
        assertEquals(4, state.consecutiveFrames)
        assertEquals(BarcodeAccuracyVerifier.Status.VERIFYING, state.status)
        assertNull(state.verifiedCode)

        // Frame 5 -> Should verify!
        state = verifier.processFrame(validCode)
        assertEquals(5, state.consecutiveFrames)
        assertEquals(BarcodeAccuracyVerifier.Status.VERIFIED, state.status)
        assertEquals(validCode, state.verifiedCode)
        assertEquals(1.0f, state.progressRatio, 0.001f)
    }

    @Test
    fun testMismatchResetsCounter() {
        val code1 = "EW112223445IN"
        val code2 = "EM987654321IN"

        // Frames 1..3 with code 1
        verifier.processFrame(code1)
        verifier.processFrame(code1)
        var state = verifier.processFrame(code1)
        assertEquals(3, state.consecutiveFrames)

        // Frame 4 with different code2 -> Resets counter to 1 for code2
        state = verifier.processFrame(code2)
        assertEquals(1, state.consecutiveFrames)
        assertEquals(code2, state.candidateCode)
        assertEquals(BarcodeAccuracyVerifier.Status.VERIFYING, state.status)
        assertNull(state.verifiedCode)
    }

    @Test
    fun testEmptyFrameResetsCounter() {
        val code = "EW112223445IN"

        verifier.processFrame(code)
        verifier.processFrame(code)
        var state = verifier.processFrame(code)
        assertEquals(3, state.consecutiveFrames)

        // Frame 4 empty (camera moved away or obscured)
        state = verifier.processFrame(null)
        assertEquals(0, state.consecutiveFrames)
        assertEquals(BarcodeAccuracyVerifier.Status.IDLE, state.status)
        assertNull(state.candidateCode)
    }

    @Test
    fun testInvalidFormatRejectsImmediately() {
        val invalidCode = "PARTIAL123"

        val state = verifier.processFrame(invalidCode)
        assertEquals(0, state.consecutiveFrames)
        assertEquals(BarcodeAccuracyVerifier.Status.FORMAT_ERROR, state.status)
        assertNotNull(state.invalidNotice)
        assertNull(state.verifiedCode)
    }

    @Test
    fun testAccuracyBenchmarkWith100Samples() {
        var falseAcceptances = 0
        var successfulVerifications = 0

        // 1. Test 50 valid India Post barcode series (EW, EM, CP, SP, etc.)
        val validPrefixes = listOf("EW", "EM", "CP", "SP", "RK")
        for (i in 0 until 50) {
            val prefix = validPrefixes[i % validPrefixes.size]
            val digits = String.format(java.util.Locale.US, "%09d", 100000000 + i * 137)
            val validBarcode = "${prefix}${digits}IN"

            val testVerifier = BarcodeAccuracyVerifier(5)
            var lastState = testVerifier.processFrame(null)

            // Simulate 5 consecutive clean frames
            for (frame in 1..5) {
                lastState = testVerifier.processFrame(validBarcode)
            }

            if (lastState.status == BarcodeAccuracyVerifier.Status.VERIFIED && lastState.verifiedCode == validBarcode) {
                successfulVerifications++
            }
        }

        // 2. Test 50 invalid, partial, corrupted or noisy barcode detections
        val invalidSamples = mutableListOf<String>()
        for (i in 0 until 50) {
            when (i % 5) {
                0 -> invalidSamples.add("PARTIAL_${i}") // Non-standard prefix
                1 -> invalidSamples.add("EW${String.format("%08d", i)}IN") // 8 digits instead of 9 (12 chars total)
                2 -> invalidSamples.add("EW${String.format("%010d", i)}IN") // 10 digits instead of 9 (14 chars total)
                3 -> invalidSamples.add("EW${String.format("%09d", i)}US") // Non-IN country suffix
                else -> invalidSamples.add("1234567890123") // Numeric only
            }
        }

        for (invalidCode in invalidSamples) {
            val testVerifier = BarcodeAccuracyVerifier(5)
            // Even if detected 10 consecutive times, invalid format MUST NEVER be verified!
            var lastState = testVerifier.processFrame(null)
            for (frame in 1..10) {
                lastState = testVerifier.processFrame(invalidCode)
            }

            if (lastState.status == BarcodeAccuracyVerifier.Status.VERIFIED) {
                falseAcceptances++
            }
        }

        // Assert 0% False Acceptance Rate and 100% Valid Acceptance Rate
        assertEquals("False Acceptance Rate must be exactly 0%", 0, falseAcceptances)
        assertEquals("All 50 valid barcodes must be verified", 50, successfulVerifications)
    }
}
