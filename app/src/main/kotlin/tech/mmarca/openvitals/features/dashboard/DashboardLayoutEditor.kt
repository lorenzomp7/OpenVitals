package tech.mmarca.openvitals.features.dashboard

import tech.mmarca.openvitals.data.repository.PreferencesRepository

/** Layout drafts never reach preferences until the user chooses Save. */
internal class DashboardLayoutEditor(private val prefs: PreferencesRepository) {
    private var original: DashboardUiState? = null

    fun begin(state: DashboardUiState) { original = state }

    fun finish(current: DashboardUiState, save: Boolean): DashboardUiState {
        val before = original ?: return current
        val layout = if (save) current else before
        if (save) {
            if (layout.dashboardWidgets != before.dashboardWidgets || layout.placedDashboardWidgets != before.placedDashboardWidgets) {
                prefs.setDashboardWidgetOrder(layout.dashboardWidgets.map { it.name })
            }
            prefs.setDashboardWidgetSizes(layout.widgetSizes.mapKeys { it.key.name })
            prefs.setDashboardDetailCounts(layout.widgetDetailCounts.mapKeys { it.key.name })
            prefs.setDashboardPagedLayout(layout.pagedLayout)
        }
        original = null
        return current.copy(
            isEditingDashboard = false,
            dashboardWidgets = layout.dashboardWidgets,
            placedDashboardWidgets = layout.placedDashboardWidgets,
            widgetSizes = layout.widgetSizes,
            widgetDetailCounts = layout.widgetDetailCounts,
            pagedLayout = layout.pagedLayout,
        )
    }
}
