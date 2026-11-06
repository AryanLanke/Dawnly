package com.dawnly.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.settings.Appearance
import com.dawnly.app.data.settings.DismissMode
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.domain.reminder.ReminderSchedule
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.ui.glass.FlowingBackground
import com.dawnly.app.ui.glass.GlassTabBar
import com.dawnly.app.ui.home.HomeContent
import com.dawnly.app.ui.home.HomeStatus
import com.dawnly.app.ui.home.HomeUiState
import com.dawnly.app.ui.home.ReminderEditor
import com.dawnly.app.ui.permissions.PermissionState
import com.dawnly.app.ui.settings.SettingsContent
import com.dawnly.app.ui.settings.SettingsUiState
import com.dawnly.app.ui.theme.DawnlyTheme
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the README animations: the real Today, Settings and reminder editor, drawn on the PC
 * frame by frame at 4x density, with time moved on [STEP_MS] at a time, so every frame is sharp and
 * every slide glides. Skipped unless given a folder for the frames:
 *
 *     ./gradlew :app:testDebugUnitTest --tests '*DemoFrames*' -Pdawnly.frames=<folder>
 *
 * Each test writes <folder>/<clip>/00000.png onwards; ffmpeg turns them into animations.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "en-rIN-w413dp-h919dp-xxxhdpi")
abstract class DemoFrames {
    @get:Rule val rule = createComposeRule()

    private val out: File? = System.getProperty("dawnly.frames")?.let(::File)
    private lateinit var dir: File
    private var count = 0

    @Before
    fun onlyWhenAsked() = Assume.assumeTrue("Pass -Pdawnly.frames=<folder> to render", out != null)

    // ---- Scenes ----

    protected fun today(name: String, dark: Boolean, order: List<Int>) {
        val reminders = mutableStateListOf(*sampleReminders())
        start(name) {
            Screen(dark, tab = TODAY) {
                Today(reminders) { r, on -> reminders[reminders.indexOfFirst { it.id == r.id }] = r.copy(on = on) }
            }
        }
        val switches = rule.onAllNodes(isToggleable()).fetchSemanticsNodes().map { it.boundsInRoot.center }
        hold(500)
        // Each switch goes off and later back on, in a mixed order.
        order.forEach { tap(switches[it]); hold(430) }
        hold(450)
    }

    protected fun settings(name: String, dark: Boolean, vararg slides: List<String>) {
        var window by mutableStateOf(Duration.ofHours(1))
        var mode by mutableStateOf(DismissMode.Ask)
        start(name) {
            Screen(dark, tab = SETTINGS) {
                SettingsContent(
                    state = SettingsUiState(
                        window, WakeSignalSettings(), if (dark) Appearance.Dark else Appearance.Light, AlertSound.Flute, mode,
                    ),
                    permissions = PermissionState.AllGranted,
                    oemBattery = null,
                    deniedSign = null,
                    onWindowChange = { window = it },
                    onPickWindow = {},
                    onSignalsChange = {},
                    onSignNeedsPermission = {},
                    onAppearanceChange = {},
                    onDismissModeChange = { mode = it },
                    onAllowOverlay = {},
                    onRequestPermission = {},
                    onOpenOemBattery = {},
                    onOpenSounds = {},
                    moreOpen = false,
                    onToggleMore = {},
                    contentPadding = Padding,
                )
            }
        }
        hold(400)
        slides.forEach { slide(it.map(::centreOf)); hold(450) }
    }

    protected fun editor(name: String, dark: Boolean, vararg slides: List<String>) {
        val reminders = mutableStateListOf(*sampleReminders())
        start(name) {
            Screen(dark, tab = TODAY) { Today(reminders) { _, _ -> } }
            // As in the app without frost: a veil over Today, the editor on top.
            Box(Modifier.fillMaxSize().background(DawnlyTheme.colors.wall.copy(alpha = 0.7f)))
            ReminderEditor(initial = null, onSave = {}, onDelete = {}, onClose = {})
        }
        hold(400)
        slides.forEach { slide(it.map(::centreOf)); hold(450) }
    }

    // ---- Frames and touches ----

    private fun start(name: String, content: @Composable () -> Unit) {
        dir = File(out, name).apply { deleteRecursively(); mkdirs() }
        count = 0
        rule.mainClock.autoAdvance = false
        rule.setContent { DawnlyTheme(darkTheme = name.endsWith("dark")) { Box(Modifier.fillMaxSize()) { content() } } }
        // Let the cards spring in before the first frame.
        rule.mainClock.advanceTimeBy(1_600, ignoreFrameDuration = true)
    }

