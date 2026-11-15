package tech.mmarca.openvitals.ui.components

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import tech.mmarca.openvitals.ui.theme.WorkoutActionMetrics
import tech.mmarca.openvitals.ui.theme.loopingMotionAllowed
import kotlin.math.sin

private val FluidBorderWidth = 1.dp
private const val FluidFrameIntervalNanos = 16_666_666L

@Composable
fun rememberFluidBrandTimeline(): FluidBrandTimeline = rememberSaveable(
    saver = Saver<FluidBrandTimeline, Long>(save = { it.startedAtMillis }, restore = { FluidBrandTimeline(it) }),
) { FluidBrandTimeline() }

/** One spring entrance, then the expressive loop. Navigation never owns or resets the clock. */
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
            .background(ToggleNight)
            .semantics { contentDescription = description }
            .testTag("ambient_fluid_brand"),
    ) {
        // Reading the frame only in DrawScope invalidates this tiny canvas, not the screen composition.
        val seconds = if (animate) timeline.secondsAt(frameMillis) else ToggleRestSeconds
        val intro = if (animate) timeline.fillAt(frameMillis) else 1f
        drawOpenVitalsToggle(seconds, intro, FluidBorderWidth.toPx())
    }
}

/** 35, 37: the four Google colours of the icon. */
internal val BrandBlue = Color(0xFF4285F4)
internal val BrandRed = Color(0xFFEA4335)
internal val BrandYellow = Color(0xFFFBBC04)
internal val BrandGreen = Color(0xFF34A853)
private val BrandColors = listOf(BrandBlue, BrandRed, BrandYellow, BrandGreen)

/** The night the light is drawn on, as in the Gemini reel the toggle follows. */
private val ToggleNight = Color(0xFF0B0F1A)
private const val ToggleRestSeconds = 4f
private const val ToggleTwoPi = (2.0 * Math.PI).toFloat()

/** One lobe of the voice wave: where it sits, how wide, and how it breathes. */
private class ToggleLobe(
    val color: Color,
    val center: Float,
    val width: Float,
    val up: Float,
    val down: Float,
    val speed: Float,
    val phase: Float,
)

private val ToggleLobes = listOf(
    ToggleLobe(BrandBlue, center = 0.28f, width = 0.16f, up = 0.36f, down = 0.30f, speed = 0.9f, phase = 0f),
    ToggleLobe(BrandRed, center = 0.40f, width = 0.22f, up = 0.12f, down = 0.18f, speed = 1.3f, phase = 1.7f),
    ToggleLobe(BrandYellow, center = 0.60f, width = 0.14f, up = 0.32f, down = 0.10f, speed = 1.1f, phase = 3.1f),
    ToggleLobe(BrandGreen, center = 0.72f, width = 0.15f, up = 0.10f, down = 0.34f, speed = 0.8f, phase = 4.4f),
)

/** Fixed sparkle seeds: position, drift, twinkle rate and tint, so every frame agrees. */
private val ToggleSparks = List(14) { index ->
    val golden = 0.618034f
    floatArrayOf(
        (index * golden) % 1f,
        ((index * 7 + 3) * golden) % 1f,
        0.6f + (index % 5) * 0.25f,
        index.toFloat(),
    )
}

/**
 * 35: the "Toggle animato OpenVitals", after the Gemini reel: on a night pill,
 * a voice wave of four light lobes in the Google colours that swell, drift and
 * add up where they cross; sparks that twinkle and drift; and a ring of the four
 * colours that runs round the edge. It opens by drawing the wave out from the
 * centre, with the M3 emphasized easing.
 */
