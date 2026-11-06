package com.dawnly.app.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.text.format.DateFormat
import com.dawnly.app.MainActivity
import java.time.Instant
import java.util.Date

/** Small pieces every notification of Dawnly needs, kept in one place. */
object Notify {
    /** A tap on the notification opens Dawnly. */
    fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    /** [at] as the phone shows times (12 or 24 hour), for a notification's text. */
    fun time(context: Context, at: Instant): String =
        DateFormat.getTimeFormat(context).format(Date.from(at))

    /** A short, soft notification sound. */
    val audio: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}
