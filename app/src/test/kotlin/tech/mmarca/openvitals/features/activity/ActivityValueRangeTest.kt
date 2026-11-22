package tech.mmarca.openvitals.features.activity

import org.junit.Assert.assertEquals
import org.junit.Test

/** A range says its unit once. */
class ActivityValueRangeTest {
    @Test fun `a shared unit is said once, at the end`() {
        assertEquals("77 – 156 bpm", activityValueRange("77 bpm", "156 bpm"))
        assertEquals("4:55 – 5:00 min/km", activityValueRange("4:55 min/km", "5:00 min/km"))
    }

    @Test fun `values without a unit or in different units stay as they are`() {
        assertEquals("3 – 7", activityValueRange("3", "7"))
        assertEquals("900 m – 1,2 km", activityValueRange("900 m", "1,2 km"))
    }
}
