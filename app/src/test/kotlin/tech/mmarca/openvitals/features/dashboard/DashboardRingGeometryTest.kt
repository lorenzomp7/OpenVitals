package tech.mmarca.openvitals.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The four-space tile's ring sits in the middle of its tile, and its text inside it. */
class DashboardRingGeometryTest {
    private val sin45 = 0.70710677f

    @Test fun `the drawn ring is centred vertically, not the full circle`() {
        val stroke = 10f
        val geometry = dashboardRingGeometry(width = 300f, height = 300f, stroke = stroke)
        val drawnTop = geometry.centerY - geometry.radius - stroke / 2f
        val drawnBottom = geometry.centerY + geometry.radius * sin45 + stroke / 2f
        assertEquals(300f - drawnBottom, drawnTop, 0.01f)
        // The open bottom lets the circle sit below the tile's middle.
        assertTrue(geometry.centerY > 150f)
        assertEquals(150f, geometry.centerX, 0.01f)
    }

    @Test fun `the ring fills the width of a square tile`() {
        val geometry = dashboardRingGeometry(width = 300f, height = 300f, stroke = 10f)
        assertEquals(145f, geometry.radius, 0.01f)
    }

    @Test fun `a short tile limits the ring by its height`() {
        val stroke = 10f
        val geometry = dashboardRingGeometry(width = 400f, height = 200f, stroke = stroke)
        assertEquals((200f - stroke) / (1f + sin45), geometry.radius, 0.01f)
        assertTrue(geometry.centerY - geometry.radius - stroke / 2f >= -0.01f)
    }

    @Test fun `the title band sits under the ring and the two are centred together`() {
        val stroke = 10f
        val label = 20f
        val geometry = dashboardRingGeometry(width = 160f, height = 160f, stroke = stroke, labelHeight = label)
        val drawnTop = geometry.centerY - geometry.radius - stroke / 2f
        assertEquals(geometry.centerY + geometry.radius * sin45 + stroke / 2f, geometry.labelTop, 0.01f)
        assertEquals(160f - (geometry.labelTop + label), drawnTop, 0.01f)
        // A square tile has room for the title band without shrinking the ring.
        assertEquals(75f, geometry.radius, 0.01f)
    }

    @Test fun `a tall title band shrinks the ring to keep it inside the tile`() {
        val geometry = dashboardRingGeometry(width = 160f, height = 160f, stroke = 10f, labelHeight = 40f)
        assertEquals((160f - 10f - 40f) / (1f + sin45), geometry.radius, 0.01f)
        assertTrue(geometry.labelTop + 40f <= 160.01f)
    }

    @Test fun `a line through the centre gets the full hole, one near the top a chord`() {
        assertEquals(50f, insideRingHalfWidth(hole = 50f, top = -5f, bottom = 5f), 0.5f)
        assertEquals(30f, insideRingHalfWidth(hole = 50f, top = -40f, bottom = -30f), 0.01f)
    }

    @Test fun `below the ring's ends a line may run as wide as the gap between them`() {
        val atEnds = insideRingHalfWidth(hole = 50f, top = 30f, bottom = 50f * sin45)
        val belowEnds = insideRingHalfWidth(hole = 50f, top = 30f, bottom = 48f)
        assertEquals(atEnds, belowEnds, 0.01f)
        assertEquals(50f * sin45, belowEnds, 0.01f)
    }

    @Test fun `lines stack as one block centred on the ring`() {
        val tops = ringLineTops(listOf(10f, 20f, 10f))
        assertEquals(listOf(-20f, -10f, 10f), tops)
    }
}
