package tech.mmarca.openvitals.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared geometry for the primary workout action and the ambient app-bar brand. */
object WorkoutActionMetrics {
    val height = 48.dp
    val spacing = 12.dp
    val screenGutter = 16.dp
    private val minimumWidth = 0.dp

    /** 107: "Start workout" and "+ Log" share the row in two equal halves, and the toggle is as wide. */
    fun primaryWidth(windowWidth: Dp): Dp =
        ((windowWidth - screenGutter * 2 - spacing) / 2).coerceAtLeast(minimumWidth)
}
