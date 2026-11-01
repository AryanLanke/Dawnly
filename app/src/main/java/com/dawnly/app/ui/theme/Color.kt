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
    /** The three flowing shapes behind the glass. */
    val blobs: List<Color>,
    val text: Color,
    val textSoft: Color,
    /** Glass tint, and the stronger tint for selected items. */
    val glass: Color,
    val glassStrong: Color,
    /** Below Android 12 there's no blur, so glass gets a stronger tint to stay readable. */
    val glassFallback: Color,
    /** The thin white rim around glass. */
    val edge: Color,
    /** The bright line along the top edge of glass. */
    val highlight: Color,
    /** Dividers between rows. */
    val line: Color,
    /** Empty part of the ring, switches when off, step dots. */
    val track: Color,
    /** Main buttons, switches when on, the progress ring. */
    val accent: Color,
    val onAccent: Color,
)

private val Navy = Color(0xFF0E1E45)
private val Blue = Color(0xFF1A4FD6)

val LightColors = DawnlyColors(
    isDark = false,
    wall = Color(0xFFF7F9FE),
    blobs = listOf(Color(0xFFA9CCFF), Color(0xFFC7E2FF), Color(0xFF8FB8FF)),
    text = Navy,
    textSoft = Navy.copy(alpha = 0.6f),
    glass = Color.White.copy(alpha = 0.14f),
    glassStrong = Color.White.copy(alpha = 0.42f),
    glassFallback = Color.White.copy(alpha = 0.5f),
    edge = Color.White.copy(alpha = 0.75f),
    highlight = Color.White.copy(alpha = 0.95f),
    line = Navy.copy(alpha = 0.10f),
    track = Navy.copy(alpha = 0.12f),
    accent = Blue,
    onAccent = Color.White,
)

val DarkColors = DawnlyColors(
    isDark = true,
    wall = Color(0xFF050A1A),
    blobs = listOf(Color(0xFF5E95FF), Color(0xFF8CBCFF), Color(0xFF4A7FF0)),
    text = Color.White,
    textSoft = Color.White.copy(alpha = 0.68f),
    glass = Color(0xFFBEDCFF).copy(alpha = 0.08f),
    glassStrong = Color(0xFFD2E6FF).copy(alpha = 0.2f),
    glassFallback = Color(0xFF14204A).copy(alpha = 0.6f),
    edge = Color.White.copy(alpha = 0.32f),
    highlight = Color.White.copy(alpha = 0.6f),
    line = Color.White.copy(alpha = 0.14f),
    track = Color.White.copy(alpha = 0.18f),
    accent = Color.White,
    onAccent = Blue,
)

val LocalDawnlyColors = staticCompositionLocalOf { LightColors }
