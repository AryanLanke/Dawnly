package com.dawnly.app.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.dawnly.app.ui.glass.GlassPress

/**
 * The glass design system from design/mockups.html. Material 3 is only the base: screens read
 * [DawnlyTheme.colors] and draw their own glass components from `ui/glass`.
 */
@Composable
fun DawnlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    // Read when the theme enters composition; the setting rarely changes while the app is open.
    val reduceMotion = remember(context) { !inPreview && context.prefersReducedMotion() }
    MaterialTheme(
        colorScheme = colors.toMaterial(),
        typography = DawnlyTypography,
    ) {
        CompositionLocalProvider(
            LocalDawnlyColors provides colors,
            LocalReduceMotion provides reduceMotion,
            LocalContentColor provides colors.text,
            // Pressing fades a light tint over the whole row or button, not a ripple.
            LocalIndication provides remember(colors.isDark) {
                GlassPress(Color.White.copy(alpha = if (colors.isDark) 0.12f else 0.45f))
            },
            content = content,
        )
    }
}

object DawnlyTheme {
    val colors: DawnlyColors
        @Composable @ReadOnlyComposable get() = LocalDawnlyColors.current
}

/** For the few Material pieces still in use, like ripples and text selection. */
private fun DawnlyColors.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        background = wall,
        onBackground = text,
        surface = wall,
        onSurface = text,
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = onAccent,
        background = wall,
        onBackground = text,
        surface = wall,
        onSurface = text,
    )
}
