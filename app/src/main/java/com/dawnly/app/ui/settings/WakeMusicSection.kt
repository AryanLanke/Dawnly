package com.dawnly.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.settings.WakeMusicApp
import com.dawnly.app.notify.WakeMusic
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.GlassSwitch
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.theme.DawnlyTheme

/** Optional music instead of the alarm. Off by default. */
@Composable
internal fun WakeMusicSection(
    choice: WakeMusicApp,
    forReminders: Boolean,
    canDismiss: Boolean,
    onChange: (WakeMusicApp) -> Unit,
    onRemindersChange: (Boolean) -> Unit,
    onAllowFullScreen: () -> Unit,
) {
    val context = LocalContext.current
    val installed = remember(choice) { WakeMusic.isInstalled(context, choice) }
    val choices = WakeMusicApp.entries
    Column {
        SectionLabel(stringResource(R.string.settings_wake_music_label))
        GlassSegmented(
            options = choices.map { stringResource(it.label) },
            selected = choices.indexOf(choice),
            onSelect = { onChange(choices[it]) },
        )
        Text(
            stringResource(choice.detail, stringResource(choice.label)),
            style = MaterialTheme.typography.bodySmall,
            color = DawnlyTheme.colors.textSoft,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
        )
        AnimatedVisibility(visible = choice != WakeMusicApp.Off && !installed) {
            Text(
                stringResource(R.string.wake_music_not_installed, stringResource(choice.label)),
                style = MaterialTheme.typography.bodySmall,
                color = DawnlyTheme.colors.textSoft,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
            )
        }
        AnimatedVisibility(visible = choice != WakeMusicApp.Off && !canDismiss) {
            Column(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)) {
                Text(
                    stringResource(R.string.wake_music_full_screen_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnlyTheme.colors.textSoft,
                )
                AccentPill(stringResource(R.string.common_allow), onAllowFullScreen, Modifier.padding(top = 10.dp))
            }
        }
        AnimatedVisibility(visible = choice != WakeMusicApp.Off) {
            GlassList(Modifier.padding(top = 12.dp)) {
                GlassRow(
                    title = stringResource(R.string.wake_music_reminders),
                    subtitle = stringResource(R.string.wake_music_reminders_detail),
                ) {
                    GlassSwitch(forReminders, onRemindersChange)
                }
            }
        }
    }
}

private val WakeMusicApp.label
    get() = when (this) {
        WakeMusicApp.Off -> R.string.wake_music_off
        WakeMusicApp.Spotify -> R.string.wake_music_spotify
        WakeMusicApp.YouTubeMusic -> R.string.wake_music_youtube
        WakeMusicApp.AnyApp -> R.string.wake_music_any
    }

private val WakeMusicApp.detail
    get() = when (this) {
        WakeMusicApp.Off -> R.string.wake_music_off_detail
        WakeMusicApp.Spotify, WakeMusicApp.YouTubeMusic -> R.string.wake_music_app_detail
        WakeMusicApp.AnyApp -> R.string.wake_music_any_detail
    }
