package tech.mmarca.openvitals.features.dashboard

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository

class DashboardLayoutEditorTest {
    @Test fun `discard restores layout but preserves newer health data and date`() {
        val prefs = mockk<PreferencesRepository>(relaxed = true)
        val editor = DashboardLayoutEditor(prefs)
        val original = DashboardUiState(dashboardWidgets = listOf(DashboardWidgetId.STEPS))
        editor.begin(original)
        val draft = original.copy(isEditingDashboard = true,
            dashboardWidgets = listOf(DashboardWidgetId.SLEEP), widgetSizes = mapOf(DashboardWidgetId.SLEEP to 4),
            widgetDetailCounts = mapOf(DashboardWidgetId.SLEEP to 3), pagedLayout = true,
            selectedDate = original.selectedDate.minusDays(1))
        val result = editor.finish(draft, false)
        assertEquals(original.dashboardWidgets, result.dashboardWidgets)
        assertEquals(original.widgetSizes, result.widgetSizes)
        assertEquals(original.widgetDetailCounts, result.widgetDetailCounts)
        assertFalse(result.pagedLayout)
        assertFalse(result.isEditingDashboard)
        assertEquals(draft.selectedDate, result.selectedDate)
        verify(exactly = 0) { prefs.setDashboardWidgetOrder(any()) }
    }
    @Test fun `save persists all controls and ends editing`() {
        val prefs = mockk<PreferencesRepository>(relaxed = true)
        val editor = DashboardLayoutEditor(prefs)
        editor.begin(DashboardUiState())
        val draft = DashboardUiState(isEditingDashboard = true,
            dashboardWidgets = listOf(DashboardWidgetId.STEPS),
            widgetSizes = mapOf(DashboardWidgetId.STEPS to 2),
            widgetDetailCounts = mapOf(DashboardWidgetId.STEPS to 3), pagedLayout = true)
        assertEquals(draft.copy(isEditingDashboard = false), editor.finish(draft, true))
        verify { prefs.setDashboardWidgetOrder(listOf("STEPS")) }
        verify { prefs.setDashboardWidgetSizes(mapOf("STEPS" to 2)) }
        verify { prefs.setDashboardDetailCounts(mapOf("STEPS" to 3)) }
        verify { prefs.setDashboardPagedLayout(true) }
    }
}
