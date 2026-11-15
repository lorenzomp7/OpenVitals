package tech.mmarca.openvitals.core.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class VisibleOrderMoveTest {
    @Test fun `hidden items keep their slots and a move undone restores the list`() {
        val full = listOf("A", "h1", "B", "C", "h2")
        val visible = setOf("A", "B", "C")
        val moved = moveWithinVisible(full, visible, "A", "C")
        assertEquals(listOf("B", "h1", "C", "A", "h2"), moved)
        val back = moveWithinVisible(moved, visible, "A", "B")
        assertEquals(full, back)
    }

    @Test fun `a new order of the visible items fills their slots and leaves hidden ones put`() {
        val full = listOf("A", "h1", "B", "C", "h2")
        assertEquals(listOf("C", "h1", "A", "B", "h2"), replaceVisibleOrder(full, listOf("C", "A", "B")))
        // A list naming an item the full list lacks is refused.
        assertEquals(full, replaceVisibleOrder(full, listOf("C", "X")))
    }
}
