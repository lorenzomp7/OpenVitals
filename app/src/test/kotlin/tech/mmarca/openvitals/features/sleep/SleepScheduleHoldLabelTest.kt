package tech.mmarca.openvitals.features.sleep

import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.SleepStage

/** Holding a finger on the sleep chart reads the night's date and hours asleep. */
class SleepScheduleHoldLabelTest {

    private val date = LocalDate.of(2026, 11, 16)
    private val start = Instant.parse("2026-11-15T23:00:00Z")
    private val end = Instant.parse("2026-11-16T06:45:00Z")

    @Test fun `the date and the hours asleep, awake left out`() {
        val day = SleepScheduleDay(
            date = date,
            inBedStart = start,
            inBedEnd = end,
            stages = listOf(
                SleepStage(start, start.plusSeconds(15 * 60), SleepStage.STAGE_AWAKE),
                SleepStage(start.plusSeconds(15 * 60), end, SleepStage.STAGE_LIGHT),
            ),
        )
        assertEquals("16 Nov · 7h 30m", sleepScheduleHoldLabel(day, Locale.UK))
    }

    @Test fun `a night with nothing recorded reads a dash`() {
        assertEquals("16 Nov · –", sleepScheduleHoldLabel(SleepScheduleDay(date, null, null), Locale.UK))
    }
}
