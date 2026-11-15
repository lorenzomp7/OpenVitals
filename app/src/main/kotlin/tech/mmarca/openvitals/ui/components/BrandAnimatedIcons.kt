package tech.mmarca.openvitals.ui.components

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.isActive
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.theme.loopingMotionAllowed

private const val BrandIconFrameNanos = 33_333_333L

/** The frame clock the brand pieces share, read only in draw so it never recomposes the screen. */
@Composable
internal fun rememberBrandClock(timeline: FluidBrandTimeline): State<Long> {
    val frameMillis = remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val animate = loopingMotionAllowed()
    LaunchedEffect(timeline, lifecycle, animate) {
        timeline.start(SystemClock.uptimeMillis())
        if (animate) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var previous = 0L
            while (isActive) withFrameNanos { frame ->
                if (frame - previous >= BrandIconFrameNanos) {
                    frameMillis.longValue = SystemClock.uptimeMillis()
                    previous = frame
                }
            }
        }
    }
    return frameMillis
}

/**
 * 78: a header button in the style of the animated OpenVitals toggle: a night
 * circle with the four Google colours running round its edge, and the icon
 * inside painted with the same colours, drifting. [content] draws the icon or
 * the profile photo; [tintContent] paints it with the colours (off for a photo).
 */
@Composable
fun BrandIconFrame(
    timeline: FluidBrandTimeline,
    modifier: Modifier = Modifier,
    tintContent: Boolean = true,
    content: @Composable () -> Unit,
) {
    val clock = rememberBrandClock(timeline)
    Box(
        modifier = modifier
            .size(BrandIconFrameSize)
            .clip(CircleShape)
            .background(ToggleNight)
            .drawWithContent {
                drawContent()
                val seconds = timeline.secondsAt(clock.value)
                val shift = (seconds * 0.11f) % 1f
                val stops = Array(17) { k -> (k / 16f) to cyclicBrandColor(k / 16f - shift) }
                val ring = Brush.sweepGradient(*stops, center = center)
                val stroke = Spacing.xs.toPx() / 2f
                drawCircle(ring, radius = size.minDimension / 2f - stroke * 1.5f, alpha = 0.35f, style = Stroke(stroke * 3f))
                drawCircle(ring, radius = size.minDimension / 2f - stroke, style = Stroke(stroke))
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = if (tintContent) {
                Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val seconds = timeline.secondsAt(clock.value)
                        val drift = (seconds * 0.15f) % 1f
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = List(5) { cyclicBrandColor(it / 4f + drift) },
                                start = Offset.Zero,
                                end = Offset(size.width, size.height),
                            ),
                            blendMode = BlendMode.SrcIn,
                        )
                    }
            } else {
                Modifier
            },
        ) { content() }
    }
}

private val BrandIconFrameSize = Spacing.xl + Spacing.md + Spacing.xs
