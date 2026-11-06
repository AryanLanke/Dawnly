package com.dawnly.app.data.reminder

import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.domain.reminder.Reminder

/** Which of Dawnly's sounds a reminder rings with. */
object ReminderSounds {
    /**
     * Reminders' own sound until the user picks another: Bubble, so a reminder never sounds like
     * the "Up already?" question or "Alarm's off" (the prompt's Flute by default).
     */
    val DEFAULT = AlertSound.Bubble

    fun of(reminder: Reminder): AlertSound =
        reminder.sound?.let { key -> AlertSound.entries.firstOrNull { it.name == key } } ?: DEFAULT
}
