package com.dawnly.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.ui.format.clockFace
import com.dawnly.app.ui.format.dayLabel
import com.dawnly.app.ui.format.formatTime
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.CheckBadge
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.glass.ProgressBar
import com.dawnly.app.ui.glass.ProgressRing
import com.dawnly.app.ui.glass.ROW_INSET
import com.dawnly.app.ui.theme.DawnlyMotion
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Instant
import kotlin.math.roundToInt

// The cards and pieces the Home screen is built from.

/** How far the alarm ring sinks, and how bright its light gets, while pressed. */
private const val PRESSED_SCALE = 0.93f
private const val PRESSED_GLOW = 0.16f

@Composable
internal fun AlarmRing(
    status: HomeStatus,
    now: Instant,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
) {
    val progress = (status as? HomeStatus.Watching)?.progress
    val description = progress?.let {
        stringResource(R.string.home_progress_description, (it * 100).roundToInt())
    }
    val watching = status as? HomeStatus.Watching
    // The whole ring is a button: set an alarm, or open the Clock app to change or delete the one
    // there. Pressed, it sinks and a soft light rises over the glass (a ripple would be hidden
    // under the glass), with a tick.
    val tapLabel = stringResource(
        if (status == HomeStatus.NoAlarm) R.string.home_set_alarm_description else R.string.home_open_clock_description,
    )
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) PRESSED_SCALE else 1f, DawnlyMotion.bouncy(), label = "ringPress")
    val glow by animateFloatAsState(if (pressed) PRESSED_GLOW else 0f, tween(if (pressed) 90 else 260), label = "ringGlow")
    val light = DawnlyTheme.colors.accent
    val tappable = Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            if (glow > 0f) drawCircle(light.copy(alpha = glow))
        }
        .clip(CircleShape)
        .clickable(interactions, indication = null, role = Role.Button, onClickLabel = tapLabel, onClick = onTap)
    ProgressRing(
        startedAt = watching?.startedAt,
        endsAt = watching?.alarmAt,
        modifier = modifier.then(
            if (description != null) Modifier.semantics { contentDescription = description } else Modifier,
        ).then(tappable),
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
                is HomeStatus.Watching -> when {
                    status.notYet -> stringResource(R.string.home_not_yet)
                    status.awake -> stringResource(R.string.home_awake)
                    else -> stringResource(R.string.home_watching, formatTime(status.startedAt))
                }
                else -> stringResource(R.string.home_skipped)
            }
            AnimatedContent(
                targetState = subtitle,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "subtitle",
            ) { Soft(it, Modifier.padding(top = 8.dp)) }
            Text(
                stringResource(R.string.home_tap_to_change),
                style = MaterialTheme.typography.labelMedium,
                color = DawnlyTheme.colors.accent,
                modifier = Modifier.padding(top = 6.dp),
            )
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

/**
 * The button pops in when watching starts, right where a tap meant for something else can land,
 * so a tap only asks; dismissing takes a second, deliberate tap.
 */
@Composable
internal fun DismissAction(alarmAt: Instant, awake: Boolean, onDismiss: () -> Unit) {
    var confirming by rememberSaveable(alarmAt) { mutableStateOf(false) }
    val dismiss = stringResource(R.string.dismiss_alarm)
    AnimatedContent(
        targetState = confirming,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "dismiss",
    ) { asking ->
        if (!asking) {
            if (awake) PrimaryButton(dismiss, { confirming = true }) else GlassButton(dismiss, { confirming = true })
            return@AnimatedContent
        }
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.home_confirm_dismiss, formatTime(alarmAt)),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(dismiss, { confirming = false; onDismiss() }, Modifier.weight(1f))
                    GlassButton(stringResource(R.string.common_cancel), { confirming = false }, Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * The signs that are on. While watching, a "Signs you're awake" bar on top shows [wakeProgress],
 * the same score that sends the prompt, so it's clear how far off the question is.
 */
@Composable
internal fun SignalCard(rows: List<SignalRow>, wakeProgress: Float?) {
    GlassList {
        if (wakeProgress != null) {
            val percent = (wakeProgress * 100).roundToInt()
            val description = stringResource(R.string.home_wake_description, percent)
            Column(
                Modifier.semantics(mergeDescendants = true) {
                    contentDescription = description
                    progressBarRangeInfo = ProgressBarRangeInfo(wakeProgress, 0f..1f)
                },
            ) {
                GlassRow(
                    title = stringResource(R.string.home_wake_title),
                    subtitle = stringResource(R.string.home_wake_hint),
                ) { Soft(stringResource(R.string.home_wake_percent, percent)) }
                ProgressBar(wakeProgress, Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 18.dp))
            }
        }
        rows.forEachIndexed { index, row ->
            GlassRow(title = signalTitle(row.kind), divider = index > 0 || wakeProgress != null) {
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
        SignalKind.Charger -> R.string.home_signal_charger
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
        // Music and the charger only add to the others, so before the window there's no goal to show.
        SignalKind.Charger, SignalKind.Music -> if (current == null) "" else stringResource(R.string.home_signal_not_yet)
    }
}

@Composable
internal fun PermissionWarning(title: String, body: String, onAllow: () -> Unit) {
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
internal fun NoteCard(text: String, onClick: (() -> Unit)? = null) {
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
internal fun describeDismiss(result: DismissResult) = when (result) {
    is DismissResult.OpenedClock -> stringResource(R.string.home_opened_clock, result.clock.label)
    DismissResult.NoClockApp -> stringResource(R.string.home_no_clock)
    is DismissResult.Requested -> ""
}
