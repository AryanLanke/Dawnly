package com.dawnly.app.ui.permissions

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.PreviewFrame
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.settings.label
import com.dawnly.app.ui.theme.DawnlyTheme

/**
 * Shown when Android's restricted settings lock kept [permission] off (see
 * [AppPermission.canBeLocked]). Android then says "Restricted setting" or that access was denied,
 * which reads as if the phone won't allow it. Dawnly can't lift the lock itself; this says it's
 * normal and how to unlock it, in four steps.
 */
@Composable
fun RestrictedSettingsHelp(permission: AppPermission, onOpenAppInfo: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            // Nearly solid, so the screen behind doesn't show through the glass card.
            .background(DawnlyTheme.colors.wall.copy(alpha = 0.92f))
            // Taps outside the card close it; taps on the card don't fall through.
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose)
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) {}) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.restricted_title, stringResource(permission.label)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.restricted_body), style = MaterialTheme.typography.bodyMedium)
                listOf(R.string.restricted_step_1, R.string.restricted_step_2, R.string.restricted_step_3, R.string.restricted_step_4).forEach {
                    Text(stringResource(it), style = MaterialTheme.typography.bodyMedium)
                }
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(stringResource(R.string.restricted_open), onOpenAppInfo, Modifier.weight(1f))
                    GlassButton(stringResource(R.string.common_cancel), onClose, Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(name = "Restricted help, light", showSystemUi = true)
@Composable
private fun RestrictedHelpLightPreview() = PreviewFrame(dark = false) {
    RestrictedSettingsHelp(AppPermission.UsageAccess, {}, {})
}

@Preview(name = "Restricted help, dark", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RestrictedHelpDarkPreview() = PreviewFrame(dark = true) {
    RestrictedSettingsHelp(AppPermission.UsageAccess, {}, {})
}
