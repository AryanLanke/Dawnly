package com.dawnly.app.ui.feasibility

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.dawnly.app.R
import com.dawnly.app.dawnly
import com.dawnly.app.domain.watch.WatchState
import com.dawnly.app.domain.watch.WatchWindow
import com.dawnly.app.service.WatchService
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Phase 2 test controls: pick the window, see what the scheduler decided, grant permissions. */
@Composable
fun WatchWindowSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.dawnly
    val scope = rememberCoroutineScope()

    val window by app.settings.watchWindow.collectAsState(initial = null)
    val state by app.watchScheduler.state.collectAsState(initial = null)
    val serviceRunning by WatchService.isRunning.collectAsState()
    var exactAllowed by remember { mutableStateOf(app.watchScheduler.canScheduleExactAlarms()) }
    var notificationsAllowed by remember { mutableStateOf(notificationsAllowed(context)) }
    // Both are changed in system settings, so check again whenever we come back.
    LifecycleResumeEffect(Unit) {
        exactAllowed = app.watchScheduler.canScheduleExactAlarms()
        notificationsAllowed = notificationsAllowed(context)
        onPauseOrDispose {}
    }
    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsAllowed = granted }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = stringResource(R.string.watch_window), style = MaterialTheme.typography.titleMedium)
        window?.let { current ->
            WindowPicker(
                window = current,
                onChange = { newWindow ->
                    scope.launch {
                        app.settings.setWatchWindow(newWindow)
                        app.watchScheduler.reconcile()
                    }
                },
            )
        }
        state?.let { InfoRow(label = stringResource(R.string.status), value = describe(it)) }
        InfoRow(
            label = stringResource(R.string.watch_service),
            value = stringResource(if (serviceRunning) R.string.running else R.string.not_running),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionRow(
                label = stringResource(R.string.exact_alarms),
                allowed = exactAllowed,
                deniedText = stringResource(R.string.exact_alarms_denied),
                onAllow = { context.startActivity(exactAlarmSettingsIntent(context)) },
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(
                label = stringResource(R.string.notifications),
                allowed = notificationsAllowed,
                deniedText = stringResource(R.string.notifications_denied),
                onAllow = { requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) },
            )
        }
    }
}

@Composable
private fun WindowPicker(window: Duration, onChange: (Duration) -> Unit) {
    var customOpen by remember { mutableStateOf(window !in WatchWindow.PRESETS) }
    val custom = customOpen || window !in WatchWindow.PRESETS
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.watch_window_hint),
            style = MaterialTheme.typography.labelMedium,
            color = softColor(),
        )
        val count = WatchWindow.PRESETS.size + 1
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            WatchWindow.PRESETS.forEachIndexed { index, preset ->
                SegmentedButton(
                    selected = !custom && window == preset,
                    onClick = {
                        customOpen = false
                        onChange(preset)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, count),
                    label = { Text(formatWindow(preset)) },
                )
            }
            SegmentedButton(
                selected = custom,
                onClick = { customOpen = true },
                shape = SegmentedButtonDefaults.itemShape(count - 1, count),
                label = { Text(stringResource(R.string.custom)) },
            )
        }
        if (custom) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedButton(
                    onClick = { onChange(window - WatchWindow.STEP) },
                    enabled = window > WatchWindow.MIN,
                ) { Text(stringResource(R.string.decrease)) }
                Text(text = formatWindow(window), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = { onChange(window + WatchWindow.STEP) },
                    enabled = window < WatchWindow.MAX,
                ) { Text(stringResource(R.string.increase)) }
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, allowed: Boolean, deniedText: String, onAllow: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        InfoRow(
            label = label,
            value = if (allowed) stringResource(R.string.allowed) else deniedText,
            modifier = Modifier.weight(1f),
        )
        if (!allowed) TextButton(onClick = onAllow) { Text(stringResource(R.string.allow)) }
    }
}

@Composable
private fun describe(state: WatchState): String = when (state) {
    WatchState.Idle -> stringResource(R.string.status_idle)
    is WatchState.Waiting -> stringResource(R.string.status_waiting, formatTime(state.startsAt))
    is WatchState.Watching -> stringResource(
        R.string.status_watching,
        formatTime(state.startedAt),
        (state.progress(Instant.now()) * 100).roundToInt(),
    )
    is WatchState.Skipped -> stringResource(R.string.status_skipped)
}

@Composable
private fun formatWindow(window: Duration): String {
    val hours = window.toHours().toInt()
    val minutes = (window.toMinutes() % 60).toInt()
    return when {
        hours == 0 -> stringResource(R.string.duration_minutes, minutes)
        minutes == 0 -> stringResource(R.string.duration_hours, hours)
        else -> stringResource(R.string.duration_hours_minutes, hours, minutes)
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** Opens Dawnly's "Alarms & reminders" page, which exists from Android 12. */
@RequiresApi(Build.VERSION_CODES.S)
private fun exactAlarmSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
