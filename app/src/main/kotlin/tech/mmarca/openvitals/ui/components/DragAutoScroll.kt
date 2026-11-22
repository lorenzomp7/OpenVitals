package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * The list a hand-made drag can scroll by itself when the finger comes near its top or
 * bottom edge, so an item can be carried past the part of the screen in view. The reorderable
 * lists of the metric screens get this from their library; the dashboard's edit grid, which
 * drags on its own, reads it from [LocalDragAutoScroll].
 */
@Stable
class DragAutoScroll(val listState: LazyListState) {
    /** The list's top and bottom on the window, in pixels. */
    var viewportTop by mutableFloatStateOf(0f)
    var viewportBottom by mutableFloatStateOf(0f)

    /**
     * How far to scroll this frame for a finger at [fingerY] on the window: nothing outside the
     * edge bands of [edgePx], up to [maxStepPx] at the very edge, faster the deeper in.
     */
    fun stepFor(fingerY: Float, edgePx: Float, maxStepPx: Float): Float {
        if (viewportBottom <= viewportTop || edgePx <= 0f) return 0f
        val fromTop = fingerY - viewportTop
        val fromBottom = viewportBottom - fingerY
        return when {
            fromTop < edgePx -> -maxStepPx * (1f - fromTop / edgePx).coerceIn(0f, 1f)
            fromBottom < edgePx -> maxStepPx * (1f - fromBottom / edgePx).coerceIn(0f, 1f)
            else -> 0f
        }
    }
}

val LocalDragAutoScroll = staticCompositionLocalOf<DragAutoScroll?> { null }

/** Keeps [autoScroll]'s viewport in step with the list this modifier sits on. */
fun dragAutoScrollViewportModifier(autoScroll: DragAutoScroll): Modifier = Modifier.onGloballyPositioned { coordinates ->
    val bounds = coordinates.boundsInWindow()
    autoScroll.viewportTop = bounds.top
    autoScroll.viewportBottom = bounds.bottom
}
