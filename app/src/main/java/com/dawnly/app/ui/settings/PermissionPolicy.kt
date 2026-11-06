package com.dawnly.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.dawnly.app.data.settings.DismissMode
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.PermissionState

/**
 * The permissions Settings lists. Usage access only matters with the apps-opened sign on, and
 * Display over other apps only when Dawnly dismisses by itself, e.g. if either was taken back since.
 */
internal fun shownPermissions(dismissMode: DismissMode, signals: WakeSignalSettings): List<AppPermission> =
    AppPermission.entries.filter {
        it.isApplicable && when (it) {
            AppPermission.UsageAccess -> signals.usePhoneUse
            AppPermission.Overlay, AppPermission.FullScreen -> dismissMode != DismissMode.Ask
            else -> it.askedUpFront
        }
    }

/**
 * The shown permissions that still need the user. Display over other apps is optional while
 * full-screen notifications are allowed: auto-dismiss works without it, with one tap while the
 * phone is in use.
 */
internal fun waitingPermissions(permissions: PermissionState, dismissMode: DismissMode, signals: WakeSignalSettings): Int =
    shownPermissions(dismissMode, signals).count { permission ->
        permission !in permissions &&
            !(permission == AppPermission.Overlay && AppPermission.FullScreen in permissions)
    }
