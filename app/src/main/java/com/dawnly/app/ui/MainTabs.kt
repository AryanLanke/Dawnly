package com.dawnly.app.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dawnly.app.R
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.domain.watch.WatchWindow
import com.dawnly.app.ui.glass.GlassTabBar
import com.dawnly.app.ui.glass.rememberHaptics
import com.dawnly.app.ui.home.AlarmWheel
import com.dawnly.app.ui.home.HomeScreen
import com.dawnly.app.ui.home.HomeViewModel
import com.dawnly.app.ui.home.ReminderEditor
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionsController
import com.dawnly.app.ui.settings.SettingsScreen
import com.dawnly.app.ui.settings.SettingsViewModel
import com.dawnly.app.ui.settings.WindowWheel
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.LocalReduceMotion
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Composable
internal fun MainTabs(
    home: HomeViewModel,
    settings: SettingsViewModel,
    permissions: PermissionsController,
    oemBattery: OemBattery?,
) {
    val pager = rememberPagerState(initialPage = TODAY) { 2 }
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptics()
    val reduceMotion = LocalReduceMotion.current
    val activity = LocalActivity.current
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
    val padding = screenPadding(bottomExtra = NAV_FADE_SOLID + NAV_FADE_EDGE)
    // What's drawn over the tabs: the reminder editor (Today's Reminders card) or the custom
    // watching window's wheels (Settings); null when nothing is open.
    var editing by remember { mutableStateOf<Overlay?>(null) }

    // Frosted behind the reminder editor (Android 12+), like glass over the screen. Without the
    // frost, a stronger veil keeps the card readable.
    val canFrost = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // The editor eases in and out: Today frosts over gradually as the card rises into place.
    val editorShown = animateFloatAsState(
        targetValue = if (editing != null) 1f else 0f,
        animationSpec = tween(EDITOR_MS, easing = FastOutSlowInEasing),
        label = "editor",
    )
    // Kept while the card eases out, after editing is already null.
    var lastEdit by remember { mutableStateOf<Overlay?>(null) }
    editing?.let { lastEdit = it }
    val frosted = if (canFrost) {
        Modifier.graphicsLayer {
            val radius = 18.dp.toPx() * editorShown.value
            renderEffect = if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Clamp) else null
        }
    } else {
        Modifier
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().then(frosted)) {
            // Today and Settings side by side: the pages follow the finger, and the other one stays
            // composed so a swipe starts without a hitch.
            HorizontalPager(state = pager, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    TODAY -> HomeScreen(
                        viewModel = home,
                        permissions = permissions.state,
                        onRequestPermission = { permissions.request(it) },
                        onEditReminder = { editing = ReminderEdit(it) },
                        onSetAlarm = { editing = AlarmPick() },
                        contentPadding = padding,
                    )
                    else -> SettingsScreen(
                        viewModel = settings,
                        permissions = permissions.state,
                        oemBattery = oemBattery,
                        onRequestPermission = permissions::request,
                        onOpenOemBattery = { oemBattery?.let(permissions::openBatterySettings) },
                        contentPadding = padding,
                        onPickWindow = { editing = WindowPick() },
                    )
                }
            }
            StatusBarFade(Modifier.align(Alignment.TopCenter))
            NavBarFade(Modifier.align(Alignment.BottomCenter))
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Only on Today: it fades out as Settings slides in, following the finger.
                FunnyLine(
                    shown = { 1f - (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, 1f) },
                    tappable = pager.targetPage == TODAY,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                GlassTabBar(
                    tabs = listOf(stringResource(R.string.tab_today), stringResource(R.string.tab_settings)),
                    selected = pager.targetPage,
                    position = { pager.currentPage + pager.currentPageOffsetFraction },
                    onSelect = ::goTo,
                )
            }
        }
        AnimatedVisibility(
            visible = editing != null,
            enter = fadeIn(tween(EDITOR_MS)),
            exit = fadeOut(tween(EDITOR_MS)),
        ) {
            Box(Modifier.fillMaxSize().background(DawnlyTheme.colors.wall.copy(alpha = if (canFrost) 0.25f else 0.7f)))
            lastEdit?.let { edit ->
                // A fresh editor for each reminder opened.
                key(edit) {
                    Box(
                        Modifier.animateEnterExit(
                            enter = scaleIn(tween(EDITOR_MS, easing = FastOutSlowInEasing), initialScale = 0.92f) +
                                slideInVertically(tween(EDITOR_MS, easing = FastOutSlowInEasing)) { it / 24 },
                            exit = scaleOut(tween(EDITOR_MS), targetScale = 0.96f),
                        ),
                    ) {
                        when (edit) {
                            is ReminderEdit -> ReminderEditor(
                                initial = edit.reminder,
                                onSave = { home.saveReminder(it); editing = null },
                                onDelete = { home.deleteReminder(it); editing = null },
                                onClose = { editing = null },
                            )
                            is AlarmPick -> AlarmWheel(
                                onDone = { time, day ->
                                    activity?.let { home.setAlarm(it, time, day) }
                                    editing = null
                                },
                                onClose = { editing = null },
                            )
                            is WindowPick -> {
                                val window = settings.uiState.collectAsStateWithLifecycle().value?.window ?: WatchWindow.DEFAULT
                                WindowWheel(
                                    window = window,
                                    onDone = { settings.setWindow(it); editing = null },
                                    onClose = { editing = null },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** How long the reminder editor takes to ease in or out. */
private const val EDITOR_MS = 340

/** What's drawn over the tabs, frosting them. A fresh one each time, so each opening starts anew. */
private sealed interface Overlay

/** The reminder editor, for an existing reminder, or null for a new one. */
private class ReminderEdit(val reminder: Reminder?) : Overlay

/** The custom watching window's wheels. */
private class WindowPick : Overlay

/** Setting a new alarm in the Clock app, on the time wheels. */
private class AlarmPick : Overlay
private const val TODAY = 0
