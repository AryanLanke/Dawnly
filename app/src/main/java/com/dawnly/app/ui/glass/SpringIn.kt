package com.dawnly.app.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay

/**
 * Springs [content] in when it first appears: it rises a little, grows to full size, and fades in.
 * [order] staggers siblings so a screen's cards land one after another.
 */
@Composable
fun SpringIn(order: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(order * 70L)
            progress.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 220f))
        }
    }
    Box(
        modifier.graphicsLayer {
            val p = progress.value
            alpha = p.coerceIn(0f, 1f)
            translationY = (1f - p) * 28.dp.toPx()
            val scale = 0.94f + 0.06f * p
            scaleX = scale
            scaleY = scale
        },
    ) {
        content()
    }
}
