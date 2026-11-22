package tech.mmarca.openvitals.features.activity

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The GPS height without its leaps, so a flat walk does not draw spikes or count climbs. */
class GpsElevationCleaningTest {

    private val start: Instant = Instant.parse("2026-11-16T15:00:00Z")

    private fun everySecond(heights: List<Double>) =
        heights.mapIndexed { index, meters -> ActivityElevationSample(start.plusSeconds(index.toLong()), meters) }

    @Test fun `a leap of twenty seconds is taken out`() {
        val heights = List(300) { 20.0 }.toMutableList()
        for (i in 140 until 160) heights[i] = 35.0
        val cleaned = cleanGpsElevation(everySecond(heights))

        assertTrue(cleaned.all { kotlin.math.abs(it.meters - 20.0) < 0.5 })
    }

    @Test fun `a slope of several minutes is kept`() {
        // Ten minutes climbing a metre a minute, then flat.
        val heights = List(600) { it / 60.0 } + List(300) { 10.0 }
        val cleaned = cleanGpsElevation(everySecond(heights))

        assertEquals(0.0, cleaned.first().meters, 0.6)
        assertEquals(10.0, cleaned.last().meters, 0.1)
        assertEquals(5.0, cleaned[300].meters, 0.2)
    }

    @Test fun `the times stay as they were`() {
        val samples = everySecond(listOf(1.0, 2.0, 3.0, 4.0))

        assertEquals(samples.map { it.time }, cleanGpsElevation(samples).map { it.time })
    }
}
