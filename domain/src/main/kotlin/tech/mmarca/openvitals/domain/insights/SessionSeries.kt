package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant

/** One reading of a series a session recorded, with the app that wrote it. */
data class SessionReading(val time: Instant, val value: Double, val source: String)

/**
 * 136: a speed or cadence series as a chart can draw it. Health Connect hands back every app's
 * samples over the session's window: a watch and the phone app that copies it interleave, which
 * drew a saw, and a record that runs past the session adds readings after its end. So: only the
 * readings inside the session, from the one app that wrote the most of them, one per second, then
 * a short rolling median that takes out a single wild reading and a short mean for the jitter.
 */
fun sessionSeries(
    readings: List<SessionReading>,
    start: Instant,
    end: Instant,
    medianHalfWindowSeconds: Long = 5,
    meanHalfWindowSeconds: Long = 5,
): List<Pair<Instant, Double>> {
    val inSession = readings.filter { !it.time.isBefore(start) && !it.time.isAfter(end) && it.value.isFinite() }
    if (inSession.isEmpty()) return emptyList()
    val source = inSession.groupingBy { it.source }.eachCount().maxByOrNull { it.value }!!.key
    val perSecond = inSession.filter { it.source == source }
        .groupBy { it.time.epochSecond }
        .toSortedMap()
        .map { (second, group) -> second to group.map { it.value }.average() }
    val smoothed = rollingMedianThenMean(
        seconds = perSecond.map { it.first },
        values = perSecond.map { it.second },
        medianHalfWindowSeconds = medianHalfWindowSeconds,
        meanHalfWindowSeconds = meanHalfWindowSeconds,
    )
    return perSecond.mapIndexed { index, (second, _) -> Instant.ofEpochSecond(second) to smoothed[index] }
}

/**
 * A median over [medianHalfWindowSeconds] each side, then a mean over [meanHalfWindowSeconds]
 * each side, both by time, not by count: a gap in the readings does not widen the window.
 * [seconds] must be sorted.
 */
fun rollingMedianThenMean(
    seconds: List<Long>,
    values: List<Double>,
    medianHalfWindowSeconds: Long,
    meanHalfWindowSeconds: Long,
): List<Double> {
    if (values.size < 3) return values
    fun windowed(input: List<Double>, half: Long, reduce: (List<Double>) -> Double): List<Double> {
        var from = 0
        var to = 0
        return input.indices.map { index ->
            while (seconds[from] < seconds[index] - half) from++
            if (to < index) to = index
            while (to + 1 <= input.lastIndex && seconds[to + 1] <= seconds[index] + half) to++
            reduce(input.subList(from, to + 1))
        }
    }
    val medians = windowed(values, medianHalfWindowSeconds) { window ->
        val sorted = window.sorted()
        val mid = sorted.size / 2
        if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }
    return windowed(medians, meanHalfWindowSeconds) { it.average() }
}

/**
 * 136: the mean of a series over time, each reading weighted by the time to the next, up to
 * [maxGapSeconds]: a stretch with a reading a second and one with a reading a minute count by
 * how long they lasted, not by how many readings they hold.
 */
fun timeWeightedMean(series: List<Pair<Instant, Double>>, maxGapSeconds: Long = 30): Double? {
    if (series.isEmpty()) return null
    if (series.size == 1) return series.first().second
    var weighted = 0.0
    var total = 0.0
    for (i in 0 until series.lastIndex) {
        val seconds = (series[i + 1].first.epochSecond - series[i].first.epochSecond).coerceIn(0, maxGapSeconds).toDouble()
        weighted += series[i].second * seconds
        total += seconds
    }
    return if (total > 0.0) weighted / total else series.map { it.second }.average()
}

/** 136: below this many steps a minute, a run's cadence has been written per foot. */
private const val RunningPerFootCadenceCeiling = 110.0

/**
 * 136: some apps write a run's cadence per foot (strides a minute, about 85) where Health
 * Connect asks for steps a minute (about 170): a run at under 110 steps a minute does not
 * happen, so such a series is counted in steps. Walks and other kinds are left as written.
 */
fun runningCadenceInSteps(exerciseType: Int?, series: List<Pair<Instant, Double>>): List<Pair<Instant, Double>> {
    if (exerciseType != ExerciseSessionRecord.EXERCISE_TYPE_RUNNING &&
        exerciseType != ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
    ) return series
    val moving = series.map { it.second }.filter { it > 20.0 }.sorted()
    if (moving.isEmpty()) return series
    val median = moving[moving.size / 2]
    return if (median < RunningPerFootCadenceCeiling) series.map { (time, value) -> time to value * 2.0 } else series
}
