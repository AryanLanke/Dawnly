package com.dawnly.app.ui.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.settings.DismissMode
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.theme.DawnlyTheme

/**
 * Ask by default. Picking an automatic choice without "Display over other apps" first says why it's
 * needed, with one tap to allow it; the choice is saved once it's allowed.
 */
@Composable
internal fun DismissModeSection(
    mode: DismissMode,
    overlayAllowed: Boolean,
    fullScreenAllowed: Boolean,
    onChange: (DismissMode) -> Unit,
    onAllowOverlay: (DismissMode) -> Unit,
) {
    var waiting by rememberSaveable { mutableStateOf<DismissMode?>(null) }
    LaunchedEffect(mode) { if (mode == waiting) waiting = null }
    val modes = DismissMode.entries
    val shown = waiting ?: mode
    Column {
        SectionLabel(stringResource(R.string.settings_dismiss_label))
        GlassSegmented(
            options = modes.map { stringResource(it.label) },
            selected = modes.indexOf(shown),
            onSelect = { index ->
                val picked = modes[index]
                // Full-screen notifications are enough to dismiss by itself; overlay only skips a tap.
                if (picked == DismissMode.Ask || overlayAllowed || fullScreenAllowed) {
                    waiting = null
                    onChange(picked)
                } else {
                    waiting = picked
                }
            },
        )
        Text(
            stringResource(shown.detail),
            style = MaterialTheme.typography.bodySmall,
            color = DawnlyTheme.colors.textSoft,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
        )
        // Works already; Display over other apps would only save the tap while the phone is in use.
        AnimatedVisibility(visible = waiting == null && mode != DismissMode.Ask && !overlayAllowed) {
            Column(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)) {
                Text(
                    stringResource(R.string.settings_auto_dismiss_tap_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnlyTheme.colors.textSoft,
                )
                AccentPill(stringResource(R.string.common_allow), { onAllowOverlay(mode) }, Modifier.padding(top = 10.dp))
            }
        }
        AnimatedVisibility(visible = waiting != null) {
            Column(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)) {
                // Android 11+ ignores the package Uri and opens the list of all apps.
                val why = stringResource(R.string.settings_auto_dismiss_why)
                val findDawnly = stringResource(R.string.settings_auto_dismiss_find)
                // Android 13+ may lock the switch for apps installed from a file; say so before it happens.
                val restricted = stringResource(R.string.settings_auto_dismiss_restricted)
                Text(
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> "$why $findDawnly $restricted"
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> "$why $findDawnly"
                        else -> why
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnlyTheme.colors.textSoft,
                )
                AccentPill(stringResource(R.string.common_allow), { waiting?.let(onAllowOverlay) }, Modifier.padding(top = 10.dp))
            }
        }
    }
}

private val DismissMode.label
    get() = when (this) {
        DismissMode.Ask -> R.string.dismiss_mode_ask
        DismissMode.Warn -> R.string.dismiss_mode_warn
        DismissMode.Instant -> R.string.dismiss_mode_instant
    }

private val DismissMode.detail
    get() = when (this) {
        DismissMode.Ask -> R.string.dismiss_mode_ask_detail
        DismissMode.Warn -> R.string.dismiss_mode_warn_detail
        DismissMode.Instant -> R.string.dismiss_mode_instant_detail
    }
