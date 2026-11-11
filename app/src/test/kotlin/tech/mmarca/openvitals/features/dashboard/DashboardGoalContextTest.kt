package tech.mmarca.openvitals.features.dashboard

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.preferences.UnitSystem

class DashboardGoalContextTest {
    private val formatter = UnitFormatter({ UnitSystem.METRIC }, { Locale.ITALY })

    @Test fun `the reported 1575 km period target is not described as a daily goal`() {
        val period = DatePeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 16))
        val display = build(period)
        val distance = display.widgets.getValue(DashboardWidgetId.DISTANCE)
        assertTrue(display.isPeriod)
        assertEquals("1.575,0 km", distance.progress!!.goalLabelValue.text)
        assertEquals(R.string.dashboard_period_goal, dashboardGoalLabelRes(distance.id, display.isPeriod))
    }

    @Test fun `week month year and custom targets all carry period context`() {
        val start = LocalDate.of(2026, 1, 1)
        listOf(7L, 31L, 365L, 19L, 1L).forEach { days ->
            val display = build(DatePeriod(start, start.plusDays(days - 1)))
            assertEquals(R.string.dashboard_period_goal, dashboardGoalLabelRes(DashboardWidgetId.STEPS, display.isPeriod))
        }
    }

    @Test fun `day targets stay daily and period sleep targets remain nightly averages`() {
        val day = build(null)
        assertFalse(day.isPeriod)
        assertEquals(R.string.daily_goal, dashboardGoalLabelRes(DashboardWidgetId.DISTANCE, day.isPeriod))
        assertEquals(R.string.dashboard_nightly_goal, dashboardGoalLabelRes(DashboardWidgetId.SLEEP, true))
    }

    private fun build(period: DatePeriod?) = DashboardPresentationMapper.build(
        data = DashboardData(date = LocalDate.of(2026, 1, 1), period = period),
        dailyGoals = DashboardDailyGoals(distanceMeters = 4_500.0).forPeriod(period),
        unitFormatter = formatter,
        dateTimeFormatterProvider = DateTimeFormatterProvider(),
    )
}
