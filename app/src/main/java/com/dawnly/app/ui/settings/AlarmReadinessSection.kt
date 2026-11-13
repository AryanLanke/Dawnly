package com.dawnly.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.dawnly.app.R
import com.dawnly.app.domain.readiness.AlarmReadiness
import com.dawnly.app.domain.readiness.ReadinessInput
import com.dawnly.app.domain.readiness.ReadinessItem
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.CheckBadge
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState

/**
 * A calm "will Dawnly actually ring?" summary. It reuses the same permission checks as the rest of
 * Settings, adds the alarm-volume check, and points each gap at the existing fix flow. Full-screen
 * takeover is shown as a note, not a failure: without it the alarm still rings as a notification.
 */
@Composable
internal fun AlarmReadinessSection(
    permissions: PermissionState,
    oemBattery: OemBattery?,
    onRequestPermission: (AppPermission) -> Unit,
    onOpenOemBattery: () -> Unit,
    onOpenTestLab: () -> Unit,
) {
    val context = LocalContext.current
    var alarmAudible by remember { mutableStateOf(isAlarmAudible(context)) }
    LifecycleResumeEffect(Unit) {
        alarmAudible = isAlarmAudible(context)
        onPauseOrDispose {}
    }
    val result = AlarmReadiness.evaluate(
        ReadinessInput(
            exactAlarms = AppPermission.ExactAlarms in permissions,
            battery = AppPermission.Battery in permissions,
            notifications = AppPermission.Notifications in permissions,
            alarmAudible = alarmAudible,
            fullScreen = AppPermission.FullScreen in permissions,
        ),
    )
    Column {
        SectionLabel(stringResource(R.string.readiness_label))
        GlassList {
            GlassRow(
                title = stringResource(if (result.ready) R.string.readiness_ready_title else R.string.readiness_attention_title),
                subtitle = stringResource(if (result.ready) R.string.readiness_ready_detail else R.string.readiness_attention_detail),
            ) {
                if (result.ready) CheckBadge()
            }
            result.needsAttention.forEach { item ->
                ReadinessFixRow(item, oemBattery, onRequestPermission, onOpenOemBattery)
            }
            if (result.fullScreenOff) {
                GlassRow(
                    title = stringResource(R.string.perm_full_screen),
                    subtitle = stringResource(R.string.readiness_fullscreen_detail),
                    divider = true,
                ) {
                    AccentPill(stringResource(R.string.common_allow), { onRequestPermission(AppPermission.FullScreen) })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassButton(stringResource(R.string.readiness_test), onOpenTestLab)
    }
}

@Composable
private fun ReadinessFixRow(
    item: ReadinessItem,
    oemBattery: OemBattery?,
    onRequestPermission: (AppPermission) -> Unit,
    onOpenOemBattery: () -> Unit,
) {
    val context = LocalContext.current
    when (item) {
        ReadinessItem.ExactAlarms -> GlassRow(
            title = stringResource(R.string.perm_exact_alarms),
            subtitle = stringResource(R.string.readiness_exact_detail),
            divider = true,
        ) {
            AccentPill(stringResource(R.string.common_allow), { onRequestPermission(AppPermission.ExactAlarms) })
        }
        ReadinessItem.Battery -> GlassRow(
            title = stringResource(oemBattery?.label ?: R.string.perm_battery),
            subtitle = stringResource(oemBattery?.hint ?: R.string.readiness_battery_detail),
            divider = true,
        ) {
            if (oemBattery != null) {
                AccentPill(stringResource(R.string.common_open), onOpenOemBattery)
            } else {
                AccentPill(stringResource(R.string.common_allow), { onRequestPermission(AppPermission.Battery) })
            }
        }
        ReadinessItem.Notifications -> GlassRow(
            title = stringResource(R.string.perm_notifications),
            subtitle = stringResource(R.string.readiness_notifications_detail),
            divider = true,
        ) {
            AccentPill(stringResource(R.string.common_allow), { onRequestPermission(AppPermission.Notifications) })
        }
        ReadinessItem.AlarmVolume -> GlassRow(
            title = stringResource(R.string.readiness_volume_title),
            subtitle = stringResource(R.string.readiness_volume_detail),
            divider = true,
        ) {
            AccentPill(stringResource(R.string.common_open), { openSoundSettings(context) })
        }
    }
}

private fun isAlarmAudible(context: Context): Boolean {
    val audio = context.getSystemService(AudioManager::class.java) ?: return true
    return audio.getStreamVolume(AudioManager.STREAM_ALARM) > 0
}

private fun openSoundSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // No sound screen on this phone; App info is the closest place to check the volume.
        context.startActivity(AppPermission.appDetails(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}