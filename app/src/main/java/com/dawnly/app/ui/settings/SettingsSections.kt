package com.dawnly.app.ui.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.domain.watch.WatchWindow
import com.dawnly.app.ui.format.formatDuration
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.GlassStepper
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.GlassSwitch
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Duration

// The sections of the Settings screen, in the order SettingsContent shows them.

@Composable
internal fun WindowSection(window: Duration, onChange: (Duration) -> Unit) {
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
                if (!customOpen) onChange(presets[index])
            },
        )
        AnimatedVisibility(
            visible = custom,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            GlassSurface(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                GlassStepper(
                    value = formatDuration(window),
                    onDecrease = { onChange(window - WatchWindow.STEP) },
                    onIncrease = { onChange(window + WatchWindow.STEP) },
                    decreaseEnabled = window > WatchWindow.MIN,
                    increaseEnabled = window < WatchWindow.MAX,
                    decreaseLabel = stringResource(R.string.settings_shorter),
                    increaseLabel = stringResource(R.string.settings_longer),
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

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
    Sign.Music -> stringResource(R.string.settings_music_detail)
}

/**
 * Off by default. Turning it on without "Display over other apps" first says why it's needed,
 * with one tap to allow it; the switch turns on once it's allowed.
 */
@Composable
internal fun AutoDismissSection(
    on: Boolean,
    overlayAllowed: Boolean,
    onChange: (Boolean) -> Unit,
    onAllowOverlay: () -> Unit,
) {
    var asking by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(on) { if (on) asking = false }
    GlassList {
        GlassRow(
            title = stringResource(R.string.settings_auto_dismiss),
            subtitle = stringResource(R.string.settings_auto_dismiss_detail),
        ) {
            GlassSwitch(on, { wanted ->
                when {
                    !wanted -> {
                        asking = false
                        onChange(false)
                    }
                    overlayAllowed -> onChange(true)
                    else -> asking = true
                }
            })
        }
        AnimatedVisibility(visible = asking && !on) {
            Column(Modifier.padding(bottom = 14.dp)) {
                // Android 11+ ignores the package Uri and opens the list of all apps.
                val why = stringResource(R.string.settings_auto_dismiss_why)
                val findDawnly = stringResource(R.string.settings_auto_dismiss_find)
                Text(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "$why $findDawnly" else why,
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnlyTheme.colors.textSoft,
                )
                AccentPill(stringResource(R.string.common_allow), onAllowOverlay, Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
internal fun PermissionsSection(
    permissions: PermissionState,
    autoDismiss: Boolean,
    oemBattery: OemBattery?,
    onRequest: (AppPermission) -> Unit,
    onOpenOemBattery: () -> Unit,
) {
    // Display over other apps only matters with auto-dismiss on, e.g. if it was taken back since.
    val shown = AppPermission.entries.filter {
        it.isApplicable && (it.askedUpFront || (it == AppPermission.Overlay && autoDismiss))
    }
    Column {
        SectionLabel(stringResource(R.string.settings_permissions_label))
        GlassList {
            shown.forEachIndexed { index, permission ->
                GlassRow(title = stringResource(permission.label), divider = index > 0) {
                    if (permission in permissions) {
                        Text(
                            stringResource(R.string.common_allowed),
                            style = MaterialTheme.typography.bodySmall,
                            color = DawnlyTheme.colors.textSoft,
                        )
                    } else {
                        AccentPill(stringResource(R.string.common_allow), { onRequest(permission) })
                    }
                }
            }
            if (oemBattery != null) {
                GlassRow(
                    title = stringResource(oemBattery.label),
                    subtitle = stringResource(oemBattery.hint),
                    divider = true,
                ) {
                    AccentPill(stringResource(R.string.common_open), onOpenOemBattery)
                }
            }
        }
    }
}

internal val AppPermission.label
    get() = when (this) {
        AppPermission.Notifications -> R.string.perm_notifications
        AppPermission.ExactAlarms -> R.string.perm_exact_alarms
        AppPermission.ActivityRecognition -> R.string.perm_activity
        AppPermission.UsageAccess -> R.string.perm_usage
        AppPermission.Battery -> R.string.perm_battery
        AppPermission.Overlay -> R.string.perm_overlay
    }
