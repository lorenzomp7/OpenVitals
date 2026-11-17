package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import kotlin.math.exp
import kotlin.math.roundToInt
import tech.mmarca.openvitals.core.geo.haversineMeters

/** One point of a route with its height, for the gradient of the ground under it. */
data class GradePoint(
    val time: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
)

/** 117: how long a session spent going up, on the flat and going down. */
data class GradientDistribution(
    val uphillSeconds: Long,
    val flatSeconds: Long,
    val downhillSeconds: Long,
) {
    val totalSeconds: Long get() = uphillSeconds + flatSeconds + downhillSeconds
}

/** A grade within this of level, either way, is flat: 2%. */
const val FlatGradeLimit = 0.02

/** The grade is read over stretches this long, so a metre of GPS wander is not a climb. */
private const val GradeStretchMeters = 50.0

/** A gap longer than this between two points is a pause, not time on any slope. */
private val gradeGap: Duration = Duration.ofSeconds(60)

/**
 * 117: the time spent uphill (steeper than +2%), on the flat and downhill (steeper than −2%),
 * reading the grade over every 50 m of the route. Null without two points or any time.
 */
fun gradientDistribution(points: List<GradePoint>): GradientDistribution? {
    val sorted = points.sortedBy { it.time }
    if (sorted.size < 2) return null
    var up = 0L
    var flat = 0L
    var down = 0L
    var anchor = 0
    var meters = 0.0
    fun close(end: Int) {
        if (meters <= 0.0) return
        val grade = (sorted[end].altitudeMeters - sorted[anchor].altitudeMeters) / meters
        val seconds = Duration.between(sorted[anchor].time, sorted[end].time).seconds.coerceAtLeast(0)
        when {
            grade > FlatGradeLimit -> up += seconds
            grade < -FlatGradeLimit -> down += seconds
            else -> flat += seconds
        }
    }
    for (i in 1 until sorted.size) {
        val a = sorted[i - 1]
        val b = sorted[i]
        if (Duration.between(a.time, b.time) > gradeGap) {
            close(i - 1)
            anchor = i
            meters = 0.0
            continue
        }
        meters += haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        if (meters >= GradeStretchMeters) {
            close(i)
            anchor = i
            meters = 0.0
        }
    }
    // The last stretch, if it is long enough to have a grade at all.
    if (meters >= GradeStretchMeters / 5) close(sorted.lastIndex)
    val result = GradientDistribution(up, flat, down)
    return result.takeIf { it.totalSeconds > 0 }
}

/** 117: an estimate of the session's aerobic and anaerobic effect, each on a 0–5 scale. */
data class TrainingEffect(val aerobic: Double, val anaerobic: Double)

/** What a training effect value means, as the watches word it. */
enum class TrainingEffectLevel { NONE, MINOR, MAINTAINING, IMPROVING, HIGHLY_IMPROVING, OVERREACHING }

fun trainingEffectLevel(value: Double): TrainingEffectLevel = when {
    value < 1.0 -> TrainingEffectLevel.NONE
    value < 2.0 -> TrainingEffectLevel.MINOR
    value < 3.0 -> TrainingEffectLevel.MAINTAINING
    value < 4.0 -> TrainingEffectLevel.IMPROVING
    value < 5.0 -> TrainingEffectLevel.HIGHLY_IMPROVING
    else -> TrainingEffectLevel.OVERREACHING
}

/**
 * How much a minute in each zone, 1 to 5, counts toward the aerobic effect. The effort's toll
 * climbs steeply with intensity, as the excess oxygen after exercise does. App heuristic, set
 * against the figures a Zepp watch gave for the same sessions; not Firstbeat's model.
 */
private val AerobicZoneWeights = doubleArrayOf(0.1, 0.3, 1.0, 3.0, 6.0)

/** The aerobic load at which the effect reaches 1, and how much more raises it by one. */
private const val AerobicFloorLoad = 6.0
private const val AerobicLoadPerPoint = 24.0

/** Minutes in zone 5 and zone 4 that make up the anaerobic stimulus. Same heuristic as above. */
private const val AnaerobicZone5Weight = 0.274
private const val AnaerobicZone4Weight = 0.0077

/**
 * 117: the aerobic and anaerobic effect from the seconds in each of the five zones (as
 * [heartRateZoneSeconds] gives them). Null without time in any zone. An estimate: the watches'
 * own figure comes from a model they do not publish.
 */
fun trainingEffect(zoneSeconds: List<Long>): TrainingEffect? {
    if (zoneSeconds.size != 5 || zoneSeconds.sum() <= 0L) return null
    val minutes = zoneSeconds.map { it / 60.0 }
    val load = minutes.indices.sumOf { minutes[it] * AerobicZoneWeights[it] }
    val aerobic = if (load < AerobicFloorLoad) {
        load / AerobicFloorLoad * (1.0 + AerobicFloorLoad / AerobicLoadPerPoint)
    } else {
        1.0 + load / AerobicLoadPerPoint
    }
    val stimulus = minutes[4] * AnaerobicZone5Weight + minutes[3] * AnaerobicZone4Weight
    val anaerobic = 5.0 * (1.0 - exp(-stimulus))
    return TrainingEffect(aerobic.coerceIn(0.0, 5.0).oneDecimal(), anaerobic.coerceIn(0.0, 5.0).oneDecimal())
}

private fun Double.oneDecimal(): Double = (this * 10).roundToInt() / 10.0

/**
 * 117: the bpm each zone, 1 to 5, spans for [maxBpm]: zone 1 has no floor and zone 5 no ceiling.
 * The edges are [heartRateZoneFloors], as [heartRateZoneSeconds] counts them.
 */
fun heartRateZoneBounds(maxBpm: Int): List<Pair<Int?, Int?>> {
    val floors = heartRateZoneFloors.map { kotlin.math.ceil(it * maxBpm).toInt() }
    return List(5) { zone ->
        val low = if (zone == 0) null else floors[zone - 1]
        val high = if (zone == 4) null else floors[zone] - 1
        low to high
    }
}

/** Steps per minute below this are standing or a sensor's noise, not a stride. */
private const val SlowestCadence = 30.0
private val strideMatch: Duration = Duration.ofSeconds(15)

/**
 * 117: the length of each step, in metres: the speed over the cadence, at each cadence reading
 * with a speed within 15 s of it. [cadence] is in steps per minute, [speed] in metres per second.
 * Lengths outside 20 cm–2.5 m are a mismatch of the two sensors, and left out.
 */
fun strideLengthSeries(
    cadence: List<Pair<Instant, Double>>,
    speed: List<Pair<Instant, Double>>,
): List<Pair<Instant, Double>> {
    val speeds = speed.filter { it.second > 0.3 }.sortedBy { it.first }
    if (speeds.isEmpty()) return emptyList()
    val times = speeds.map { it.first }
    return cadence.sortedBy { it.first }.filter { it.second >= SlowestCadence }.mapNotNull { (time, rate) ->
        val at = times.binarySearch(time).let { if (it >= 0) it else -it - 1 }
        val nearest = listOfNotNull(speeds.getOrNull(at - 1), speeds.getOrNull(at))
            .minByOrNull { Duration.between(it.first, time).abs() }
            ?.takeIf { Duration.between(it.first, time).abs() <= strideMatch }
            ?: return@mapNotNull null
        val stride = nearest.second * 60.0 / rate
        (time to stride).takeIf { stride in 0.2..2.5 }
    }
}
