package com.dawnly.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.glass.CheckBadge
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.ProgressBar
import com.dawnly.app.ui.glass.ROW_INSET
import kotlin.math.roundToInt

// The "Signs you're awake" card.

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
