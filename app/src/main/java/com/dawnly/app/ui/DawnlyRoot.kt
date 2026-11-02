package com.dawnly.app.ui

import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import com.dawnly.app.ui.glass.GlassShapes
import com.dawnly.app.ui.glass.rememberHaptics
import kotlin.random.Random
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
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
    val pager = rememberPagerState(initialPage = TODAY) { 2 }
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptics()
    val reduceMotion = LocalReduceMotion.current
    fun goTo(page: Int) {
        scope.launch {
            if (reduceMotion) pager.scrollToPage(page)
            else pager.animateScrollToPage(page, animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium))
        }
    }
    BackHandler(enabled = pager.targetPage != TODAY) { goTo(TODAY) }
    // A tick each time a page settles, after a swipe or a tap on a tab.
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect { haptic.tick() }
    }
    // Room for the funny line and the floating tab bar, so the last card can scroll clear of them.
    val padding = screenPadding(bottomExtra = 128.dp)

    Box(Modifier.fillMaxSize()) {
        // Today and Settings side by side: the pages follow the finger, and the other one stays
        // composed so a swipe starts without a hitch.
        HorizontalPager(state = pager, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
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
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FunnyLine(Modifier.padding(bottom = 8.dp))
            GlassTabBar(
                tabs = listOf(stringResource(R.string.tab_today), stringResource(R.string.tab_settings)),
                selected = pager.targetPage,
                position = { pager.currentPage + pager.currentPageOffsetFraction },
                onSelect = ::goTo,
            )
        }
    }
}

/** A light line above the tabs, a different one each time Dawnly opens. Tap it for another. */
@Composable
private fun FunnyLine(modifier: Modifier = Modifier) {
    val lines = stringArrayResource(R.array.funny_lines)
    var index by rememberSaveable { mutableIntStateOf(Random.nextInt(lines.size)) }
    val haptic = rememberHaptics()
    AnimatedContent(
        targetState = index,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "funnyLine",
        modifier = modifier,
    ) { shown ->
        Text(
            lines[shown % lines.size],
            style = MaterialTheme.typography.bodySmall,
            color = DawnlyTheme.colors.textSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(GlassShapes.Pill)
                .clickable(onClickLabel = stringResource(R.string.funny_line_next)) {
                    haptic.tick()
                    // Any other line, never the same one twice in a row.
                    index = (index + 1 + Random.nextInt(lines.size - 1)) % lines.size
                }
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

private const val TODAY = 0

/** Fades scrolled content out under the status bar, so its icons stay readable. */
@Composable
private fun StatusBarFade(modifier: Modifier = Modifier) {
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
