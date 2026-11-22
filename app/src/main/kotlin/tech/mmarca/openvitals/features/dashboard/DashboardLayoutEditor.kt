package tech.mmarca.openvitals.features.dashboard

import tech.mmarca.openvitals.data.repository.PreferencesRepository

/** Layout drafts never reach preferences until the user chooses Save. */
internal class DashboardLayoutEditor(private val prefs: PreferencesRepository) {
    private var original: DashboardUiState? = null

    fun begin(state: DashboardUiState) { original = state }

    /** Whether the layout differs from where editing began; moving a tile and back is no change. */
    fun hasChanges(
        current: DashboardUiState,
        defaultSpaces: Map<DashboardWidgetId, Int> = emptyMap(),
        defaultDetailCount: Int = 2,
    ): Boolean {
        val before = original ?: return false
        // A size or a value count set back to the tile's default is the same as never set.
        fun Map<DashboardWidgetId, Int>.sizes() = filterNot { (id, spaces) -> defaultSpaces[id] == spaces }
        fun Map<DashboardWidgetId, Int>.counts() = filterNot { (_, count) -> count == defaultDetailCount }
        return current.dashboardWidgets != before.dashboardWidgets ||
            current.widgetSizes.sizes() != before.widgetSizes.sizes() ||
            current.widgetDetailCounts.counts() != before.widgetDetailCounts.counts() ||
            current.pagedLayout != before.pagedLayout
    }

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

/** The values a tile shows when none was chosen, as the specs default it. */
internal const val DefaultDetailCount = 2
