package com.dawnly.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.ui.glass.CheckBadge
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.settings.rememberSoundPreview

/**
 * Choosing a sound: Dawnly's sounds in a glass list. Tapping one picks it and plays it, so the
 * user hears what their reminder (or alarm) will sound like.
 */
@Composable
internal fun SoundChoice(selected: AlertSound, onPick: (AlertSound) -> Unit, onDone: () -> Unit) {
    val preview = rememberSoundPreview()
    GlassSurface(Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.reminder_sound), style = MaterialTheme.typography.titleMedium)
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                GlassList(Modifier.selectableGroup()) {
                    AlertSound.entries.forEachIndexed { index, sound ->
                        GlassRow(
                            title = stringResource(sound.label),
                            divider = index > 0,
                            modifier = Modifier.selectable(selected = sound == selected, role = Role.RadioButton) {
                                onPick(sound)
                                preview.play(sound)
                            },
                        ) {
                            if (sound == selected) CheckBadge()
                        }
                    }
                }
            }
            GlassButton(
                stringResource(R.string.reminder_done),
                {
                    preview.stop()
                    onDone()
                },
                Modifier.fillMaxWidth(),
            )
        }
    }
}
