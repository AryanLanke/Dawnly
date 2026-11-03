package com.dawnly.app.domain.learn

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** What the user answered when Dawnly asked "Up already?". */
enum class Answer {
    /** They were up: Dismiss alarm, or an automatic dismiss they let happen. */
    Up,

    /** Not yet: Dawnly asked too early. */
    NotYet,
}

/** How far each sign had got toward its goal (0 to 1), or null when it was off. */
data class SignShares(val screen: Float?, val apps: Float?, val steps: Float?, val movement: Float?)

/** One time Dawnly asked, and the answer once there is one. Unanswered ones teach nothing. */
data class Morning(
    val askedAt: Instant,
    val shares: SignShares,
    val answer: Answer? = null,
    val answeredAt: Instant? = null,
)

/** How much each sign is trusted, around 1. Steered by which signs showed on real mornings. */
data class SignWeights(val screen: Float = 1f, val apps: Float = 1f, val steps: Float = 1f, val movement: Float = 1f) {
    companion object {
        val Even = SignWeights()
    }
}

/**
 * What Dawnly has learned from the user's answers (see [Learning]): how cautious to be, when
 * they're usually up, and which signs to trust.
 */
data class Learned(
    /** Answered mornings it has learned from. */
    val mornings: Int,
    /** Goals are multiplied by this: above 1 waits for more, below 1 asks sooner. */
    val caution: Float,
    val usualWeekday: LocalTime?,
    val usualWeekend: LocalTime?,
    val weights: SignWeights,
) {
    /**
     * How much to multiply the goals by at [now]: the [caution] from the answers, and more
     * readily near the usual wake-up time, less so hours before it.
     */
    fun goalFactor(now: LocalDateTime): Float = (caution * timeFactor(now)).coerceIn(Learning.MIN_FACTOR, Learning.MAX_FACTOR)

    fun timeFactor(now: LocalDateTime): Float {
        val weekend = now.dayOfWeek == DayOfWeek.SATURDAY || now.dayOfWeek == DayOfWeek.SUNDAY
        val usual = (if (weekend) usualWeekend else usualWeekday) ?: return 1f
        // Minutes until the usual time, 0 to 1439; over half a day means it's already past.
        val ahead = Math.floorMod(usual.toSecondOfDay() / 60 - now.toLocalTime().toSecondOfDay() / 60, MINUTES_A_DAY)
        if (ahead > MINUTES_A_DAY / 2 || ahead <= NEAR_MINUTES) return NEAR_FACTOR
        if (ahead >= FAR_MINUTES) return FAR_FACTOR
        val t = (ahead - NEAR_MINUTES).toFloat() / (FAR_MINUTES - NEAR_MINUTES)
        return NEAR_FACTOR + (FAR_FACTOR - NEAR_FACTOR) * t
    }

    companion object {
        /** Before any answers, or with learning off: Dawnly as it ships. */
        val Nothing = Learned(0, 1f, null, null, SignWeights.Even)

        private const val MINUTES_A_DAY = 24 * 60

        /** Within 45 minutes of the usual time (or after it), goals are 10% easier. */
        private const val NEAR_MINUTES = 45
        private const val NEAR_FACTOR = 0.9f

        /** Two and a half hours or more before it, 30% harder: a 3 AM phone check is no morning. */
        private const val FAR_MINUTES = 150
        private const val FAR_FACTOR = 1.3f
    }
}

/**
 * Learns from the user's answers, all on the phone. Pure, so it's unit tested.
 *
 * 1. Answers: each Not yet makes Dawnly 15% more cautious; each quick Dismiss alarm (within 2
 *    minutes) 4% less, a slow one 1%. Caution stays between 0.75 and 1.5.
 * 2. Usual time: the middle of the last 10 times they were up, weekdays and weekends apart, once
 *    there are 3.
 * 3. Signs: a sign that was well on its way (30%+) on mornings they were up is trusted a little
 *    more; one that was on mornings they said Not yet, twice as much less. Between 0.7 and 1.3.
 */
object Learning {
    const val MIN_FACTOR = 0.7f
    const val MAX_FACTOR = 1.6f

    private const val MIN_CAUTION = 0.75f
    private const val MAX_CAUTION = 1.5f
    private val QUICK = Duration.ofMinutes(2)
    private const val USUAL_FROM = 3
    private const val USUAL_OF = 10
    private const val SHOWED = 0.3f

    fun learn(history: List<Morning>, zone: ZoneId): Learned {
        val answered = history.filter { it.answer != null }.sortedBy { it.askedAt }
        if (answered.isEmpty()) return Learned.Nothing

        var caution = 1f
        for (morning in answered) {
            caution *= when (morning.answer) {
                Answer.NotYet -> 1.15f
                else -> {
                    val quick = morning.answeredAt?.let { Duration.between(morning.askedAt, it) <= QUICK } ?: false
                    if (quick) 0.96f else 0.99f
                }
            }
            caution = caution.coerceIn(MIN_CAUTION, MAX_CAUTION)
        }

        val upTimes = answered.filter { it.answer == Answer.Up }.map { LocalDateTime.ofInstant(it.askedAt, zone) }
        fun usual(weekend: Boolean): LocalTime? {
            val times = upTimes
                .filter { (it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY) == weekend }
                .takeLast(USUAL_OF)
                .map { it.toLocalTime().toSecondOfDay() }
                .sorted()
            if (times.size < USUAL_FROM) return null
            return LocalTime.ofSecondOfDay(times[times.size / 2].toLong())
        }

        fun weight(share: (SignShares) -> Float?): Float {
            val up = answered.count { it.answer == Answer.Up && (share(it.shares) ?: 0f) >= SHOWED }
            val notYet = answered.count { it.answer == Answer.NotYet && (share(it.shares) ?: 0f) >= SHOWED }
            return (1f + 0.3f * (up - 2 * notYet) / maxOf(answered.size, 4)).coerceIn(0.7f, 1.3f)
        }

        return Learned(
            mornings = answered.size,
            caution = caution,
            usualWeekday = usual(weekend = false),
            usualWeekend = usual(weekend = true),
            weights = SignWeights(
                screen = weight { it.screen },
                apps = weight { it.apps },
                steps = weight { it.steps },
                movement = weight { it.movement },
            ),
        )
    }
}
