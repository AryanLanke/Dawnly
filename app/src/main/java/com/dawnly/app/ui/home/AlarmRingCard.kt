package com.dawnly.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.format.clockFace
import com.dawnly.app.ui.format.dayLabel
import com.dawnly.app.ui.format.formatTime
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.glass.ProgressRing
import com.dawnly.app.ui.theme.DawnlyMotion
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Instant
import kotlin.math.roundToInt

// The alarm ring and the dismiss button, as pressed.

/** How far the alarm ring sinks, and how bright its light gets, while pressed. */
private const val PRESSED_SCALE = 0.93f
private const val PRESSED_GLOW = 0.16f

@Composable
internal fun AlarmRing(
    status: HomeStatus,
    now: Instant,
    modifier: Modifier = Modifier,
    dawnlyOwned: Boolean = false,
    onTap: () -> Unit = {},
) {
    val progress = (status as? HomeStatus.Watching)?.progress
    val description = progress?.let {
        stringResource(R.string.home_progress_description, (it * 100).roundToInt())
    }
    val watching = status as? HomeStatus.Watching
    // The whole ring is a button: set an alarm, edit Dawnly's own, or open the Clock app to change
    // or delete the one there. Pressed, it sinks and a soft light rises over the glass (a ripple
    // would be hidden under the glass), with a tick.
    val tapLabel = stringResource(
        when {
            status == HomeStatus.NoAlarm -> R.string.home_set_alarm_description
            dawnlyOwned -> R.string.home_open_dawnly_description
            else -> R.string.home_open_clock_description
        },
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
                stringResource(if (dawnlyOwned) R.string.home_tap_to_edit else R.string.home_tap_to_change),
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
