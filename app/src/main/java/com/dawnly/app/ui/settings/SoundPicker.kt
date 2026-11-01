package com.dawnly.app.ui.settings

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.ui.glass.CheckBadge
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.PreviewFrame
import com.dawnly.app.ui.glass.SpringIn
import com.dawnly.app.ui.theme.DawnlyTheme

/**
 * The list of prompt sounds. Tapping one chooses it and plays it, so the user hears what they picked.
 * [silenced] shows a hint when previews can't be heard.
 */
@Composable
fun SoundPicker(
    selected: AlertSound,
    silenced: Boolean,
    onSelect: (AlertSound) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 18.dp, bottom = 18.dp),
        ) {
            GlassSurface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                onClick = onBack,
                onClickLabel = stringResource(R.string.sound_back),
                contentAlignment = Alignment.Center,
            ) {
                Icon(GlassIcons.Back, contentDescription = stringResource(R.string.sound_back), modifier = Modifier.size(22.dp))
            }
            Text(
                stringResource(R.string.sound_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
        AnimatedVisibility(visible = silenced) {
            Text(
                stringResource(R.string.sound_silent_hint),
                style = MaterialTheme.typography.bodySmall,
                color = DawnlyTheme.colors.textSoft,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
            )
        }
        SpringIn(order = 0) {
            GlassList(Modifier.selectableGroup()) {
                AlertSound.entries.forEachIndexed { index, sound ->
                    GlassRow(
                        title = stringResource(sound.label),
                        divider = index > 0,
                        modifier = Modifier.selectable(
                            selected = sound == selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(sound) },
                        ),
                    ) {
                        AnimatedVisibility(
                            visible = sound == selected,
                            enter = fadeIn() + scaleIn(initialScale = 0.6f),
                            exit = fadeOut() + scaleOut(targetScale = 0.6f),
                        ) {
                            CheckBadge()
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** The Settings row that opens the picker, showing the current choice. */
@Composable
fun SoundRow(selected: AlertSound, onClick: () -> Unit) {
    GlassList {
        GlassRow(
            title = stringResource(R.string.settings_sound_label),
            modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClick = onClick),
        ) {
            Text(
                stringResource(selected.label),
                style = MaterialTheme.typography.bodySmall,
                color = DawnlyTheme.colors.textSoft,
            )
            Icon(
                GlassIcons.Chevron,
                contentDescription = null,
                tint = DawnlyTheme.colors.textSoft,
                modifier = Modifier.padding(start = 6.dp).size(18.dp),
            )
        }
    }
}

@Preview(name = "Sound picker", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SoundPickerPreview() = PreviewFrame(dark = true) {
    SoundPicker(
        selected = AlertSound.Sparkle,
        silenced = true,
        onSelect = {},
        onBack = {},
        contentPadding = PaddingValues(top = 40.dp, bottom = 100.dp),
    )
}
