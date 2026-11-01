package com.dawnly.app.data.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.dawnly.app.data.safeData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.onboardingStore by preferencesDataStore(name = "onboarding")

/** Whether the user has been through onboarding. Kept apart from the user's settings on purpose. */
class OnboardingStore(context: Context) {
    private val store = context.applicationContext.onboardingStore

    val completed: Flow<Boolean> = store.safeData
        .map { it[COMPLETED] ?: false }
        .distinctUntilChanged()

    suspend fun setCompleted() {
        store.edit { it[COMPLETED] = true }
    }

    private companion object {
        val COMPLETED = booleanPreferencesKey("completed")
    }
}
