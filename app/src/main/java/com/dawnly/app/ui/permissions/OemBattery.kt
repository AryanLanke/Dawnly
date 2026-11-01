package com.dawnly.app.ui.permissions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.dawnly.app.R

/**
 * Phone brands that stop background apps more aggressively than stock Android, and where to send
 * the user to stop that. From dontkillmyapp.com. The settings screens are private to each brand
 * and move between OS versions, so each has fallbacks ending in Dawnly's own app info page.
 */
enum class OemBattery(@StringRes val label: Int, @StringRes val hint: Int) {
    Samsung(R.string.oem_samsung, R.string.oem_samsung_hint),
    Xiaomi(R.string.oem_xiaomi, R.string.oem_xiaomi_hint),
    ;

    /** Settings screens to try, best first. */
    fun intents(context: Context): List<Intent> {
        val brandScreens = when (this) {
            // Device care > Battery, where "Background usage limits" lives.
            Samsung -> listOf(
                component("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                component("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            )
            // Autostart first; it's the switch that most often stops alarms on MIUI and HyperOS.
            Xiaomi -> listOf(
                component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                component("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
                    .putExtra("package_name", context.packageName)
                    .putExtra("package_label", context.getString(R.string.app_name)),
            )
        }
        return brandScreens + AppPermission.appDetails("package:${context.packageName}".toUri())
    }

    companion object {
        /** The brand of this phone, if it needs extra battery steps. [manufacturer] is `Build.MANUFACTURER`. */
        fun detect(manufacturer: String): OemBattery? = when (manufacturer.trim().lowercase()) {
            "samsung" -> Samsung
            // Redmi and POCO phones report "Xiaomi", but some ROMs report the sub-brand.
            "xiaomi", "redmi", "poco" -> Xiaomi
            else -> null
        }

        private fun component(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))
    }
}
