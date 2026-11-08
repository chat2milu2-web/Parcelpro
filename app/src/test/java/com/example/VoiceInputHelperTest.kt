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
    fun testWeightDecimalSpoken() {
        val result = VoiceInputHelper.parseVoiceResult("250 point 5", isNumericField = true)
        assertEquals("250.5", result)
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
    }
}
