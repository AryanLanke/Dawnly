package com.dawnly.app.ui

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dawnly.app.data.settings.Appearance
import com.dawnly.app.ui.glass.FlowingBackground
import com.dawnly.app.ui.home.HomeViewModel
import com.dawnly.app.ui.onboarding.OnboardingScreen
import com.dawnly.app.ui.onboarding.onboardingSteps
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionsController
import com.dawnly.app.ui.permissions.RestrictedSettingsHelp
import com.dawnly.app.ui.permissions.rememberPermissionsController
import com.dawnly.app.ui.settings.SettingsViewModel
import com.dawnly.app.ui.settings.Sign
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.clockSaysNight
import com.dawnly.app.ui.theme.prefersReducedMotion

/** The whole app: theme, the flowing background, then onboarding or the two tabs. */
@Composable
fun DawnlyRoot(main: MainViewModel, home: HomeViewModel, settings: SettingsViewModel) {
    val appearance by main.appearance.collectAsStateWithLifecycle()
    val onboardingDone by main.onboardingDone.collectAsStateWithLifecycle()
    val dark = when (appearance) {
        Appearance.Light -> false
        Appearance.Dark -> true
        Appearance.System -> isSystemInDarkTheme()
        // Clock is the default, so it also covers the moment before settings load.
        Appearance.Clock, null -> clockSaysNight()
    }
    SystemBarsFollow(dark)

    // Switching light and dark sweeps the new look in from a corner (light from the top-right, dark from the bottom-left).
    val context = LocalContext.current
    val still = remember(context) { context.prefersReducedMotion() }
    ThemeSweep(dark = dark, still = still) { shownDark ->
        DawnlyTheme(darkTheme = shownDark) {
            Box(Modifier.fillMaxSize()) {
                FlowingBackground()
                val done = onboardingDone ?: return@Box
                val permissions = rememberPermissionsController()
                val oemBattery = remember { OemBattery.detect(Build.MANUFACTURER) }
                AnimatedContent(
                    targetState = done,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut() },
                    label = "root",
                ) { isDone ->
                    if (isDone) {
                        MainTabs(home, settings, permissions, oemBattery)
                    } else {
                        Onboarding(main, permissions, oemBattery)
                    }
                }
                permissions.restrictedHelp?.let { locked ->
                    RestrictedSettingsHelp(locked, onOpenAppInfo = permissions::openAppInfo, onClose = permissions::closeRestrictedHelp)
                }
            }
        }
    }
}

@Composable
private fun Onboarding(main: MainViewModel, permissions: PermissionsController, oemBattery: OemBattery?) {
    // Decided once, so granting a permission doesn't make its step vanish mid-animation.
    val stepNames = rememberSaveable {
        onboardingSteps(permissions.state, oemBattery).joinToString(",") { it.name }
    }
    val steps = remember(stepNames) {
        stepNames.split(',').filter { it.isNotEmpty() }.map(AppPermission::valueOf)
    }
    OnboardingScreen(
        steps = steps,
        permissions = permissions.state,
        oemBattery = oemBattery,
        onAllow = { permission, onDone ->
            permissions.request(permission) { granted ->
                if (granted && permission == AppPermission.ActivityRecognition) main.useSign(Sign.Steps)
                if (granted && permission == AppPermission.UsageAccess) main.useSign(Sign.PhoneUse)
                onDone()
            }
        },
        onOpenOemBattery = { oemBattery?.let(permissions::openBatterySettings) },
        onFinished = main::completeOnboarding,
        contentPadding = screenPadding(bottomExtra = 0.dp),
    )
}
