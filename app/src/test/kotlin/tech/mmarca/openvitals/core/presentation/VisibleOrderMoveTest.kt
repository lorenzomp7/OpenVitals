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
}
