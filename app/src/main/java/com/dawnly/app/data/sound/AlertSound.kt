package com.dawnly.app.data.sound

import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.dawnly.app.R

/**
 * The sound of the "You're up early" prompt. [PhoneDefault] follows the phone's own notification
 * sound; the rest are bundled in `res/raw` (Mixkit Sound Effects Free License, see
 * `assets/licenses/mixkit_sounds.txt`), renamed to short names of our own.
 *
 * Each sound has a [reversed] twin for "Alarm dismissed": forwards means you're up, backwards means
 * the alarm is off, so the user knows by ear (the user's idea, 2026-11-03). The phone's own sound
 * can't be reversed, so [PhoneDefault] borrows the reversed flute.
 */
enum class AlertSound(
    @StringRes val label: Int,
    @RawRes private val raw: Int?,
    @RawRes private val reversed: Int,
) {
    PhoneDefault(R.string.sound_phone_default, null, R.raw.sound_flute_reversed),
    Flute(R.string.sound_flute, R.raw.sound_flute, R.raw.sound_flute_reversed),
    Sparkle(R.string.sound_sparkle, R.raw.sound_sparkle, R.raw.sound_sparkle_reversed),
    Bubble(R.string.sound_bubble, R.raw.sound_bubble, R.raw.sound_bubble_reversed),
    Fizz(R.string.sound_fizz, R.raw.sound_fizz, R.raw.sound_fizz_reversed),
    Pop(R.string.sound_pop, R.raw.sound_pop, R.raw.sound_pop_reversed),
    Bloop(R.string.sound_bloop, R.raw.sound_bloop, R.raw.sound_bloop_reversed),
    Simmer(R.string.sound_simmer, R.raw.sound_simmer, R.raw.sound_simmer_reversed),
    Potion(R.string.sound_potion, R.raw.sound_potion, R.raw.sound_potion_reversed),
    Timer(R.string.sound_timer, R.raw.sound_timer, R.raw.sound_timer_reversed),
    Beep(R.string.sound_beep, R.raw.sound_beep, R.raw.sound_beep_reversed),
    Click(R.string.sound_click, R.raw.sound_click, R.raw.sound_click_reversed),
    LevelUp(R.string.sound_level_up, R.raw.sound_level_up, R.raw.sound_level_up_reversed),
    Retro(R.string.sound_retro, R.raw.sound_retro, R.raw.sound_retro_reversed),
    Score(R.string.sound_score, R.raw.sound_score, R.raw.sound_score_reversed),
    Jump(R.string.sound_jump, R.raw.sound_jump, R.raw.sound_jump_reversed),
    Jackpot(R.string.sound_jackpot, R.raw.sound_jackpot, R.raw.sound_jackpot_reversed),
    Pixel(R.string.sound_pixel, R.raw.sound_pixel, R.raw.sound_pixel_reversed),
    ;

    /** What to play: a bundled file, or the phone's notification sound. For channels and previews. */
    fun uri(context: Context): Uri = raw?.let { rawUri(context, it) } ?: Settings.System.DEFAULT_NOTIFICATION_URI

    /** The same sound played backwards, for "Alarm dismissed". */
    fun reversedUri(context: Context): Uri = rawUri(context, reversed)

    // By name, not resource number: a channel keeps its sound across app updates, and the numbers
    // can shift from one version to the next.
    private fun rawUri(context: Context, @RawRes res: Int): Uri =
        "android.resource://${context.packageName}/raw/${context.resources.getResourceEntryName(res)}".toUri()

    companion object {
        /** Dawnly's own sound until the user picks another (the user's choice, 2026-11-01). */
        val DEFAULT = Flute

        /** Saved by name; nothing saved, or an unknown name (a removed sound), gives [DEFAULT]. */
        fun fromKey(key: String?): AlertSound = entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}
