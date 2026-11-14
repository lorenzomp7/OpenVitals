package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.domain.model.ActivityCalorieSample
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.preferences.BiologicalSex

/** Energy burned in one slice of a session. */
data class CalorieInterval(
    val start: Instant,
    val end: Instant,
    val kcal: Double,
)

/** Where the slices came from: the heart rate through the Keytel equation, or the recorded calories. */
enum class CalorieIntervalSource { HEART_RATE, RECORDED }

data class SessionCalories(
    val intervals: List<CalorieInterval>,
    val source: CalorieIntervalSource,
    val bucket: Duration,
) {
    val totalKcal: Double get() = intervals.sumOf { it.kcal }
    val peakKcal: Double get() = intervals.maxOfOrNull { it.kcal } ?: 0.0
}

/** One slice per minute for a short session, wider for a long one, so a session draws 15 to 60 bars. */
fun calorieBucketFor(start: Instant, end: Instant): Duration {
    val minutes = Duration.between(start, end).toMinutes()
    return when {
        minutes <= 45 -> Duration.ofMinutes(1)
        minutes <= 90 -> Duration.ofMinutes(2)
        minutes <= 180 -> Duration.ofMinutes(5)
        else -> Duration.ofMinutes(10)
    }
}

/**
 * kcal per minute at [heartRateBpm] from Keytel et al. (2005), the heart-rate
 * equation most watches use without a VO2max. Without a recorded sex, the mean
 * of the two equations. Never negative: at rest the line crosses zero.
 */
fun keytelKcalPerMinute(heartRateBpm: Double, weightKg: Double, ageYears: Int, sex: BiologicalSex?): Double {
    val male = (-55.0969 + 0.6309 * heartRateBpm + 0.1988 * weightKg + 0.2017 * ageYears) / 4.184
    val female = (-20.4022 + 0.4472 * heartRateBpm - 0.1263 * weightKg + 0.074 * ageYears) / 4.184
    val rate = when (sex) {
        BiologicalSex.MALE -> male
        BiologicalSex.FEMALE -> female
        null -> (male + female) / 2.0
    }
    return rate.coerceAtLeast(0.0)
}

/**
 * The energy of each slice of a session, from heart rate when the profile has a
 * weight and an age and the session has heart rate, else from the recorded
 * running total. Null when neither is available. A slice without a heart-rate
 * sample is left out rather than guessed.
 */
fun sessionCalories(
    start: Instant,
    end: Instant,
    heartRate: List<HeartRateSample>,
    recorded: List<ActivityCalorieSample>,
    weightKg: Double?,
    ageYears: Int?,
    sex: BiologicalSex?,
): SessionCalories? {
    if (!start.isBefore(end)) return null
    val bucket = calorieBucketFor(start, end)
    if (heartRate.isNotEmpty() && weightKg != null && ageYears != null) {
        val sorted = heartRate.sortedBy { it.time }
        val intervals = buckets(start, end, bucket).mapNotNull { (from, to) ->
            val inSlice = sorted.filter { !it.time.isBefore(from) && it.time.isBefore(to) }
            if (inSlice.isEmpty()) return@mapNotNull null
            val bpm = inSlice.map { it.beatsPerMinute.toDouble() }.average()
            val minutes = Duration.between(from, to).seconds / 60.0
            CalorieInterval(from, to, keytelKcalPerMinute(bpm, weightKg, ageYears, sex) * minutes)
        }
        if (intervals.any { it.kcal > 0.0 }) return SessionCalories(intervals, CalorieIntervalSource.HEART_RATE, bucket)
    }
    val totals = recorded.sortedBy { it.time }
    if (totals.size < 2 || totals.last().cumulativeKcal < 1.0) return null
    val intervals = buckets(start, end, bucket).map { (from, to) ->
        CalorieInterval(from, to, (totalAt(totals, to) - totalAt(totals, from)).coerceAtLeast(0.0))
    }
    return SessionCalories(intervals, CalorieIntervalSource.RECORDED, bucket)
}

private fun buckets(start: Instant, end: Instant, bucket: Duration): List<Pair<Instant, Instant>> = buildList {
    var from = start
    while (from.isBefore(end)) {
        val to = from.plus(bucket).let { if (it.isAfter(end)) end else it }
        add(from to to)
        from = to
    }
}

/** The running total at [time], read linearly between the recorded points. */
private fun totalAt(totals: List<ActivityCalorieSample>, time: Instant): Double {
    if (!time.isAfter(totals.first().time)) return totals.first().cumulativeKcal
    if (!time.isBefore(totals.last().time)) return totals.last().cumulativeKcal
    val after = totals.indexOfFirst { it.time.isAfter(time) }
    val a = totals[after - 1]
    val b = totals[after]
    val span = Duration.between(a.time, b.time).toMillis().toDouble()
    val fraction = if (span <= 0.0) 1.0 else Duration.between(a.time, time).toMillis() / span
    return a.cumulativeKcal + (b.cumulativeKcal - a.cumulativeKcal) * fraction
}
