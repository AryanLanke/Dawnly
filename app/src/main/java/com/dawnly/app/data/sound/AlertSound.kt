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
 */
enum class AlertSound(@StringRes val label: Int, @RawRes private val raw: Int?) {
    PhoneDefault(R.string.sound_phone_default, null),
    Flute(R.string.sound_flute, R.raw.sound_flute),
    Sparkle(R.string.sound_sparkle, R.raw.sound_sparkle),
    Bubble(R.string.sound_bubble, R.raw.sound_bubble),
    Fizz(R.string.sound_fizz, R.raw.sound_fizz),
    Pop(R.string.sound_pop, R.raw.sound_pop),
    Bloop(R.string.sound_bloop, R.raw.sound_bloop),
    Simmer(R.string.sound_simmer, R.raw.sound_simmer),
    Potion(R.string.sound_potion, R.raw.sound_potion),
    Timer(R.string.sound_timer, R.raw.sound_timer),
    Beep(R.string.sound_beep, R.raw.sound_beep),
    Click(R.string.sound_click, R.raw.sound_click),
    LevelUp(R.string.sound_level_up, R.raw.sound_level_up),
    Retro(R.string.sound_retro, R.raw.sound_retro),
    Score(R.string.sound_score, R.raw.sound_score),
    Jump(R.string.sound_jump, R.raw.sound_jump),
    Jackpot(R.string.sound_jackpot, R.raw.sound_jackpot),
    Pixel(R.string.sound_pixel, R.raw.sound_pixel),
    ;

    /** What to play: a bundled file, or the phone's notification sound. For channels and previews. */
    fun uri(context: Context): Uri = raw
        // By name, not resource number: a channel keeps its sound across app updates, and the
        // numbers can shift from one version to the next.
        ?.let { "android.resource://${context.packageName}/raw/${context.resources.getResourceEntryName(it)}".toUri() }
        ?: Settings.System.DEFAULT_NOTIFICATION_URI

    companion object {
        /** Dawnly's own sound until the user picks another (the user's choice, 2026-11-01). */
        val DEFAULT = Flute

        /** Saved by name; nothing saved, or an unknown name (a removed sound), gives [DEFAULT]. */
        fun fromKey(key: String?): AlertSound = entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}
