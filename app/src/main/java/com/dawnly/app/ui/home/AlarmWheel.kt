package com.dawnly.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.glass.DateWheel
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.TimeWheel
import java.time.LocalDate
import java.time.LocalTime

/** The steps of the alarm picker: first the date (for a custom date), then the time. */
private enum class AlarmStep { Date, Time }

/**
 * Setting an alarm from Today: the same glass circle with time wheels as the reminder editor.
 * Today, Tomorrow or a custom date (picked on its own wheels first). Each change slides across.
 * [onDone] gets the time and the day, where null means today. Tapping around the wheels or Back
 * closes it without an alarm.
 */
@Composable
internal fun AlarmWheel(onDone: (LocalTime, LocalDate?) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val today = remember { LocalDate.now() }
    // 0 today, 1 tomorrow, 2 a custom date.
    var choice by remember { mutableStateOf(0) }
    var date by remember { mutableStateOf(today.plusDays(1)) }
    // A custom date is picked first; its time comes after.
    var pickingDate by remember { mutableStateOf(false) }
    val step = if (choice == CUSTOM && pickingDate) AlarmStep.Date else AlarmStep.Time
    val dayOf: () -> LocalDate? = {
        when (choice) {
            TOMORROW -> today.plusDays(1)
            CUSTOM -> date
            else -> null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose)
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Taps on the wheels stay on the wheels.
        Column(
            Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlassSegmented(
                options = listOf(
                    stringResource(R.string.alarm_today),
                    stringResource(R.string.alarm_tomorrow),
                    stringResource(R.string.alarm_custom_date),
                ),
                selected = choice,
                onSelect = {
                    choice = it
                    pickingDate = it == CUSTOM
                },
            )
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState == AlarmStep.Time
                    (slideInHorizontally { if (forward) it / 2 else -it / 2 } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it / 2 else it / 2 } + fadeOut())
                },
                label = "alarmStep",
            ) { current ->
                if (current == AlarmStep.Date) {
                    DateWheel(stringResource(R.string.alarm_date_label), date, today, onDone = {
                        date = it
                        pickingDate = false
                    })
                } else {
                    TimeWheel(stringResource(R.string.alarm_pick_label), DEFAULT_ALARM, onDone = { onDone(it, dayOf()) })
                }
            }
        }
    }
}

private const val TOMORROW = 1
private const val CUSTOM = 2

/** Where the wheels start: a usual morning alarm. */
private val DEFAULT_ALARM: LocalTime = LocalTime.of(7, 0)
