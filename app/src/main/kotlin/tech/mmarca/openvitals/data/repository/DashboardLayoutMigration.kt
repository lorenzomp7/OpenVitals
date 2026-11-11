package tech.mmarca.openvitals.data.repository

import android.content.SharedPreferences

/** Import the first dashboard editor's settings once; canonical settings always win. */
internal fun migrateDashboardLayoutPreferences(prefs: SharedPreferences, legacy: SharedPreferences) {
    if (prefs.getBoolean("dashboard_layout_migrated", false)) return
    val edit = prefs.edit()
    if (!prefs.contains("dashboard_widget_sizes")) {
        val sizes = legacy.all.mapNotNull { (key, value) ->
            if (key.startsWith("size_") && value is Int && value in listOf(1, 2, 4)) {
                "${key.removePrefix("size_")}=$value"
            } else null
        }
        if (sizes.isNotEmpty()) edit.putString("dashboard_widget_sizes", sizes.joinToString(","))
    }
    if (!prefs.contains("dashboard_paged_layout") && legacy.contains("six_tile_carousel")) {
        edit.putBoolean("dashboard_paged_layout", legacy.getBoolean("six_tile_carousel", true))
    }
    edit.putBoolean("dashboard_layout_migrated", true).apply()
}

internal class DashboardLayoutPreferences(private val prefs: SharedPreferences) {
    fun sizes(): Map<String, Int> = prefs.getString("dashboard_widget_sizes", null)
        ?.split(',')?.mapNotNull { entry ->
            val parts = entry.split('=')
            parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in listOf(1, 2, 4) }?.let { parts[0] to it }
        }?.toMap().orEmpty()

    fun setSizes(sizes: Map<String, Int>) {
        require(sizes.values.all { it in listOf(1, 2, 4) })
        prefs.edit().putString("dashboard_widget_sizes", sizes.entries.joinToString(",") { "${it.key}=${it.value}" }).apply()
    }

    fun detailCounts(): Map<String, Int> = prefs.getString("dashboard_widget_details", null)
        ?.split(',')?.mapNotNull { entry ->
            val parts = entry.split('=')
            parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..3 }?.let { parts[0] to it }
        }?.toMap().orEmpty()

    fun setDetailCounts(counts: Map<String, Int>) {
        require(counts.values.all { it in 1..3 })
        prefs.edit().putString("dashboard_widget_details", counts.entries.joinToString(",") { "${it.key}=${it.value}" }).apply()
    }

    fun paged(): Boolean = prefs.getBoolean("dashboard_paged_layout", true)
    fun setPaged(paged: Boolean) { prefs.edit().putBoolean("dashboard_paged_layout", paged).apply() }
}
