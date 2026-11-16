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
enum class WorkoutTypeChart { PACE, ELEVATION_WITH_SPEED, CADENCE_WITH_HEART_RATE, HEART_RATE_WITH_SETS }

/** Which kind-specific chart a session of [exerciseType] gets, if any. */
fun workoutTypeChart(exerciseType: Int): WorkoutTypeChart? = when (exerciseType) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
    // 117: a treadmill's pace, from the belt's speed or the splits.
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    -> WorkoutTypeChart.PACE
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> WorkoutTypeChart.ELEVATION_WITH_SPEED
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> WorkoutTypeChart.CADENCE_WITH_HEART_RATE
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
    -> WorkoutTypeChart.HEART_RATE_WITH_SETS
    else -> null
}

/** 110: the kinds that get the time-in-zone chart, besides their own. */
private val heartRateZoneExercises: Set<Int> = setOf(
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
)

/** 110: whether a session of [exerciseType] gets the time in each heart rate zone. */
fun showsHeartRateZones(exerciseType: Int): Boolean = exerciseType in heartRateZoneExercises

/** 112: a treadmill's speed is the belt's setting, not a reading worth a chart. */
fun showsSpeedChart(exerciseType: Int): Boolean = exerciseType != ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL

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

/**
 * 88: of the pace series a session has, the one that covers most of it. A watch can write its
 * speed only for the last seconds while the route or the splits run the whole way: drawing the
 * speed then put the whole line at the end. A recorded series wins a tie.
 */
fun bestPaceSeries(
    candidates: List<List<Pair<Instant, Double>>>,
    sessionStart: Instant,
    sessionEnd: Instant,
): List<Pair<Instant, Double>> {
    val sessionMillis = Duration.between(sessionStart, sessionEnd).toMillis().coerceAtLeast(1)
    fun coverage(series: List<Pair<Instant, Double>>): Double {
        val inside = series.filter { !it.first.isBefore(sessionStart) && !it.first.isAfter(sessionEnd) }
        if (inside.size < 2) return 0.0
        return Duration.between(inside.first().first, inside.last().first).toMillis().toDouble() / sessionMillis
    }
    var best: List<Pair<Instant, Double>> = emptyList()
    var bestCoverage = 0.0
    candidates.forEach { series ->
        val sorted = series.sortedBy { it.first }
        val covered = coverage(sorted)
        // A later candidate must cover clearly more: a recorded speed beats a reconstruction at par.
        if (covered > bestCoverage + 0.1 || best.isEmpty() && sorted.size >= 2) {
            best = sorted
            bestCoverage = covered
        }
    }
    return best
}

/**
 * 88: the average pace of a series, as time over distance: the time-weighted mean speed,
 * inverted. The mean of the paces themselves leans toward the slow stretches.
 */
fun averagePaceSecondsPerKm(series: List<Pair<Instant, Double>>): Double? {
    if (series.isEmpty()) return null
    if (series.size == 1) return series.single().second
    var seconds = 0.0
    var meters = 0.0
    for (i in 0 until series.lastIndex) {
        val dt = Duration.between(series[i].first, series[i + 1].first).toMillis() / 1_000.0
        if (dt <= 0.0 || series[i].second <= 0.0) continue
        val step = dt.coerceAtMost(paceSmoothing.seconds.toDouble() * 2)
        seconds += step
        meters += step * 1_000.0 / series[i].second
    }
    return if (meters > 0.0) seconds / meters * 1_000.0 else series.map { it.second }.average()
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

/**
 * 92: the stretches of a strength session spent on a set: every segment that is not a rest or
 * a pause. 102: a segment over half the session is the workout itself, as some watches write
 * it, not a set: shading it greyed the whole chart.
 */
fun strengthSetSpans(
    segments: List<ExerciseSegmentData>,
    sessionStart: Instant,
    sessionEnd: Instant,
): List<Pair<Instant, Instant>> {
    val session = Duration.between(sessionStart, sessionEnd)
    return segments
        .filter {
            it.segmentType != ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST &&
                it.segmentType != ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE &&
                it.endTime.isAfter(it.startTime) &&
                Duration.between(it.startTime, it.endTime).multipliedBy(2) < session
        }
        .sortedBy { it.startTime }
        .map { it.startTime to it.endTime }
}

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

/**
 * 103: the heart rate after the end, read where it was recorded and estimated where it was
 * not, falling from the last reading toward the resting rate with a two-minute time constant.
 */
data class RecoveryCurve(
    /** Seconds since the end to bpm, as recorded. */
    val measured: List<Pair<Long, Double>>,
    /** Seconds since the end to bpm, estimated past the last reading. Empty when none was needed. */
    val estimated: List<Pair<Long, Double>>,
    val endBpm: Double,
    val oneMinute: RecoveryMark?,
    val twoMinutes: RecoveryMark?,
    /** The resting rate the estimate falls toward, when there is an estimate. */
    val restingBpm: Int,
)

data class RecoveryMark(val bpm: Double, val estimated: Boolean)

/** The fall's time constant: about a third of the way to rest in the first minute. */
private const val RecoveryTimeConstantSeconds = 120.0
private const val DefaultRestingBpm = 60

fun recoveryCurve(trace: List<Pair<Long, Long>>, lastSessionBpm: Long?, restingBpm: Int?): RecoveryCurve? {
    val measured = trace.map { it.first to it.second.toDouble() }
    val anchor = measured.lastOrNull() ?: lastSessionBpm?.let { 0L to it.toDouble() } ?: return null
    val endBpm = measured.firstOrNull()?.second ?: anchor.second
    val window = recoveryChartWindow.seconds
    val rest = minOf((restingBpm ?: DefaultRestingBpm).toDouble(), anchor.second - 5.0)
    val estimated = if (anchor.first >= window - 10) {
        emptyList()
    } else {
        (anchor.first..window step 5).map { t ->
            t to rest + (anchor.second - rest) * kotlin.math.exp(-(t - anchor.first) / RecoveryTimeConstantSeconds)
        }
    }
    fun at(seconds: Long): RecoveryMark? =
        measured.filter { kotlin.math.abs(it.first - seconds) <= 20 }.minByOrNull { kotlin.math.abs(it.first - seconds) }
            ?.let { RecoveryMark(it.second, estimated = false) }
            ?: estimated.minByOrNull { kotlin.math.abs(it.first - seconds) }
                ?.takeIf { kotlin.math.abs(it.first - seconds) <= 5 }
                ?.let { RecoveryMark(it.second, estimated = true) }
    return RecoveryCurve(measured, estimated, endBpm, at(60), at(120), rest.roundToInt())
}
