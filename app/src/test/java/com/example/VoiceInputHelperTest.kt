package com.example

import com.example.util.VoiceInputHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceInputHelperTest {

    @Test
    fun testEnglishSpokenDigits() {
        val result = VoiceInputHelper.parseVoiceResult("seven zero zero zero zero one", isNumericField = true)
        assertEquals("700001", result)
    }

    @Test
    fun testDoubleAndTriplePincodeDigits() {
        assertEquals(
            "700001",
            VoiceInputHelper.parseVoiceResult("seven double zero double zero one", isNumericField = true)
        )
        assertEquals(
            "700012",
            VoiceInputHelper.parseVoiceResult("seven triple zero one two", isNumericField = true)
        )
        assertEquals(
            "700001",
            VoiceInputHelper.parseVoiceResult("সাত ডাবল শূন্য ডাবল শূন্য এক", isNumericField = true)
        )
        assertEquals(
            "700111",
            VoiceInputHelper.parseVoiceResult("सात डबल शून्य ट्रिपल एक", isNumericField = true)
        )
    }

    @Test
    fun testHindiSpokenDigits() {
        val result = VoiceInputHelper.parseVoiceResult("सात शून्य शून्य शून्य शून्य एक", isNumericField = true)
        assertEquals("700001", result)
    }

    @Test
    fun testBengaliSpokenDigits() {
        val result = VoiceInputHelper.parseVoiceResult("সাত শূন্য শূন্য শূন্য শূন্য এক", isNumericField = true)
        assertEquals("700001", result)
    }

    @Test
    fun testEnglishNameFormatting() {
        val result = VoiceInputHelper.parseVoiceResult("rahul sharma", isNumericField = false)
        assertEquals("RAHUL SHARMA", result)
    }

    @Test
    fun testBengaliAndHindiNameTransliteration() {
        assertEquals("MILAN DAS", VoiceInputHelper.parseVoiceResult("মিলন দাস", isNumericField = false))
        assertEquals("RAHIM SHEKH", VoiceInputHelper.parseVoiceResult("রহিম শেখ", isNumericField = false))
        assertEquals("RAHUL SHARMA", VoiceInputHelper.parseVoiceResult("राहुल शर्मा", isNumericField = false))
        assertEquals("AMIT KAR", VoiceInputHelper.parseVoiceResult("অমিত কর", isNumericField = false))
        assertEquals(
            "MILAN DAS",
            VoiceInputHelper.parseVoiceCandidates(listOf("মিলন দাস"), isNumericField = false)
        )
    }

    @Test
    fun testWeightDecimalAndFractionalSpoken() {
        assertEquals("250.5", VoiceInputHelper.parseVoiceResult("250 point 5", isNumericField = true))
        assertEquals("1.5", VoiceInputHelper.parseVoiceResult("দেড় কেজি", isNumericField = true))
        assertEquals("2.5", VoiceInputHelper.parseVoiceResult("আড়াই কেজি", isNumericField = true))
        assertEquals("2.25", VoiceInputHelper.parseVoiceResult("সওয়া দুই", isNumericField = true))
        assertEquals("2.75", VoiceInputHelper.parseVoiceResult("পৌনে তিন", isNumericField = true))
        assertEquals("0.5", VoiceInputHelper.parseVoiceResult("হাফ কেজি", isNumericField = true))
        assertEquals("250", VoiceInputHelper.parseVoiceResult("আড়াইশো গ্রাম", isNumericField = true))
    }

    @Test
    fun testMultipleDimensionsSpoken() {
        val result1 = VoiceInputHelper.extractMultipleNumbers("12 23 4")
        assertEquals(listOf("12", "23", "4"), result1)

        val result2 = VoiceInputHelper.extractMultipleNumbers("12 by 23 by 4")
        assertEquals(listOf("12", "23", "4"), result2)

        val result3 = VoiceInputHelper.extractMultipleNumbers("12 cross 23 cross 4")
        assertEquals(listOf("12", "23", "4"), result3)

        val result4 = VoiceInputHelper.extractMultipleNumbers("twelve twenty three four")
        assertEquals(listOf("12", "23", "4"), result4)

        val result5 = VoiceInputHelper.extractMultipleNumbers("বারো বাই পনেরো বাই আট")
        assertEquals(listOf("12", "15", "8"), result5)
    }
}
