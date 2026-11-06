package com.dawnly.app.ui.debug

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.dawnly.app.R
import com.dawnly.app.dawnly
import com.dawnly.app.domain.wake.WakeScore
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.domain.wake.WakeSignals
import com.dawnly.app.domain.watch.WatchState
import com.dawnly.app.service.WakeWatcher
import com.dawnly.app.service.WatchService
import com.dawnly.app.service.WatchSession
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Duration
import java.time.Instant
import java.util.Date
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** What the step counter on this phone can do. */
private enum class StepSensor { WakeUp, Sleeps, None }

/** Everything the debug screen shows. */
private data class DebugState(
    val watch: WatchState?,
    val serviceRunning: Boolean,
    val signals: WakeSignals?,
    val settings: WakeSignalSettings,
    val activityAllowed: Boolean,
    val usageAllowed: Boolean,
    val stepSensor: StepSensor,
)

/**
 * Dev-only screen with live wake-signal values, to test detection on a phone. Opened from the
 * "Dawnly debug" launcher icon in debug builds.
 * It can also change which signals count and their goals, so detection can be tried with short
 * goals.
 */
@Composable
fun DebugScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.dawnly
    val signals by WatchSession.signals.collectAsState()
    val settings by app.settings.wakeSignals.collectAsState(initial = WakeSignalSettings())
    val watch by app.watchScheduler.state.collectAsState(initial = null)
    val serviceRunning by WatchService.isRunning.collectAsState()
    val stepSensor = remember { stepSensor(context) }

    var activityAllowed by remember { mutableStateOf(activityAllowed(context)) }
    var usageAllowed by remember { mutableStateOf(WakeWatcher.hasUsageAccess(context)) }
    LifecycleResumeEffect(Unit) {
        activityAllowed = activityAllowed(context)
        usageAllowed = WakeWatcher.hasUsageAccess(context)
        onPauseOrDispose { }
    }
    val requestActivity = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        activityAllowed = it
    }

    DebugContent(
        state = DebugState(watch, serviceRunning, signals, settings, activityAllowed, usageAllowed, stepSensor),
        onSettingsChange = { app.scope.launch { app.settings.setWakeSignals(it) } },
        onAllowActivity = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestActivity.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        },
        onAllowUsage = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        modifier = modifier,
    )
}

