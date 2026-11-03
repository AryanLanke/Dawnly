package com.dawnly.app.ui.home

import android.content.res.Configuration
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dawnly.app.R
import com.dawnly.app.data.alarm.AppRef
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.domain.wake.WakeSignals
import com.dawnly.app.domain.watch.WatchState
import com.dawnly.app.ui.glass.PreviewFrame
import com.dawnly.app.ui.glass.SpringIn
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.PermissionState
import com.dawnly.app.ui.theme.NightClock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    permissions: PermissionState,
    onRequestPermission: (AppPermission) -> Unit,
    contentPadding: PaddingValues,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dismissResult by viewModel.dismissResult.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    // The reminder being changed, or Adding for a new one; null when the editor is closed.
    var editing by remember { mutableStateOf<ReminderEdit?>(null) }
    val activity = LocalActivity.current
    HomeContent(
        state = state,
        permissions = permissions,
        dismissResult = dismissResult,
        reminders = reminders,
        onEditReminder = { editing = ReminderEdit(it) },
        onToggleReminder = { reminder, on -> viewModel.saveReminder(reminder.copy(on = on)) },
        onDismissAlarm = { activity?.let(viewModel::dismissAlarm) },
        onClearDismissResult = viewModel::clearDismissResult,
        onRequestPermission = onRequestPermission,
        contentPadding = contentPadding,
    )
    editing?.let { edit ->
        ReminderEditor(
            initial = edit.reminder,
            onSave = { viewModel.saveReminder(it); editing = null },
            onDelete = { viewModel.deleteReminder(it); editing = null },
            onClose = { editing = null },
        )
    }
}

/** The editor's subject: an existing reminder, or null for a new one. */
private class ReminderEdit(val reminder: Reminder?)

@Composable
private fun HomeContent(
    state: HomeUiState,
    permissions: PermissionState,
    dismissResult: DismissResult?,
    reminders: List<Reminder>,
    onEditReminder: (Reminder?) -> Unit,
    onToggleReminder: (Reminder, Boolean) -> Unit,
    onDismissAlarm: () -> Unit,
    onClearDismissResult: () -> Unit,
    onRequestPermission: (AppPermission) -> Unit,
    contentPadding: PaddingValues,
    now: Instant = Instant.now(),
) {
    val status = state.status
    if (status == HomeStatus.Loading) return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        SpringIn(order = 0) {
            AlarmRing(status, now, Modifier.fillMaxWidth(0.72f).widthIn(max = 320.dp))
        }
        SpringIn(order = 0) {
            Text(
                stringResource(hello(status, now)),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))

        var order = 1
        if (status != HomeStatus.NoAlarm) {
            if (AppPermission.ExactAlarms !in permissions) {
                SpringIn(order++) {
                    PermissionWarning(
                        title = stringResource(R.string.home_exact_off_title),
                        body = stringResource(R.string.home_exact_off_body),
                        onAllow = { onRequestPermission(AppPermission.ExactAlarms) },
                    )
                }
            } else if (AppPermission.Notifications !in permissions) {
                SpringIn(order++) {
                    PermissionWarning(
                        title = stringResource(R.string.home_notifications_off_title),
                        body = stringResource(R.string.home_notifications_off_body),
                        onAllow = { onRequestPermission(AppPermission.Notifications) },
                    )
                }
            }
        }
        if (status is HomeStatus.Skipped) {
            SpringIn(order++) { NoteCard(stringResource(R.string.home_skipped_detail)) }
        }
        if (state.signals.isNotEmpty()) {
            SpringIn(order++) { SignalCard(state.signals, (status as? HomeStatus.Watching)?.wakeProgress) }
        }
        AnimatedVisibility(visible = dismissResult != null, enter = fadeIn(), exit = fadeOut()) {
            dismissResult?.let { NoteCard(describe(it), onClick = onClearDismissResult) }
        }
        if (status is HomeStatus.Watching) {
            SpringIn(order++) { DismissAction(status.alarmAt, status.awake, onDismissAlarm) }
        }
        SpringIn(order) { RemindersCard(reminders, onEditReminder, onToggleReminder) }
        Spacer(Modifier.height(8.dp))
    }
}

/** The friendly line under the ring. It follows what's happening, and says good night at night. */
private fun hello(status: HomeStatus, now: Instant): Int = when (status) {
    HomeStatus.Loading, HomeStatus.NoAlarm -> R.string.home_hello_no_alarm
    is HomeStatus.Waiting ->
        if (NightClock.isNight(now.atZone(ZoneId.systemDefault()).toLocalTime())) R.string.home_hello_night
        else R.string.home_hello_day
    is HomeStatus.Watching -> if (status.awake) R.string.home_hello_awake else R.string.home_hello_watching
    is HomeStatus.Skipped -> R.string.home_hello_skipped
}

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
