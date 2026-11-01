package com.dawnly.app.ui.onboarding

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.IconTile
import com.dawnly.app.ui.glass.PreviewFrame
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.glass.TextAction
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.LocalReduceMotion

/** The permissions to walk through: the ones this Android has and the user hasn't granted yet. */
fun onboardingSteps(permissions: PermissionState, oemBattery: OemBattery?): List<AppPermission> =
    AppPermission.entries.filter { permission ->
        val needed = permission.isApplicable && permission !in permissions
        // Brand phones need extra battery steps even when Android's own exemption is granted.
        needed || (permission == AppPermission.Battery && oemBattery != null)
    }

/**
 * Friendly step-by-step permissions, one per screen, each saying why it's needed. Every step can be
 * skipped; Settings has the same controls later.
 */
@Composable
fun OnboardingScreen(
    steps: List<AppPermission>,
    permissions: PermissionState,
    oemBattery: OemBattery?,
    onAllow: (AppPermission, onDone: () -> Unit) -> Unit,
    onOpenOemBattery: () -> Unit,
    onFinished: () -> Unit,
    contentPadding: PaddingValues,
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    fun next() {
        if (index < steps.lastIndex) index++ else onFinished()
    }
    BackHandler(enabled = index > 0) { index-- }

    val reduceMotion = LocalReduceMotion.current
    // Sits at the bottom like the mockup, and scrolls if a small screen or big text needs it.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (steps.size > 1) StepDots(count = steps.size, current = index)
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        val direction = if (targetState > initialState) 1 else -1
                        val slide = spring<IntOffset>(dampingRatio = 0.85f, stiffness = 300f)
                        (slideInHorizontally(slide) { it / 3 * direction } + fadeIn()) togetherWith
                            (slideOutHorizontally(slide) { -it / 3 * direction } + fadeOut())
                    }
                },
                label = "step",
            ) { shown ->
                val permission = steps.getOrNull(shown)
                if (permission == null) {
                    // Nothing left to ask for: just say hello.
                    StepBody(
                        title = stringResource(R.string.onboarding_welcome_title),
                        lead = stringResource(R.string.onboarding_welcome_lead),
                        card = null,
                        cta = stringResource(R.string.onboarding_get_started),
                        onCta = onFinished,
                        onSkip = null,
                    )
                } else {
                    val copy = permission.copy
                    val granted = permission in permissions
                    StepBody(
                        title = stringResource(if (shown == 0) R.string.onboarding_welcome_title else copy.title),
                        lead = stringResource(if (shown == 0) R.string.onboarding_welcome_lead else copy.lead),
                        card = {
                            PermissionCard(copy.icon, stringResource(copy.name), stringResource(copy.why))
                            if (permission == AppPermission.Battery && oemBattery != null) {
                                OemCard(oemBattery, onOpenOemBattery)
                            }
                        },
                        cta = stringResource(if (granted) R.string.onboarding_continue else copy.cta),
                        onCta = { if (granted) next() else onAllow(permission) { next() } },
                        onSkip = if (granted) null else ::next,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepBody(
    title: String,
    lead: String,
    card: (@Composable () -> Unit)?,
    cta: String,
    onCta: () -> Unit,
    onSkip: (() -> Unit)?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            lead,
            style = MaterialTheme.typography.bodyMedium,
            color = DawnlyTheme.colors.textSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 22.dp),
        )
        if (card != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                card()
            }
        }
        PrimaryButton(cta, onCta)
        // Keeps the layout the same height whether or not the step can be skipped.
        Box(Modifier.height(56.dp).padding(top = 6.dp), contentAlignment = Alignment.Center) {
            if (onSkip != null) TextAction(stringResource(R.string.onboarding_not_now), onSkip)
        }
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, name: String, why: String) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            IconTile(icon)
            Column(Modifier.padding(start = 14.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    why,
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnlyTheme.colors.textSoft,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun OemCard(oem: OemBattery, onOpen: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(oem.hint),
                style = MaterialTheme.typography.bodySmall,
                color = DawnlyTheme.colors.textSoft,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            AccentPill(stringResource(R.string.common_open), onOpen)
        }
    }
}

@Composable
private fun StepDots(count: Int, current: Int) {
    val description = stringResource(R.string.onboarding_step, current + 1, count)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 20.dp).semantics { contentDescription = description },
    ) {
        repeat(count) { i ->
            val color by animateColorAsState(
                if (i == current) DawnlyTheme.colors.text else DawnlyTheme.colors.track,
                label = "dot",
            )
            Box(Modifier.width(24.dp).height(5.dp).background(color, CircleShape))
        }
    }
}

/** The words and icon for each permission step. */
private class StepCopy(
    val icon: ImageVector,
    @StringRes val name: Int,
    @StringRes val why: Int,
    @StringRes val title: Int,
    @StringRes val lead: Int,
    @StringRes val cta: Int,
)

private val AppPermission.copy: StepCopy
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
        AppPermission.Battery -> StepCopy(
            GlassIcons.Battery, R.string.perm_battery, R.string.perm_battery_why,
            R.string.onboarding_battery_title, R.string.onboarding_battery_lead, R.string.onboarding_allow_battery,
        )
    }

@Composable
private fun OnboardingPreview(dark: Boolean, oem: OemBattery?) {
    PreviewFrame(dark) {
        OnboardingScreen(
            steps = AppPermission.entries,
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
