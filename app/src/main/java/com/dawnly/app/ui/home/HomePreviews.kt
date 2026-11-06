package com.dawnly.app.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dawnly.app.data.alarm.AppRef
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.domain.wake.WakeSignals
import com.dawnly.app.domain.watch.WatchState
import com.dawnly.app.ui.glass.PreviewFrame
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.PermissionState
import java.time.Duration
import java.time.Instant

// Previews use fake WakeSignals; the real ones come from WatchService during a window.

private val PreviewNow: Instant = Instant.parse("2026-11-02T04:58:00Z")
private val PreviewAlarm: Instant = PreviewNow.plus(Duration.ofMinutes(32))
private val PreviewSettings = WakeSignalSettings(useSteps = true, useMusic = false)
private val PreviewPadding = PaddingValues(top = 40.dp, bottom = 100.dp)

private fun watching(live: WakeSignals) = HomeUiState.from(
    WatchState.Watching(PreviewAlarm, PreviewAlarm.minus(Duration.ofHours(1))),
    live,
    PreviewSettings,
    PreviewNow,
)

@Composable
private fun HomePreview(state: HomeUiState, dark: Boolean, permissions: PermissionState = PermissionState.AllGranted) {
    PreviewFrame(dark) {
        HomeContent(
            state = state,
            permissions = permissions,
            dismissResult = null,
            reminders = emptyList(),
            onEditReminder = {},
            onToggleReminder = { _, _ -> },
            onDismissAlarm = {},
            onClearDismissResult = {},
            onRequestPermission = {},
            contentPadding = PreviewPadding,
            now = PreviewNow,
        )
    }
}

@Preview(name = "Watching, light", showSystemUi = true)
@Composable
private fun WatchingLightPreview() =
    HomePreview(watching(WakeSignals(screenOnTime = Duration.ofMinutes(2), steps = 12, movingTime = Duration.ofSeconds(20))), dark = false)

@Preview(name = "Watching, dark", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WatchingDarkPreview() =
    HomePreview(watching(WakeSignals(screenOnTime = Duration.ofMinutes(2), steps = 12, movingTime = Duration.ofSeconds(20))), dark = true)

@Preview(name = "Awake", showSystemUi = true)
@Composable
private fun AwakePreview() =
    HomePreview(watching(WakeSignals(screenOnTime = Duration.ofMinutes(6), steps = 64, movingTime = Duration.ofMinutes(2))), dark = false)

@Preview(name = "Waiting, exact alarms off", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WaitingPreview() = HomePreview(
    HomeUiState.from(
        WatchState.Waiting(PreviewAlarm.plus(Duration.ofHours(20)), PreviewAlarm.plus(Duration.ofHours(19))),
        null,
        PreviewSettings,
        PreviewNow,
    ),
    dark = true,
    permissions = PermissionState(setOf(AppPermission.Notifications)),
)

@Preview(name = "No alarm", showSystemUi = true)
@Composable
private fun NoAlarmPreview() = HomePreview(HomeUiState(HomeStatus.NoAlarm), dark = false)

@Preview(name = "Opened Clock", showSystemUi = true)
@Composable
private fun OpenedClockPreview() {
    PreviewFrame(dark = false) {
        HomeContent(
            state = watching(WakeSignals(screenOnTime = Duration.ofMinutes(6), steps = 64, movingTime = Duration.ofMinutes(2))),
            permissions = PermissionState.AllGranted,
            dismissResult = DismissResult.OpenedClock(AppRef("com.example.clock", "Clock")),
            reminders = emptyList(),
            onEditReminder = {},
            onToggleReminder = { _, _ -> },
            onDismissAlarm = {},
            onClearDismissResult = {},
            onRequestPermission = {},
            contentPadding = PreviewPadding,
            now = PreviewNow,
        )
    }
}
