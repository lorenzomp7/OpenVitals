package tech.mmarca.openvitals.features.activity.elevation

import org.junit.Assert.assertEquals
import org.junit.Test

/** Two terrain models read side by side, each correcting the other. */
class TerrainReconcileTest {

    @Test fun `where the models agree their mean is drawn`() {
        assertEquals(listOf(101.0, 103.0), reconcileTerrain(listOf(100.0, 102.0), listOf(102.0, 104.0)))
    }

    @Test fun `a leap in one model is replaced by the other`() {
        // A bridge deck in the first model at the third point.
        val first = listOf(100.0, 101.0, 140.0, 102.0)
        val second = listOf(100.0, 101.0, 101.5, 102.0)
        assertEquals(listOf(100.0, 101.0, 101.5, 102.0), reconcileTerrain(first, second))
    }

    @Test fun `a hole in one model is filled by the other`() {
        assertEquals(listOf(100.0, 105.0), reconcileTerrain(listOf(100.0, null), listOf(100.0, 105.0)))
    }

    @Test fun `the tiles decode to metres`() {
        // Mapbox: -10000 + (R*65536 + G*256 + B) * 0.1; 0x01_86_A0 = 100000 -> 0 m.
        assertEquals(0.0, terrainHeightMapbox(0xFF0186A0.toInt()), 1e-9)
        // Terrarium: R*256 + G + B/256 - 32768; (128, 100, 128) -> 100.5 m.
        assertEquals(100.5, terrainHeightTerrarium(0xFF806480.toInt()), 1e-9)
    }
}
