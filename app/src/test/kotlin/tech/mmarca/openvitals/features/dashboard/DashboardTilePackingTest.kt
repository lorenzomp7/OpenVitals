package tech.mmarca.openvitals.features.dashboard

import org.junit.Assert.*
import org.junit.Test

class DashboardTilePackingTest {
    private val ids = DashboardWidgetId.entries.take(10)

    @Test fun sixRectanglesFitExactlyOnOnePageAndSeventhStartsNext() {
        val result = packDashboardTiles(ids.take(7).map { it to DashboardTileSize.MEDIUM }, rowsPerPage = 3)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 1), result.map { it.page })
        assertEquals(listOf(0, 2, 0, 2, 0, 2), result.take(6).map { it.column })
        assertEquals(listOf(0, 0, 1, 1, 2, 2), result.take(6).map { it.row })
    }

    @Test fun mixedSizesNeverOverlapOrLeaveThePage() {
        val sizes = listOf(DashboardTileSize.LARGE, DashboardTileSize.SMALL, DashboardTileSize.MEDIUM,
            DashboardTileSize.SMALL, DashboardTileSize.LARGE, DashboardTileSize.MEDIUM)
        val packed = packDashboardTiles(ids.zip(sizes), rowsPerPage = 3)
        val occupied = mutableSetOf<Triple<Int, Int, Int>>()
        packed.forEach { p ->
            assertTrue(p.column + p.size.columns <= 4)
            assertTrue(p.row + p.size.rows <= 3)
            for (r in p.row until p.row + p.size.rows) for (c in p.column until p.column + p.size.columns) {
                assertTrue(occupied.add(Triple(p.page, r, c)))
            }
        }
        assertEquals(sizes.sumOf { it.spaces }, occupied.size)
    }
}
