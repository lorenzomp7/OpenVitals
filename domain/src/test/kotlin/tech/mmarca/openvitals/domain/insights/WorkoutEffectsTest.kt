package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutEffectsTest {
    private val start = Instant.parse("2026-11-10T18:00:00Z")
    private fun at(seconds: Long) = start.plusSeconds(seconds)

    // 0.000 45° of latitude is about 50 m.
    private fun point(seconds: Long, step: Int, altitude: Double) =
        GradePoint(at(seconds), 45.0 + step * 0.000_45, 9.0, altitude)

    @Test fun `climbing, flat and descending stretches are timed apart`() {
        val points = listOf(
            point(0, 0, 100.0), point(20, 1, 105.0), // +10%: up
            point(40, 2, 105.2), // flat
            point(60, 3, 100.0), // down
        )
        val distribution = gradientDistribution(points)!!
        assertEquals(20L, distribution.uphillSeconds)
        assertEquals(20L, distribution.flatSeconds)
        assertEquals(20L, distribution.downhillSeconds)
    }

    @Test fun `a long gap is a pause, not time on a slope`() {
        val points = listOf(point(0, 0, 100.0), point(20, 1, 100.0), point(600, 2, 100.0), point(620, 3, 100.0))
        assertEquals(40L, gradientDistribution(points)!!.totalSeconds)
    }

    @Test fun `the training effect follows the time in the hard zones`() {
        fun minutes(vararg m: Double) = m.map { (it * 60).toLong() }
        // An easy ride, a hard run and a harder treadmill session, as a Zepp watch scored them: 2.7/0.2, 5.0/3.4, 5.0/4.8.
        val ride = trainingEffect(minutes(46.6, 32.5, 12.0, 5.3, 0.0))!!
        val run = trainingEffect(minutes(0.0, 0.5, 6.6, 23.6, 3.5))!!
        val treadmill = trainingEffect(minutes(0.0, 1.0, 1.6, 28.6, 8.5))!!
        assertEquals(2.7, ride.aerobic, 0.2)
        assertEquals(0.2, ride.anaerobic, 0.1)
        assertEquals(5.0, run.aerobic, 0.01)
        assertEquals(3.4, run.anaerobic, 0.2)
        assertEquals(5.0, treadmill.aerobic, 0.01)
        assertEquals(4.8, treadmill.anaerobic, 0.3)
        assertEquals(TrainingEffectLevel.MAINTAINING, trainingEffectLevel(ride.aerobic))
        assertNull(trainingEffect(listOf(0L, 0L, 0L, 0L, 0L)))
    }

    @Test fun `zone bounds meet without a gap`() {
        val bounds = heartRateZoneBounds(192)
        assertEquals(null to 115, bounds[0])
        assertEquals(116 to 134, bounds[1])
        assertEquals(173 to null, bounds[4])
    }

    @Test fun `a stride is the speed over the cadence`() {
        // 2.77 m/s at 163 steps a minute: about 1.02 m a step.
        val strides = strideLengthSeries(listOf(at(10) to 163.0, at(100) to 163.0), listOf(at(12) to 2.77))
        assertEquals(1, strides.size)
        assertEquals(1.02, strides.single().second, 0.01)
        assertTrue(strideLengthSeries(listOf(at(10) to 10.0), listOf(at(10) to 2.0)).isEmpty())
    }
}
