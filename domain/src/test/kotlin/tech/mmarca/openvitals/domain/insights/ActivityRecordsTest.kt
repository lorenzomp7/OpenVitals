package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData

class ActivityRecordsTest {
    private val start = Instant.parse("2026-07-03T18:00:00Z")
    private fun at(seconds: Long) = start.plusSeconds(seconds)

    private fun session(id: String, type: Int, minutes: Long, meters: Double?, segments: List<ExerciseSegmentData> = emptyList()) =
        ExerciseData(
            id = id,
            title = null,
            exerciseType = type,
            source = "watch",
            startTime = start,
            endTime = start.plusSeconds(minutes * 60),
            durationMs = minutes * 60_000,
            totalDistanceMeters = meters,
            segments = segments,
        )

    @Test fun `jumps are counted from the jump rope segments`() {
        val segments = listOf(
            ExerciseSegmentData(at(0), at(150), ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMP_ROPE, 200),
            ExerciseSegmentData(at(150), at(300), ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST, 0),
            ExerciseSegmentData(at(300), at(390), ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMP_ROPE, 139),
        )
        val summary = jumpRopeSummary(segments)!!
        assertEquals(339, summary.jumps)
        assertEquals(2, summary.sets)
        assertEquals(339 / 4.0, summary.jumpsPerMinute, 0.01)
        assertNull(jumpRopeSummary(segments.filter { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST }))
    }

    @Test fun `the longest and fastest of its kind are records, a tie is not`() {
        val hike = ExerciseSessionRecord.EXERCISE_TYPE_HIKING
        val current = session("now", hike, 116, 8_330.0)
        val others = listOf(
            session("a", hike, 90, 6_000.0),
            session("b", hike, 116, 9_000.0),
            session("c", ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, 300, 40_000.0),
        )
        val kinds = personalRecords(current, others).map { it.kind }
        // Not the farthest (9 km before), as long as "b" (a tie) and slower than it.
        assertTrue(kinds.isEmpty())
        val farther = personalRecords(session("now", hike, 100, 10_000.0), others)
        assertEquals(
            listOf(PersonalRecordKind.DISTANCE, PersonalRecordKind.PACE),
            farther.map { it.kind },
        )
        assertEquals(600.0, farther.last().value, 0.01)
    }

    @Test fun `no earlier session of the kind, no records`() {
        assertTrue(personalRecords(session("x", 1, 10, 1_000.0), emptyList()).isEmpty())
    }
}
