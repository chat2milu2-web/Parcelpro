package com.example

import com.example.scanner.AddressOcrProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressOcrProcessorTest {

    @Test
    fun testExtractionLogicWithStandardAddress() {
        val sampleText = """
            RAHUL SHARMA
            Flat 4B, Green Meadows Apartment
            Park Street, Near Metro Station
            Kolkata - 700016
            Mob: 9830012345
        """.trimIndent()

        val result = AddressOcrProcessor.extractAddressInfoFromText(sampleText)

        assertEquals("RAHUL SHARMA", result.addresseeName)
        assertEquals(1, result.pincodes.size)
        assertEquals("700016", result.pincodes[0])
    }

    @Test
    fun testExtractionWithToPrefixAndMultiplePins() {
        val sampleText = """
            To: PRIYA SEN
            Sector V, Salt Lake City
            PIN: 700091
            Hub Pin: 700001
            Phone: +91 9123456789
        """.trimIndent()

        val result = AddressOcrProcessor.extractAddressInfoFromText(sampleText)

        assertEquals("PRIYA SEN", result.addresseeName)
        // Should find both 700091 and 700001, without matching the 10-digit phone number
        assertEquals(2, result.pincodes.size)
        assertTrue(result.pincodes.contains("700091"))
        assertTrue(result.pincodes.contains("700001"))
    }

    @Test
    fun testPhoneNumbersNotAcceptedAsPincode() {
        val sampleText = """
            ANIL KUMAR
            Main Road, Ranchi
            Contact: 9431102938
        """.trimIndent()

        val result = AddressOcrProcessor.extractAddressInfoFromText(sampleText)

        assertEquals("ANIL KUMAR", result.addresseeName)
        // Phone number should NOT be parsed as a 6-digit PIN code
        assertEquals(0, result.pincodes.size)
    }
}
