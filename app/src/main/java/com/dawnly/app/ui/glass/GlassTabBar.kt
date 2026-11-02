package com.dawnly.app.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyTheme
import kotlin.math.roundToInt

/**
 * The floating glass pill at the bottom. Tabs share one width; the bright pill sits at [position],
 * which follows the pages as they're swiped (0 is the first tab, 0.5 halfway to the second). It's
 * read only while placing the pill, so swiping doesn't recompose the bar. Press and hold to feel it,
 * then slide to another tab: the pill follows the finger and the page changes as it crosses over.
 */
@Composable
fun GlassTabBar(
    tabs: List<String>,
    selected: Int,
    position: () -> Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slide = rememberHoldAndSlide(tabs.size, onHover = onSelect, onRelease = { index, _ -> onSelect(index) })
    var width by remember { mutableIntStateOf(0) }
    val tabWidth = if (tabs.isEmpty()) 0f else width / tabs.size.toFloat()
    val tabWidthDp = with(LocalDensity.current) { tabWidth.toDp() }

    GlassSurface(modifier = modifier, shape = GlassShapes.Pill) {
        Box(Modifier.padding(5.dp).width(IntrinsicSize.Max).onSizeChanged { width = it.width }.then(slide.modifier)) {
            if (width > 0) {
                // matchParentSize: the pill takes the tabs' height without making the bar taller.
                Box(Modifier.matchParentSize()) {
                    Box(
                        Modifier
                            .offset { IntOffset(((slide.scrub ?: position()) * tabWidth).roundToInt(), 0) }
                            .width(tabWidthDp)
                            .fillMaxHeight()
                            .outerShadow(GlassShapes.Pill, DawnlyTheme.colors.selectedShadow)
                            .clip(GlassShapes.Pill)
                            .background(DawnlyTheme.colors.glassStrong),
                    )
                }
            }
            Row(Modifier.selectableGroup()) {
                tabs.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 104.dp)
                            .clip(GlassShapes.Pill)
                            .selectable(selected = index == selected, role = Role.Tab, onClick = { onSelect(index) })
                            .padding(horizontal = 22.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}