@Composable
private fun DebugContent(
    state: DebugState,
    onSettingsChange: (WakeSignalSettings) -> Unit,
    onAllowActivity: () -> Unit,
    onAllowUsage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.headlineMedium)

        Section(stringResource(R.string.debug_watch_window)) {
            ValueRow(stringResource(R.string.debug_state), watchText(state.watch))
            ValueRow(
                stringResource(R.string.debug_service),
                stringResource(if (state.serviceRunning) R.string.debug_running else R.string.debug_not_running),
            )
        }

        Section(stringResource(R.string.debug_live_signals)) {
            val signals = state.signals
            if (signals == null) {
                Text(
                    stringResource(R.string.debug_not_watching),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                val score = WakeScore.of(signals, settings)
                ValueRow(
                    stringResource(R.string.debug_screen_use),
                    stringResource(
                        R.string.debug_of_goal,
                        durationText(signals.screenOnTime),
                        durationText(settings.screenTimeGoal),
                    ),
                )
                ValueRow(stringResource(R.string.debug_apps), appsText(signals, settings))
                ValueRow(stringResource(R.string.debug_steps), stepsText(signals, settings))
                ValueRow(
                    stringResource(R.string.debug_movement),
                    if (!signals.movementAvailable) {
                        stringResource(R.string.debug_movement_unavailable)
                    } else {
                        stringResource(R.string.debug_of_goal, durationText(signals.movingTime), durationText(settings.movementGoal))
                    },
                )
                ValueRow(stringResource(R.string.debug_music), durationText(signals.musicTime))
                ValueRow(stringResource(R.string.debug_charger), yesNo(signals.unplugged))
                ValueRow(
                    stringResource(R.string.debug_stretch),
                    stringResource(
                        R.string.debug_stretch_value,
                        durationText(signals.stretch),
                        signals.idle?.let { durationText(it) } ?: "-",
                    ),
                )
                ValueRow(stringResource(R.string.debug_kept_going), yesNo(signals.keptGoing))
                ValueRow(
                    stringResource(R.string.debug_score),
                    stringResource(R.string.debug_percent, (score.value * 100).roundToInt()),
                )
                ValueRow(stringResource(R.string.debug_awake), yesNo(score.isAwake), bold = true)
            }
        }

        Section(stringResource(R.string.debug_signals_that_count)) {
            SwitchRow(stringResource(R.string.debug_screen_use), settings.useScreenTime) {
                onSettingsChange(settings.copy(useScreenTime = it))
            }
            Stepper(
                label = stringResource(R.string.debug_screen_goal),
                value = durationText(settings.screenTimeGoal),
                onDecrease = {
                    val minutes = (settings.screenTimeGoal.toMinutes() - 1).coerceAtLeast(1)
                    onSettingsChange(settings.copy(screenTimeGoal = Duration.ofMinutes(minutes)))
                },
                onIncrease = {
                    val minutes = (settings.screenTimeGoal.toMinutes() + 1).coerceAtMost(30)
                    onSettingsChange(settings.copy(screenTimeGoal = Duration.ofMinutes(minutes)))
                },
            )
            SwitchRow(stringResource(R.string.debug_apps), settings.usePhoneUse) {
                onSettingsChange(settings.copy(usePhoneUse = it))
            }
            SwitchRow(stringResource(R.string.debug_steps), settings.useSteps) {
                onSettingsChange(settings.copy(useSteps = it))
            }
            Stepper(
                label = stringResource(R.string.debug_steps_goal),
                value = settings.stepsGoal.toString(),
                onDecrease = { onSettingsChange(settings.copy(stepsGoal = (settings.stepsGoal - 10).coerceAtLeast(10))) },
                onIncrease = { onSettingsChange(settings.copy(stepsGoal = (settings.stepsGoal + 10).coerceAtMost(500))) },
            )
            SwitchRow(stringResource(R.string.debug_movement), settings.useMovement) {
                onSettingsChange(settings.copy(useMovement = it))
            }
            Stepper(
                label = stringResource(R.string.debug_movement_goal),
                value = durationText(settings.movementGoal),
                onDecrease = {
                    val minutes = (settings.movementGoal.toMinutes() - 1).coerceAtLeast(1)
                    onSettingsChange(settings.copy(movementGoal = Duration.ofMinutes(minutes)))
                },
                onIncrease = {
                    val minutes = (settings.movementGoal.toMinutes() + 1).coerceAtMost(10)
                    onSettingsChange(settings.copy(movementGoal = Duration.ofMinutes(minutes)))
                },
            )
            SwitchRow(stringResource(R.string.debug_music), settings.useMusic) {
                onSettingsChange(settings.copy(useMusic = it))
            }
            SwitchRow(stringResource(R.string.debug_charger), settings.useCharger) {
                onSettingsChange(settings.copy(useCharger = it))
            }
        }

        Section(stringResource(R.string.debug_usage_access)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.debug_usage_access), Modifier.weight(1f))
                if (state.usageAllowed) {
                    Text(stringResource(R.string.debug_allowed))
                } else {
                    TextButton(onClick = onAllowUsage) { Text(stringResource(R.string.debug_allow)) }
                }
            }
        }

        Section(stringResource(R.string.debug_steps_setup)) {
            ValueRow(
                stringResource(R.string.debug_step_sensor),
                stringResource(
                    when (state.stepSensor) {
                        StepSensor.WakeUp -> R.string.debug_sensor_wake_up
                        StepSensor.Sleeps -> R.string.debug_sensor_sleeps
                        StepSensor.None -> R.string.debug_sensor_none
                    },
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.debug_activity_recognition), Modifier.weight(1f))
                if (state.activityAllowed) {
                    Text(stringResource(R.string.debug_allowed))
                } else {
                    TextButton(onClick = onAllowActivity) { Text(stringResource(R.string.debug_allow)) }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        HorizontalDivider()
        content()
    }
}

