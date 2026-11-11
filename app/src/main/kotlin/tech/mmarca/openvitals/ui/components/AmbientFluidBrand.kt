package tech.mmarca.openvitals.ui.components

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.isActive
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.SleepColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WorkoutActionMetrics
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import tech.mmarca.openvitals.ui.theme.loopingMotionAllowed
import kotlin.math.cos
import kotlin.math.sin

private val FluidBorderWidth = 1.dp
private const val FluidFrameIntervalNanos = 33_333_333L
private val FluidPalette = listOf(DistanceColor, SleepColor, HeartColor, CaloriesColor, StepsColor, WorkoutColor)

@Composable
fun rememberFluidBrandTimeline(): FluidBrandTimeline = rememberSaveable(
    saver = Saver<FluidBrandTimeline, Long>(save = { it.startedAtMillis }, restore = { FluidBrandTimeline(it) }),
) { FluidBrandTimeline() }

/** One initial fill, then continuous paint-like flow. Navigation never owns or resets the clock. */
@Composable
fun AmbientFluidBrand(timeline: FluidBrandTimeline, windowWidth: Dp, modifier: Modifier = Modifier) {
    var frameMillis by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val animate = loopingMotionAllowed()
    val description = stringResource(R.string.app_name)
    LaunchedEffect(timeline, lifecycle, animate) {
        timeline.start(SystemClock.uptimeMillis())
        if (animate) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var previousFrame = 0L
            while (isActive) withFrameNanos { frame ->
                if (frame - previousFrame >= FluidFrameIntervalNanos) {
                    frameMillis = SystemClock.uptimeMillis()
                    previousFrame = frame
                }
            }
        }
    }
    Canvas(
        modifier = modifier
            .size(WorkoutActionMetrics.primaryWidth(windowWidth), WorkoutActionMetrics.height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(FluidBorderWidth, MaterialTheme.colorScheme.outlineVariant.copy(alpha = Emphasis.subtle), CircleShape)
            .semantics { contentDescription = description }
            .testTag("ambient_fluid_brand"),
    ) {
        // Reading the frame only in DrawScope invalidates this tiny canvas, not the screen composition.
        val seconds = if (animate) timeline.secondsAt(frameMillis) else 0f
        val fill = if (animate) timeline.fillAt(frameMillis) else 1f
        clipPath(fluidSurface(seconds, fill)) { drawFluidPaint(seconds) }
    }
}

private fun DrawScope.fluidSurface(seconds: Float, fill: Float): Path = Path().apply {
    val level = size.height * (1f - fill * 1.12f)
    val amplitude = size.height * (0.055f - fill * 0.025f)
    moveTo(0f, size.height)
    for (index in 0..48) {
        val fraction = index / 48f
        val wave = sin(fraction * 7f + seconds * 1.8f) + sin(fraction * 13f - seconds * 1.2f) * 0.4f
        lineTo(size.width * fraction, level + wave * amplitude)
    }
    lineTo(size.width, size.height)
    close()
}

private fun DrawScope.drawFluidPaint(seconds: Float) {
    drawRect(Brush.linearGradient(
        colors = listOf(WorkoutColor, DistanceColor, SleepColor, HeartColor),
        start = Offset(size.width * (0.3f + sin(seconds * 0.17f) * 0.2f), 0f),
        end = Offset(size.width * (0.7f + cos(seconds * 0.13f) * 0.3f), size.height),
    ))
    FluidPalette.forEachIndexed { index, color ->
        val phase = index * 1.37f
        val center = Offset(
            size.width * (0.5f + sin(seconds * 0.22f + phase) * 0.53f),
            size.height * (0.5f + cos(seconds * 0.31f + phase) * 0.62f),
        )
        val radius = size.width * (0.42f + sin(seconds * 0.19f + phase) * 0.1f)
        drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius), radius, center)
    }
    val ribbon = Path().apply {
        moveTo(-size.width * 0.1f, size.height * 0.8f)
        cubicTo(size.width * 0.3f, size.height * sin(seconds * 0.4f),
            size.width * 0.6f, size.height * (1f + cos(seconds * 0.35f)), size.width * 1.1f, size.height * 0.1f)
        lineTo(size.width * 1.1f, size.height * 0.5f)
        cubicTo(size.width * 0.6f, size.height * (1.3f + cos(seconds * 0.35f)),
            size.width * 0.3f, size.height * (0.3f + sin(seconds * 0.4f)), -size.width * 0.1f, size.height)
        close()
    }
    drawPath(ribbon, Brush.linearGradient(listOf(HeartColor.copy(alpha = Emphasis.fill),
        WorkoutColor.copy(alpha = Emphasis.subtle), Color.Transparent)))
}
