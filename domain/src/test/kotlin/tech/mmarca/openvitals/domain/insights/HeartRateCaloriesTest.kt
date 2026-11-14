package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ActivityCalorieSample
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.preferences.BiologicalSex

class HeartRateCaloriesTest {
    private val start = Instant.parse("2026-04-19T18:05:00Z")
    private val end = start.plus(Duration.ofMinutes(10))

    @Test fun `keytel gives a plausible male burn at moderate effort and never a negative one`() {
        val rate = keytelKcalPerMinute(140.0, 75.0, 40, BiologicalSex.MALE)
        assertTrue("rate $rate", rate in 10.0..14.0)
        assertEquals(0.0, keytelKcalPerMinute(40.0, 60.0, 20, BiologicalSex.FEMALE), 0.0)
    }

    @Test fun `heart rate gives one slice per minute with harder minutes burning more`() {
        val samples = (0 until 10).flatMap { minute ->
            val bpm = if (minute < 5) 110L else 160L
            listOf(HeartRateSample(start.plusSeconds(minute * 60L + 10), bpm, "w"))
        }
        val result = sessionCalories(start, end, samples, emptyList(), 70.0, 35, null)!!
        assertEquals(CalorieIntervalSource.HEART_RATE, result.source)
        assertEquals(10, result.intervals.size)
        assertTrue(result.intervals.last().kcal > result.intervals.first().kcal)
    }

    @Test fun `without a profile weight the recorded total is sliced instead`() {
        val recorded = listOf(
            ActivityCalorieSample(start, 0.0, active = false),
            ActivityCalorieSample(end, 100.0, active = false),
        )
        val result = sessionCalories(start, end, emptyList(), recorded, null, null, null)!!
        assertEquals(CalorieIntervalSource.RECORDED, result.source)
        assertEquals(100.0, result.totalKcal, 0.01)
        assertEquals(10.0, result.intervals.first().kcal, 0.01)
    }

    @Test fun `no heart rate and no recorded calories is no chart`() {
        assertNull(sessionCalories(start, end, emptyList(), emptyList(), 70.0, 35, null))
    }
}
