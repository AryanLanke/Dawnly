package com.dawnly.app.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.IconTile
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.glass.TextAction
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.theme.DawnlyTheme

@Composable
internal fun StepBody(
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
internal fun PermissionCard(icon: ImageVector, name: String, why: String) {
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
internal fun OemCard(oem: OemBattery, onOpen: () -> Unit) {
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
internal fun StepDots(count: Int, current: Int) {
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
