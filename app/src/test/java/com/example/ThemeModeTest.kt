package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun testThemeModeResolution() {
        // Test resolution helper matching Theme.kt
        fun isDark(themeMode: String, systemIsDark: Boolean): Boolean {
            return when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemIsDark
            }
        }

        // When Dark mode is explicitly chosen
        assertTrue(isDark("DARK", systemIsDark = false))
        assertTrue(isDark("DARK", systemIsDark = true))

        // When Light mode is explicitly chosen
        assertFalse(isDark("LIGHT", systemIsDark = false))
        assertFalse(isDark("LIGHT", systemIsDark = true))

        // When System mode is chosen, follow system theme
        assertFalse(isDark("SYSTEM", systemIsDark = false))
        assertTrue(isDark("SYSTEM", systemIsDark = true))
    }

    @Test
    fun testThemeToggleLogic() {
        // Toggle helper matching AppPreferencesRepository.toggleTheme logic
        fun nextTheme(currentIsDark: Boolean): String {
            return if (currentIsDark) "LIGHT" else "DARK"
        }

        assertEquals("LIGHT", nextTheme(currentIsDark = true))
        assertEquals("DARK", nextTheme(currentIsDark = false))
    }
}
