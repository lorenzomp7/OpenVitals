package tech.mmarca.openvitals.features.dashboard

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange

class DashboardPeriodTest {
    @Test fun `monthly selection covers the month and never future dates`() {
        val today = LocalDate.now()
        val state = DashboardUiState(selectedDate = today, selectedRange = TimeRange.MONTH)
        assertEquals(today.withDayOfMonth(1), state.dashboardPeriod().start)
        assertEquals(today, state.dashboardPeriod().end)
        assertFalse(state.canAdvancePeriod())
    }

    @Test fun `period goals scale cumulative metrics but leave mean sleep goal daily`() {
        val start = LocalDate.of(2026, 2, 1)
        val daily = DashboardDailyGoals(steps = 1000.0, sleepHours = 8.0)
        val monthly = daily.forPeriod(DatePeriod(start, start.plusDays(27)))
        assertEquals(28000.0, monthly.steps, 0.0)
        assertEquals(8.0, monthly.sleepHours, 0.0)
    }
}
