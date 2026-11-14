package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MatjarTheme(
    themeMode: String = "dark",
    accentColorHex: String = "#00C896",
    content: @Composable () -> Unit
) {
    val accent = parseAccentColor(accentColorHex)

    val colorScheme = when (themeMode) {
        "light" -> lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.16f),
            onPrimaryContainer = Color(0xFF073B2D),
            secondary = VerifiedBlue,
            onSecondary = Color.White,
            background = LightBg,
            onBackground = Color(0xFF111B21),
            surface = LightSurface,
            onSurface = Color(0xFF111B21),
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF54656F),
            outline = Color(0xFFD1D7DB)
        )
        "midnight" -> darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF041410),
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = Color.White,
            secondary = VerifiedBlue,
            onSecondary = Color.White,
            background = MidnightBg,
            onBackground = Color(0xFFE9EDEF),
            surface = MidnightSurface,
            onSurface = Color(0xFFE9EDEF),
            surfaceVariant = MidnightSurfaceVariant,
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF23324D)
        )
        else -> darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF041C15),
            primaryContainer = accent.copy(alpha = 0.20f),
            onPrimaryContainer = Color.White,
            secondary = VerifiedBlue,
            onSecondary = Color.White,
            background = ObsidianBg,
            onBackground = Color(0xFFE9EDEF),
            surface = ObsidianSurface,
            onSurface = Color(0xFFE9EDEF),
            surfaceVariant = ObsidianSurfaceVariant,
            onSurfaceVariant = Color(0xFF8696A0),
            outline = Color(0xFF2A3942)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
