package com.example

import com.example.data.pincode.IndianPincodeDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndianPincodeDirectoryTest {

    @Test
    fun testMajorCityPincodeLookup() {
        val kolkata = IndianPincodeDirectory.lookup("700001")
        assertNotNull(kolkata)
        assertEquals("Kolkata", kolkata!!.cityOrDistrict)
        assertEquals("West Bengal", kolkata.state)
        assertEquals("Kolkata, West Bengal", kolkata.displayName)
        assertTrue(kolkata.isExactMatch)

        val delhi = IndianPincodeDirectory.lookup("110001")
        assertNotNull(delhi)
        assertEquals("New Delhi", delhi!!.cityOrDistrict)
        assertEquals("Delhi", delhi.state)
        assertEquals("New Delhi, Delhi", delhi.displayName)

        val mumbai = IndianPincodeDirectory.lookup("400001")
        assertNotNull(mumbai)
        assertEquals("Mumbai", mumbai!!.cityOrDistrict)
        assertEquals("Maharashtra", mumbai.state)

        val bangalore = IndianPincodeDirectory.lookup("560001")
        assertNotNull(bangalore)
        assertEquals("Bengaluru", bangalore!!.cityOrDistrict)
        assertEquals("Karnataka", bangalore.state)

        val chennai = IndianPincodeDirectory.lookup("600001")
        assertNotNull(chennai)
        assertEquals("Chennai", chennai!!.cityOrDistrict)
        assertEquals("Tamil Nadu", chennai.state)
    }

    @Test
    fun testTwoDigitCircleFallback() {
        // 75 prefix is Odisha
        val odisha = IndianPincodeDirectory.lookup("759999")
        assertNotNull(odisha)
        assertEquals("Odisha", odisha!!.state)

        // 80 prefix is Bihar
        val bihar = IndianPincodeDirectory.lookup("809999")
        assertNotNull(bihar)
        assertEquals("Bihar", bihar!!.state)
    }

    @Test
    fun testInvalidPincode() {
        assertNull(IndianPincodeDirectory.lookup("12345")) // 5 digits
        assertNull(IndianPincodeDirectory.lookup("1234567")) // 7 digits
        assertNull(IndianPincodeDirectory.lookup(""))
        assertNull(IndianPincodeDirectory.lookup("ABCDEF"))
    }
}
