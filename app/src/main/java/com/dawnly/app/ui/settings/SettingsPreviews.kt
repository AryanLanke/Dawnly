package com.dawnly.app.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dawnly.app.data.settings.Appearance
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.ui.glass.PreviewFrame
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState
import java.time.Duration

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
