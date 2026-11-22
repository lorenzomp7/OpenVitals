package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.core.geo.haversineMeters
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/**
 * The speed and step cadence an activity's charts draw, checked against the session's own
 * totals. The speed, the cadence and the stride (speed over cadence) come from here, so the
 * three charts agree with each other and with the distance and steps in the summary.
 *
 * [speedFromRoute]: the speed is the GPS track's, because the recorded series was missing or
 * did not add up to the distance.
 */
data class MovementSeries(
    val speed: List<Pair<Instant, Double>>,
    val stepCadence: List<Pair<Instant, Double>>,
    val speedFromRoute: Boolean = false,
)

/** A recorded mean within this share of the session's own average is believed as it is. */
private const val MovementTolerance = 0.2

/** Below this, no one is moving: a reading at a stop is not part of the mean. */
private const val MovingSpeedMetersPerSecond = 0.3
private const val MovingCadenceStepsPerMinute = 20.0

/**
 * [speedReadings] and [stepCadenceReadings] as the charts should draw them.
 *
 * - Only the readings inside the session and outside its [pauses]; the app that wrote the session
 *   ([preferredSource]) first, else the app with the most readings; cleaned of single wild
 *   readings ([sessionSeries]).
 * - The speed's moving average is checked against the distance over the moving time. When it is
 *   off by more than a fifth, the GPS track's speed is drawn if it agrees; else the recorded
 *   shape is kept and scaled to the distance (a series written in the wrong unit).
 * - Without recorded speed, the GPS track's.
 * - The cadence's moving average is checked against the steps over the moving time: half of it
 *   is a cadence written per foot and is doubled, twice it is halved. Without the steps, a run
 *   under 110 a minute is doubled ([runningCadenceInSteps]).
 */
fun movementSeries(
    exerciseType: Int,
    start: Instant,
    end: Instant,
    speedReadings: List<SessionReading>,
    stepCadenceReadings: List<SessionReading>,
    preferredSource: String?,
    route: List<ExerciseRoutePoint>,
    distanceMeters: Double?,
    steps: Long?,
    pauses: List<Pair<Instant, Instant>> = emptyList(),
): MovementSeries {
    fun moving(time: Instant) = pauses.none { (from, to) -> !time.isBefore(from) && time.isBefore(to) }
    val pausedSeconds = pauses.sumOf { (from, to) ->
        Duration.between(maxOf(from, start), minOf(to, end)).seconds.coerceAtLeast(0)
    }
    val movingSeconds = (Duration.between(start, end).seconds - pausedSeconds).coerceAtLeast(0)

    // Speed.
    val recordedSpeed = sessionSeries(speedReadings, start, end, preferredSource = preferredSource)
        .filter { moving(it.first) }
    val routeSpeed = routeSpeedSeries(route, start, end).filter { moving(it.first) }
    val expectedSpeed = distanceMeters?.takeIf { it > 0.0 && movingSeconds >= 60 }?.let { it / movingSeconds }
    fun movingMean(series: List<Pair<Instant, Double>>) =
        timeWeightedMean(series.filter { it.second > MovingSpeedMetersPerSecond })
    fun agrees(series: List<Pair<Instant, Double>>): Boolean {
        val mean = movingMean(series) ?: return false
        return expectedSpeed != null && kotlin.math.abs(mean / expectedSpeed - 1.0) <= MovementTolerance
    }
    var fromRoute = false
    val speed = when {
        recordedSpeed.size >= 2 && (expectedSpeed == null || agrees(recordedSpeed)) -> recordedSpeed
        routeSpeed.size >= 2 && (expectedSpeed == null || agrees(routeSpeed)) -> {
            fromRoute = true
            routeSpeed
        }
        recordedSpeed.size >= 2 -> {
            val mean = movingMean(recordedSpeed)
            if (mean != null && mean > 0.0 && expectedSpeed != null) {
                recordedSpeed.map { (time, value) -> time to value * expectedSpeed / mean }
            } else {
                recordedSpeed
            }
        }
        routeSpeed.size >= 2 -> {
            fromRoute = true
            routeSpeed
        }
        else -> emptyList()
    }

    // Step cadence.
    val recordedCadence = sessionSeries(stepCadenceReadings, start, end, preferredSource = preferredSource)
        .filter { moving(it.first) }
    val expectedCadence = steps?.takeIf { it > 0 && movingSeconds >= 60 }?.let { it * 60.0 / movingSeconds }
    val cadenceMean = timeWeightedMean(recordedCadence.filter { it.second > MovingCadenceStepsPerMinute })
    val cadence = when {
        recordedCadence.isEmpty() -> emptyList()
        expectedCadence != null && cadenceMean != null -> {
            val ratio = cadenceMean / expectedCadence
            when {
                ratio in 0.4..0.65 -> recordedCadence.map { (time, value) -> time to value * 2.0 }
                ratio in 1.6..2.5 -> recordedCadence.map { (time, value) -> time to value / 2.0 }
                else -> recordedCadence
            }
        }
        else -> runningCadenceInSteps(exerciseType, recordedCadence)
    }
    return MovementSeries(speed = speed, stepCadence = cadence, speedFromRoute = fromRoute)
}

/**
 * The GPS track's speed: the distance covered over the [windowSeconds] before each point,
 * then the same median and mean [sessionSeries] uses. A point closer than five seconds to the
 * window's start, or a jump faster than [MaxGpsSpeedMetersPerSecond], is no speed.
 */
fun routeSpeedSeries(
    points: List<ExerciseRoutePoint>,
    start: Instant,
    end: Instant,
    windowSeconds: Long = 10,
): List<Pair<Instant, Double>> {
    val sorted = points.filter { !it.time.isBefore(start) && !it.time.isAfter(end) }.sortedBy { it.time }
    if (sorted.size < 2) return emptyList()
    val cumulative = DoubleArray(sorted.size)
    for (i in 1 until sorted.size) {
        val a = sorted[i - 1]
        val b = sorted[i]
        cumulative[i] = cumulative[i - 1] + haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
    }
    val raw = mutableListOf<Pair<Long, Double>>()
    var from = 0
    for (i in 1 until sorted.size) {
        while (Duration.between(sorted[from].time, sorted[i].time).seconds > windowSeconds) from++
        val seconds = Duration.between(sorted[from].time, sorted[i].time).toMillis() / 1_000.0
        if (seconds < 5.0) continue
        val speed = (cumulative[i] - cumulative[from]) / seconds
        if (speed <= MaxGpsSpeedMetersPerSecond) raw += sorted[i].time.epochSecond to speed
    }
    val perSecond = raw.groupBy { it.first }.toSortedMap().map { (second, group) -> second to group.map { it.second }.average() }
    if (perSecond.isEmpty()) return emptyList()
    val smoothed = rollingMedianThenMean(perSecond.map { it.first }, perSecond.map { it.second }, 5, 5)
    return perSecond.mapIndexed { index, (second, _) -> Instant.ofEpochSecond(second) to smoothed[index] }
}

/** Faster than a car in town: a GPS jump, not a run, a walk or a ride. */
private const val MaxGpsSpeedMetersPerSecond = 25.0
