package com.dawnly.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dawnly.app.R

/** Figtree only, bundled so the app stays offline. One variable font file covers every weight. */
private fun figtree(weight: FontWeight) = Font(
    resId = R.font.figtree,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Figtree = FontFamily(
    figtree(FontWeight.Light),
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
)

private val Base = TextStyle(fontFamily = Figtree)

/**
 * Light for big times, Regular for body, SemiBold for titles and buttons. Sizes are the mockup's,
 * scaled up from its 300px-wide phone frame.
 */
val DawnlyTypography = Typography(
    // The alarm time in the ring.
    displayLarge = Base.copy(fontWeight = FontWeight.Light, fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-0.04).em),
    // "No alarm" in the ring.
    displaySmall = Base.copy(fontWeight = FontWeight.Light, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.03).em),
    // Screen titles.
    headlineMedium = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em),
    // Onboarding headline.
    headlineSmall = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em),
    titleMedium = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = Base.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = Base.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = Base.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    // Buttons.
    labelLarge = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    // Segments and tabs.
    labelMedium = Base.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    // Section labels.
    labelSmall = Base.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 16.sp),
)
