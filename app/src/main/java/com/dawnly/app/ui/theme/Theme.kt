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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.dawnly.app.ui.glass.NoPress

/**
 * The glass design system from the original design mockups. Material 3 is only the base: screens read
 * [DawnlyTheme.colors] and draw their own glass components from `ui/glass`.
 */
@Composable
fun DawnlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    // Read when the theme enters composition; the setting rarely changes while the app is open.
    val reduceMotion = remember(context) { !inPreview && context.prefersReducedMotion() }
    // Switches at once; the sweep across the screen is drawn on top by `ThemeSweep`.
    val night = remember { mutableFloatStateOf(if (darkTheme) 1f else 0f) }
    night.floatValue = if (darkTheme) 1f else 0f
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors.toMaterial(),
        typography = DawnlyTypography,
    ) {
        CompositionLocalProvider(
            LocalDawnlyColors provides colors,
            LocalReduceMotion provides reduceMotion,
            LocalNight provides night,
            LocalContentColor provides colors.text,
            // No highlight on press: what moves on a tap is the feedback.
            LocalIndication provides NoPress,
            content = content,
        )
    }
}

/**
 * How far into night the app is: 0 (light) or 1 (dark). A [State], so the sky reads it only while
 * drawing, and switching doesn't rebuild its clouds, stars and moon.
 */
val LocalNight = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(0f) }

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
