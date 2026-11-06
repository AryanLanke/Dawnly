package com.dawnly.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.domain.watch.WatchWindow
import com.dawnly.app.ui.format.formatDuration
import com.dawnly.app.ui.glass.DurationWheel
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.glass.ValueButton
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Duration

@Composable
internal fun WindowSection(window: Duration, onChange: (Duration) -> Unit, onPickCustom: () -> Unit) {
    val presets = WatchWindow.PRESETS
    var customOpen by rememberSaveable { mutableStateOf(false) }
    val custom = customOpen || window !in presets
    Column {
        SectionLabel(stringResource(R.string.settings_window_label))
        GlassSegmented(
            options = presets.map { formatDuration(it) } + stringResource(R.string.settings_window_custom),
            selected = if (custom) presets.size else presets.indexOf(window),
            onSelect = { index ->
                customOpen = index == presets.size
                if (customOpen) onPickCustom() else onChange(presets[index])
            },
        )
        AnimatedVisibility(
            visible = custom,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            CustomWindow(window, onPickCustom)
        }
    }
}

/**
 * The custom window, from 5 minutes to 3 hours: "Custom  45 min", which opens the hours and
 * minutes wheels in a glass circle, like a reminder's custom repeat (see [WindowWheel]). Under
 * [WatchWindow.RECOMMENDED_MIN] a note says it's short.
 */
@Composable
private fun CustomWindow(window: Duration, onPick: () -> Unit) {
    Column {
        ValueButton(
            stringResource(R.string.settings_window_custom),
            formatDuration(window),
            Modifier.fillMaxWidth().padding(top = 10.dp),
            onClick = onPick,
        )
        AnimatedVisibility(visible = window < WatchWindow.RECOMMENDED_MIN) {
            Text(
                stringResource(R.string.settings_window_short),
                style = MaterialTheme.typography.bodySmall,
                color = DawnlyTheme.colors.textSoft,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
            )
        }
    }
}

/**
 * Picking the custom window on the wheels, drawn over the screen like the reminder editor: the
 * glass circle with hours (0 to 3) and minutes, kept between 5 minutes and 3 hours. Tapping around
 * it or Back closes it unchanged.
 */
@Composable
internal fun WindowWheel(window: Duration, onDone: (Duration) -> Unit, onClose: () -> Unit) {
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
        Box(Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {}) {
            DurationWheel(
                stringResource(R.string.settings_window_pick),
                window,
                WatchWindow.MIN,
                WatchWindow.MAX,
                onDone = { onDone(WatchWindow.clamp(it)) },
            )
        }
    }
}
