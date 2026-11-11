package tech.mmarca.openvitals.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardWidgetSizingTest {
    @Test
    fun `six standard widgets fit on a three-row carousel page`() {
        val widgets = listOf(
            DashboardWidgetId.DISTANCE,
            DashboardWidgetId.WEIGHT,
            DashboardWidgetId.HEIGHT,
            DashboardWidgetId.BMI,
            DashboardWidgetId.FFMI,
            DashboardWidgetId.BODY_FAT,
            DashboardWidgetId.AVG_HEART_RATE,
        )
        val pages = dashboardWidgetIdsInGridPages(widgets, rows = 3)
        assertEquals(widgets.take(6), pages.first())
        assertEquals(widgets.drop(6), pages.last())
    }

    @Test
    fun `four-space widgets consume two full grid rows`() {
        val widgets = listOf(
            DashboardWidgetId.STEPS,
            DashboardWidgetId.WEEKLY_CARDIO_LOAD,
            DashboardWidgetId.DISTANCE,
        )
        val pages = dashboardWidgetIdsInGridPages(widgets, rows = 2)
        assertEquals(widgets.take(2), pages.first())
        assertEquals(listOf(DashboardWidgetId.DISTANCE), pages.last())
    }

    @Test
    fun `mixed sizes fit without dropping widgets`() {
        val widgets = listOf(
            DashboardWidgetId.DISTANCE,
            DashboardWidgetId.WEIGHT,
            DashboardWidgetId.HEIGHT,
            DashboardWidgetId.BMI,
            DashboardWidgetId.FFMI,
        )
        val sizes = mapOf(
            DashboardWidgetId.DISTANCE to 4,
            DashboardWidgetId.WEIGHT to 1,
            DashboardWidgetId.HEIGHT to 1,
            DashboardWidgetId.BMI to 2,
            DashboardWidgetId.FFMI to 2,
        )
        val pages = dashboardWidgetIdsInGridPages(
            widgets, rows = 3, sizeFor = { sizes.getValue(it) },
        )
        assertEquals(widgets.toSet(), pages.flatten().toSet())
        assertEquals(widgets.size, pages.flatten().size)
    }
}
