package com.dawnly.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dawnly.app.R
import com.dawnly.app.data.settings.DismissMode
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState
import com.dawnly.app.ui.theme.DawnlyTheme

@Composable
internal fun PermissionsSection(
    permissions: PermissionState,
    dismissMode: DismissMode,
    signals: WakeSignalSettings,
    oemBattery: OemBattery?,
    onRequest: (AppPermission) -> Unit,
    onOpenOemBattery: () -> Unit,
) {
    val shown = shownPermissions(dismissMode, signals)
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
        AppPermission.FullScreen -> R.string.perm_full_screen
    }
