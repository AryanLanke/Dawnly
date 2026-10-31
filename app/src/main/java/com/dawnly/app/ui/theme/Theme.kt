package com.dawnly.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Minimal palette from design/mockups.html. The full glass design system lands in Phase 5.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1A4FD6),
    onPrimary = Color.White,
    background = Color(0xFFF7F9FE),
    onBackground = Color(0xFF0E1E45),
    surface = Color(0xFFF7F9FE),
    onSurface = Color(0xFF0E1E45),
)

private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color(0xFF1A4FD6),
    background = Color(0xFF050A1A),
    onBackground = Color.White,
    surface = Color(0xFF050A1A),
    onSurface = Color.White,
)

@Composable
fun DawnlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
