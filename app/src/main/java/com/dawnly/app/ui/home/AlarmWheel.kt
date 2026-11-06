package com.dawnly.app.ui.home

import androidx.activity.compose.BackHandler
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
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.TimeWheel
import java.time.LocalTime

/**
 * Setting an alarm from Today: the same glass circle with time wheels as the reminder editor.
 * Done sets it in the Clock app; tapping around it or Back closes it without one.
 */
@Composable
internal fun AlarmWheel(onDone: (LocalTime, Boolean) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var tomorrow by remember { mutableStateOf(false) }
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
                options = listOf(stringResource(R.string.alarm_today), stringResource(R.string.alarm_tomorrow)),
                selected = if (tomorrow) 1 else 0,
                onSelect = { tomorrow = it == 1 },
            )
            TimeWheel(stringResource(R.string.alarm_pick_label), DEFAULT_ALARM, onDone = { onDone(it, tomorrow) })
        }
    }
}

/** Where the wheels start: a usual morning alarm. */
private val DEFAULT_ALARM: LocalTime = LocalTime.of(7, 0)