    /** Moves time on one frame and saves it. */
    private fun frame() {
        rule.mainClock.advanceTimeBy(STEP_MS, ignoreFrameDuration = true)
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(dir, "%05d.png".format(count++))).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun hold(ms: Long) = repeat((ms / STEP_MS).toInt()) { frame() }

    private fun tap(at: Offset) {
        rule.onRoot().performTouchInput { down(at) }
        hold(96)
        rule.onRoot().performTouchInput { up() }
    }

    /** Press and hold on the first point, glide through the others (eased, a step every frame), let go. */
    private fun slide(points: List<Offset>) {
        rule.onRoot().performTouchInput { down(points.first()) }
        hold(640) // the hold that lets a slide start
        points.zipWithNext().forEach { (from, to) ->
            val steps = (MOVE_MS / STEP_MS).toInt()
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                val p = lerp(from, to, t * t * (3 - 2 * t))
                rule.onRoot().performTouchInput { moveTo(p) }
                frame()
            }
            hold(PAUSE_MS)
        }
        rule.onRoot().performTouchInput { up() }
    }

    private fun centreOf(text: String): Offset =
        rule.onAllNodesWithText(text).fetchSemanticsNodes().first().boundsInRoot.center

    // ---- What's on screen ----

    @Composable
    private fun Screen(dark: Boolean, tab: Int, content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            FlowingBackground()
            content()
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                GlassTabBar(
                    tabs = listOf(stringResource(R.string.tab_today), stringResource(R.string.tab_settings)),
                    selected = tab,
                    position = { tab.toFloat() },
                    onSelect = {},
                )
            }
        }
    }

    @Composable
    private fun Today(reminders: List<Reminder>, onToggle: (Reminder, Boolean) -> Unit) {
        val now = LocalDateTime.of(2026, 11, 6, 21, 30).atZone(ZoneId.systemDefault()).toInstant()
        HomeContent(
            state = HomeUiState(HomeStatus.NoAlarm),
            permissions = PermissionState.AllGranted,
            dismissResult = null,
            reminders = reminders,
            onEditReminder = {},
            onToggleReminder = onToggle,
            onDismissAlarm = {},
            onClearDismissResult = {},
            onRequestPermission = {},
            contentPadding = Padding,
            now = now,
        )
    }

    private fun sampleReminders() = arrayOf(
        Reminder(1, "Stretch", "Get up and move", ReminderSchedule.Every(60, LocalTime.of(8, 0), LocalTime.of(22, 0))),
        Reminder(2, "Call mother", "She loves it", ReminderSchedule.Daily(LocalTime.of(21, 0))),
        Reminder(3, "Take vitamins", "With breakfast", ReminderSchedule.Daily(LocalTime.of(8, 30))),
    )

    private companion object {
        /** 12 ms a frame (83 fps): the shortest frame Chrome, Safari and Firefox all play at full speed. */
        const val STEP_MS = 12L
        const val MOVE_MS = 372L
        const val PAUSE_MS = 264L
        const val TODAY = 0
        const val SETTINGS = 1
        val Padding = PaddingValues(top = 18.dp, bottom = NAV_FADE_SOLID + NAV_FADE_EDGE)
    }
}

/** The light clips. A class of their own so they render alongside the dark ones. */
class DemoFramesLight : DemoFrames() {
    @Test fun today() = today("today-light", dark = false, order = listOf(1, 0, 1, 2, 0, 2))

    @Test fun settings() = settings(
        "settings-light", dark = false,
        listOf("1 hr", "2 hr", "30 min", "Custom", "1 hr"),
        listOf("Ask me", "Warn first", "Right away", "Ask me"),
    )

    @Test fun editor() = editor(
        "editor-light", dark = false,
        listOf("Every", "At a time", "Every"),
        listOf("20 min", "1 hr", "15 min", "30 min", "20 min"),
    )
}

/** The dark clips, in a different order from the light ones. */
class DemoFramesDark : DemoFrames() {
    @Test fun today() = today("today-dark", dark = true, order = listOf(2, 1, 0, 2, 1, 0))

    @Test fun settings() = settings(
        "settings-dark", dark = true,
        listOf("Ask me", "Right away", "Warn first", "Ask me"),
        listOf("1 hr", "30 min", "2 hr", "1 hr"),
    )

    @Test fun editor() = editor(
        "editor-dark", dark = true,
        listOf("20 min", "30 min", "1 hr", "15 min", "20 min"),
        listOf("Every", "At a time", "Every"),
    )
}
