package com.dawnly.app.data.alarm

import android.content.Context
import android.util.Log
import com.dawnly.app.domain.alarm.DawnlyAlarm
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** The user's Dawnly alarms, kept on the phone as a small JSON list. */
class DawnlyAlarmStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(decode(prefs.getString(LIST, null)))
    private val mutex = Mutex()

    val alarms: StateFlow<List<DawnlyAlarm>> = mutable.asStateFlow()

    suspend fun current(): List<DawnlyAlarm> = mutable.value

    /** Adds [alarm], or replaces the one with its id. */
    suspend fun save(alarm: DawnlyAlarm) = update { list ->
        if (list.any { it.id == alarm.id }) list.map { if (it.id == alarm.id) alarm else it } else list + alarm
    }

    suspend fun delete(id: Long) = update { list -> list.filterNot { it.id == id } }

    /** Commit before scheduling: apply() could lose a change on process death (see TimeStore). */
    suspend fun update(change: (List<DawnlyAlarm>) -> List<DawnlyAlarm>) = mutex.withLock {
        val list = change(mutable.value)
        prefs.edit().putString(LIST, encode(list)).commit()
        mutable.value = list
    }

    internal companion object {
        const val TAG = "DawnlyAlarmStore"
        private const val PREFS = "dawnly_alarms"
        private const val LIST = "list"

        fun encode(list: List<DawnlyAlarm>): String = JSONArray().apply {
            for (a in list) {
                put(
                    JSONObject()
                        .put("id", a.id)
                        .put("label", a.label)
                        .put("time", a.time.toString())
                        .put("on", a.on)
                        .put("days", JSONArray(a.days.map { it.name }))
                        .apply { a.sound?.let { put("sound", it) } },
                )
            }
        }.toString()

        /** An alarm that can't be read is skipped, not allowed to lose the rest. */
        fun decode(json: String?): List<DawnlyAlarm> = try {
            val array = JSONArray(json ?: "[]")
            (0 until array.length()).mapNotNull { i ->
                try {
                    val o = array.getJSONObject(i)
                    val days = o.optJSONArray("days")
                    DawnlyAlarm(
                        id = o.getLong("id"),
                        label = o.optString("label"),
                        time = LocalTime.parse(o.getString("time")),
                        days = (0 until (days?.length() ?: 0)).mapNotNull { j ->
                            runCatching { DayOfWeek.valueOf(days!!.getString(j)) }.getOrNull()
                        }.toSet(),
                        sound = o.optString("sound").takeIf { it.isNotEmpty() },
                        on = o.optBoolean("on", true),
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping an alarm that can't be read", e)
                    null
                }
            }
        } catch (e: JSONException) {
            Log.w(TAG, "Dawnly alarms couldn't be read", e)
            emptyList()
        }
    }
}