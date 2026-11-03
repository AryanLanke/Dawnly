package com.dawnly.app.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
 * The glass design system from design/mockups.html. Material 3 is only the base: screens read
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
    // Switching between light and dark fades over most of a second instead of popping.
    val night = animateFloatAsState(
        targetValue = if (darkTheme) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(THEME_FADE_MS),
        label = "night",
    )
    val colors = when (val amount = night.value) {
        0f -> LightColors
        1f -> DarkColors
        else -> blend(LightColors, DarkColors, amount)
    }
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

private const val THEME_FADE_MS = 700

/**
 * How far into night the app is, 0 (light) to 1 (dark): in between while the theme fades. A
 * [State], so the sky reads it only while drawing and isn't rebuilt every frame of the fade.
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
