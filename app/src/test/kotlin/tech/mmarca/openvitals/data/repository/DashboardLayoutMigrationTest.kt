package tech.mmarca.openvitals.data.repository

import org.junit.Assert.*
import org.junit.Test
import tech.mmarca.openvitals.util.FakeSharedPreferences

class DashboardLayoutMigrationTest {
    @Test fun importsValidLegacySizesAndPagingExactlyOnce() {
        val canonical = FakeSharedPreferences()
        val old = FakeSharedPreferences()
        old.edit().putInt("size_STEPS", 1).putInt("size_DISTANCE", 4).putInt("size_WEIGHT", 7)
            .putBoolean("six_tile_carousel", false).apply()
        migrateDashboardLayoutPreferences(canonical, old)
        val layout = DashboardLayoutPreferences(canonical)
        assertEquals(mapOf("STEPS" to 1, "DISTANCE" to 4), layout.sizes())
        assertFalse(layout.paged())
        layout.setSizes(mapOf("STEPS" to 2))
        migrateDashboardLayoutPreferences(canonical, old)
        assertEquals(mapOf("STEPS" to 2), layout.sizes())
    }

    @Test fun canonicalPreferencesWinOverOldEditor() {
        val canonical = FakeSharedPreferences()
        DashboardLayoutPreferences(canonical).apply { setSizes(mapOf("STEPS" to 4)); setPaged(true) }
        val old = FakeSharedPreferences()
        old.edit().putInt("size_STEPS", 1).putBoolean("six_tile_carousel", false).apply()
        migrateDashboardLayoutPreferences(canonical, old)
        assertEquals(mapOf("STEPS" to 4), DashboardLayoutPreferences(canonical).sizes())
        assertTrue(DashboardLayoutPreferences(canonical).paged())
    }
}
