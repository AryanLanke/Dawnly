package com.dawnly.app.ui.feasibility

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.alarm.AppRef
import com.dawnly.app.data.alarm.ClockApp
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.data.alarm.NextAlarm
import com.dawnly.app.data.alarm.NextAlarmSource
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/** The last tap of "Dismiss next alarm" and the alarm it was aimed at. */
private data class DismissAttempt(val result: DismissResult, val target: NextAlarm)

/**
 * Test screen. Phase 1: can we read the next alarm, and can the Clock app dismiss it?
 * Phase 2: the watch window. Deliberately plain; the real Home screen replaces it in Phase 5.
 */
@Composable
fun FeasibilityScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val source = remember { NextAlarmSource(context) }
    val clockApp = remember { ClockApp(context) }

    val initialAlarm = remember { source.current() }
    val alarm by source.nextAlarm.collectAsState(initialAlarm)
    val owner = remember(alarm?.ownerPackage) { alarm?.ownerPackage?.let(clockApp::appRef) }
    val dismissHandler = remember { clockApp.dismissHandler() }
    var attempt by remember { mutableStateOf<DismissAttempt?>(null) }
    val now by produceState(Instant.now()) {
        while (true) {
            delay(30_000)
            value = Instant.now()
        }
    }

    FeasibilityContent(
        alarm = alarm,
        owner = owner,
        dismissHandler = dismissHandler,
        attempt = attempt,
        now = now,
        onDismiss = {
            alarm?.let { target ->
                attempt = DismissAttempt(clockApp.dismissNextAlarm(context, target.ownerPackage), target)
            }
        },
        onOpenClock = { clockApp.openClock(context, alarm?.ownerPackage) },
        modifier = modifier,
    )
}

@Composable
private fun FeasibilityContent(
    alarm: NextAlarm?,
    owner: AppRef?,
    dismissHandler: AppRef?,
    attempt: DismissAttempt?,
    now: Instant,
    onDismiss: () -> Unit,
    onOpenClock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(R.string.feasibility_title),
                style = MaterialTheme.typography.labelLarge,
                color = softColor(),
            )
            AlarmSummary(alarm, now)
            if (alarm != null) {
                InfoRow(
                    label = stringResource(R.string.set_by),
                    value = owner?.let { describe(it) } ?: stringResource(R.string.unknown_app),
                )
            }
            InfoRow(
                label = stringResource(R.string.dismiss_goes_to),
                value = dismissHandler?.let { describe(it) }
                    ?: stringResource(R.string.no_dismiss_handler),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss,
                    enabled = alarm != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.dismiss_next_alarm))
                }
                OutlinedButton(onClick = onOpenClock, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.open_clock))
                }
            }
            if (attempt != null) {
                HorizontalDivider()
                AttemptResult(attempt, current = alarm)
            }
            if (!LocalInspectionMode.current) {
                HorizontalDivider()
                WatchWindowSection()
            }
        }
    }
}

@Composable
private fun AlarmSummary(alarm: NextAlarm?, now: Instant) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.next_alarm),
            style = MaterialTheme.typography.titleMedium,
        )
        if (alarm == null) {
            Text(
                text = stringResource(R.string.no_alarm),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
            )
            Text(
                text = stringResource(R.string.no_alarm_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = softColor(),
            )
        } else {
            Text(
                text = formatTime(alarm.triggerAt),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Light,
            )
            Text(
                text = stringResource(
                    R.string.day_and_countdown,
                    dayLabel(alarm.triggerAt, now),
                    countdown(alarm.triggerAt, now),
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = softColor(),
            )
        }
    }
}

@Composable
internal fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = softColor())
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AttemptResult(attempt: DismissAttempt, current: NextAlarm?) {
    val text = when (val result = attempt.result) {
        is DismissResult.Requested -> {
            val asked = stringResource(
                R.string.result_requested,
                result.handler.label,
                formatTime(attempt.target.triggerAt),
            )
            val change = stringResource(
                if (current != attempt.target) R.string.result_alarm_changed
                else R.string.result_alarm_unchanged,
            )
            "$asked\n$change"
        }
        is DismissResult.OpenedClock -> stringResource(R.string.result_opened_clock, result.clock.label)
        DismissResult.NoClockApp -> stringResource(R.string.result_no_clock)
    }
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
internal fun softColor() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

@Composable
private fun describe(app: AppRef): String =
    if (app.label == app.packageName) app.packageName
    else stringResource(R.string.app_and_package, app.label, app.packageName)

/** Formats a time the way the phone does, following its 12/24-hour setting. */
@Composable
internal fun formatTime(instant: Instant): String {
    val skeleton = if (DateFormat.is24HourFormat(LocalContext.current)) "Hm" else "hm"
    return formatPattern(instant, skeleton)
}

@Composable
private fun dayLabel(instant: Instant, now: Instant): String {
    val zone = ZoneId.systemDefault()
    val today = now.atZone(zone).toLocalDate()
    return when (instant.atZone(zone).toLocalDate()) {
        today -> stringResource(R.string.today)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> formatPattern(instant, "EEEdMMM")
    }
}

@Composable
private fun countdown(instant: Instant, now: Instant): String {
    val minutes = Duration.between(now, instant).toMinutes()
    return when {
        minutes < 1 -> stringResource(R.string.due_now)
        minutes < 60 -> stringResource(R.string.in_minutes, minutes)
        else -> stringResource(R.string.in_hours_minutes, minutes / 60, minutes % 60)
    }
}

private fun formatPattern(instant: Instant, skeleton: String): String {
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
    return DateTimeFormatter.ofPattern(pattern).format(instant.atZone(ZoneId.systemDefault()))
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeasibilityPreview() {
    val clock = AppRef("com.google.android.deskclock", "Clock")
    val alarm = NextAlarm(Instant.now().plusSeconds(95 * 60), clock.packageName)
    DawnlyTheme {
        FeasibilityContent(
            alarm = alarm,
            owner = clock,
            dismissHandler = clock,
            attempt = DismissAttempt(DismissResult.Requested(clock), alarm),
            now = Instant.now(),
            onDismiss = {},
            onOpenClock = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeasibilityNoAlarmPreview() {
    DawnlyTheme {
        FeasibilityContent(
            alarm = null,
            owner = null,
            dismissHandler = null,
            attempt = null,
            now = Instant.now(),
            onDismiss = {},
            onOpenClock = {},
        )
    }
}
