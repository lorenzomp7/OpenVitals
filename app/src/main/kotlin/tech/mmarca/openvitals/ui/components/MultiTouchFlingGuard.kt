package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity

/**
 * 85: no fling after a gesture that had two fingers down. When one of them lifts, a list's drag
 * goes on with the other, and its velocity tracker reads the jump from one finger to the other
 * as a flick of thousands of pixels a second: the page shot to its top or its end. Scrolling
 * with the fingers still works; only the throw at the end is dropped.
 */
fun Modifier.multiTouchFlingGuard(): Modifier = composed {
    val guard = remember { MultiTouchFlingConnection() }
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            guard.multiTouch = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } > 1) guard.multiTouch = true
            } while (event.changes.any { it.pressed })
        }
    }.nestedScroll(guard)
}

internal class MultiTouchFlingConnection : NestedScrollConnection {
    var multiTouch = false

    /** Takes the whole velocity before the list can fling with it. */
    override suspend fun onPreFling(available: Velocity): Velocity = if (multiTouch) available else Velocity.Zero
}
