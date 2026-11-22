package tech.mmarca.openvitals.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

/** 149, 150: the home screen's palette comes from the tiles the screen shows. */
class DashboardPaletteIdsTest {
    private val ids = DashboardWidgetId.entries.take(24)

    @Test fun `the rings are the first three shown tiles, not the saved order`() {
        val shown = listOf(ids[5], ids[6], ids[7]) + ids.drop(8)
        val palette = dashboardPaletteIds(shown, hasHero = { true }, sizeOf = { DashboardTileSize.MEDIUM }, paged = true)
        assertEquals(listOf(ids[5], ids[6], ids[7]), palette.hero)
    }

    @Test fun `in pages the home screen is the first page`() {
        val palette = dashboardPaletteIds(ids.take(13), hasHero = { true }, sizeOf = { DashboardTileSize.MEDIUM }, paged = true)
        // Three rows of two medium tiles a page.
        assertEquals(ids.subList(3, 9), palette.homeScreen)
    }

    @Test fun `without pages the home screen is the first four by four block`() {
        val small = dashboardPaletteIds(ids.take(23), hasHero = { false }, sizeOf = { DashboardTileSize.SMALL }, paged = false)
        assertEquals(ids.take(16), small.homeScreen)
        val large = dashboardPaletteIds(ids.take(9), hasHero = { false }, sizeOf = { DashboardTileSize.LARGE }, paged = false)
        assertEquals(ids.take(4), large.homeScreen)
    }

    @Test fun `no rings when a top tile cannot be one`() {
        val palette = dashboardPaletteIds(ids.take(6), hasHero = { it != ids[1] }, sizeOf = { DashboardTileSize.MEDIUM }, paged = true)
        assertEquals(emptyList<DashboardWidgetId>(), palette.hero)
    }
}
