package com.dawnly.app.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyMotion
import com.dawnly.app.ui.theme.DawnlyTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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

/** The mockup's switch: 44 x 26, a springy thumb, accent when on. */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DawnlyTheme.colors
    val haptic = rememberHaptics()
    val thumbOffset by animateDpAsState(if (checked) 21.dp else 3.dp, DawnlyMotion.bouncy(), label = "thumb")
    val track by animateColorAsState(if (checked) colors.accent else colors.track, label = "track")
    val thumb by animateColorAsState(if (checked) colors.onAccent else Color.White, label = "thumbColor")
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(CircleShape)
            .background(track)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { on ->
                haptic.toggle(on)
                onCheckedChange(on)
            }),
    ) {
        Box(
            Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 3.dp.roundToPx()) }
                .size(20.dp)
                .shadow(1.5.dp, CircleShape)
                .background(thumb, CircleShape),
        )
    }
}

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

/** Minus, a value, plus. Used for the custom window and goal settings. */
@Composable
fun GlassStepper(
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean,
    decreaseLabel: String,
    increaseLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepButton(GlassIcons.Minus, decreaseLabel, decreaseEnabled, onDecrease)
        Text(value, style = MaterialTheme.typography.titleMedium)
        StepButton(GlassIcons.Plus, increaseLabel, increaseEnabled, onIncrease)
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.35f, label = "stepAlpha")
    val haptics = rememberHaptics()
    val step by rememberUpdatedState(onClick)
    val canStep by rememberUpdatedState(enabled)
    GlassSurface(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { this.alpha = alpha }
            .pointerInput(Unit) {
                repeatWhileHeld {
                    if (canStep) {
                        haptics.tick()
                        step()
                    }
                }
            },
        shape = CircleShape,
        strong = true,
        onClick = if (enabled) onClick else null,
        onClickLabel = label,
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
    }
}

/**
 * Holding a step button keeps stepping: the first repeat after 400 ms, then faster, down to one
 * every 60 ms. A plain tap is left to the button's own click. After repeating, the lift is
 * consumed so letting go doesn't add one more step.
 */
private suspend fun PointerInputScope.repeatWhileHeld(step: () -> Unit) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false)
    var wait = 400L
    var repeating = false
    while (true) {
        var up: PointerInputChange? = null
        val ended = withTimeoutOrNull(wait) {
            up = waitForUpOrCancellation(PointerEventPass.Initial)
            true
        } == true
        if (ended) {
            if (repeating) up?.consume()
            break
        }
        step()
        repeating = true
        wait = (wait * 0.75f).toLong().coerceAtLeast(60L)
    }
}

/**
 * A slider over whole steps, 0 to [max], for big jumps (the stepper is for fine ones). The track
 * fills with the accent up to a round thumb, like the switch, and each step ticks under the
 * finger. [onValueChange] follows the finger; [onValueChangeFinished] comes when it lets go.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSlider(
    value: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = DawnlyTheme.colors
    val haptics = rememberHaptics()
    Slider(
        value = value.toFloat(),
        onValueChange = { raw ->
            val stepped = raw.roundToInt().coerceIn(0, max)
            if (stepped != value) {
                haptics.tick()
                onValueChange(stepped)
            }
        },
        valueRange = 0f..max.toFloat(),
        steps = max - 1,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
        thumb = {
            Box(
                Modifier
                    .size(26.dp)
                    .outerShadow(CircleShape, colors.selectedShadow)
                    .background(colors.onAccent, CircleShape)
                    .border(1.dp, colors.accent.copy(alpha = 0.25f), CircleShape),
            )
        },
        track = { state ->
            val range = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (range > 0f) (state.value - state.valueRange.start) / range else 0f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(colors.track),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
            }
        },
    )
}

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

/** A glass card of rows with thin dividers between them. */
@Composable
fun GlassList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), content = content)
    }
}

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
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(DawnlyTheme.colors.line))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
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
