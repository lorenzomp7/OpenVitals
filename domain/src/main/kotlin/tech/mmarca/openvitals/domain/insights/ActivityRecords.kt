package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSegment
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.prefersPaceExercises

/** A jump rope session in numbers: the jumps, how many a minute, and in how many sets. */
data class JumpRopeSummary(val jumps: Int, val jumpsPerMinute: Double, val sets: Int)

/**
 * The jumps counted in the session's jump rope segments, their rate over the time spent
 * jumping, and the sets. Null when no segment is jump rope with a count: Health Connect has no
 * jump rope exercise of its own, only the segment.
 */
fun jumpRopeSummary(segments: List<ExerciseSegmentData>): JumpRopeSummary? {
    val jumping = segments.filter {
        it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMP_ROPE && it.repetitions > 0 && it.durationMs > 0
    }
    if (jumping.isEmpty()) return null
    val jumps = jumping.sumOf { it.repetitions }
    val minutes = jumping.sumOf { it.durationMs } / 60_000.0
    return JumpRopeSummary(jumps, jumps / minutes, jumping.size)
}

/** What an activity can be the best of its kind at. */
enum class PersonalRecordKind { DISTANCE, DURATION, PACE, SPEED, JUMPS }

data class PersonalRecord(val kind: PersonalRecordKind, val value: Double)

/** A pace or speed over less than this is a stroll to the start line, not a record. */
private const val RecordMinimumDistanceMeters = 1_000.0

/**
 * The records [current] holds against [others] of the same kind: the longest distance and
 * duration, the fastest average pace (or speed, for a kind read in speed) over at least 1 km,
 * and the most jumps. A tie with an earlier session is not a record. [others] may include
 * [current]; it is left out by id.
 */
fun personalRecords(current: ExerciseData, others: List<ExerciseData>): List<PersonalRecord> {
    val rest = others.filter { it.id != current.id && it.exerciseType == current.exerciseType }
    if (rest.isEmpty()) return emptyList()
    fun distance(it: ExerciseData) = it.totalDistanceMeters?.takeIf { d -> d.isFinite() && d > 0.0 }
    fun speed(it: ExerciseData): Double? {
        val meters = distance(it)?.takeIf { d -> d >= RecordMinimumDistanceMeters } ?: return null
        val seconds = it.durationMs / 1_000.0
        return (meters / seconds).takeIf { s -> seconds > 0 && s.isFinite() }
    }
    fun jumps(it: ExerciseData) = jumpRopeSummary(it.segments)?.jumps?.toDouble()
    val records = mutableListOf<PersonalRecord>()
    fun beats(mine: Double?, theirs: List<Double?>, kind: PersonalRecordKind, shown: Double? = mine) {
        if (mine == null || mine <= 0.0) return
        if (theirs.filterNotNull().all { mine > it }) records += PersonalRecord(kind, shown ?: mine)
    }
    beats(distance(current), rest.map(::distance), PersonalRecordKind.DISTANCE)
    beats(current.durationMs.toDouble(), rest.map { it.durationMs.toDouble() }, PersonalRecordKind.DURATION)
    val mySpeed = speed(current)
    if (current.exerciseType in prefersPaceExercises) {
        // Shown as seconds per kilometre.
        beats(mySpeed, rest.map(::speed), PersonalRecordKind.PACE, mySpeed?.let { 1_000.0 / it })
    } else {
        beats(mySpeed, rest.map(::speed), PersonalRecordKind.SPEED)
    }
    beats(jumps(current), rest.map(::jumps), PersonalRecordKind.JUMPS)
    return records
}
