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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.TimeWheel
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Setting an alarm from Today, or changing one set from Dawnly ([editing] is when it rings): the
 * same glass circle with time wheels as the reminder editor. Done sets or changes it in the Clock
 * app; when changing, Delete alarm switches it off. Tapping around it or Back closes it unchanged.
 */
@Composable
internal fun AlarmWheel(
    editing: Instant?,
    onDone: (LocalTime) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
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
            val start = editing?.atZone(ZoneId.systemDefault())?.toLocalTime()?.withSecond(0)?.withNano(0)
                ?: DEFAULT_ALARM
            TimeWheel(
                stringResource(if (editing == null) R.string.alarm_pick_label else R.string.alarm_edit_label),
                start,
                onDone = onDone,
            )
            if (editing != null) GlassButton(stringResource(R.string.alarm_delete), onDelete)
        }
    }
}

/** Where the wheels start for a new alarm: a usual morning alarm. */
private val DEFAULT_ALARM: LocalTime = LocalTime.of(7, 0)
