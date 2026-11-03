package com.dawnly.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The whole palette, taken from the CSS variables in design/mockups.html. One accent, a few
 * neutrals, everything in the blue family.
 */
@Immutable
data class DawnlyColors(
    val isDark: Boolean,
    /** The plain background behind everything. */
    val wall: Color,
    /** The morning sky, top to bottom (light mode). Empty at night, where the wall shows. */
    val sky: List<Color>,
    /** The soft glows in the night sky. */
    val glow: Color,
    /** Shooting star trails, and the brighter dot at their head. */
    val star: Color,
    val starHead: Color,
    val text: Color,
    val textSoft: Color,
    /** Glass tint, and the stronger tint for selected items. */
    val glass: Color,
    val glassStrong: Color,
    /** The thin white rim around glass. */
    val edge: Color,
    /** The bright line along the top edge of glass. */
    val highlight: Color,
    /** How bright the liquid rim gets where light catches the edge, 0 to 1. */
    val rimStrength: Float,
    /** A faint light inside the top of glass, so it reads as a thick pane rather than an outline. */
    val sheen: Color,
    /** The soft shadow that lifts glass off the sky. */
    val lift: Color,
    /** The soft shadow under selected pills. */
    val selectedShadow: Color,
    /** Dividers between rows. */
    val line: Color,
    /** Empty part of the ring, switches when off, step dots. */
    val track: Color,
    /** Main buttons, switches when on, the progress ring. */
    val accent: Color,
    val onAccent: Color,
    /** The check that marks a sign as seen. */
    val tick: Color,
)

private val Navy = Color(0xFF0E1E45)
private val Blue = Color(0xFF1A4FD6)

val LightColors = DawnlyColors(
    isDark = false,
    wall = Color(0xFFF7F9FE),
    sky = listOf(Color(0xFFC6DDFF), Color(0xFFD9E8FF), Color(0xFFE6F0FF)),
    glow = Color(0xFFA9CCFF).copy(alpha = 0.55f),
    star = Color(0xFF7FB0FF),
    starHead = Color(0xFF5E95FF),
    text = Navy,
    textSoft = Navy.copy(alpha = 0.6f),
    glass = Color.White.copy(alpha = 0.05f),
    glassStrong = Color.White.copy(alpha = 0.32f),
    edge = Color.White.copy(alpha = 0.22f),
    highlight = Color.White.copy(alpha = 0.95f),
    rimStrength = 0.8f,
    sheen = Color.Transparent,
    lift = Color(0xFF3C5AA0).copy(alpha = 0.14f),
    selectedShadow = Color(0xFF0A1E5A).copy(alpha = 0.15f),
    line = Navy.copy(alpha = 0.10f),
    track = Navy.copy(alpha = 0.12f),
    accent = Blue,
    onAccent = Color.White,
    tick = Blue,
)

val DarkColors = DawnlyColors(
    isDark = true,
    wall = Color(0xFF0A0B12),
    sky = emptyList(),
    glow = Color(0xFF5E78DC).copy(alpha = 0.28f),
    star = Color(0xFFCFE2FF),
    starHead = Color.White,
    text = Color.White,
    textSoft = Color.White.copy(alpha = 0.68f),
    glass = Color(0xFFBEDCFF).copy(alpha = 0.06f),
    glassStrong = Color(0xFFD2E6FF).copy(alpha = 0.2f),
    edge = Color(0xFFDCE8FF).copy(alpha = 0.04f),
    highlight = Color(0xFFDCE8FF).copy(alpha = 0.24f),
    rimStrength = 0.4f,
    sheen = Color(0xFFBED6FF).copy(alpha = 0.08f),
    lift = Color.Black.copy(alpha = 0.35f),
    selectedShadow = Color.Black.copy(alpha = 0.25f),
    line = Color.White.copy(alpha = 0.14f),
    track = Color.White.copy(alpha = 0.18f),
    accent = Color.White,
    onAccent = Blue,
    tick = Color(0xFF4A8DFF),
)

val LocalDawnlyColors = staticCompositionLocalOf { LightColors }

/**
 * Part way from [from] to [to] ([amount] 0 to 1), for fading between light and dark. The glass
 * edge's colours ([DawnlyColors.edge], highlight, rim strength, sheen, shadows) and [isDark] switch
 * once at halfway rather than blending: the rims are drawn into bitmaps, and redrawing them every
 * frame of the fade would make it stutter.
 */
fun blend(from: DawnlyColors, to: DawnlyColors, amount: Float): DawnlyColors {
    fun mix(a: Color, b: Color) = androidx.compose.ui.graphics.lerp(a, b, amount)
    val edgeSide = if (amount < 0.5f) from else to
    return to.copy(
        isDark = edgeSide.isDark,
        wall = mix(from.wall, to.wall),
        sky = if (from.sky.size == to.sky.size) from.sky.zip(to.sky, ::mix) else edgeSide.sky,
        glow = mix(from.glow, to.glow),
        star = mix(from.star, to.star),
        starHead = mix(from.starHead, to.starHead),
        text = mix(from.text, to.text),
        textSoft = mix(from.textSoft, to.textSoft),
        glass = mix(from.glass, to.glass),
        glassStrong = mix(from.glassStrong, to.glassStrong),
        edge = edgeSide.edge,
        highlight = edgeSide.highlight,
        rimStrength = edgeSide.rimStrength,
        sheen = edgeSide.sheen,
        lift = edgeSide.lift,
        selectedShadow = edgeSide.selectedShadow,
        line = mix(from.line, to.line),
        track = mix(from.track, to.track),
        accent = mix(from.accent, to.accent),
        onAccent = mix(from.onAccent, to.onAccent),
        tick = mix(from.tick, to.tick),
    )
}
