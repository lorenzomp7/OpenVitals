package tech.mmarca.openvitals.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared geometry for the primary workout action and the ambient app-bar brand. */
object WorkoutActionMetrics {
    val height = 48.dp
    val editSize = 44.dp
    val spacing = 12.dp
    val screenGutter = 16.dp
    const val primaryWeight = 1f
    const val secondaryWeight = 0.70f
    private val minimumWidth = 0.dp

    fun primaryWidth(windowWidth: Dp): Dp =
        ((windowWidth - screenGutter * 2 - editSize - spacing * 2) /
            (primaryWeight + secondaryWeight)).coerceAtLeast(minimumWidth)
}
