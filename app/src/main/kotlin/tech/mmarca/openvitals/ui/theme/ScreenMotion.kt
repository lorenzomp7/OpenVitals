package tech.mmarca.openvitals.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin

/**
 * The one way a view opens over another and closes back to it, shared by the screens and
 * by the dashboard's editor. Opening, the new view fades and grows in over the old, which stays
 * drawn underneath; closing, the view returned to is there at once and the one left fades and
 * shrinks away over it. No frame is ever empty.
 */
object ScreenMotion {
    const val DurationMillis = 240
    private const val Scale = 0.96f

    fun openEnter(): EnterTransition =
        fadeIn(tween(DurationMillis)) + scaleIn(tween(DurationMillis), initialScale = Scale)

    /** The view being covered stays as it is until the one opening has covered it. */
    fun openExit(): ExitTransition = fadeOut(tween(durationMillis = 1, delayMillis = DurationMillis))

    fun closeExit(): ExitTransition =
        fadeOut(tween(DurationMillis)) + scaleOut(tween(DurationMillis), targetScale = Scale)

    /**
     * The dashboard's editor and the dashboard swap places in the same spot, so they never
     * overlap: the one leaving fades out first, then the one arriving fades in, growing a little
     * from its top edge. Both ways the same.
     */
    const val FadeThroughOutMillis = 90
    private const val FadeThroughInMillis = 210
    private const val FadeThroughScale = 0.94f

    fun fadeThroughEnter(): EnterTransition =
        fadeIn(tween(FadeThroughInMillis, delayMillis = FadeThroughOutMillis)) +
            scaleIn(
                tween(FadeThroughInMillis, delayMillis = FadeThroughOutMillis),
                initialScale = FadeThroughScale,
                transformOrigin = TransformOrigin(0.5f, 0f),
            )

    fun fadeThroughExit(): ExitTransition = fadeOut(tween(FadeThroughOutMillis))
}
