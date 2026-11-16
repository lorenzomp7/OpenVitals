package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import tech.mmarca.openvitals.core.geo.haversineMeters
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample

/** The extra chart an activity gets for its kind (88–92); the recovery chart (93) is for any with a heart rate. */
enum class WorkoutTypeChart { PACE, HEART_RATE_ZONES, ELEVATION_WITH_SPEED, CADENCE_WITH_HEART_RATE, HEART_RATE_WITH_SETS }

/** Which kind-specific chart a session of [exerciseType] gets, if any. */
fun workoutTypeChart(exerciseType: Int): WorkoutTypeChart? = when (exerciseType) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
    -> WorkoutTypeChart.PACE
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> WorkoutTypeChart.HEART_RATE_ZONES
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> WorkoutTypeChart.ELEVATION_WITH_SPEED
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> WorkoutTypeChart.CADENCE_WITH_HEART_RATE
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
    -> WorkoutTypeChart.HEART_RATE_WITH_SETS
    else -> null
}

/** The heart rate a zone is a share of: the one the profile names, else Tanaka's 208 − 0.7 × age. */
fun workoutMaxHeartRate(explicitBpm: Int?, ageYears: Int?): Int? =
    explicitBpm?.takeIf { it in 100..240 } ?: ageYears?.takeIf { it in 5..110 }?.let { (208 - 0.7 * it).roundToInt() }

/** The lower edge of zones 2 to 5, as a share of the maximum: zone 1 is everything under 60%. */
val heartRateZoneFloors: List<Double> = listOf(0.6, 0.7, 0.8, 0.9)

/** A gap longer than this between two readings is not time spent at the earlier one. */
private val zoneSampleGap: Duration = Duration.ofSeconds(30)

/** 89: seconds in each of the five zones between [start] and [end]. Empty without readings. */
fun heartRateZoneSeconds(samples: List<HeartRateSample>, start: Instant, end: Instant, maxBpm: Int): List<Long> {
    val inSession = samples.filter { !it.time.isBefore(start) && it.time.isBefore(end) }.sortedBy { it.time }
    if (inSession.isEmpty() || maxBpm <= 0) return emptyList()
    val seconds = LongArray(5)
    inSession.forEachIndexed { index, sample ->
        val next = inSession.getOrNull(index + 1)?.time ?: end
        val until = minOf(next, sample.time.plus(zoneSampleGap), end)
        val share = sample.beatsPerMinute.toDouble() / maxBpm
        val zone = heartRateZoneFloors.count { share >= it }
        seconds[zone] += Duration.between(sample.time, until).seconds.coerceAtLeast(0)
    }
    return seconds.toList()
}

/** Paces slower than this are standing still, not a pace to draw: 20 min/km. */
private const val SlowestPaceSecondsPerKm = 1_200.0

/** Readings are averaged over this window, so a GPS jitter is not a sprint. */
private val paceSmoothing: Duration = Duration.ofSeconds(30)

/** 88: seconds per kilometre over time, from the recorded speed. */
fun paceFromSpeed(samples: List<SpeedSample>): List<Pair<Instant, Double>> =
    smoothed(
        samples.sortedBy { it.time }
            .filter { it.metersPerSecond > 0.0 }
            .map { it.time to 1_000.0 / it.metersPerSecond }
            .filter { it.second <= SlowestPaceSecondsPerKm },
    )

/** 88: seconds per kilometre over time, from the route when no speed was recorded. */
fun paceFromRoute(points: List<ExerciseRoutePoint>): List<Pair<Instant, Double>> {
    val sorted = points.sortedBy { it.time }
    if (sorted.size < 2) return emptyList()
    val cumulative = DoubleArray(sorted.size)
    for (i in 1 until sorted.size) {
        val a = sorted[i - 1]
        val b = sorted[i]
        cumulative[i] = cumulative[i - 1] + haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
    }
    val raw = mutableListOf<Pair<Instant, Double>>()
    var from = 0
    for (i in 1 until sorted.size) {
        while (Duration.between(sorted[from].time, sorted[i].time) > paceSmoothing) from++
        val seconds = Duration.between(sorted[from].time, sorted[i].time).toMillis() / 1_000.0
        val meters = cumulative[i] - cumulative[from]
        if (seconds < 5.0 || meters <= 0.0) continue
        val pace = seconds / meters * 1_000.0
        if (pace <= SlowestPaceSecondsPerKm) raw += sorted[i].time to pace
    }
    return raw
}

private fun smoothed(values: List<Pair<Instant, Double>>): List<Pair<Instant, Double>> {
    var from = 0
    var sum = 0.0
    return values.mapIndexed { index, (time, value) ->
        sum += value
        while (Duration.between(values[from].first, time) > paceSmoothing) {
            sum -= values[from].second
            from++
        }
        time to sum / (index - from + 1)
    }
}

/** 92: the stretches of a strength session spent on a set: every segment that is not a rest or a pause. */
fun strengthSetSpans(segments: List<ExerciseSegmentData>): List<Pair<Instant, Instant>> =
    segments
        .filter {
            it.segmentType != ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST &&
                it.segmentType != ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE &&
                it.endTime.isAfter(it.startTime)
        }
        .sortedBy { it.startTime }
        .map { it.startTime to it.endTime }

/** How long after the end the recovery chart follows the heart. */
val recoveryChartWindow: Duration = Duration.ofMinutes(2)

/** A reading this close before the end stands for the heart rate at the end. */
private val recoveryLeadIn: Duration = Duration.ofSeconds(15)

/** 93: the heart rate from the end of the session to two minutes after, as seconds since the end. */
fun recoveryTrace(samples: List<HeartRateSample>, sessionEnd: Instant): List<Pair<Long, Long>> {
    val from = sessionEnd.minus(recoveryLeadIn)
    val until = sessionEnd.plus(recoveryChartWindow)
    return samples
        .filter { !it.time.isBefore(from) && !it.time.isAfter(until) }
        .sortedBy { it.time }
        .map { Duration.between(sessionEnd, it.time).seconds.coerceAtLeast(0) to it.beatsPerMinute }
}
