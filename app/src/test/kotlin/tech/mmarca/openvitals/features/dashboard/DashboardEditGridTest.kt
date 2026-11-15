package tech.mmarca.openvitals.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardEditGridTest {
    private val ids = listOf(
        DashboardWidgetId.STEPS,
        DashboardWidgetId.CALORIES_OUT,
        DashboardWidgetId.DISTANCE,
        DashboardWidgetId.SLEEP,
        DashboardWidgetId.HYDRATION,
        DashboardWidgetId.BMI,
    )

    @Test fun `the hero order lists left centre right and turns back`() {
        val shown = heroDisplayOrder(ids)
        assertEquals(listOf(DashboardWidgetId.CALORIES_OUT, DashboardWidgetId.STEPS, DashboardWidgetId.DISTANCE), shown.take(3))
        assertEquals(ids, heroDisplayOrder(shown))
    }

    @Test fun `the rings stand in a column on the left, each beside its ring`() {
        val sizes = ids.map { DashboardTileSize.MEDIUM }
        val plan = dashboardEditPlan(heroDisplayOrder(ids), sizes, pagedLayout = false)
        val ringTiles = plan.cells.filterIsInstance<DashboardEditCell.Tile>().filter { it.ring }
        assertEquals(listOf(DashboardWidgetId.CALORIES_OUT, DashboardWidgetId.STEPS, DashboardWidgetId.DISTANCE), ringTiles.map { it.id })
        assertTrue(ringTiles.all { it.column == 0 })
        val diagrams = plan.cells.filterIsInstance<DashboardEditCell.RingDiagram>()
        assertEquals(listOf(HeroRole.LEFT, HeroRole.CENTRE, HeroRole.RIGHT), diagrams.map { it.role })
        assertEquals(ringTiles.map { it.row }, diagrams.map { it.row })
        assertEquals(ringTiles.map { it.id }, diagrams.map { it.ringOf })
    }

    @Test fun `a 2x2 tile stands beside two stacked 2x1 tiles, as on the dashboard`() {
        val sizes = listOf(
            DashboardTileSize.MEDIUM, DashboardTileSize.MEDIUM, DashboardTileSize.MEDIUM,
            DashboardTileSize.LARGE, DashboardTileSize.MEDIUM, DashboardTileSize.MEDIUM,
        )
        val plan = dashboardEditPlan(ids, sizes, pagedLayout = false)
        val tiles = plan.cells.filterIsInstance<DashboardEditCell.Tile>().filterNot { it.ring }.associateBy { it.id }
        val large = tiles.getValue(DashboardWidgetId.SLEEP)
        val first = tiles.getValue(DashboardWidgetId.HYDRATION)
        val second = tiles.getValue(DashboardWidgetId.BMI)
        assertEquals(0, large.column)
        assertEquals(2, large.rows)
        assertEquals(listOf(2, 2), listOf(first.column, second.column))
        assertEquals(listOf(large.row, large.row + 1), listOf(first.row, second.row))
        // Ring label, three rings, tiles label, two rows of tiles.
        assertEquals(7, plan.rowCount)
    }
}
