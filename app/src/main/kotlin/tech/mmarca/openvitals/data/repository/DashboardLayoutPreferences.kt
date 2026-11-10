package tech.mmarca.openvitals.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit

/** Persists tile sizing and paging separately from the other app preferences. */
internal class DashboardLayoutPreferences(private val prefs: SharedPreferences) {
    fun widgetSizes(): Map<String, Int> =
        prefs.getString(WidgetSizesKey, null)
            ?.split(Separator)
            ?.mapNotNull { entry ->
                val parts = entry.split('=')
                parts.getOrNull(1)?.toIntOrNull()?.let { parts[0] to it }
            }
            ?.toMap()
            .orEmpty()

    fun setWidgetSizes(sizes: Map<String, Int>) {
        prefs.edit {
            putString(WidgetSizesKey, sizes.entries.joinToString(Separator) { "${it.key}=${it.value}" })
        }
    }

    fun pagedLayout(): Boolean = prefs.getBoolean(PagedLayoutKey, false)

    fun setPagedLayout(paged: Boolean) {
        prefs.edit { putBoolean(PagedLayoutKey, paged) }
    }

    private companion object {
        const val WidgetSizesKey = "dashboard_widget_sizes"
        const val PagedLayoutKey = "dashboard_paged_layout"
        const val Separator = ","
    }
}
