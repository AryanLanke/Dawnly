package com.dawnly.app.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyTheme

/** The blue tick that marks a sign as seen, or the chosen item in a list. */
@Composable
fun CheckBadge(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Icon(GlassIcons.Check, contentDescription = null, tint = DawnlyTheme.colors.tick, modifier = modifier.size(size))
}

/** A glass tile holding an icon, as in the onboarding permission card. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier = modifier.size(40.dp),
        shape = GlassShapes.Icon,
        strong = true,
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
    }
}

/** The small soft label above a group, like "Signs you're awake". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = DawnlyTheme.colors.textSoft,
        modifier = modifier.padding(start = 8.dp, bottom = 8.dp),
    )
}

/**
 * A glass card of rows with thin dividers between them. Rows reach the card's edges, so a pressed
 * row lights up edge to edge; anything else put in here brings its own [ROW_INSET].
 */
@Composable
fun GlassList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.clip(GlassShapes.Card), content = content)
    }
}

/** How far a [GlassList]'s rows keep their words and dividers in from the card's edges. */
val ROW_INSET = 18.dp

/** One row of a [GlassList]: a title, an optional soft subtitle, and something on the end. */
@Composable
fun GlassRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    divider: Boolean = false,
    end: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier) {
        if (divider) {
            Box(Modifier.padding(horizontal = ROW_INSET).fillMaxWidth().height(1.dp).background(DawnlyTheme.colors.line))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_INSET, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = DawnlyTheme.colors.textSoft,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            end()
        }
    }
}
