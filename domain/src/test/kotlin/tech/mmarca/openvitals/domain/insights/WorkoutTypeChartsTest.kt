package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample

class WorkoutTypeChartsTest {
    private val start = Instant.parse("2026-11-10T18:00:00Z")
    private fun at(seconds: Long) = start.plusSeconds(seconds)
    private fun hr(seconds: Long, bpm: Long) = HeartRateSample(at(seconds), bpm, "w")

    @Test fun `each kind of activity gets its own chart`() {
        assertEquals(WorkoutTypeChart.PACE, workoutTypeChart(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING))
        assertEquals(WorkoutTypeChart.PACE, workoutTypeChart(ExerciseSessionRecord.EXERCISE_TYPE_WALKING))
        assertNull(workoutTypeChart(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL))
        assertEquals(WorkoutTypeChart.ELEVATION_WITH_SPEED, workoutTypeChart(ExerciseSessionRecord.EXERCISE_TYPE_BIKING))
        assertEquals(WorkoutTypeChart.CADENCE_WITH_HEART_RATE, workoutTypeChart(ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY))
        assertEquals(WorkoutTypeChart.HEART_RATE_WITH_SETS, workoutTypeChart(ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING))
        assertNull(workoutTypeChart(ExerciseSessionRecord.EXERCISE_TYPE_YOGA))
    }

    @Test fun `the maximum is the profile's, else from age`() {
        assertEquals(190, workoutMaxHeartRate(190, 40))
        assertEquals(180, workoutMaxHeartRate(null, 40))
        assertNull(workoutMaxHeartRate(null, null))
    }

    @Test fun `time in zones adds up and a long gap is not counted`() {
        // Max 200: 110 is zone 1, 150 zone 3, 185 zone 5.
        val samples = listOf(hr(0, 110), hr(10, 110), hr(20, 150), hr(30, 150), hr(40, 185), hr(200, 185))
        val zones = heartRateZoneSeconds(samples, start, at(210), 200)
        assertEquals(listOf(20L, 0L, 20L, 0L, 30L + 10L), zones)
    }

    @Test fun `pace is the inverse of speed, smoothed, without standing still`() {
        val speed = listOf(SpeedSample(at(0), 4.0, "w"), SpeedSample(at(10), 4.0, "w"), SpeedSample(at(20), 0.2, "w"))
        val pace = paceFromSpeed(speed)
        assertEquals(2, pace.size)
        assertEquals(250.0, pace.last().second, 0.01)
    }

    @Test fun `pace comes from the route when no speed was recorded`() {
        // 0.001° of latitude is about 111 m; 111 m in 30 s is about 4:30 per km.
        val points = (0..6).map { ExerciseRoutePoint(at(it * 10L), 45.0 + it * 0.000_37, 9.0, null, null, null) }
        val pace = paceFromRoute(points)
        assertTrue(pace.isNotEmpty())
        assertEquals(243.0, pace.last().second, 5.0)
    }

    @Test fun `running, the treadmill and both bikes get the zones`() {
        assertTrue(showsHeartRateZones(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING))
        assertTrue(showsHeartRateZones(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL))
        assertTrue(showsHeartRateZones(ExerciseSessionRecord.EXERCISE_TYPE_BIKING))
        assertTrue(showsHeartRateZones(ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY))
        assertFalse(showsHeartRateZones(ExerciseSessionRecord.EXERCISE_TYPE_WALKING))
        assertFalse(showsSpeedChart(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL))
        assertTrue(showsSpeedChart(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING))
    }

    @Test fun `the pace series that covers the session wins over a few readings at the end`() {
        val speedAtEnd = listOf(at(590) to 300.0, at(600) to 295.0)
        val route = (0..60).map { at(it * 10L) to 297.0 }
        assertEquals(route, bestPaceSeries(listOf(speedAtEnd, route), start, at(600)))
        // At par the first, recorded, series stays.
        val speedWhole = (0..60).map { at(it * 10L) to 296.0 }
        assertEquals(speedWhole, bestPaceSeries(listOf(speedWhole, route), start, at(600)))
    }

    @Test fun `the average pace is time over distance, not the mean of the paces`() {
        // 30 s at 4:00/km then 30 s at 6:00/km: 125 m + 83.3 m in 60 s is 4:48/km, not 5:00.
        val series = listOf(at(0) to 240.0, at(30) to 360.0, at(60) to 360.0)
        assertEquals(288.0, averagePaceSecondsPerKm(series)!!, 0.5)
    }

    @Test fun `sets are the segments that are not rests`() {
        val segments = listOf(
            ExerciseSegmentData(at(0), at(40), ExerciseSegment.EXERCISE_SEGMENT_TYPE_BENCH_PRESS, 10),
            ExerciseSegmentData(at(40), at(120), ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST, 0),
            ExerciseSegmentData(at(120), at(160), ExerciseSegment.EXERCISE_SEGMENT_TYPE_BENCH_PRESS, 10),
        )
        assertEquals(listOf(at(0) to at(40), at(120) to at(160)), strengthSetSpans(segments, at(0), at(600)))
    }

    @Test fun `a segment covering the workout is not a set`() {
        val whole = listOf(ExerciseSegmentData(at(0), at(3_600), ExerciseSegment.EXERCISE_SEGMENT_TYPE_UNKNOWN, 0))
        assertTrue(strengthSetSpans(whole, at(0), at(3_660)).isEmpty())
    }

    @Test fun `recovery is estimated past the last reading, falling toward rest`() {
        val curve = recoveryCurve(listOf(0L to 105L, 5L to 109L), lastSessionBpm = 105, restingBpm = 60)!!
        assertEquals(105.0, curve.endBpm, 0.0)
        assertTrue(curve.estimated.isNotEmpty())
        assertEquals(5L, curve.estimated.first().first)
        assertEquals(109.0, curve.estimated.first().second, 0.01)
        val oneMinute = curve.oneMinute!!
        assertTrue(oneMinute.estimated)
        assertTrue(oneMinute.bpm < 109.0 && oneMinute.bpm > 60.0)
        assertTrue(curve.twoMinutes!!.bpm < oneMinute.bpm)
    }

    @Test fun `a recorded recovery needs no estimate`() {
        val trace = listOf(0L to 150L, 60L to 128L, 120L to 112L)
        val curve = recoveryCurve(trace, lastSessionBpm = 150, restingBpm = 55)!!
        assertTrue(curve.estimated.isEmpty())
        assertEquals(RecoveryMark(128.0, estimated = false), curve.oneMinute)
    }

    @Test fun `with no reading after the end the last one of the session starts the estimate`() {
        val curve = recoveryCurve(emptyList(), lastSessionBpm = 120, restingBpm = null)!!
        assertEquals(120.0, curve.endBpm, 0.0)
        assertEquals(0L, curve.estimated.first().first)
        assertNull(recoveryCurve(emptyList(), lastSessionBpm = null, restingBpm = null))
    }

    @Test fun `recovery follows the heart for two minutes after the end`() {
        val samples = listOf(hr(590, 170), hr(600, 168), hr(660, 140), hr(720, 120), hr(800, 100))
        val trace = recoveryTrace(samples, at(600))
        assertEquals(listOf(0L to 170L, 0L to 168L, 60L to 140L, 120L to 120L), trace)
    }
}
