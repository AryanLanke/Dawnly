package com.dawnly.app.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyMotion
import com.dawnly.app.ui.theme.DawnlyTheme

/** The one main action on a screen: solid accent, full width. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DawnlyTheme.colors
    val interactions = remember { MutableInteractionSource() }
    val scale = pressScale(interactions)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scaled(scale)
            .clip(GlassShapes.Button)
            .background(colors.accent)
            .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = colors.onAccent)
    }
}

/** A secondary action: strong glass. */
@Composable
fun GlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = GlassShapes.SmallButton,
        strong = true,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(vertical = 15.dp),
        )
    }
}

/** A small accent pill for inline actions such as "Allow". */
@Composable
fun AccentPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DawnlyTheme.colors
    val interactions = remember { MutableInteractionSource() }
    val scale = pressScale(interactions)
    Box(
        modifier = modifier
            .scaled(scale)
            .clip(GlassShapes.Pill)
            .background(colors.accent)
            .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = colors.onAccent)
    }
}

/** A quiet text action, like "Not now". */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = DawnlyTheme.colors.textSoft,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(GlassShapes.SmallButton)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

/** How far a pressed button sinks. */
@Composable
internal fun pressScale(interactions: MutableInteractionSource): State<Float> {
    val pressed by interactions.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) 0.97f else 1f, DawnlyMotion.bouncy(), label = "press")
}

private fun Modifier.scaled(scale: State<Float>): Modifier = graphicsLayer {
    scaleX = scale.value
    scaleY = scale.value
}
