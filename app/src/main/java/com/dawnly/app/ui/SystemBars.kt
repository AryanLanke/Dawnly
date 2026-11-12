package com.dawnly.app.ui

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyTheme

/** Fades scrolled content out under the status bar, so its icons stay readable. */
@Composable
internal fun StatusBarFade(modifier: Modifier = Modifier) {
    val wall = DawnlyTheme.colors.let { it.sky.firstOrNull() ?: it.wall }
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(Brush.verticalGradient(listOf(wall, wall.copy(alpha = 0.85f)))),
    )
    Box(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(20.dp)
            .background(Brush.verticalGradient(listOf(wall.copy(alpha = 0.85f), wall.copy(alpha = 0f)))),
    )
}

/**
 * Fades scrolled cards out behind the funny line and the tab bar, so their text never runs
 * through the line's. A plain gradient: no blur, so scrolling stays smooth.
 */
@Composable
internal fun NavBarFade(modifier: Modifier = Modifier) {
    val bottom = DawnlyTheme.colors.let { it.sky.lastOrNull() ?: it.wall }
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(NAV_FADE_EDGE)
                .background(Brush.verticalGradient(listOf(bottom.copy(alpha = 0f), bottom.copy(alpha = 0.92f)))),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(bottom.copy(alpha = 0.92f))
                .navigationBarsPadding()
                .height(NAV_FADE_SOLID),
        )
    }
}

/** The solid part of [NavBarFade]: the tab bar, the funny line and the gaps around them. */
internal val NAV_FADE_SOLID = 108.dp

/** How tall the soft edge above the solid part is. */
internal val NAV_FADE_EDGE = 40.dp

/**
 * Keeps clear of the status bar, cutouts and navigation bar, plus [bottomExtra]. Not the keyboard:
 * following it would re-lay out both tabs, and redo the frost behind the reminder editor, on every
 * frame the keyboard slides.
 */
@Composable
internal fun screenPadding(bottomExtra: Dp): PaddingValues {
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = insets.calculateStartPadding(direction),
        top = insets.calculateTopPadding(),
        end = insets.calculateEndPadding(direction),
        bottom = insets.calculateBottomPadding() + bottomExtra,
    )
}

/** Status and navigation bar icons follow the app's own Light/Dark choice, not just the phone's. */
@Composable
internal fun SystemBarsFollow(dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    DisposableEffect(activity, dark) {
        val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose {}
    }
}