private fun DrawScope.drawOpenVitalsToggle(seconds: Float, intro: Float, borderPx: Float) {
    val w = size.width
    val h = size.height
    val mid = h / 2f
    val open = EmphasizedEasing.transform(intro.coerceIn(0f, 1f))
    // Soft halo behind the wave.
    drawRect(
        Brush.radialGradient(
            colors = listOf(BrandBlue.copy(alpha = 0.22f * open), Color.Transparent),
            center = Offset(w * (0.5f + sin(seconds * 0.3f) * 0.15f), mid),
            radius = w * 0.45f,
        ),
    )
    // The wave: each lobe a lens above and below the midline, its height breathing.
    ToggleLobes.forEach { lobe ->
        val drift = sin(seconds * 0.35f * lobe.speed + lobe.phase) * 0.06f
        val cx = (0.5f + (lobe.center + drift - 0.5f) * open) * w
        val halfWidth = lobe.width * w * (0.4f + 0.6f * open)
        val breath = 0.55f + 0.45f * sin(seconds * lobe.speed * 1.6f + lobe.phase)
        val up = lobe.up * h * breath * open
        val down = lobe.down * h * (1.1f - breath * 0.6f) * open
        val path = Path().apply {
            val steps = 28
            moveTo(cx - halfWidth * 2.2f, mid)
            for (step in 0..steps) {
                val x = cx - halfWidth * 2.2f + halfWidth * 4.4f * step / steps
                val u = (x - cx) / halfWidth
                lineTo(x, mid - up * kotlin.math.exp(-u * u))
            }
            for (step in steps downTo 0) {
                val x = cx - halfWidth * 2.2f + halfWidth * 4.4f * step / steps
                val u = (x - cx) / halfWidth
                lineTo(x, mid + down * kotlin.math.exp(-u * u))
            }
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lobe.color.copy(alpha = 0.95f),
                    lobe.color.copy(alpha = 0.35f),
                    lobe.color.copy(alpha = 0.95f),
                ),
                startY = mid - up,
                endY = mid + down,
            ),
            blendMode = BlendMode.Plus,
        )
        drawPath(
            path = path,
            color = lerpColor(lobe.color, Color.White, 0.3f).copy(alpha = 0.7f),
            style = Stroke(width = borderPx),
            blendMode = BlendMode.Plus,
        )
    }
    // The bright thread the lobes hang from.
    drawLine(
        brush = Brush.horizontalGradient(BrandColors.map { it.copy(alpha = 0.55f * open) }),
        start = Offset(w * 0.08f, mid),
        end = Offset(w * 0.92f, mid),
        strokeWidth = borderPx,
    )
    // Sparks: drift up and sideways, twinkle.
    ToggleSparks.forEach { (sx, sy, rate, tint) ->
        val x = ((sx + seconds * 0.015f * rate) % 1f) * w
        val y = (((sy - seconds * 0.04f * rate) % 1f + 1f) % 1f) * h
        val twinkle = (0.5f + 0.5f * sin(seconds * rate * 2.2f + tint)).let { it * it }
        val color = lerpColor(BrandColors[tint.toInt() % BrandColors.size], Color.White, 0.6f)
        drawCircle(color.copy(alpha = 0.25f * twinkle * open), radius = h * 0.06f, center = Offset(x, y))
        drawCircle(color.copy(alpha = 0.95f * twinkle * open), radius = h * 0.018f, center = Offset(x, y))
    }
    // The ring of colours running round the edge, with a soft glow under it.
    val shift = (seconds * 0.11f) % 1f
    val stops = Array(17) { k ->
        val position = k / 16f
        position to cyclicBrandColor(position - shift)
    }
    val ring = Brush.sweepGradient(*stops, center = Offset(w / 2f, mid))
    val radius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)
    drawRoundRect(brush = ring, alpha = 0.35f * open, style = Stroke(width = borderPx * 4f), cornerRadius = radius)
    drawRoundRect(brush = ring, alpha = open, style = Stroke(width = borderPx * 1.5f), cornerRadius = radius)
}

/** The four colours around a circle, blended between neighbours; [position] wraps. */
private fun cyclicBrandColor(position: Float): Color {
    val p = ((position % 1f) + 1f) % 1f * BrandColors.size
    val index = p.toInt() % BrandColors.size
    return lerpColor(BrandColors[index], BrandColors[(index + 1) % BrandColors.size], p - p.toInt())
}

/** M3 emphasized easing, cubic-bezier(0.2, 0, 0, 1). */
private val EmphasizedEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t)
