package com.dawnly.app.ui.settings

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dawnly.app.R
import com.dawnly.app.data.settings.Appearance
import com.dawnly.app.data.sound.AlertSound
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
import com.dawnly.app.ui.glass.PreviewFrame
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.glass.SpringIn
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Duration

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    permissions: PermissionState,
    oemBattery: OemBattery?,
    onRequestPermission: (AppPermission, (Boolean) -> Unit) -> Unit,
    onOpenOemBattery: () -> Unit,
    contentPadding: PaddingValues,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value ?: return
    val latest by rememberUpdatedState(state)
    var stepsDenied by remember { mutableStateOf(false) }
    var pickingSound by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = pickingSound) { pickingSound = false }
    val preview = rememberSoundPreview()

    AnimatedContent(
        targetState = pickingSound,
        transitionSpec = {
            val direction = if (targetState) 1 else -1
            (slideInHorizontally { it / 6 * direction } + fadeIn()) togetherWith
                (slideOutHorizontally { -it / 6 * direction } + fadeOut())
        },
        label = "settingsPage",
    ) { picking ->
        if (picking) {
            SoundPicker(
                selected = state.sound,
                silenced = remember { preview.isSilenced },
                onSelect = { sound ->
                    viewModel.setSound(sound)
                    preview.play(sound)
                },
                onBack = {
                    preview.stop()
                    pickingSound = false
                },
                contentPadding = contentPadding,
            )
        } else {
            SettingsContent(
                state = state,
                permissions = permissions,
                oemBattery = oemBattery,
                stepsDenied = stepsDenied,
                onWindowChange = viewModel::setWindow,
                onSignalsChange = viewModel::setSignals,
                onStepsOn = {
                    // Steps need Physical activity. Turn the sign on only once it's allowed.
                    onRequestPermission(AppPermission.ActivityRecognition) { granted ->
                        stepsDenied = !granted
                        if (granted) viewModel.setSignals(latest.signals.copy(useSteps = true))
                    }
                },
                onAppearanceChange = viewModel::setAppearance,
                onRequestPermission = { onRequestPermission(it) {} },
                onOpenOemBattery = onOpenOemBattery,
                onOpenSounds = { pickingSound = true },
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    permissions: PermissionState,
    oemBattery: OemBattery?,
    stepsDenied: Boolean,
    onWindowChange: (Duration) -> Unit,
    onSignalsChange: (WakeSignalSettings) -> Unit,
    onStepsOn: () -> Unit,
    onAppearanceChange: (Appearance) -> Unit,
    onRequestPermission: (AppPermission) -> Unit,
    onOpenOemBattery: () -> Unit,
    onOpenSounds: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 18.dp),
        )
        SpringIn(order = 0) { WindowSection(state.window, onWindowChange) }
        Gap()
        SpringIn(order = 1) { SignalsSection(state.signals, stepsDenied, onSignalsChange, onStepsOn) }
        Gap()
        SpringIn(order = 2) {
            Column {
                SectionLabel(stringResource(R.string.settings_sensitivity_label))
                val levels = Sensitivity.entries
                GlassSegmented(
                    options = levels.map { stringResource(it.label) },
                    selected = levels.indexOf(Sensitivity.of(state.signals)),
                    onSelect = { onSignalsChange(levels[it].applyTo(state.signals)) },
                )
            }
        }
        Gap()
        SpringIn(order = 3) { SoundRow(state.sound, onOpenSounds) }
        Gap()
        SpringIn(order = 4) { PermissionsSection(permissions, oemBattery, onRequestPermission, onOpenOemBattery) }
        Gap()
        SpringIn(order = 5) {
            Column {
                SectionLabel(stringResource(R.string.settings_appearance_label))
                val modes = Appearance.entries
                GlassSegmented(
                    options = modes.map { stringResource(it.label) },
                    selected = modes.indexOf(state.appearance),
                    onSelect = { onAppearanceChange(modes[it]) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Gap() = Spacer(Modifier.height(22.dp))

@Composable
private fun WindowSection(window: Duration, onChange: (Duration) -> Unit) {
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
private fun SignalsSection(
    signals: WakeSignalSettings,
    stepsDenied: Boolean,
    onChange: (WakeSignalSettings) -> Unit,
    onStepsOn: () -> Unit,
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
            GlassRow(title = stringResource(R.string.settings_unlock)) {
                GlassSwitch(signals.useUnlock, { toggle(Sign.Unlock, it) })
            }
            GlassRow(
                title = stringResource(R.string.settings_screen),
                subtitle = signals.screenTimeGoal.toMinutes().toInt()
                    .let { pluralStringResource(R.plurals.settings_screen_goal, it, it) },
                divider = true,
            ) {
                GlassSwitch(signals.useScreenTime, { toggle(Sign.ScreenTime, it) })
            }
            GlassRow(
                title = stringResource(R.string.settings_steps),
                subtitle = pluralStringResource(R.plurals.settings_steps_goal, signals.stepsGoal, signals.stepsGoal),
                divider = true,
            ) {
                GlassSwitch(signals.useSteps, { on -> if (on) onStepsOn() else toggle(Sign.Steps, false) })
            }
        }
        val hint = when {
            lastOneRefused -> stringResource(R.string.settings_signals_min)
            stepsDenied && !signals.useSteps -> stringResource(R.string.settings_steps_denied)
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

@Composable
private fun PermissionsSection(
    permissions: PermissionState,
    oemBattery: OemBattery?,
    onRequest: (AppPermission) -> Unit,
    onOpenOemBattery: () -> Unit,
) {
    val shown = AppPermission.entries.filter { it.isApplicable }
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

private val Sensitivity.label
    get() = when (this) {
        Sensitivity.Low -> R.string.sensitivity_low
        Sensitivity.Medium -> R.string.sensitivity_medium
        Sensitivity.High -> R.string.sensitivity_high
    }

private val Appearance.label
    get() = when (this) {
        Appearance.Light -> R.string.appearance_light
        Appearance.Dark -> R.string.appearance_dark
        Appearance.System -> R.string.appearance_system
    }

internal val AppPermission.label
    get() = when (this) {
        AppPermission.Notifications -> R.string.perm_notifications
        AppPermission.ExactAlarms -> R.string.perm_exact_alarms
        AppPermission.ActivityRecognition -> R.string.perm_activity
        AppPermission.Battery -> R.string.perm_battery
    }

@Composable
private fun SettingsPreview(dark: Boolean, window: Duration, oem: OemBattery?) {
    PreviewFrame(dark) {
        SettingsContent(
            state = SettingsUiState(window, WakeSignalSettings(), Appearance.System, AlertSound.Flute),
            permissions = PermissionState(setOf(AppPermission.Notifications, AppPermission.ActivityRecognition)),
            oemBattery = oem,
            stepsDenied = false,
            onWindowChange = {},
            onSignalsChange = {},
            onStepsOn = {},
            onAppearanceChange = {},
            onRequestPermission = {},
            onOpenOemBattery = {},
            onOpenSounds = {},
            contentPadding = PaddingValues(top = 40.dp, bottom = 100.dp),
        )
    }
}

@Preview(name = "Settings, light", showSystemUi = true)
@Composable
private fun SettingsLightPreview() = SettingsPreview(dark = false, window = Duration.ofHours(1), oem = null)

@Preview(name = "Settings, dark, custom, Samsung", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsDarkPreview() =
    SettingsPreview(dark = true, window = Duration.ofMinutes(45), oem = OemBattery.Samsung)
