package com.dawnly.app.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.theme.DawnlyTheme

/**
 * The row that opens the rest of Settings in place: signs, sensitivity, and
 * permissions. If a permission still needs allowing it says so, so nothing important hides here.
 */
@Composable
internal fun MoreOptionsRow(open: Boolean, needsYou: Int, onToggle: () -> Unit) {
    val turn by animateFloatAsState(if (open) 90f else 0f, label = "moreChevron")
    GlassList {
        GlassRow(
            title = stringResource(R.string.settings_more),
            subtitle = if (needsYou > 0) null else stringResource(R.string.settings_more_detail),
            modifier = Modifier.clickable(role = Role.Button, onClick = onToggle),
        ) {
            if (needsYou > 0) {
                Text(
                    pluralStringResource(R.plurals.settings_more_needs_you, needsYou, needsYou),
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnlyTheme.colors.tick,
                )
            }
            Icon(
                GlassIcons.Chevron,
                contentDescription = null,
                tint = DawnlyTheme.colors.textSoft,
                modifier = Modifier.padding(start = 6.dp).size(18.dp).rotate(turn),
            )
        }
    }
}
