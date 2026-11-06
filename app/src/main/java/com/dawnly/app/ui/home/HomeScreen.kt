package com.dawnly.app.ui.home

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dawnly.app.R
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.SpringIn
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.PermissionState
import com.dawnly.app.ui.theme.NightClock
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    permissions: PermissionState,
    onRequestPermission: (AppPermission) -> Unit,
    onEditReminder: (Reminder?) -> Unit,
    onSetAlarm: () -> Unit,
    onEditAlarm: (Instant) -> Unit,
    contentPadding: PaddingValues,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dismissResult by viewModel.dismissResult.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    HomeContent(
        state = state,
        permissions = permissions,
        dismissResult = dismissResult,
        reminders = reminders,
        onEditReminder = onEditReminder,
        onToggleReminder = { reminder, on -> viewModel.saveReminder(reminder.copy(on = on)) },
        onDismissAlarm = { activity?.let(viewModel::dismissAlarm) },
        onClearDismissResult = viewModel::clearDismissResult,
        onRequestPermission = onRequestPermission,
        contentPadding = contentPadding,
        onSetAlarm = onSetAlarm,
        onEditAlarm = onEditAlarm,
        isDawnlyAlarm = viewModel::isDawnlyAlarm,
        onOpenClock = { activity?.let(viewModel::openClock) },
    )
}

@Composable
internal fun HomeContent(
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
    onSetAlarm: () -> Unit = {},
    onEditAlarm: (Instant) -> Unit = {},
    isDawnlyAlarm: (Instant) -> Boolean = { false },
    onOpenClock: () -> Unit = {},
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
            val alarmAt = status.ringsAt
            // Read again when the alarm changes, e.g. after one set from Dawnly shows up.
            val changeable = remember(alarmAt) { alarmAt != null && isDawnlyAlarm(alarmAt) }
            AlarmRing(
                status,
                now,
                Modifier.fillMaxWidth(0.72f).widthIn(max = 320.dp),
                changeable = changeable,
                onTap = {
                    when {
                        alarmAt == null -> onSetAlarm()
                        changeable -> onEditAlarm(alarmAt)
                        else -> onOpenClock()
                    }
                },
            )
        }
        // With an alarm already set, another one is still a tap away.
        if (status != HomeStatus.NoAlarm) {
            SpringIn(order = 0) { AccentPill(stringResource(R.string.home_add_alarm), onSetAlarm) }
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
            dismissResult?.let { NoteCard(describeDismiss(it), onClick = onClearDismissResult) }
        }
        if (status is HomeStatus.Watching) {
            SpringIn(order++) { DismissAction(status.alarmAt, status.awake && !status.notYet, onDismissAlarm) }
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
    is HomeStatus.Watching -> when {
        status.notYet -> R.string.home_hello_not_yet
        status.awake -> R.string.home_hello_awake
        else -> R.string.home_hello_watching
    }
    is HomeStatus.Skipped -> R.string.home_hello_skipped
}
