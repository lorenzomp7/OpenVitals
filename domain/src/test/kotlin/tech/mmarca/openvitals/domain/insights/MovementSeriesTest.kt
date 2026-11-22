package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/** Speed and step cadence checked against the session's own distance and steps. */
class MovementSeriesTest {

    private val start = Instant.parse("2026-11-16T07:00:00Z")
    private val end = start.plusSeconds(1_800)
    private val run = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING

    private fun readings(value: Double, source: String = "watch", every: Long = 1) =
        (0L..1_800L step every).map { SessionReading(start.plusSeconds(it), value, source) }

    /** A straight track north at [metersPerSecond], one point a second. */
    private fun route(metersPerSecond: Double) = (0L..1_800L).map { second ->
        ExerciseRoutePoint(
            time = start.plusSeconds(second),
            latitude = 45.0 + second * metersPerSecond / 111_195.0,
            longitude = 9.0,
            altitudeMeters = null,
            horizontalAccuracyMeters = null,
            verticalAccuracyMeters = null,
        )
    }

    private fun mean(series: List<Pair<Instant, Double>>) = timeWeightedMean(series)!!

    @Test fun `a recorded speed that adds up to the distance is drawn as it is`() {
        val movement = movementSeries(run, start, end, readings(3.0), emptyList(), "watch", route(3.0), 5_400.0, null)
        assertEquals(3.0, mean(movement.speed), 0.01)
        assertFalse(movement.speedFromRoute)
    }

    @Test fun `a recorded speed that does not add up gives way to the GPS track`() {
        // Written in km/h as if it were m/s: 10.8 for a 3 m/s run.
        val movement = movementSeries(run, start, end, readings(10.8), emptyList(), "watch", route(3.0), 5_400.0, null)
        assertEquals(3.0, mean(movement.speed), 0.1)
        assertTrue(movement.speedFromRoute)
    }

    @Test fun `without a track a wrong unit is scaled to the distance`() {
        val movement = movementSeries(run, start, end, readings(10.8), emptyList(), "watch", emptyList(), 5_400.0, null)
        assertEquals(3.0, mean(movement.speed), 0.01)
    }

    @Test fun `the session's own app wins over the app with more readings`() {
        val speed = readings(3.0, "watch", every = 5) + readings(6.0, "phone")
        val movement = movementSeries(run, start, end, speed, emptyList(), "watch", emptyList(), null, null)
        assertEquals(3.0, mean(movement.speed), 0.01)
    }

    @Test fun `a cadence written per foot is doubled against the steps`() {
        // 5 100 steps over 30 minutes: 170 a minute; the app wrote 85.
        val movement = movementSeries(run, start, end, emptyList(), readings(85.0), "watch", emptyList(), null, 5_100)
        assertEquals(170.0, mean(movement.stepCadence), 0.5)
    }

    @Test fun `a walk's cadence that agrees with its steps is left alone`() {
        val walk = ExerciseSessionRecord.EXERCISE_TYPE_WALKING
        val movement = movementSeries(walk, start, end, emptyList(), readings(105.0), "watch", emptyList(), null, 3_150)
        assertEquals(105.0, mean(movement.stepCadence), 0.5)
    }

    @Test fun `readings inside a pause are left out`() {
        val pause = start.plusSeconds(600) to start.plusSeconds(900)
        val movement = movementSeries(run, start, end, readings(3.0), emptyList(), "watch", emptyList(), null, null, listOf(pause))
        assertTrue(movement.speed.none { !it.first.isBefore(pause.first) && it.first.isBefore(pause.second) })
    }
}
