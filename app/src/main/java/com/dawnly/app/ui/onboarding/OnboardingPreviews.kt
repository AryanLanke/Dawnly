package com.dawnly.app.ui.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.glass.PreviewFrame
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState

@Composable
private fun OnboardingPreview(dark: Boolean, oem: OemBattery?) {
    PreviewFrame(dark) {
        OnboardingScreen(
            steps = AppPermission.entries.filter { it.askedUpFront },
            permissions = PermissionState(emptySet()),
            oemBattery = oem,
            onAllow = { _, _ -> },
            onOpenOemBattery = {},
            onFinished = {},
            contentPadding = PaddingValues(top = 40.dp, bottom = 24.dp),
        )
    }
}

@Preview(name = "Onboarding, light", showSystemUi = true)
@Composable
private fun OnboardingLightPreview() = OnboardingPreview(dark = false, oem = null)

@Preview(name = "Onboarding, dark", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingDarkPreview() = OnboardingPreview(dark = true, oem = OemBattery.Xiaomi)

@Preview(name = "Step dots", widthDp = 200, heightDp = 40)
@Composable
private fun DotsPreview() = PreviewFrame(dark = false) {
    Box(Modifier.size(200.dp, 40.dp), contentAlignment = Alignment.Center) { StepDots(4, 1) }
}
