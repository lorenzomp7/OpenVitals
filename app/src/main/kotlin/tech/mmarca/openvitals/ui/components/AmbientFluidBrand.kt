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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
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
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.WorkoutActionMetrics
import tech.mmarca.openvitals.ui.theme.loopingMotionAllowed
import kotlin.math.cos
import kotlin.math.sin

private val FluidBorderWidth = 1.dp
private const val FluidFrameIntervalNanos = 33_333_333L

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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(FluidBorderWidth, MaterialTheme.colorScheme.outlineVariant.copy(alpha = Emphasis.subtle), CircleShape)
            .semantics { contentDescription = description }
            .testTag("ambient_fluid_brand"),
    ) {
        // Reading the frame only in DrawScope invalidates this tiny canvas, not the screen composition.
        val seconds = if (animate) timeline.secondsAt(frameMillis) else ExpressiveRestSeconds
        val intro = if (animate) timeline.fillAt(frameMillis) else 1f
        drawExpressiveBrand(seconds, intro)
    }
}

/** 35, 37: the four Google colours of the icon. */
internal val BrandBlue = Color(0xFF4285F4)
internal val BrandRed = Color(0xFFEA4335)
internal val BrandYellow = Color(0xFFFBBC04)
internal val BrandGreen = Color(0xFF34A853)
private val BrandColors = listOf(BrandBlue, BrandRed, BrandYellow, BrandGreen)

/** One choreography cycle: a hop runs along the four shapes, then they rest and breathe. */
private const val ExpressiveCycleSeconds = 3.2f
private const val ExpressiveStaggerSeconds = 0.16f
private const val ExpressiveHopFraction = 0.3f
private const val ExpressiveRestSeconds = 2.6f

/** M3 emphasized easing, cubic-bezier(0.2, 0, 0, 1). */
private val EmphasizedEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** A spatial spring in closed form: overshoots once and settles, as M3 Expressive springs do. */
private fun expressiveSpring(t: Float): Float {
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    val damping = 5.2f
    val frequency = 9.5f
    return 1f - kotlin.math.exp(-damping * t) * cos(frequency * t)
}

/**
 * M3 Expressive motion on the Google palette: the shapes spring in one after the
 * other, then a hop travels along them. Each shape squashes and stretches, and
 * morphs between a circle and a rounded square while it turns, as the Material 3
 * Expressive shape morphs do. A soft wash of the four colours drifts behind.
 */
private fun DrawScope.drawExpressiveBrand(seconds: Float, intro: Float) {
    val w = size.width
    val h = size.height
    // The wash: the icon's gradient, very light, sliding slowly.
    val drift = sin(seconds * 0.35f) * w * 0.25f
    drawRect(
        Brush.linearGradient(
            colors = BrandColors.map { it.copy(alpha = 0.16f) },
            start = Offset(-w * 0.25f + drift, 0f),
            end = Offset(w * 1.25f + drift, h),
        ),
    )
    val count = BrandColors.size
    val dot = minOf(h * 0.4f, w / (count * 2.6f))
    val gap = dot * 0.75f
    val rowWidth = count * dot + (count - 1) * gap
    val left = (w - rowWidth) / 2f
    val baseline = h / 2f
    BrandColors.forEachIndexed { index, color ->
        // Entrance: staggered spring scale.
        val enter = expressiveSpring(((intro * 1.4f) - index * 0.12f).coerceIn(0f, 1f))
        if (enter <= 0f) return@forEachIndexed
        val local = (((seconds - index * ExpressiveStaggerSeconds) % ExpressiveCycleSeconds) +
            ExpressiveCycleSeconds) % ExpressiveCycleSeconds / ExpressiveCycleSeconds
        var lift = 0f
        var stretchY = 1f
        var stretchX = 1f
        var morph = 0f
        if (local < ExpressiveHopFraction) {
            val p = local / ExpressiveHopFraction
            lift = sin(Math.PI.toFloat() * EmphasizedEasing.transform(p)) * h * 0.2f
            // Stretch in the air, squash on take-off and landing.
            val air = sin(Math.PI.toFloat() * p)
            stretchY = 1f + 0.22f * air - 0.18f * (1f - air) * (if (p < 0.12f || p > 0.88f) 1f else 0f)
            stretchX = 1f / stretchY
            morph = air
        } else {
            // Rest: a slow breath.
            val breath = sin((local - ExpressiveHopFraction) * 2f * Math.PI.toFloat())
            stretchY = 1f + 0.03f * breath
            stretchX = 1f - 0.03f * breath
        }
        val width = dot * stretchX * enter
        val height = dot * stretchY * enter
        val cx = left + dot / 2f + index * (dot + gap)
        val cy = baseline - lift + (dot - height) / 2f * 0f
        // Morph: circle (radius = half) to rounded square (radius = 28%), turning a quarter.
        val radius = minOf(width, height) * (0.5f - 0.22f * morph)
        val angle = 90f * EmphasizedEasing.transform(morph.coerceIn(0f, 1f)) * (if (index % 2 == 0) 1f else -1f)
        val topLeft = Offset(cx - width / 2f, cy - height / 2f)
        val sizeRect = androidx.compose.ui.geometry.Size(width, height)
        val shade = Brush.linearGradient(
            colors = listOf(color, lerpColor(color, Color.White, 0.18f)),
            start = topLeft,
            end = Offset(topLeft.x + width, topLeft.y + height),
        )
        rotate(angle, Offset(cx, cy)) {
            drawRoundRect(
                brush = shade,
                topLeft = topLeft,
                size = sizeRect,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t)
