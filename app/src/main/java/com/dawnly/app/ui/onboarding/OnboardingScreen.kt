package com.dawnly.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState
import com.dawnly.app.ui.theme.LocalReduceMotion

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
