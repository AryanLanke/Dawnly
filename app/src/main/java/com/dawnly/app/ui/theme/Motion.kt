package com.dawnly.app.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** True when the user turned animations off ("Remove animations", or animator scale 0). */
val LocalReduceMotion = staticCompositionLocalOf { false }

fun Context.prefersReducedMotion(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** The app's springs: soft and slightly bouncy. Everything snaps when motion is reduced. */
object DawnlyMotion {
    @Composable
    @ReadOnlyComposable
    fun <T> bouncy(): FiniteAnimationSpec<T> =
        if (LocalReduceMotion.current) snap()
        else spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)

    @Composable
    @ReadOnlyComposable
    fun <T> gentle(): FiniteAnimationSpec<T> =
        if (LocalReduceMotion.current) snap()
        else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow)
}
