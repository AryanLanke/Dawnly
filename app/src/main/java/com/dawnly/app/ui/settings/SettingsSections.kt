package com.dawnly.app.ui.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import com.dawnly.app.ui.glass.GlassIcons
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.settings.DismissMode
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.domain.watch.WatchWindow
import com.dawnly.app.ui.format.formatDuration
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.GlassSlider
import com.dawnly.app.ui.glass.GlassStepper
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.GlassSwitch
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.permissions.OemBattery
import com.dawnly.app.ui.permissions.PermissionState
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Duration

// The sections of the Settings screen, in the order SettingsContent shows them.

@Composable
internal fun WindowSection(window: Duration, onChange: (Duration) -> Unit) {
    val presets = WatchWindow.PRESETS
    var customOpen by rememberSaveable { mutableStateOf(false) }
    val custom = customOpen || window !in presets
    Column {
        SectionLabel(stringResource(R.string.settings_window_label))
        GlassSegmented(
            options = presets.map { formatDuration(it) } + stringResource(R.string.settings_window_custom),
            selected = if (custom) presets.size else presets.indexOf(window),
            onSelect = { index ->
                customOpen = index == presets.size
                if (!customOpen) onChange(presets[index])
            },
        )
        AnimatedVisibility(
            visible = custom,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            CustomWindow(window, onChange)
        }
    }
}

/**
 * The custom window: drag the slider for big jumps (5 min to 3 hr in one swipe, in 5-minute
 * steps), tap or hold minus and plus to move a minute at a time, down to 1 minute. Under
 * [WatchWindow.RECOMMENDED_MIN] a note says it's very short. While dragging, the time follows the
 * finger and is saved on letting go, so a swipe is one settings change rather than dozens.
 */
