package com.dawnly.app.ui.home

import android.content.res.Configuration
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dawnly.app.R
import com.dawnly.app.data.alarm.AppRef
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.domain.wake.WakeSignals
import com.dawnly.app.domain.watch.WatchState
import com.dawnly.app.ui.format.clockFace
import com.dawnly.app.ui.format.dayLabel
import com.dawnly.app.ui.format.formatTime
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.CheckBadge
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.PreviewFrame
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.glass.ProgressRing
import com.dawnly.app.ui.glass.SpringIn
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.PermissionState
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    permissions: PermissionState,
    onRequestPermission: (AppPermission) -> Unit,
    contentPadding: PaddingValues,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dismissResult by viewModel.dismissResult.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    HomeContent(
        state = state,
        permissions = permissions,
        dismissResult = dismissResult,
        onDismissAlarm = { activity?.let(viewModel::dismissAlarm) },
        onClearDismissResult = viewModel::clearDismissResult,
        onRequestPermission = onRequestPermission,
        contentPadding = contentPadding,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    permissions: PermissionState,
    dismissResult: DismissResult?,
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
            SpringIn(order++) { SignalCard(state.signals) }
        }
        AnimatedVisibility(visible = dismissResult != null, enter = fadeIn(), exit = fadeOut()) {
            dismissResult?.let { NoteCard(describe(it), onClick = onClearDismissResult) }
        }
        if (status is HomeStatus.Watching) {
            SpringIn(order) {
                if (status.awake) {
                    PrimaryButton(stringResource(R.string.dismiss_alarm), onDismissAlarm)
                } else {
                    GlassButton(stringResource(R.string.dismiss_alarm), onDismissAlarm)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AlarmRing(status: HomeStatus, now: Instant, modifier: Modifier = Modifier) {
    val progress = (status as? HomeStatus.Watching)?.progress
    val description = progress?.let {
        stringResource(R.string.home_progress_description, (it * 100).roundToInt())
    }
    ProgressRing(
        progress = progress,
        modifier = modifier.then(
            if (description != null) Modifier.semantics { contentDescription = description } else Modifier,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            val alarmAt = when (status) {
                is HomeStatus.Waiting -> status.alarmAt
                is HomeStatus.Watching -> status.alarmAt
                is HomeStatus.Skipped -> status.alarmAt
                HomeStatus.NoAlarm, HomeStatus.Loading -> null
            }
            if (alarmAt == null) {
                Text(stringResource(R.string.home_no_alarm), style = MaterialTheme.typography.displaySmall)
                Soft(stringResource(R.string.home_no_alarm_hint), Modifier.padding(top = 8.dp))
                return@Column
            }
            Soft(dayLabel(alarmAt, now), Modifier.padding(bottom = 4.dp))
            AlarmTime(alarmAt)
            val subtitle = when (status) {
                is HomeStatus.Waiting -> stringResource(R.string.home_waiting, formatTime(status.startsAt))
                is HomeStatus.Watching ->
                    if (status.awake) stringResource(R.string.home_awake)
                    else stringResource(R.string.home_watching, formatTime(status.startedAt))
                else -> stringResource(R.string.home_skipped)
            }
            AnimatedContent(
                targetState = subtitle,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "subtitle",
            ) { Soft(it, Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
private fun AlarmTime(alarmAt: Instant) {
    val face = clockFace(alarmAt)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(face.digits, style = MaterialTheme.typography.displayLarge)
        if (face.marker != null) {
            Text(
                face.marker,
                style = MaterialTheme.typography.titleMedium,
                color = DawnlyTheme.colors.textSoft,
                modifier = Modifier.padding(start = 4.dp, bottom = 9.dp),
            )
        }
    }
}

@Composable
private fun SignalCard(rows: List<SignalRow>) {
    GlassList {
        rows.forEachIndexed { index, row ->
            GlassRow(title = signalTitle(row.kind), divider = index > 0) {
                AnimatedContent(
                    targetState = row,
                    contentKey = { it.met },
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith fadeOut() },
                    label = "signal",
                ) { shown ->
                    if (shown.met) {
                        val seen = stringResource(R.string.home_signal_seen)
                        CheckBadge(Modifier.semantics { contentDescription = seen })
                    } else {
                        Soft(signalValue(shown))
                    }
                }
            }
        }
    }
}

@Composable
private fun signalTitle(kind: SignalKind) = stringResource(
    when (kind) {
        SignalKind.ScreenTime -> R.string.home_signal_screen
        SignalKind.PhoneUse -> R.string.home_signal_apps
        SignalKind.Steps -> R.string.home_signal_steps
        SignalKind.Movement -> R.string.home_signal_movement
        SignalKind.Music -> R.string.home_signal_music
    },
)

@Composable
private fun signalValue(row: SignalRow): String {
    val current = row.current
    return when (row.kind) {
        SignalKind.ScreenTime, SignalKind.Movement ->
            if (current == null) stringResource(R.string.home_signal_minutes_goal, row.goal)
            else stringResource(R.string.home_signal_minutes_of, current, row.goal)
        SignalKind.PhoneUse ->
            if (current == null) pluralStringResource(R.plurals.home_signal_apps_goal, row.goal, row.goal)
            else stringResource(R.string.home_signal_count_of, current, row.goal)
        SignalKind.Steps ->
            if (current == null) pluralStringResource(R.plurals.home_signal_steps_goal, row.goal, row.goal)
            else stringResource(R.string.home_signal_count_of, current, row.goal)
        // Music only adds to movement, so before the window there's no goal to show.
        SignalKind.Music -> if (current == null) "" else stringResource(R.string.home_signal_not_yet)
    }
}

@Composable
private fun PermissionWarning(title: String, body: String, onAllow: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Soft(body, Modifier.padding(top = 2.dp), align = TextAlign.Start)
            }
            AccentPill(stringResource(R.string.common_allow), onAllow)
        }
    }
}

@Composable
private fun NoteCard(text: String, onClick: (() -> Unit)? = null) {
    GlassSurface(Modifier.fillMaxWidth(), onClick = onClick) {
        Soft(text, Modifier.padding(horizontal = 18.dp, vertical = 16.dp), align = TextAlign.Start)
    }
}

@Composable
private fun Soft(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Center) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = DawnlyTheme.colors.textSoft,
        textAlign = align,
        modifier = modifier,
    )
}

@Composable
private fun describe(result: DismissResult) = when (result) {
    is DismissResult.OpenedClock -> stringResource(R.string.home_opened_clock, result.clock.label)
    DismissResult.NoClockApp -> stringResource(R.string.home_no_clock)
    is DismissResult.Requested -> ""
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
            onDismissAlarm = {},
            onClearDismissResult = {},
            onRequestPermission = {},
            contentPadding = PreviewPadding,
            now = PreviewNow,
        )
    }
}
