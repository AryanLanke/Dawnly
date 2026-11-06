package com.dawnly.app.data.reminder

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dawnly.app.data.safeData
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.domain.reminder.ReminderSchedule
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private val Context.reminderStore by preferencesDataStore(name = "reminders")

/** The user's reminders, kept on the phone as a small JSON list. */
class ReminderStore(context: Context) {
    private val store = context.applicationContext.reminderStore

    val reminders: Flow<List<Reminder>> = store.safeData
        .map { prefs -> prefs[LIST]?.let(::decode).orEmpty() }
        .distinctUntilChanged()

    suspend fun current(): List<Reminder> = reminders.first()

    /** Adds [reminder], or replaces the one with its id. */
    suspend fun save(reminder: Reminder) = update { list ->
        if (list.any { it.id == reminder.id }) list.map { if (it.id == reminder.id) reminder else it } else list + reminder
    }

    suspend fun delete(id: Long) = update { list -> list.filterNot { it.id == id } }

    private suspend fun update(change: (List<Reminder>) -> List<Reminder>) {
        store.edit { prefs -> prefs[LIST] = encode(change(prefs[LIST]?.let(::decode).orEmpty())) }
    }

    internal companion object {
        const val TAG = "ReminderStore"
        val LIST = stringPreferencesKey("list")

        fun encode(list: List<Reminder>): String = JSONArray().apply {
            for (r in list) {
                put(
                    JSONObject()
                        .put("id", r.id)
                        .put("name", r.name)
                        .put("why", r.why)
                        .put("on", r.on)
                        .put("ring", r.ring)
                        .apply { r.sound?.let { put("sound", it) } }
                        .apply { r.firstDay?.let { put("firstDay", it.toString()) } }
                        .apply { r.lastDay?.let { put("lastDay", it.toString()) } }
                        .apply {
                            when (val s = r.schedule) {
                                is ReminderSchedule.Daily -> put("kind", "daily").put("at", s.at.toString())
                                is ReminderSchedule.Every -> put("kind", "every").put("minutes", s.minutes)
                                    .put("from", s.from.toString()).put("until", s.until.toString())
                            }
                        },
                )
            }
        }.toString()

        /** A reminder that can't be read is skipped, not allowed to lose the rest. */
        fun decode(json: String): List<Reminder> = try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                try {
                    val o = array.getJSONObject(i)
                    val schedule = when (o.getString("kind")) {
                        "daily" -> ReminderSchedule.Daily(LocalTime.parse(o.getString("at")))
                        else -> ReminderSchedule.Every(
                            o.getInt("minutes"),
                            LocalTime.parse(o.getString("from")),
                            LocalTime.parse(o.getString("until")),
                        )
                    }
                    Reminder(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        why = o.optString("why"),
                        schedule = schedule,
                        on = o.optBoolean("on", true),
                        sound = o.optString("sound").takeIf { it.isNotEmpty() },
                        firstDay = o.optString("firstDay").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
                        lastDay = o.optString("lastDay").takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
                        ring = o.optBoolean("ring", false),
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping a reminder that can't be read", e)
                    null
                }
            }
        } catch (e: JSONException) {
            Log.w(TAG, "Reminders couldn't be read", e)
            emptyList()
        }
    }
}