@Composable
private fun CustomWindow(window: Duration, onChange: (Duration) -> Unit) {
    val first = WatchWindow.RECOMMENDED_MIN
    val stepMinutes = WatchWindow.STEP.toMinutes()
    val steps = ((WatchWindow.MAX - first).toMinutes() / stepMinutes).toInt()
    // Windows under 5 minutes sit at the slider's start; nearest step otherwise.
    fun toStep(d: Duration) = (((d - first).toMinutes() + stepMinutes / 2) / stepMinutes).toInt().coerceIn(0, steps)
    fun toWindow(step: Int): Duration = first + WatchWindow.STEP.multipliedBy(step.toLong())
    var dragging by remember { mutableStateOf<Int?>(null) }
    val shown = dragging?.let(::toWindow) ?: window
    Column {
        GlassSurface(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Column(Modifier.padding(10.dp)) {
                GlassStepper(
                    value = formatDuration(shown),
                    onDecrease = { onChange(WatchWindow.clamp(window - WatchWindow.FINE_STEP)) },
                    onIncrease = { onChange(WatchWindow.clamp(window + WatchWindow.FINE_STEP)) },
                    decreaseEnabled = window > WatchWindow.MIN,
                    increaseEnabled = window < WatchWindow.MAX,
                    decreaseLabel = stringResource(R.string.settings_shorter),
                    increaseLabel = stringResource(R.string.settings_longer),
                )
                GlassSlider(
                    value = dragging ?: toStep(window),
                    max = steps,
                    onValueChange = { dragging = it },
                    onValueChangeFinished = {
                        dragging?.let { onChange(toWindow(it)) }
                        dragging = null
                    },
                    label = stringResource(R.string.settings_window_slider),
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
        AnimatedVisibility(visible = shown < WatchWindow.RECOMMENDED_MIN) {
            Text(
                stringResource(R.string.settings_window_short),
                style = MaterialTheme.typography.bodySmall,
                color = DawnlyTheme.colors.textSoft,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
            )
        }
    }
}

@Composable
internal fun SignalsSection(
    signals: WakeSignalSettings,
    deniedSign: Sign?,
    onChange: (WakeSignalSettings) -> Unit,
    onNeedsPermission: (Sign) -> Unit,
) {
    var lastOneRefused by remember { mutableStateOf(false) }
    fun toggle(sign: Sign, on: Boolean) {
        val updated = signals.withSign(sign, on)
        lastOneRefused = updated == null
        if (updated != null) onChange(updated)
    }
    Column {
        SectionLabel(stringResource(R.string.settings_signals_label))
        GlassList {
            Sign.entries.forEachIndexed { index, sign ->
                GlassRow(title = stringResource(sign.title), subtitle = sign.subtitle(signals), divider = index > 0) {
                    GlassSwitch(signals.uses(sign), { on ->
                        if (on && sign.needsPermission) onNeedsPermission(sign) else toggle(sign, on)
                    })
                }
            }
        }
        val hint = when {
            lastOneRefused -> stringResource(R.string.settings_signals_min)
            deniedSign == Sign.Steps && !signals.useSteps -> stringResource(R.string.settings_steps_denied)
            deniedSign == Sign.PhoneUse && !signals.usePhoneUse -> stringResource(R.string.settings_apps_denied)
            else -> null
        }
        AnimatedVisibility(visible = hint != null) {
            Text(
                hint.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = DawnlyTheme.colors.textSoft,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            )
        }
    }
}

private fun WakeSignalSettings.uses(sign: Sign) = when (sign) {
    Sign.ScreenTime -> useScreenTime
    Sign.PhoneUse -> usePhoneUse
    Sign.Steps -> useSteps
    Sign.Movement -> useMovement
    Sign.Music -> useMusic
}

/** Steps and apps opened turn on only once their permission is allowed. */
private val Sign.needsPermission get() = this == Sign.Steps || this == Sign.PhoneUse

private val Sign.title
    get() = when (this) {
        Sign.ScreenTime -> R.string.settings_screen
        Sign.PhoneUse -> R.string.settings_apps
        Sign.Steps -> R.string.settings_steps
        Sign.Movement -> R.string.settings_movement
        Sign.Music -> R.string.settings_music
    }

@Composable
private fun Sign.subtitle(signals: WakeSignalSettings): String = when (this) {
    Sign.ScreenTime -> signals.screenTimeGoal.toMinutes().toInt()
        .let { pluralStringResource(R.plurals.settings_screen_goal, it, it) }
    Sign.PhoneUse -> pluralStringResource(R.plurals.settings_apps_goal, signals.appsOpenedGoal, signals.appsOpenedGoal)
    Sign.Steps -> pluralStringResource(R.plurals.settings_steps_goal, signals.stepsGoal, signals.stepsGoal)
    Sign.Movement -> signals.movementGoal.toMinutes().toInt()
        .let { pluralStringResource(R.plurals.settings_movement_goal, it, it) }
    Sign.Music -> stringResource(R.string.settings_music_detail)
}

/**
 * Ask by default. Picking an automatic choice without "Display over other apps" first says why it's
 * needed, with one tap to allow it; the choice is saved once it's allowed.
 */
@Composable
internal fun DismissModeSection(
    mode: DismissMode,
    overlayAllowed: Boolean,
    fullScreenAllowed: Boolean,
    onChange: (DismissMode) -> Unit,
    onAllowOverlay: (DismissMode) -> Unit,
) {
    var waiting by rememberSaveable { mutableStateOf<DismissMode?>(null) }
    LaunchedEffect(mode) { if (mode == waiting) waiting = null }
    val modes = DismissMode.entries
    val shown = waiting ?: mode
    Column {
        SectionLabel(stringResource(R.string.settings_dismiss_label))
        GlassSegmented(
            options = modes.map { stringResource(it.label) },
            selected = modes.indexOf(shown),
            onSelect = { index ->
                val picked = modes[index]
                // Full-screen notifications are enough to dismiss by itself; overlay only skips a tap.
                if (picked == DismissMode.Ask || overlayAllowed || fullScreenAllowed) {
                    waiting = null
                    onChange(picked)
                } else {
                    waiting = picked
                }
            },
        )
        Text(
            stringResource(shown.detail),
            style = MaterialTheme.typography.bodySmall,
            color = DawnlyTheme.colors.textSoft,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
        )
        // Works already; Display over other apps would only save the tap while the phone is in use.
        AnimatedVisibility(visible = waiting == null && mode != DismissMode.Ask && !overlayAllowed) {
            Column(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)) {
                Text(
                    stringResource(R.string.settings_auto_dismiss_tap_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnlyTheme.colors.textSoft,
                )
                AccentPill(stringResource(R.string.common_allow), { onAllowOverlay(mode) }, Modifier.padding(top = 10.dp))
            }
        }
        AnimatedVisibility(visible = waiting != null) {
            Column(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)) {
                // Android 11+ ignores the package Uri and opens the list of all apps.
                val why = stringResource(R.string.settings_auto_dismiss_why)
                val findDawnly = stringResource(R.string.settings_auto_dismiss_find)
                Text(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "$why $findDawnly" else why,
                    style = MaterialTheme.typography.bodySmall,
                    color = DawnlyTheme.colors.textSoft,
                )
                AccentPill(stringResource(R.string.common_allow), { waiting?.let(onAllowOverlay) }, Modifier.padding(top = 10.dp))
            }
        }
    }
}

private val DismissMode.label
    get() = when (this) {
        DismissMode.Ask -> R.string.dismiss_mode_ask
        DismissMode.Warn -> R.string.dismiss_mode_warn
        DismissMode.Instant -> R.string.dismiss_mode_instant
    }

private val DismissMode.detail
    get() = when (this) {
        DismissMode.Ask -> R.string.dismiss_mode_ask_detail
        DismissMode.Warn -> R.string.dismiss_mode_warn_detail
        DismissMode.Instant -> R.string.dismiss_mode_instant_detail
    }

@Composable
internal fun PermissionsSection(
    permissions: PermissionState,
    dismissMode: DismissMode,
    signals: WakeSignalSettings,
    oemBattery: OemBattery?,
    onRequest: (AppPermission) -> Unit,
    onOpenOemBattery: () -> Unit,
) {
    val shown = shownPermissions(dismissMode, signals)
    Column {
        SectionLabel(stringResource(R.string.settings_permissions_label))
        GlassList {
            shown.forEachIndexed { index, permission ->
                GlassRow(title = stringResource(permission.label), divider = index > 0) {
                    if (permission in permissions) {
                        Text(
                            stringResource(R.string.common_allowed),
                            style = MaterialTheme.typography.bodySmall,
                            color = DawnlyTheme.colors.textSoft,
                        )
                    } else {
                        AccentPill(stringResource(R.string.common_allow), { onRequest(permission) })
                    }
                }
            }
            if (oemBattery != null) {
                GlassRow(
                    title = stringResource(oemBattery.label),
                    subtitle = stringResource(oemBattery.hint),
                    divider = true,
                ) {
                    AccentPill(stringResource(R.string.common_open), onOpenOemBattery)
                }
            }
        }
    }
}

/**
 * The permissions Settings lists. Usage access only matters with the apps-opened sign on, and
 * Display over other apps only when Dawnly dismisses by itself, e.g. if either was taken back since.
 */
internal fun shownPermissions(dismissMode: DismissMode, signals: WakeSignalSettings): List<AppPermission> =
    AppPermission.entries.filter {
        it.isApplicable && when (it) {
            AppPermission.UsageAccess -> signals.usePhoneUse
            AppPermission.Overlay, AppPermission.FullScreen -> dismissMode != DismissMode.Ask
            else -> it.askedUpFront
        }
    }

/**
 * The shown permissions that still need the user. Display over other apps is optional while
 * full-screen notifications are allowed: auto-dismiss works without it, with one tap while the
 * phone is in use.
 */
internal fun waitingPermissions(permissions: PermissionState, dismissMode: DismissMode, signals: WakeSignalSettings): Int =
    shownPermissions(dismissMode, signals).count { permission ->
        permission !in permissions &&
            !(permission == AppPermission.Overlay && AppPermission.FullScreen in permissions)
    }

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

internal val AppPermission.label
    get() = when (this) {
        AppPermission.Notifications -> R.string.perm_notifications
        AppPermission.ExactAlarms -> R.string.perm_exact_alarms
        AppPermission.ActivityRecognition -> R.string.perm_activity
        AppPermission.UsageAccess -> R.string.perm_usage
        AppPermission.Battery -> R.string.perm_battery
        AppPermission.Overlay -> R.string.perm_overlay
        AppPermission.FullScreen -> R.string.perm_full_screen
    }
