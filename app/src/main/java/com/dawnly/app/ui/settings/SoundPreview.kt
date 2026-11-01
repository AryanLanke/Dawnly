package com.dawnly.app.ui.settings

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dawnly.app.data.sound.AlertSound

/**
 * Plays a sound once, the way the prompt will: on the notification stream, so it follows the
 * notification volume and stays quiet when the phone is on silent. One sound at a time.
 */
class SoundPreview(private val context: Context) {
    private var player: MediaPlayer? = null

    /** True when the notification volume is down or the phone is on silent or vibrate. */
    val isSilenced: Boolean
        get() {
            val audio = context.getSystemService(AudioManager::class.java)
            return audio.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
                audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0
        }

    fun play(sound: AlertSound) {
        stop()
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, sound.uri(context))
                setOnPreparedListener { it.start() }
                setOnCompletionListener { stop() }
                prepareAsync()
            }
        } catch (e: Exception) {
            // A missing or unreadable phone sound shouldn't crash Settings.
            Log.w(TAG, "Couldn't preview $sound", e)
            null
        }
    }

    fun stop() {
        player?.release()
        player = null
    }

    private companion object {
        const val TAG = "SoundPreview"
    }
}

/** A [SoundPreview] that stops when it leaves the screen. */
@Composable
fun rememberSoundPreview(): SoundPreview {
    val context = LocalContext.current
    val preview = remember(context) { SoundPreview(context) }
    DisposableEffect(preview) { onDispose { preview.stop() } }
    return preview
}
