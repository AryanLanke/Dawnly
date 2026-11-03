package com.dawnly.app.data.learn

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dawnly.app.data.safeData
import com.dawnly.app.data.settings.SettingsRepository
import com.dawnly.app.domain.learn.Answer
import com.dawnly.app.domain.learn.Learned
import com.dawnly.app.domain.learn.Learning
import com.dawnly.app.domain.learn.Morning
import com.dawnly.app.domain.learn.SignShares
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.learningStore by preferencesDataStore(name = "learning")

/**
 * Every time Dawnly asked "Up already?" and what the user answered, the last [KEEP] of them, kept
 * on the phone. [Learning] turns them into what Dawnly has learned.
 */
class LearningStore(context: Context) {
    private val store = context.applicationContext.learningStore

    val mornings: Flow<List<Morning>> = store.safeData
        .map { prefs -> prefs[LIST]?.let(::decode).orEmpty() }
        .distinctUntilChanged()

    /** What's been learned, or nothing while the user has learning off. */
    fun learned(settings: SettingsRepository): Flow<Learned> =
        combine(mornings, settings.learn) { list, on ->
            if (on) Learning.learn(list, ZoneId.systemDefault()) else Learned.Nothing
        }.distinctUntilChanged()

    suspend fun current(settings: SettingsRepository): Learned = learned(settings).first()

    /** Dawnly just asked (or is about to dismiss by itself). */
    suspend fun asked(at: Instant, shares: SignShares) = update { it + Morning(at, shares) }

    /**
     * The user answered the latest question, if it was asked in the last few hours and not
     * answered yet. Anything else (a dismiss with no question) teaches nothing.
     */
    suspend fun answer(answer: Answer, at: Instant) = update { list ->
        val last = list.lastOrNull()
        if (last == null || last.answer != null || Duration.between(last.askedAt, at) > RECENT) {
            list
        } else {
            list.dropLast(1) + last.copy(answer = answer, answeredAt = at)
        }
    }

    suspend fun clear() {
        store.edit { it.remove(LIST) }
    }

    private suspend fun update(change: (List<Morning>) -> List<Morning>) {
        store.edit { prefs -> prefs[LIST] = encode(change(prefs[LIST]?.let(::decode).orEmpty()).takeLast(KEEP)) }
    }

    private companion object {
        const val TAG = "LearningStore"
        const val KEEP = 60
        val RECENT: Duration = Duration.ofHours(4)
        val LIST = stringPreferencesKey("mornings")

        fun encode(list: List<Morning>): String = JSONArray().apply {
            for (m in list) {
                put(
                    JSONObject()
                        .put("asked", m.askedAt.toEpochMilli())
                        .putShare("screen", m.shares.screen)
                        .putShare("apps", m.shares.apps)
                        .putShare("steps", m.shares.steps)
                        .putShare("movement", m.shares.movement)
                        .apply {
                            m.answer?.let { put("answer", it.name) }
                            m.answeredAt?.let { put("answered", it.toEpochMilli()) }
                        },
                )
            }
        }.toString()

        fun JSONObject.putShare(key: String, value: Float?): JSONObject = if (value == null) this else put(key, value.toDouble())

        fun JSONObject.share(key: String): Float? = if (has(key)) getDouble(key).toFloat() else null

        fun decode(json: String): List<Morning> = try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Morning(
                    askedAt = Instant.ofEpochMilli(o.getLong("asked")),
                    shares = SignShares(o.share("screen"), o.share("apps"), o.share("steps"), o.share("movement")),
                    answer = o.optString("answer").takeIf { it.isNotEmpty() }?.let { name -> Answer.entries.firstOrNull { it.name == name } },
                    answeredAt = if (o.has("answered")) Instant.ofEpochMilli(o.getLong("answered")) else null,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "What Dawnly learned couldn't be read; starting over", e)
            emptyList()
        }
    }
}
