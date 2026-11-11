package tech.mmarca.openvitals.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardTileDetailsTest {
    @Test fun `small tile always shows one value and larger tiles allow two or three`() {
        listOf(-1, 1, 2, 3, 7).forEach { requested ->
            assertEquals(1, dashboardDetailCount(DashboardTileSize.SMALL, requested))
        }
        listOf(DashboardTileSize.MEDIUM, DashboardTileSize.LARGE).forEach { size ->
            assertEquals(2, dashboardDetailCount(size, 1))
            assertEquals(2, dashboardDetailCount(size, 2))
            assertEquals(3, dashboardDetailCount(size, 3))
        }
    }
}
