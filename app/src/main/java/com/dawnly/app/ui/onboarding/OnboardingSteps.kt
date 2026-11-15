package com.dawnly.app.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.dawnly.app.R
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState

/**
 * The few permissions to walk through: the ones this Android has and the user hasn't granted yet.
 * The battery step only on brand phones known to stop apps ([OemBattery]), where it's needed even
 * with Android's own exemption granted; elsewhere Settings lists it.
 */
fun onboardingSteps(permissions: PermissionState, oemBattery: OemBattery?): List<AppPermission> =
    AppPermission.entries.filter { it.askedUpFront }.filter { permission ->
        if (permission == AppPermission.Battery) {
            oemBattery != null
        } else {
            permission.isApplicable && permission !in permissions
        }
    }

/** The words and icon for each permission step. */
internal class StepCopy(
    val icon: ImageVector,
    @StringRes val name: Int,
    @StringRes val why: Int,
    @StringRes val title: Int,
    @StringRes val lead: Int,
    @StringRes val cta: Int,
)

internal val AppPermission.copy: StepCopy
    get() = when (this) {
        AppPermission.Notifications -> StepCopy(
            GlassIcons.Bell, R.string.perm_notifications, R.string.perm_notifications_why,
            R.string.onboarding_welcome_title, R.string.onboarding_welcome_lead, R.string.onboarding_allow_notifications,
        )
        AppPermission.ExactAlarms -> StepCopy(
            GlassIcons.AlarmClock, R.string.perm_exact_alarms, R.string.perm_exact_alarms_why,
            R.string.onboarding_exact_title, R.string.onboarding_exact_lead, R.string.onboarding_allow_exact_alarms,
        )
        AppPermission.Battery -> StepCopy(
            GlassIcons.Battery, R.string.perm_battery, R.string.perm_battery_why,
            R.string.onboarding_battery_title, R.string.onboarding_battery_lead, R.string.onboarding_allow_battery,
        )
        AppPermission.ActivityRecognition, AppPermission.UsageAccess, AppPermission.Overlay, AppPermission.FullScreen ->
            error("$this is asked from Settings only")
    }
