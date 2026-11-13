package com.dawnly.app.ui.time

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dawnly.app.R
import com.dawnly.app.data.time.TimeStore
import com.dawnly.app.domain.time.StopwatchState
import com.dawnly.app.domain.time.TimerPhase
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassShapes
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.glass.ROW_INSET
import com.dawnly.app.ui.glass.TextAction
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.PermissionState
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.notify.TimerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Both tools keep running independently of this screen, the pager, or the activity. */
@Composable
fun TimeToolsScreen(
    permissions: PermissionState,
    onRequestPermission: (AppPermission) -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current
    val focus = LocalFocusManager.current
    val state by remember(context) { TimeStore.states(context) }.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(TimeStore.now(context)) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    val timer = state.timer
    val stopwatch = state.stopwatch

    fun result(value: TimerController.Result) {
        error = when (value) {
            TimerController.Result.Success -> null
            TimerController.Result.InvalidDuration -> R.string.time_invalid_duration
            TimerController.Result.ExactAlarmRequired -> R.string.time_exact_body
            TimerController.Result.StorageError -> R.string.time_storage_error
            TimerController.Result.SchedulingFailed -> R.string.time_schedule_error
        }
        if (value == TimerController.Result.ExactAlarmRequired) onRequestPermission(AppPermission.ExactAlarms)
    }
    fun saved(success: Boolean) { error = if (success) null else R.string.time_storage_error }

    LifecycleResumeEffect(context) {
        now = TimeStore.now(context)
        // Don't automatically open a permission screen just because the app was resumed.
        val synced = TimerController.sync(context)
        if (synced == TimerController.Result.StorageError) error = R.string.time_storage_error
        if (synced == TimerController.Result.SchedulingFailed) error = R.string.time_schedule_error
        onPauseOrDispose { TimeStore.checkpoint(context) }
    }
    LaunchedEffect(context, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            now = TimeStore.now(context)
            var checkpointAt = now.elapsedMs
            while (isActive) {
                now = now.copy(elapsedMs = SystemClock.elapsedRealtime())
                // A bounded reboot recovery checkpoint, not a ticking persisted counter.
                if (now.elapsedMs - checkpointAt >= 5_000L) {
                    saved(TimeStore.checkpoint(context))
                    checkpointAt = now.elapsedMs
                }
                delay(50L)
            }
        }
    }
    val remaining = timer.remaining(now)
    LaunchedEffect(timer.generation, timer.phase, timer.completionPending, remaining == 0L) {
        if (timer.completionPending || (timer.phase == TimerPhase.Running && remaining == 0L)) {
            TimerController.sync(context)
        }
    }

    // Draft input survives activity recreation; the chosen duration is also persisted on Start.
    var hours by rememberSaveable { mutableStateOf((timer.durationMs / 3_600_000L).toString()) }
    var minutes by rememberSaveable { mutableStateOf((timer.durationMs / 60_000L % 60).toString()) }
    var seconds by rememberSaveable { mutableStateOf((timer.durationMs / 1000L % 60).toString()) }
    fun restoreDuration() {
        val duration = TimeStore.states(context).value.timer.durationMs
        hours = (duration / 3_600_000L).toString()
        minutes = (duration / 60_000L % 60).toString()
        seconds = (duration / 1000L % 60).toString()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.tab_tools), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.time_tools_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = DawnlyTheme.colors.textSoft,
            textAlign = TextAlign.Center,
        )
        if (AppPermission.ExactAlarms !in permissions) {
            PermissionCard(R.string.time_exact_title, R.string.time_exact_body) { onRequestPermission(AppPermission.ExactAlarms) }
        }
        if (AppPermission.Notifications !in permissions) {
            PermissionCard(R.string.time_notifications_title, R.string.time_notifications_body) { onRequestPermission(AppPermission.Notifications) }
        }
        error?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = DawnlyTheme.colors.textSoft) }

        GlassList {
            GlassRow(
                title = stringResource(R.string.time_timer),
                subtitle = stringResource(when (timer.phase) {
                    TimerPhase.Ready -> R.string.time_timer_ready
                    TimerPhase.Running -> R.string.time_running
                    TimerPhase.Paused -> if (timer.pausedAfterReboot) R.string.time_reboot_paused else R.string.time_paused
                    TimerPhase.Finished -> R.string.time_timer_finished
                }),
            )
            Column(
                Modifier.padding(horizontal = ROW_INSET).padding(bottom = ROW_INSET),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (timer.phase == TimerPhase.Ready) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DurationField(hours, { hours = it }, R.string.time_hours, 99, Modifier.weight(1f))
                        DurationField(minutes, { minutes = it }, R.string.time_minutes, 59, Modifier.weight(1f))
                        DurationField(seconds, { seconds = it }, R.string.time_seconds, 59, Modifier.weight(1f))
                    }
                } else {
                    TimeDisplay(timerText(remaining))
                }
                when (timer.phase) {
                    TimerPhase.Ready -> PrimaryButton(stringResource(R.string.time_start_timer), onClick = {
                        focus.clearFocus()
                        val duration = ((hours.toLongOrNull() ?: 0L) * 3600 + (minutes.toLongOrNull() ?: 0L) * 60 + (seconds.toLongOrNull() ?: 0L)) * 1000
                        result(TimerController.start(context, duration))
                        now = TimeStore.now(context)
                    })
                    TimerPhase.Running -> PrimaryButton(stringResource(R.string.time_pause), onClick = {
                        result(TimerController.pause(context)); now = TimeStore.now(context)
                    })
                    TimerPhase.Paused -> PrimaryButton(stringResource(R.string.time_resume), onClick = {
                        result(TimerController.resume(context)); now = TimeStore.now(context)
                    })
                    TimerPhase.Finished -> PrimaryButton(stringResource(R.string.time_reset), onClick = {
                        result(TimerController.reset(context)); restoreDuration()
                    })
                }
                if (timer.phase == TimerPhase.Running || timer.phase == TimerPhase.Paused) {
                    TextAction(stringResource(R.string.time_reset), onClick = { result(TimerController.reset(context)); restoreDuration() })
                }
            }
        }

        GlassList {
            GlassRow(
                title = stringResource(R.string.time_stopwatch),
                subtitle = stringResource(when {
                    stopwatch.running -> R.string.time_running
                    stopwatch.pausedAfterReboot -> R.string.time_reboot_paused
                    stopwatch.elapsedMs > 0 -> R.string.time_paused
                    else -> R.string.time_stopwatch_ready
                }),
            )
            Column(
                Modifier.padding(horizontal = ROW_INSET).padding(bottom = ROW_INSET),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TimeDisplay(stopwatchText(stopwatch.elapsed(now)))
                PrimaryButton(
                    stringResource(when {
                        stopwatch.running -> R.string.time_pause
                        stopwatch.elapsedMs > 0 -> R.string.time_resume
                        else -> R.string.time_start_stopwatch
                    }),
                    onClick = {
                        focus.clearFocus()
                        saved(if (stopwatch.running) TimeStore.pauseStopwatch(context) else TimeStore.startStopwatch(context))
                        now = TimeStore.now(context)
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (stopwatch.running && stopwatch.laps.size < StopwatchState.MAX_LAPS) {
                        AccentPill(stringResource(R.string.time_lap), onClick = { saved(TimeStore.lap(context)) })
                    }
                    if (stopwatch.running || stopwatch.elapsedMs > 0 || stopwatch.laps.isNotEmpty()) {
                        TextAction(stringResource(R.string.time_reset), onClick = { saved(TimeStore.resetStopwatch(context)) })
                    }
                }
                if (stopwatch.laps.size == StopwatchState.MAX_LAPS) {
                    Text(stringResource(R.string.time_lap_limit, StopwatchState.MAX_LAPS), style = MaterialTheme.typography.bodySmall)
                }
            }
            for (index in stopwatch.laps.indices.reversed()) {
                val total = stopwatch.laps[index]
                val split = total - (stopwatch.laps.getOrNull(index - 1) ?: 0L)
                GlassRow(
                    title = stringResource(R.string.time_lap_number, index + 1),
                    subtitle = stringResource(R.string.time_lap_split, stopwatchText(split)),
                    divider = true,
                    end = { Text(stopwatchText(total), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")) },
                )
            }
        }
        Text(stringResource(R.string.time_reboot_note), style = MaterialTheme.typography.bodySmall, color = DawnlyTheme.colors.textSoft)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionCard(title: Int, body: Int, onAllow: () -> Unit) {
    GlassList {
        GlassRow(stringResource(title), subtitle = stringResource(body))
        AccentPill(stringResource(R.string.time_allow), onAllow, Modifier.padding(start = ROW_INSET, bottom = ROW_INSET))
    }
}

@Composable
private fun DurationField(value: String, onChange: (String) -> Unit, label: Int, maximum: Int, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.length <= 2 && input.all { it in '0'..'9' } && (input.toIntOrNull() ?: 0) <= maximum) onChange(input)
        },
        label = { Text(stringResource(label), style = MaterialTheme.typography.bodySmall) },
        modifier = modifier,
        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center, fontFeatureSettings = "tnum"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = GlassShapes.SmallButton,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DawnlyTheme.colors.accent,
            unfocusedBorderColor = DawnlyTheme.colors.line,
            focusedTextColor = DawnlyTheme.colors.text,
            unfocusedTextColor = DawnlyTheme.colors.text,
            focusedLabelColor = DawnlyTheme.colors.textSoft,
            unfocusedLabelColor = DawnlyTheme.colors.textSoft,
        ),
    )
}

@Composable
private fun TimeDisplay(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
        textAlign = TextAlign.Center,
    )
}
