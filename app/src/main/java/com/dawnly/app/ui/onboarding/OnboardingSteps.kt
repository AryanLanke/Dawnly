package com.dawnly.app.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.dawnly.app.R
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState

/** The permissions to walk through: the ones this Android has and the user hasn't granted yet. */
fun onboardingSteps(permissions: PermissionState, oemBattery: OemBattery?): List<AppPermission> =
    AppPermission.entries.filter { it.askedUpFront }.filter { permission ->
        val needed = permission.isApplicable && permission !in permissions
        // Brand phones need extra battery steps even when Android's own exemption is granted.
        needed || (permission == AppPermission.Battery && oemBattery != null)
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
        AppPermission.ActivityRecognition -> StepCopy(
            GlassIcons.Steps, R.string.perm_activity, R.string.perm_activity_why,
            R.string.onboarding_activity_title, R.string.onboarding_activity_lead, R.string.onboarding_allow_activity,
        )
        AppPermission.UsageAccess -> StepCopy(
            GlassIcons.Phone, R.string.perm_usage, R.string.perm_usage_why,
            R.string.onboarding_usage_title, R.string.onboarding_usage_lead, R.string.onboarding_allow_usage,
        )
        AppPermission.Battery -> StepCopy(
            GlassIcons.Battery, R.string.perm_battery, R.string.perm_battery_why,
            R.string.onboarding_battery_title, R.string.onboarding_battery_lead, R.string.onboarding_allow_battery,
        )
        AppPermission.Overlay, AppPermission.FullScreen -> error("$this is asked from Settings only")
    }