@Composable
private fun ValueRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f))
        Text(value, fontWeight = if (bold) FontWeight.SemiBold else null)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Stepper(label: String, value: String, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f))
        TextButton(onClick = onDecrease) { Text(stringResource(R.string.debug_less)) }
        Text(value)
        TextButton(onClick = onIncrease) { Text(stringResource(R.string.debug_more)) }
    }
}

@Composable
private fun watchText(state: WatchState?): String = when (state) {
    null -> ""
    WatchState.Idle -> stringResource(R.string.debug_state_idle)
    is WatchState.Waiting -> stringResource(R.string.debug_state_waiting, timeText(state.startsAt))
    is WatchState.Watching -> stringResource(R.string.debug_state_watching, timeText(state.alarmAt))
    is WatchState.Skipped -> stringResource(R.string.debug_state_skipped)
}

@Composable
private fun stepsText(signals: WakeSignals, settings: WakeSignalSettings): String = when {
    !signals.stepsAvailable -> stringResource(R.string.debug_steps_unavailable)
    !settings.useSteps -> stringResource(R.string.debug_not_counted, signals.steps)
    else -> stringResource(R.string.debug_of_goal, signals.steps.toString(), settings.stepsGoal.toString())
}

@Composable
private fun appsText(signals: WakeSignals, settings: WakeSignalSettings): String = when {
    !signals.appsAvailable -> stringResource(R.string.debug_apps_unavailable)
    !settings.usePhoneUse -> stringResource(R.string.debug_not_counted, signals.appsOpened)
    else -> stringResource(R.string.debug_of_goal, signals.appsOpened.toString(), settings.appsOpenedGoal.toString())
}

@Composable
private fun yesNo(value: Boolean): String = stringResource(if (value) R.string.debug_yes else R.string.debug_no)

@Composable
private fun durationText(duration: Duration): String {
    val minutes = duration.toMinutes().toInt()
    val seconds = (duration.seconds % 60).toInt()
    return if (seconds == 0) {
        stringResource(R.string.debug_minutes, minutes)
    } else {
        stringResource(R.string.debug_minutes_seconds, minutes, seconds)
    }
}

@Composable
private fun timeText(instant: Instant): String =
    DateFormat.getTimeFormat(LocalContext.current).format(Date.from(instant))

private fun activityAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
        PackageManager.PERMISSION_GRANTED

private fun stepSensor(context: Context): StepSensor {
    val sensors = context.getSystemService(SensorManager::class.java)
    return when {
        sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER, true) != null -> StepSensor.WakeUp
        sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null -> StepSensor.Sleeps
        else -> StepSensor.None
    }
}

@Preview(showBackground = true)
@Composable
private fun DebugContentPreview() {
    val now = Instant.now()
    DawnlyTheme {
        Surface {
            DebugContent(
                state = DebugState(
                    watch = WatchState.Watching(alarmAt = now.plusSeconds(1_800), startedAt = now.minusSeconds(1_800)),
                    serviceRunning = true,
                    signals = WakeSignals(
                        screenOnTime = Duration.ofSeconds(200),
                        appsOpened = 2,
                        steps = 42,
                        movingTime = Duration.ofSeconds(75),
                        musicTime = Duration.ofSeconds(30),
                    ),
                    settings = WakeSignalSettings(useSteps = true, usePhoneUse = true),
                    activityAllowed = false,
                    usageAllowed = true,
                    stepSensor = StepSensor.WakeUp,
                ),
                onSettingsChange = {},
                onAllowActivity = {},
                onAllowUsage = {},
            )
        }
    }
}
