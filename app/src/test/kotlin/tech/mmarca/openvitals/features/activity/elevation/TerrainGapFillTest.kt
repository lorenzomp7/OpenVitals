package tech.mmarca.openvitals.features.activity.elevation

import org.junit.Assert.assertEquals
import org.junit.Test

/** 104: a terrain tile that would not come leaves its points to be drawn between their neighbours. */
class TerrainGapFillTest {

    @Test fun `a gap is filled in a straight line`() {
        assertEquals(listOf(10.0, 12.0, 14.0, 16.0), fillGaps(listOf(10.0, null, null, 16.0)))
    }

    @Test fun `the ends take the nearest height`() {
        assertEquals(listOf(5.0, 5.0, 7.0, 7.0), fillGaps(listOf(null, 5.0, 7.0, null)))
    }

    @Test fun `nothing known is nothing`() {
        assertEquals(emptyList<Double>(), fillGaps(listOf(null, null)))
    }
}
