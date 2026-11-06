package com.dawnly.app.data.sound

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dawnly.app.data.safeData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.soundStore by preferencesDataStore(name = "sound")

/** The user's choice of prompt sound, in its own small store. */
class SoundStore(context: Context) {
    private val store = context.applicationContext.soundStore

    val selected: Flow<AlertSound> = store.safeData
        .map { AlertSound.fromKey(it[SOUND]) }
        .distinctUntilChanged()

    suspend fun setSelected(sound: AlertSound) {
        store.edit { it[SOUND] = sound.name }
    }

    private companion object {
        val SOUND = stringPreferencesKey("prompt_sound")
    }
}
