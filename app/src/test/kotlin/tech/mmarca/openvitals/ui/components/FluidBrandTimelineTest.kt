package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FluidBrandTimelineTest {
    @Test fun `leaving and returning to the dashboard does not repeat the fill`() {
        val timeline = FluidBrandTimeline()
        timeline.start(1_000L)
        assertEquals(0f, timeline.fillAt(1_000L), 0f)
        timeline.start(6_000L)
        assertEquals(1_000L, timeline.startedAtMillis)
        assertEquals(1f, timeline.fillAt(6_000L), 0f)
        assertEquals(5f, timeline.secondsAt(6_000L), 0f)
    }

    @Test fun `restoring the shell keeps the original animation position`() {
        val first = FluidBrandTimeline().apply { start(2_000L) }
        val restored = FluidBrandTimeline(first.startedAtMillis)
        restored.start(20_000L)
        assertEquals(first.secondsAt(20_000L), restored.secondsAt(20_000L), 0f)
        assertEquals(1f, restored.fillAt(20_000L), 0f)
    }

    @Test fun `fill progresses once while the fluid clock continues afterwards`() {
        val timeline = FluidBrandTimeline().apply { start(1_000L) }
        assertEquals(0.5f, timeline.fillAt(2_400L), 0.001f)
        assertEquals(1f, timeline.fillAt(30_000L), 0f)
        assertEquals(29f, timeline.secondsAt(30_000L), 0f)
    }
}
