package com.dawnly.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.dawnly.app.R
import com.dawnly.app.domain.learn.Learned
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSwitch
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.glass.TextAction
import com.dawnly.app.ui.home.clock

/**
 * "Learning from you" in More options: the switch, and in plain words what Dawnly has learned from
 * the user's answers (`Learning`): how cautious it's become, their usual time, the sign it trusts
 * most. Start over forgets it all.
 */
@Composable
internal fun LearningSection(learning: LearningUi, onLearnChange: (Boolean) -> Unit, onReset: () -> Unit) {
    val learned = learning.learned
    Column {
        SectionLabel(stringResource(R.string.learning_label))
        GlassList {
            GlassRow(
                title = stringResource(R.string.learning_switch),
                subtitle = if (learned.mornings == 0) {
                    stringResource(R.string.learning_nothing)
                } else {
                    pluralStringResource(R.plurals.learning_mornings, learned.mornings, learned.mornings)
                },
            ) {
                GlassSwitch(learning.on, onLearnChange)
            }
            if (learning.on && learned.mornings > 0) {
                insights(learned).forEach { line -> GlassRow(title = line, divider = true) }
            }
        }
        if (learned.mornings > 0) {
            TextAction(
                stringResource(R.string.learning_reset),
                { onReset() },
                Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            )
        }
    }
}

/** What's been learned, one line each, only where it has moved from how Dawnly ships. */
@Composable
private fun insights(learned: Learned): List<String> = buildList {
    when {
        learned.caution > 1.03f -> add(stringResource(R.string.learning_later))
        learned.caution < 0.97f -> add(stringResource(R.string.learning_sooner))
    }
    learned.usualWeekday?.let { add(stringResource(R.string.learning_weekday, clock(it))) }
    learned.usualWeekend?.let { add(stringResource(R.string.learning_weekend, clock(it))) }
    val w = learned.weights
    val best = listOf(
        R.string.learning_sign_screen to w.screen,
        R.string.learning_sign_apps to w.apps,
        R.string.learning_sign_steps to w.steps,
        R.string.learning_sign_movement to w.movement,
    ).maxBy { it.second }
    if (best.second > 1.05f) add(stringResource(R.string.learning_trusts, stringResource(best.first)))
}
