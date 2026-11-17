package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 136: speed and cadence as the charts draw them. */
class SessionSeriesTest {

    private val start = Instant.parse("2026-11-16T19:18:00Z")
    private val end = start.plusSeconds(600)

    @Test fun `one app's readings, inside the session only`() {
        val watch = (0..100).map { SessionReading(start.plusSeconds(it.toLong()), 3.0, "watch") }
        val phone = (0..20).map { SessionReading(start.plusSeconds(it * 5L), 9.0, "phone") }
        val after = SessionReading(end.plusSeconds(30), 3.0, "watch")

        val series = sessionSeries(watch + phone + after, start, end)

        assertTrue(series.all { it.second == 3.0 })
        assertTrue(series.all { !it.first.isAfter(end) })
    }

    @Test fun `a single wild reading is taken out`() {
        val readings = (0..60).map { SessionReading(start.plusSeconds(it.toLong()), if (it == 30) 15.0 else 3.0, "watch") }

        assertTrue(sessionSeries(readings, start, end).all { kotlin.math.abs(it.second - 3.0) < 1e-9 })
    }

    @Test fun `the mean is weighted by time`() {
        val series = listOf(start to 2.0, start.plusSeconds(10) to 4.0, start.plusSeconds(11) to 4.0)

        assertEquals((2.0 * 10 + 4.0 * 1) / 11, timeWeightedMean(series)!!, 1e-9)
    }

    @Test fun `a run written per foot is counted in steps`() {
        val series = (0..10).map { start.plusSeconds(it.toLong()) to 84.0 }

        val run = runningCadenceInSteps(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, series)
        val walk = runningCadenceInSteps(ExerciseSessionRecord.EXERCISE_TYPE_WALKING, series)

        assertTrue(run.all { it.second == 168.0 })
        assertTrue(walk.all { it.second == 84.0 })
    }

    @Test fun `a run already in steps is left alone`() {
        val series = (0..10).map { start.plusSeconds(it.toLong()) to 168.0 }

        assertEquals(series, runningCadenceInSteps(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL, series))
    }
}
