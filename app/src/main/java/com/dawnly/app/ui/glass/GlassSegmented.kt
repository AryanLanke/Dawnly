package com.dawnly.app.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyMotion
import com.dawnly.app.ui.theme.DawnlyTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Glass segmented control. The selected segment is a brighter glass pill that springs between
 * options. [selected] may be -1 when nothing matches.
 */
@Composable
fun GlassSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DawnlyTheme.colors
    val haptic = rememberHaptics()
    var width by remember { mutableIntStateOf(0) }
    val segment = if (options.isEmpty()) 0f else width / options.size.toFloat()
    // The pill springs to the choice, and follows the finger while held (see HoldAndSlide).
    val pill = remember { Animatable(selected.coerceAtLeast(0).toFloat()) }
    val spring = DawnlyMotion.bouncy<Float>()
    LaunchedEffect(selected) {
        if (selected >= 0) pill.animateTo(selected.toFloat(), spring)
    }
    val scope = rememberCoroutineScope()
    val slide = rememberHoldAndSlide(options.size) { index, at ->
        scope.launch {
            pill.snapTo(at)
            pill.animateTo(index.toFloat(), spring)
        }
        if (index != selected) onSelect(index)
    }
    val indicatorAlpha by animateFloatAsState(if (selected >= 0) 1f else 0f, label = "segmentAlpha")
    val segmentDp = with(LocalDensity.current) { segment.toDp() }

    GlassSurface(modifier = modifier.fillMaxWidth(), shape = GlassShapes.Segmented) {
        Box(Modifier.padding(4.dp).onSizeChanged { width = it.width }.then(slide.modifier)) {
            if (width > 0) {
                Box(
                    Modifier
                        .offset { IntOffset(((slide.scrub ?: pill.value) * segment).roundToInt(), 0) }
                        .width(segmentDp)
                        .height(40.dp)
                        .graphicsLayer { alpha = indicatorAlpha }
                        .outerShadow(GlassShapes.Segment, colors.selectedShadow)
                        .clip(GlassShapes.Segment)
                        .background(colors.glassStrong)
                        .border(1.dp, colors.edge.copy(alpha = colors.edge.alpha * 0.6f), GlassShapes.Segment),
                )
            }
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                options.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(GlassShapes.Segment)
                            .selectable(
                                selected = index == selected,
                                role = Role.RadioButton,
                                onClick = {
                                    if (index != selected) haptic.tick()
                                    onSelect(index)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}
