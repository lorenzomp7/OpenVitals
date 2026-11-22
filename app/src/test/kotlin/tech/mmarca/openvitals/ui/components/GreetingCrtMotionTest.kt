package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The greeting switches on and off like a cathode-ray television. */
class GreetingCrtMotionTest {

    @Test fun `nothing before it switches on or after it switches off`() {
        assertNull(greetingCrtAt(0f))
        assertNull(greetingCrtAt(GreetingCrtOffEnd + 0.1f))
    }

    @Test fun `it switches on as a bright line that widens, then opens`() {
        val line = greetingCrtAt(GreetingCrtOnStart + 0.05f)!!
        assertEquals(GreetingCrtLine, line.scaleY, 1e-6f)
        assertEquals(1f, line.beam, 1e-6f)
        assertTrue(line.scaleX < 1f)
        val opening = greetingCrtAt((GreetingCrtOnStart + GreetingCrtOnEnd) / 2f + 0.15f)!!
        assertEquals(1f, opening.scaleX, 1e-6f)
        assertTrue(opening.scaleY > GreetingCrtLine && opening.scaleY < 1f)
    }

    @Test fun `it holds full and still`() {
        val held = greetingCrtAt(2f)!!
        assertEquals(1f, held.scaleX, 1e-6f)
        assertEquals(1f, held.scaleY, 1e-6f)
        assertEquals(0f, held.beam, 1e-6f)
    }

    @Test fun `it switches off closing from top and bottom onto a line, then to a dot`() {
        val closing = greetingCrtAt(GreetingCrtOffStart + 0.15f)!!
        assertEquals(1f, closing.scaleX, 1e-6f)
        assertTrue(closing.scaleY < 1f)
        val dot = greetingCrtAt(GreetingCrtOffEnd - 0.005f)!!
        assertEquals(GreetingCrtLine, dot.scaleY, 1e-6f)
        assertTrue(dot.scaleX < 0.2f)
        assertTrue(dot.presence < 0.2f)
    }
}
