package com.dawnly.app.ui.settings

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dawnly.app.R
import com.dawnly.app.data.settings.Appearance
import com.dawnly.app.data.settings.DismissMode
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.domain.wake.Sensitivity
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.PreviewFrame
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.glass.SpringIn
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState
import java.time.Duration

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    permissions: PermissionState,
    oemBattery: OemBattery?,
    onRequestPermission: (AppPermission, (Boolean) -> Unit) -> Unit,
    onOpenOemBattery: () -> Unit,
    contentPadding: PaddingValues,
    onPickWindow: () -> Unit,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value ?: return
    val learning by viewModel.learning.collectAsStateWithLifecycle()
    val latest by rememberUpdatedState(state)
    var deniedSign by remember { mutableStateOf<Sign?>(null) }
    var pickingSound by rememberSaveable { mutableStateOf(false) }
    // Kept here so it stays open after visiting the sound picker.
    var moreOpen by rememberSaveable { mutableStateOf(false) }
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
                deniedSign = deniedSign,
                onWindowChange = viewModel::setWindow,
                onPickWindow = onPickWindow,
                onSignalsChange = viewModel::setSignals,
                onSignNeedsPermission = { sign ->
                    // Steps need Physical activity, apps opened Usage access. Turn the sign on only
                    // once it's allowed.
                    val permission = if (sign == Sign.Steps) AppPermission.ActivityRecognition else AppPermission.UsageAccess
                    onRequestPermission(permission) { granted ->
                        deniedSign = sign.takeUnless { granted }
                        if (granted) latest.signals.withSign(sign, true)?.let(viewModel::setSignals)
                    }
                },
                onAppearanceChange = viewModel::setAppearance,
                onDismissModeChange = viewModel::setDismissMode,
                onAllowOverlay = { mode ->
                    onRequestPermission(AppPermission.Overlay) { granted ->
                        if (granted) viewModel.setDismissMode(mode)
                    }
                },
                onRequestPermission = { onRequestPermission(it) {} },
                onOpenOemBattery = onOpenOemBattery,
                onOpenSounds = { pickingSound = true },
                moreOpen = moreOpen,
                onToggleMore = { moreOpen = !moreOpen },
                contentPadding = contentPadding,
                learning = learning,
                onLearnChange = viewModel::setLearn,
                onResetLearning = viewModel::resetLearning,
            )
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    permissions: PermissionState,
    oemBattery: OemBattery?,
    deniedSign: Sign?,
    onWindowChange: (Duration) -> Unit,
    onPickWindow: () -> Unit,
    onSignalsChange: (WakeSignalSettings) -> Unit,
    onSignNeedsPermission: (Sign) -> Unit,
    onAppearanceChange: (Appearance) -> Unit,
    onDismissModeChange: (DismissMode) -> Unit,
    onAllowOverlay: (DismissMode) -> Unit,
    onRequestPermission: (AppPermission) -> Unit,
    onOpenOemBattery: () -> Unit,
    onOpenSounds: () -> Unit,
    moreOpen: Boolean,
    onToggleMore: () -> Unit,
    contentPadding: PaddingValues,
    learning: LearningUi = LearningUi.Start,
    onLearnChange: (Boolean) -> Unit = {},
    onResetLearning: () -> Unit = {},
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
        SpringIn(order = 0) { WindowSection(state.window, onWindowChange, onPickWindow) }
        Gap()
        SpringIn(order = 1) {
            DismissModeSection(
                mode = state.dismissMode,
                overlayAllowed = AppPermission.Overlay in permissions,
                fullScreenAllowed = AppPermission.FullScreen in permissions,
                onChange = onDismissModeChange,
                onAllowOverlay = onAllowOverlay,
            )
        }
        Gap()
        SpringIn(order = 2) { SoundRow(state.sound, onOpenSounds) }
        Gap()
        SpringIn(order = 3) {
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
        Gap()
        SpringIn(order = 4) {
            val waiting = waitingPermissions(permissions, state.dismissMode, state.signals)
            MoreOptionsRow(open = moreOpen, needsYou = waiting, onToggle = onToggleMore)
        }
        AnimatedVisibility(
            visible = moreOpen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Gap()
                SignalsSection(state.signals, deniedSign, onSignalsChange, onSignNeedsPermission)
                Gap()
                Column {
                    SectionLabel(stringResource(R.string.settings_sensitivity_label))
                    val levels = Sensitivity.entries
                    GlassSegmented(
                        options = levels.map { stringResource(it.label) },
                        selected = levels.indexOf(Sensitivity.of(state.signals)),
                        onSelect = { onSignalsChange(levels[it].applyTo(state.signals)) },
                    )
                }
                Gap()
                LearningSection(learning, onLearnChange, onResetLearning)
                Gap()
                PermissionsSection(permissions, state.dismissMode, state.signals, oemBattery, onRequestPermission, onOpenOemBattery)
            }
        }
        Gap()
        SpringIn(order = 5) { FeedbackRow() }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Gap() = Spacer(Modifier.height(22.dp))


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
        Appearance.Clock -> R.string.appearance_clock
    }


@Composable
private fun SettingsPreview(dark: Boolean, window: Duration, oem: OemBattery?) {
    PreviewFrame(dark) {
        SettingsContent(
            state = SettingsUiState(window, WakeSignalSettings(), Appearance.System, AlertSound.Flute),
            permissions = PermissionState(setOf(AppPermission.Notifications, AppPermission.ActivityRecognition)),
            oemBattery = oem,
            deniedSign = null,
            onPickWindow = {},
            onWindowChange = {},
            onSignalsChange = {},
            onSignNeedsPermission = {},
            onAppearanceChange = {},
            onDismissModeChange = {},
            onAllowOverlay = {},
            onRequestPermission = {},
            onOpenOemBattery = {},
            onOpenSounds = {},
            moreOpen = true,
            onToggleMore = {},
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
