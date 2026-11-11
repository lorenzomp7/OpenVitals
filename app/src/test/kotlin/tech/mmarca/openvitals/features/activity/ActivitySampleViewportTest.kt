package tech.mmarca.openvitals.features.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.ui.components.ChartViewport

class ActivitySampleViewportTest {
    @Test fun `25 late samples get room while keeping their session fractions`() {
        val fractions = (0 until 25).map { 0.95f + it * 0.001f }
        val viewport = activitySampleViewport(fractions)
        assertTrue(viewport.span < 0.04f)
        assertTrue(fractions.all { viewport.visibleFraction(it) in 0f..1f })
        assertTrue(viewport.start > 0.9f)
    }

    @Test fun `full recordings keep the full session and sparse empty data is safe`() {
        assertEquals(ChartViewport.Full, activitySampleViewport(listOf(0f, 0.5f, 1f)))
        assertEquals(ChartViewport.Full, activitySampleViewport(emptyList()))
        assertEquals(ChartViewport.Full, activitySampleViewport(listOf(1f, 1f)))
        assertEquals(ChartViewport.Full, activitySampleViewport(listOf(Float.NaN)))
    }

    @Test fun `end of session samples stay inside the axis`() {
        val viewport = activitySampleViewport(listOf(0.999f, 1f))
        assertEquals(1f, viewport.end, 0.00001f)
        assertTrue(viewport.span >= ChartViewport.MinimumSpan - 0.00001f)
    }
}
