package com.dawnly.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSwitch
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.theme.DawnlyTheme

@Composable
internal fun SignalsSection(
    signals: WakeSignalSettings,
    deniedSign: Sign?,
    onChange: (WakeSignalSettings) -> Unit,
    onNeedsPermission: (Sign) -> Unit,
) {
    var lastOneRefused by remember { mutableStateOf(false) }
    fun toggle(sign: Sign, on: Boolean) {
        val updated = signals.withSign(sign, on)
        lastOneRefused = updated == null
        if (updated != null) onChange(updated)
    }
    Column {
        SectionLabel(stringResource(R.string.settings_signals_label))
        GlassList {
            Sign.entries.forEachIndexed { index, sign ->
                GlassRow(title = stringResource(sign.title), subtitle = sign.subtitle(signals), divider = index > 0) {
                    GlassSwitch(signals.uses(sign), { on ->
                        if (on && sign.needsPermission) onNeedsPermission(sign) else toggle(sign, on)
                    })
                }
            }
        }
        val hint = when {
            lastOneRefused -> stringResource(R.string.settings_signals_min)
            deniedSign == Sign.Steps && !signals.useSteps -> stringResource(R.string.settings_steps_denied)
            deniedSign == Sign.PhoneUse && !signals.usePhoneUse -> stringResource(R.string.settings_apps_denied)
            else -> null
        }
        AnimatedVisibility(visible = hint != null) {
            Text(
                hint.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = DawnlyTheme.colors.textSoft,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            )
        }
    }
}

private fun WakeSignalSettings.uses(sign: Sign) = when (sign) {
    Sign.ScreenTime -> useScreenTime
    Sign.PhoneUse -> usePhoneUse
    Sign.Steps -> useSteps
    Sign.Movement -> useMovement
    Sign.Charger -> useCharger
    Sign.Music -> useMusic
}

/** Steps and apps opened turn on only once their permission is allowed. */
private val Sign.needsPermission get() = this == Sign.Steps || this == Sign.PhoneUse

private val Sign.title
    get() = when (this) {
        Sign.ScreenTime -> R.string.settings_screen
        Sign.PhoneUse -> R.string.settings_apps
        Sign.Steps -> R.string.settings_steps
        Sign.Movement -> R.string.settings_movement
        Sign.Charger -> R.string.settings_charger
        Sign.Music -> R.string.settings_music
    }

@Composable
private fun Sign.subtitle(signals: WakeSignalSettings): String = when (this) {
    Sign.ScreenTime -> signals.screenTimeGoal.toMinutes().toInt()
        .let { pluralStringResource(R.plurals.settings_screen_goal, it, it) }
    Sign.PhoneUse -> pluralStringResource(R.plurals.settings_apps_goal, signals.appsOpenedGoal, signals.appsOpenedGoal)
    Sign.Steps -> pluralStringResource(R.plurals.settings_steps_goal, signals.stepsGoal, signals.stepsGoal)
    Sign.Movement -> signals.movementGoal.toMinutes().toInt()
        .let { pluralStringResource(R.plurals.settings_movement_goal, it, it) }
    Sign.Charger -> stringResource(R.string.settings_charger_detail)
    Sign.Music -> stringResource(R.string.settings_music_detail)
}
