package tech.mmarca.openvitals.features.activity

import org.junit.Assert.assertEquals
import org.junit.Test

/** 137: a line is drawn from the session's start. */
class ChartStartAnchorTest {

    @Test fun `a late first reading gets an estimated start, the median of the first ones`() {
        val points = listOf(0.05f to 140.0, 0.06f to 90.0, 0.07f to 100.0, 0.08f to 110.0, 0.09f to 105.0, 0.1f to 120.0)

        val anchored = anchoredAtStart(points)

        assertEquals(0f to 105.0, anchored.first())
        assertEquals(points, anchored.drop(1))
    }

    @Test fun `a line that already starts at the start is untouched`() {
        val points = listOf(0f to 1.0, 0.5f to 2.0)

        assertEquals(points, anchoredAtStart(points))
    }
}
