package com.dawnly.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.theme.DawnlyTheme

// The small cards Today shows under the ring.

@Composable
internal fun PermissionWarning(title: String, body: String, onAllow: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Soft(body, Modifier.padding(top = 2.dp), align = TextAlign.Start)
            }
            AccentPill(stringResource(R.string.common_allow), onAllow)
        }
    }
}

@Composable
internal fun NoteCard(text: String, onClick: (() -> Unit)? = null) {
    GlassSurface(Modifier.fillMaxWidth(), onClick = onClick) {
        Soft(text, Modifier.padding(horizontal = 18.dp, vertical = 16.dp), align = TextAlign.Start)
    }
}

@Composable
internal fun Soft(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Center) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = DawnlyTheme.colors.textSoft,
        textAlign = align,
        modifier = modifier,
    )
}

@Composable
internal fun describeDismiss(result: DismissResult) = when (result) {
    is DismissResult.OpenedClock -> stringResource(R.string.home_opened_clock, result.clock.label)
    DismissResult.NoClockApp -> stringResource(R.string.home_no_clock)
    is DismissResult.DawnlyDismissed -> stringResource(R.string.home_dawnly_dismissed)
    is DismissResult.Requested -> ""
}
