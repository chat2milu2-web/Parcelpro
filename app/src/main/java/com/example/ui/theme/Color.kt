package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// VIBRANT COLOR PALETTE (Colourful & Modern)
// ==========================================

// Primary Brand Colors: Electric Indigo & Royal Blue
val PrimaryIndigo = Color(0xFF4F46E5)
val PrimaryIndigoDark = Color(0xFF3730A3)
val PrimaryIndigoLight = Color(0xFF818CF8)
val RoyalBlue = Color(0xFF2563EB)
val RoyalBlueDark = Color(0xFF1D4ED8)
val RoyalBlueLight = Color(0xFF60A5FA)

// Secondary & Accent Colors: Vivid Cyan & Teal
val CyanVibrant = Color(0xFF06B6D4)
val CyanAccentLight = Color(0xFF38BDF8)
val CyanAccentDark = Color(0xFF0891B2)
val EmeraldGreen = Color(0xFF10B981)
val EmeraldDark = Color(0xFF059669)

// Tertiary & High-Energy Workflows: Vivid Purple, Amber & Coral
val VividPurple = Color(0xFF8B5CF6)
val DeepViolet = Color(0xFF7C3AED)
val AmberVivid = Color(0xFFF59E0B)
val AmberDark = Color(0xFFD97706)
val SunsetOrange = Color(0xFFF97316)
val RosePink = Color(0xFFF43F5E)
val CrimsonRed = Color(0xFFE11D48)

// Status / Feedback Colors
val SuccessGreen = Color(0xFF10B981)
val WarningOrange = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)
val InfoBlue = Color(0xFF3B82F6)

// LIGHT THEME SURFACES & BACKGROUNDS (Crisp, colorful, high contrast)
val LightBackground = Color(0xFFF1F5F9) // Subtle crisp slate-blue background
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightPrimaryContainer = Color(0xFFEEF2FF) // Soft indigo container
val LightOnPrimaryContainer = Color(0xFF312E81)
val LightSecondaryContainer = Color(0xFFE0F2FE) // Soft sky container
val LightOnSecondaryContainer = Color(0xFF075985)
val LightTertiaryContainer = Color(0xFFF3E8FF) // Soft purple container
val LightOnTertiaryContainer = Color(0xFF581C87)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFFCBD5E1)

// DARK THEME SURFACES & BACKGROUNDS (Deep cosmic navy with glowing accents)
val DarkBackground = Color(0xFF0B0F19) // Deep cosmic navy
val DarkSurface = Color(0xFF131B2E) // Rich midnight card surface
val DarkSurfaceVariant = Color(0xFF1E293B) // High contrast elevated card
val DarkPrimaryContainer = Color(0xFF312E81)
val DarkOnPrimaryContainer = Color(0xFFE0E7FF)
val DarkSecondaryContainer = Color(0xFF0C4A6E)
val DarkOnSecondaryContainer = Color(0xFFBAE6FD)
val DarkTertiaryContainer = Color(0xFF581C87)
val DarkOnTertiaryContainer = Color(0xFFF3E8FF)
val DarkOnSurface = Color(0xFFF8FAFC)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)

// ==========================================
// COLORFUL GRADIENT PRESETS FOR WORKFLOW CARDS
// ==========================================
object AppGradients {
    val Hero = Brush.linearGradient(
        colors = listOf(Color(0xFF4F46E5), Color(0xFF2563EB), Color(0xFF06B6D4))
    )
    val SequentialMode = Brush.linearGradient(
        colors = listOf(Color(0xFF1D4ED8), Color(0xFF0284C7), Color(0xFF06B6D4))
    )
    val FieldWiseMode = Brush.linearGradient(
        colors = listOf(Color(0xFF6D28D9), Color(0xFF8B5CF6), Color(0xFFD946EF))
    )
    val ExportExcel = Brush.linearGradient(
        colors = listOf(Color(0xFF047857), Color(0xFF10B981), Color(0xFF34D399))
    )
    val BatchAction = Brush.linearGradient(
        colors = listOf(Color(0xFFB45309), Color(0xFFF59E0B), Color(0xFFFBBF24))
    )
    val HistoryAction = Brush.linearGradient(
        colors = listOf(Color(0xFF1E40AF), Color(0xFF3B82F6), Color(0xFF60A5FA))
    )
    val BackupAction = Brush.linearGradient(
        colors = listOf(Color(0xFFBE123C), Color(0xFFF43F5E), Color(0xFFFB7185))
    )
    val SettingsAction = Brush.linearGradient(
        colors = listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF64748B))
    )
}
