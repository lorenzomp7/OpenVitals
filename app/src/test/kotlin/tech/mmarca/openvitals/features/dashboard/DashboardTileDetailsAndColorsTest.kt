package tech.mmarca.openvitals.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 153-182: what a tile's details go on with, the tiles' colours and the cardio duplicate. */
class DashboardTileDetailsAndColorsTest {
    private val tiles = DashboardWidgetId.entries - DashboardWidgetId.WORKOUT

    @Test fun `every tile has related readings enough to fill three values`() {
        tiles.forEach { id ->
            val related = dashboardRelatedWidgetIds(id)
            assertTrue("$id has ${related.size}", related.size >= 3)
            assertFalse("$id lists itself", id in related)
            assertEquals("$id repeats one", related.distinct(), related)
        }
    }

    @Test fun `elevation and wheelchair pushes are not followed by distance`() {
        assertFalse(DashboardWidgetId.DISTANCE in dashboardRelatedWidgetIds(DashboardWidgetId.ELEVATION))
        assertFalse(DashboardWidgetId.DISTANCE in dashboardRelatedWidgetIds(DashboardWidgetId.WHEELCHAIR_PUSHES))
    }

    @Test fun `tiles with no reading of their own still get related lines`() {
        listOf(DashboardWidgetId.MEDICAL_RECORDS, DashboardWidgetId.CYCLE, DashboardWidgetId.BLOOD_GLUCOSE).forEach { id ->
            assertTrue(id.name, dashboardRelatedWidgetIds(id).size >= 3)
        }
    }

    @Test fun `tiles of one category share one colour`() {
        tiles.forEach { id ->
            assertEquals(id.name, dashboardWidgetCategory(id).accentColor, dashboardWidgetAccent(id))
        }
    }

    @Test fun `body fat and FFMI are coloured like the other body tiles`() {
        val body = dashboardWidgetAccent(DashboardWidgetId.LEAN_MASS)
        listOf(DashboardWidgetId.FFMI, DashboardWidgetId.BODY_FAT, DashboardWidgetId.BONE_MASS, DashboardWidgetId.WEIGHT)
            .forEach { assertEquals(it.name, body, dashboardWidgetAccent(it)) }
    }

    @Test fun `different categories have different colours`() {
        val colors = DashboardWidgetCategory.entries.map { it.accentColor }
        assertEquals(colors.size, colors.distinct().size)
    }

    @Test fun `a saved legacy cardio tile shows the weekly one once`() {
        val ids = customizableDashboardWidgetIds(
            listOf(DashboardWidgetId.STEPS, DashboardWidgetId.CARDIO_LOAD, DashboardWidgetId.WEEKLY_CARDIO_LOAD),
        )
        assertEquals(listOf(DashboardWidgetId.STEPS, DashboardWidgetId.WEEKLY_CARDIO_LOAD), ids)
        assertEquals(
            listOf(DashboardWidgetId.WEEKLY_CARDIO_LOAD),
            dashboardWidgetIdsFromStored(listOf("CARDIO_LOAD")),
        )
    }

    @Test fun `edit mode offers the weekly cardio tile once`() {
        assertFalse(DashboardWidgetId.CARDIO_LOAD in DashboardEditableWidgetIds)
        assertTrue(DashboardWidgetId.WEEKLY_CARDIO_LOAD in DashboardEditableWidgetIds)
    }
}
