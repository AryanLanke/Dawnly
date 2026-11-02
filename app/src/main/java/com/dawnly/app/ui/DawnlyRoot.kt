package com.dawnly.app.ui

import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dawnly.app.R
import com.dawnly.app.data.settings.Appearance
import com.dawnly.app.ui.glass.FlowingBackground
import com.dawnly.app.ui.glass.GlassTabBar
import com.dawnly.app.ui.glass.LocalHazeState
import com.dawnly.app.ui.home.HomeScreen
import com.dawnly.app.ui.home.HomeViewModel
import com.dawnly.app.ui.onboarding.OnboardingScreen
import com.dawnly.app.ui.onboarding.onboardingSteps
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionsController
import com.dawnly.app.ui.permissions.rememberPermissionsController
import com.dawnly.app.ui.settings.SettingsScreen
import com.dawnly.app.ui.settings.SettingsViewModel
import com.dawnly.app.ui.settings.Sign
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.LocalReduceMotion
import com.dawnly.app.ui.theme.clockSaysNight
import dev.chrisbanes.haze.rememberHazeState

/** The whole app: theme, the flowing background, then onboarding or the two tabs. */
@Composable
fun DawnlyRoot(main: MainViewModel, home: HomeViewModel, settings: SettingsViewModel) {
    val appearance by main.appearance.collectAsStateWithLifecycle()
    val onboardingDone by main.onboardingDone.collectAsStateWithLifecycle()
    val dark = when (appearance) {
        Appearance.Light -> false
        Appearance.Dark -> true
        Appearance.System, null -> isSystemInDarkTheme()
        Appearance.Clock -> clockSaysNight()
    }
    SystemBarsFollow(dark)

    DawnlyTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalHazeState provides rememberHazeState()) {
            Box(Modifier.fillMaxSize()) {
                FlowingBackground()
                val done = onboardingDone ?: return@Box
                val permissions = rememberPermissionsController()
                val oemBattery = remember { OemBattery.detect(Build.MANUFACTURER) }
                AnimatedContent(
                    targetState = done,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut() },
                    label = "root",
                ) { isDone ->
                    if (isDone) {
                        MainTabs(home, settings, permissions, oemBattery)
                    } else {
                        Onboarding(main, permissions, oemBattery)
                    }
                }
            }
        }
    }
}

@Composable
private fun Onboarding(main: MainViewModel, permissions: PermissionsController, oemBattery: OemBattery?) {
    // Decided once, so granting a permission doesn't make its step vanish mid-animation.
    val stepNames = rememberSaveable {
        onboardingSteps(permissions.state, oemBattery).joinToString(",") { it.name }
    }
    val steps = remember(stepNames) {
        stepNames.split(',').filter { it.isNotEmpty() }.map(AppPermission::valueOf)
    }
    OnboardingScreen(
        steps = steps,
        permissions = permissions.state,
        oemBattery = oemBattery,
        onAllow = { permission, onDone ->
            permissions.request(permission) { granted ->
                if (granted && permission == AppPermission.ActivityRecognition) main.useSign(Sign.Steps)
                if (granted && permission == AppPermission.UsageAccess) main.useSign(Sign.PhoneUse)
                onDone()
            }
        },
        onOpenOemBattery = { oemBattery?.let(permissions::openBatterySettings) },
        onFinished = main::completeOnboarding,
        contentPadding = screenPadding(bottomExtra = 0.dp),
    )
}

@Composable
private fun MainTabs(
    home: HomeViewModel,
    settings: SettingsViewModel,
    permissions: PermissionsController,
    oemBattery: OemBattery?,
) {
    var tab by rememberSaveable { mutableIntStateOf(TODAY) }
    BackHandler(enabled = tab != TODAY) { tab = TODAY }
    val reduceMotion = LocalReduceMotion.current
    // Room for the floating tab bar, so the last card can scroll clear of it.
    val padding = screenPadding(bottomExtra = 96.dp)

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn() togetherWith fadeOut()
                } else {
                    val direction = if (targetState > initialState) 1 else -1
                    val slide = spring<IntOffset>(dampingRatio = 0.85f, stiffness = 380f)
                    (slideInHorizontally(slide) { it / 8 * direction } + fadeIn()) togetherWith
                        (slideOutHorizontally(slide) { -it / 8 * direction } + fadeOut())
                }
            },
            label = "tab",
        ) { shown ->
            when (shown) {
                TODAY -> HomeScreen(
                    viewModel = home,
                    permissions = permissions.state,
                    onRequestPermission = { permissions.request(it) },
                    contentPadding = padding,
                )
                else -> SettingsScreen(
                    viewModel = settings,
                    permissions = permissions.state,
                    oemBattery = oemBattery,
                    onRequestPermission = permissions::request,
                    onOpenOemBattery = { oemBattery?.let(permissions::openBatterySettings) },
                    contentPadding = padding,
                )
            }
        }
        StatusBarFade(Modifier.align(Alignment.TopCenter))
        GlassTabBar(
            tabs = listOf(stringResource(R.string.tab_today), stringResource(R.string.tab_settings)),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        )
    }
}

private const val TODAY = 0

/** Fades scrolled content out under the status bar, so its icons stay readable. */
@Composable
private fun StatusBarFade(modifier: Modifier = Modifier) {
    val wall = DawnlyTheme.colors.wall
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

/** Keeps clear of the status bar, cutouts and navigation bar, plus [bottomExtra]. */
@Composable
private fun screenPadding(bottomExtra: Dp): PaddingValues {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
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
private fun SystemBarsFollow(dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    DisposableEffect(activity, dark) {
        val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose {}
    }
}
