package com.dawnly.app.notify

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.dawnly.app.data.sound.AlertSound

/**
 * Android fixes a channel's sound when the channel is made, so every sound gets its own channel,
 * named [prefix] plus the sound (`awake_flute`). Choosing a sound makes its channel and deletes
 * the old one.
 */
internal object SoundChannels {
    fun id(prefix: String, sound: AlertSound) = prefix + sound.name.lowercase()

    /**
     * Keeps just the channel for [sound] under [prefix] (and removes any in [alsoRemove]).
     * [create] makes it when it doesn't exist yet. Cheap to call on every app start.
     */
    fun use(
        context: Context,
        prefix: String,
        sound: AlertSound,
        alsoRemove: Set<String> = emptySet(),
        create: (id: String) -> Unit,
    ) {
        val manager = NotificationManagerCompat.from(context)
        val wanted = id(prefix, sound)
        manager.notificationChannelsCompat
            .filter { it.id in alsoRemove || (it.id.startsWith(prefix) && it.id != wanted) }
            .forEach { manager.deleteNotificationChannel(it.id) }
        if (manager.getNotificationChannelCompat(wanted) == null) create(wanted)
    }

    /** The existing channel under [prefix], or the default sound's after [useDefault] makes it. */
    fun current(context: Context, prefix: String, useDefault: () -> Unit): String {
        val existing = NotificationManagerCompat.from(context).notificationChannelsCompat
            .firstOrNull { it.id.startsWith(prefix) }
        if (existing != null) return existing.id
        useDefault()
        return id(prefix, AlertSound.DEFAULT)
    }
}
