package com.dawnly.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dawnly.app.R
import com.dawnly.app.domain.worldclock.WorldClock
import com.dawnly.app.ui.format.clockFace
import com.dawnly.app.ui.format.formatTime
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.ROW_INSET
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.glass.TextAction
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive

// Today's clocks: the live local time above the alarm ring, and the world clock card below it.

/**
 * Today's heartbeat: one tick a second, only while the app is resumed, like the tools' ticker.
 * The screen hands this to its clocks without reading it, so only the clocks recompose each
 * second, not the whole screen.
 */
@Composable
internal fun rememberTodayClock(): StateFlow<Instant> {
    val clock = remember { MutableStateFlow(Instant.now()) }
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(clock, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                clock.value = Instant.now()
                delay(1_000)
            }
        }
    }
    return clock
}

/** The current local time, calm and large above the ring, in the phone's own 12/24-hour style. */
@Composable
internal fun LiveClock(clock: StateFlow<Instant>) {
    val now by clock.collectAsStateWithLifecycle()
    val face = clockFace(now)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(face.digits, style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"))
        if (face.marker != null) {
            Text(
                face.marker,
                style = MaterialTheme.typography.titleMedium,
                color = DawnlyTheme.colors.textSoft,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
    }
}

/**
 * The user's saved cities on Today: each with its current time and how far it is from home, and a
 * row to add one. The times tick with [clock]; the cities stay on the phone in [WorldClockStore].
 * [onAdd] opens the picker, which rises over Today in the same glass frame as the other editors.
 */
@Composable
internal fun WorldClockCard(
    clock: StateFlow<Instant>,
    cities: List<String>,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val now by clock.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.worldclock_label))
        GlassList {
            cities.forEachIndexed { index, id ->
                val zone = remember(id) { ZoneId.of(id) }
                val name = Cities.firstOrNull { it.zoneId == id }?.let { stringResource(it.label) }
                    ?: id.substringAfterLast('/').replace('_', ' ')
                GlassRow(
                    title = name,
                    subtitle = WorldClock.offsetLabel(zone, now),
                    divider = index > 0,
                ) {
                    Text(
                        formatTime(now, zone),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onRemove(id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            GlassIcons.Minus,
                            contentDescription = stringResource(R.string.worldclock_remove, name),
                            tint = DawnlyTheme.colors.textSoft,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            GlassRow(
                title = stringResource(R.string.worldclock_add),
                subtitle = if (cities.isEmpty()) stringResource(R.string.worldclock_add_hint) else null,
                divider = cities.isNotEmpty(),
                modifier = Modifier.clickable(role = Role.Button) { onAdd() },
            ) {
                Icon(GlassIcons.Plus, contentDescription = null, tint = DawnlyTheme.colors.textSoft, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * A glass pane of the cities not added yet; tapping one adds it and closes the picker. It fills the
 * screen so taps beside the card close it, with Today frosted behind (see MainTabs), like the
 * reminder and alarm editors.
 */
@Composable
internal fun CityPicker(added: List<String>, onAdd: (String) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose)
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassList(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                // Taps on the card stay on the card.
                .clickable(remember { MutableInteractionSource() }, indication = null) {},
        ) {
            Text(
                stringResource(R.string.worldclock_pick_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = ROW_INSET, top = 18.dp, bottom = 10.dp),
            )
            val choices = cityChoices(added)
            if (choices.isEmpty()) {
                Text(
                    stringResource(R.string.worldclock_all_added),
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnlyTheme.colors.textSoft,
                    modifier = Modifier.padding(horizontal = ROW_INSET, vertical = 14.dp),
                )
            } else {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    choices.forEachIndexed { index, city ->
                        GlassRow(
                            title = stringResource(city.label),
                            divider = index > 0,
                            modifier = Modifier.clickable(role = Role.Button) { onAdd(city.zoneId) },
                        )
                    }
                }
            }
            TextAction(stringResource(R.string.common_cancel), onClose, Modifier.fillMaxWidth().padding(bottom = 6.dp))
        }
    }
}

/** One city offered in the picker: its name as a string, and the zone it keeps time in. */
internal class City(val label: Int, val zoneId: String)

/** The cities still offered once [added] are on the list; ids outside the catalog leave it whole. */
internal fun cityChoices(added: List<String>): List<City> = Cities.filterNot { it.zoneId in added }

/** The major cities offered, around the world. Mumbai and Beijing share their country's one zone. */
internal val Cities = listOf(
    City(R.string.worldclock_city_london, "Europe/London"),
    City(R.string.worldclock_city_paris, "Europe/Paris"),
    City(R.string.worldclock_city_berlin, "Europe/Berlin"),
    City(R.string.worldclock_city_moscow, "Europe/Moscow"),
    City(R.string.worldclock_city_cairo, "Africa/Cairo"),
    City(R.string.worldclock_city_dubai, "Asia/Dubai"),
    City(R.string.worldclock_city_mumbai, "Asia/Kolkata"),
    City(R.string.worldclock_city_kathmandu, "Asia/Kathmandu"),
    City(R.string.worldclock_city_bangkok, "Asia/Bangkok"),
    City(R.string.worldclock_city_singapore, "Asia/Singapore"),
    City(R.string.worldclock_city_beijing, "Asia/Shanghai"),
    City(R.string.worldclock_city_tokyo, "Asia/Tokyo"),
    City(R.string.worldclock_city_seoul, "Asia/Seoul"),
    City(R.string.worldclock_city_sydney, "Australia/Sydney"),
    City(R.string.worldclock_city_auckland, "Pacific/Auckland"),
    City(R.string.worldclock_city_honolulu, "Pacific/Honolulu"),
    City(R.string.worldclock_city_los_angeles, "America/Los_Angeles"),
    City(R.string.worldclock_city_chicago, "America/Chicago"),
    City(R.string.worldclock_city_new_york, "America/New_York"),
    City(R.string.worldclock_city_sao_paulo, "America/Sao_Paulo"),
)