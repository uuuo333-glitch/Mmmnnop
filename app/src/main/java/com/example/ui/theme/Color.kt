package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core WhatsApp-inspired luxury store surfaces
val ObsidianBg = Color(0xFF0B141A)
val ObsidianSurface = Color(0xFF111B21)
val ObsidianSurfaceVariant = Color(0xFF1F2C34)
val ObsidianIncomingBubble = Color(0xFF202C33)
val ObsidianOutgoingBubble = Color(0xFF005C4B)

val MidnightBg = Color(0xFF050811)
val MidnightSurface = Color(0xFF0D1426)
val MidnightSurfaceVariant = Color(0xFF17223B)

val LightBg = Color(0xFFEFEAE2)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F2F5)
val LightOutgoingBubble = Color(0xFFD9FDD3)

// Signature accents
val EmeraldAccent = Color(0xFF00C896)
val VerifiedBlue = Color(0xFF1D9BF0)
val TurboRed = Color(0xFFFF3B5C)
val GoldAmber = Color(0xFFF59E0B)
val RoyalViolet = Color(0xFF8B5CF6)

val AccentColorOptions: List<Pair<String, String>> = listOf(
    "#00C896" to "أخضر زمردي",
    "#1D9BF0" to "أزرق موثق",
    "#FF3B5C" to "أحمر تيربو",
    "#F59E0B" to "ذهبي رياضي",
    "#8B5CF6" to "بنفسجي ملكي"
)

fun parseAccentColor(hex: String): Color = when (hex.uppercase()) {
    "#1D9BF0" -> VerifiedBlue
    "#FF3B5C" -> TurboRed
    "#F59E0B" -> GoldAmber
    "#8B5CF6" -> RoyalViolet
    else -> EmeraldAccent
}
