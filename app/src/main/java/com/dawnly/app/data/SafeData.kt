package com.dawnly.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/** The store's data, falling back to defaults if the file can't be read. */
internal val DataStore<Preferences>.safeData: Flow<Preferences>
    get() = data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
