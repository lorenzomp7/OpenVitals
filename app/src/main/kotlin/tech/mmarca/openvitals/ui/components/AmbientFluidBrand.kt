package tech.mmarca.openvitals.ui.components

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
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

/**
 * 94: what the toggle says about the selected period. [fills] are the four goals in the lobes'
 * colours, each `0..1`: the steps, the active calories, the workout (all or nothing) and the
 * sleep. [name] is who it welcomes back when the app opens; null welcomes no one by name.
 */
@androidx.compose.runtime.Immutable
data class BrandDay(
    val name: String?,
    val fills: List<Float>,
)

/**
 * 97: the colours the brand pieces of the home screen are painted with (the toggle, the
 * round header buttons, the action pills), and whether they move or stand still.
 */
@androidx.compose.runtime.Immutable
data class BrandLook(val colors: List<Color>, val animated: Boolean = true)

val LocalBrandLook = androidx.compose.runtime.staticCompositionLocalOf { BrandLook(BrandColors) }

/**
 * 100: the greeting belongs to the app's opening, once per launch, never to a return to it. It
 * keeps the moment it began rather than a flag: the loading screen's header draws a toggle first
 * and the loaded dashboard draws another, and with a flag the first took the greeting with it
 * when it left, so the dashboard never showed it.
 */
private object BrandGreetingGate {
    var startedAt: Long? = null

    fun since(now: Long): Long? {
        val started = startedAt ?: now.also { startedAt = it }
        return started.takeIf { now - it < GreetingMillis }
    }
}

/** One spring entrance, then the expressive loop. Navigation never owns or resets the clock. */
@Composable
fun AmbientFluidBrand(
    timeline: FluidBrandTimeline,
    windowWidth: Dp,
    modifier: Modifier = Modifier,
    day: BrandDay? = null,
    onClick: (() -> Unit)? = null,
    /** 106: the pill's width when the header has less room than the action row; else as wide as an action. */
    width: Dp? = null,
) {
    var frameMillis by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val look = LocalBrandLook.current
    val colors = look.colors.ifEmpty { BrandColors }
    // 97: the OpenVitals toggle always moves; the Animated switch is for the buttons around it.
    val animate = loopingMotionAllowed()
    // 100: greet only on the first toggle the app draws since it was launched. Keyed on the day
    // arriving: the first frames draw before the dashboard has loaded, and the welcome waited
    // for nothing then, so it never showed.
    val greetSince = remember(day != null) {
        if (day != null) BrandGreetingGate.since(SystemClock.uptimeMillis()) else null
    }
    val greeting = day?.takeIf { greetSince != null }?.let { brandGreeting(it.name) }
    val description = stringResource(R.string.app_name)
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    // 115: a light theme draws the toggle on its light surface, with dark writing.
    val light = isLightBrandTheme()
    val surface = brandButtonSurface()
    // 100: the greeting is lettered as "Start workout" and "+ Log" are: their type, and the
    // home screen colours running through it.
    val greetingStyle = MaterialTheme.typography.bodyLarge
    val inkColors = brandInkColors(colors)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pillWidth = width ?: WorkoutActionMetrics.primaryWidth(windowWidth)
    // 98: the greeting fits the pill left of the sync light, shrinking rather than running under it.
    val greetingLayout = remember(greeting, greetingStyle, pillWidth, density) {
        greeting?.let { text ->
            val room = with(density) { (pillWidth - WorkoutActionMetrics.height * 0.8f).toPx() }.toInt().coerceAtLeast(1)
            var style = greetingStyle
            var layout = measurer.measure(text, style, maxLines = 1, softWrap = false)
            repeat(6) {
                if (layout.size.width <= room) return@repeat
                style = style.copy(fontSize = style.fontSize * 0.9f)
                layout = measurer.measure(text, style, maxLines = 1, softWrap = false)
            }
            layout
        }
    }
    val fills = remember(day?.fills) { day?.fills?.toFloatArray() }
    LaunchedEffect(timeline, lifecycle, animate) {
        timeline.start(SystemClock.uptimeMillis())
        // A still toggle (97) still runs its clock for the opening greeting, then stops.
        val greetingUntil = greetSince?.plus(GreetingMillis)
        if (animate || greetingUntil != null) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var previousFrame = 0L
            while (isActive && (animate || SystemClock.uptimeMillis() < (greetingUntil ?: 0L))) withFrameNanos { frame ->
                if (frame - previousFrame >= FluidFrameIntervalNanos) {
                    frameMillis = SystemClock.uptimeMillis()
                    previousFrame = frame
                }
            }
        }
    }
    Canvas(
        modifier = modifier
            .size(pillWidth, WorkoutActionMetrics.height)
            .clip(CircleShape)
            .background(surface)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = stringResource(R.string.brand_open_summary), onClick = onClick)
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = description }
            .testTag("ambient_fluid_brand"),
    ) {
        // Reading the frame only in DrawScope invalidates this tiny canvas, not the screen composition.
        val seconds = if (animate) timeline.secondsAt(frameMillis) else ToggleRestSeconds
        val intro = if (animate) timeline.fillAt(frameMillis) else 1f
        // 94: the greeting holds the pill while the app opens, then gives way to the wave.
        // 100: it switches on and off like a cathode-ray television (GreetingCrtMotion).
        val crt = if (greetingLayout != null && greetSince != null) {
            greetingCrtAt((frameMillis - greetSince).coerceAtLeast(0L) / 1_000f)
        } else {
            null
        }
        drawOpenVitalsToggle(seconds, intro * (1f - (crt?.presence ?: 0f)), FluidBorderWidth.toPx(), fills, colors, light)
        if (greetingLayout != null && crt != null) {
            // 106: no sync light at the end any more, so the greeting centres in the whole pill.
            val room = size.width - size.height * 0.4f
            val topLeft = Offset(
                size.height * 0.4f + (room - size.height * 0.4f - greetingLayout.size.width).coerceAtLeast(0f) / 2f,
                (size.height - greetingLayout.size.height) / 2f,
            )
            val textWidth = greetingLayout.size.width.toFloat()
            val textHeight = greetingLayout.size.height.toFloat()
            val centre = topLeft + Offset(textWidth / 2f, textHeight / 2f)
            // The buttons' ink: the palette drifting along the letters at the buttons' pace.
            val drift = (timeline.secondsAt(frameMillis) * 0.15f) % 1f
            val ink = Brush.linearGradient(
                colors = List(5) { cyclicBrandColor(it / 4f + drift, inkColors) },
                start = topLeft,
                end = topLeft + Offset(textWidth, textHeight),
            )
            withTransform({ scale(crt.scaleX, crt.scaleY, pivot = centre) }) {
                drawText(textLayoutResult = greetingLayout, brush = ink, alpha = crt.alpha, topLeft = topLeft)
            }
            if (crt.beam > 0f) {
                // The electron beam: a white line where the picture is squeezed, glowing around.
                val lineWidth = textWidth * crt.scaleX
                val lineHeight = (textHeight * crt.scaleY).coerceAtLeast(2.dp.toPx())
                val lineTopLeft = Offset(centre.x - lineWidth / 2f, centre.y - lineHeight / 2f)
                val glow = lineHeight * 3f
                drawRoundRect(
                    brush = ink,
                    topLeft = Offset(lineTopLeft.x, centre.y - glow / 2f),
                    size = androidx.compose.ui.geometry.Size(lineWidth, glow),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(glow / 2f),
                    alpha = 0.35f * crt.beam * crt.alpha,
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = lineTopLeft,
                    size = androidx.compose.ui.geometry.Size(lineWidth, lineHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(lineHeight / 2f),
                    alpha = 0.9f * crt.beam * crt.alpha,
                )
            }
        }
    }
}

private const val GreetingMillis = 3_600L

/** 100: "Welcome back, name" as the app opens; without a name, "Welcome back". */
@Composable
private fun brandGreeting(name: String?): String {
    val first = name?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }
    return if (first != null) stringResource(R.string.brand_welcome_back_name, first) else stringResource(R.string.brand_welcome_back)
}

/** 35, 37: the four Google colours of the icon. */
internal val BrandBlue = Color(0xFF4285F4)
internal val BrandRed = Color(0xFFEA4335)
internal val BrandYellow = Color(0xFFFBBC04)
internal val BrandGreen = Color(0xFF34A853)
internal val BrandColors = listOf(BrandBlue, BrandRed, BrandYellow, BrandGreen)

/** The night the light is drawn on, as in the Gemini reel the toggle follows. */
internal val ToggleNight = Color(0xFF0B0F1A)
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

/** 148: the share of the pill's height a lobe spans, above and below the thread, at 100 %. */
private const val ToggleWaveSpan = 0.8f

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
private fun DrawScope.drawOpenVitalsToggle(
    seconds: Float,
    intro: Float,
    borderPx: Float,
    fills: FloatArray? = null,
    colors: List<Color> = BrandColors,
    /** 115: on a light surface: no additive light, which washes out to white there. */
    light: Boolean = false,
) {
    val blend = if (light) BlendMode.SrcOver else BlendMode.Plus
    // Sparks and edges lean to white on the night, to a deeper shade on a light surface.
    val sheen = if (light) Color.Black else Color.White
    val w = size.width
    val h = size.height
    val mid = h / 2f
    val open = EmphasizedEasing.transform(intro.coerceIn(0f, 1f))
    // Soft halo behind the wave.
    drawRect(
        Brush.radialGradient(
            colors = listOf(colors.first().copy(alpha = (if (light) 0.12f else 0.22f) * open), Color.Transparent),
            center = Offset(w * (0.5f + sin(seconds * 0.3f) * 0.15f), mid),
            radius = w * 0.45f,
        ),
    )
    // The wave: each lobe a lens above and below the midline, its height breathing.
    // 98: while the greeting holds the pill the wave is flat; a flat lobe's outline would draw a line through it.
    if (open > 0.02f) ToggleLobes.forEachIndexed { index, lobe ->
        val lobeColor = colors[index % colors.size].let { if (light) lerpColor(it, Color.Black, 0.1f) else it }
        // 94: a lobe swells as far as its goal is met.
        // 148: every lobe measures its goal on the same scale: at 100 % any of the four spans the
        // pill's full wave height, whatever its shape; the lobe's own shape only decides how
        // that height splits above and below the thread, and the breathing stays small.
        val goal = fills?.getOrNull(index)?.coerceIn(0f, 1f)?.let { 0.12f + 0.88f * it } ?: 1f
        val drift = sin(seconds * 0.35f * lobe.speed + lobe.phase) * 0.06f
        val cx = (0.5f + (lobe.center + drift - 0.5f) * open) * w
        val halfWidth = lobe.width * w * (0.4f + 0.6f * open)
        val upShare = lobe.up / (lobe.up + lobe.down)
        val span = ToggleWaveSpan * h * open * goal
        val breathUp = 0.9f + 0.1f * sin(seconds * lobe.speed * 1.6f + lobe.phase)
        val breathDown = 0.9f + 0.1f * sin(seconds * lobe.speed * 1.6f + lobe.phase + 1.6f)
        val up = span * upShare * breathUp
        val down = span * (1f - upShare) * breathDown
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
                    lobeColor.copy(alpha = if (light) 0.75f else 0.95f),
                    lobeColor.copy(alpha = if (light) 0.25f else 0.35f),
                    lobeColor.copy(alpha = if (light) 0.75f else 0.95f),
                ),
                startY = mid - up,
                endY = mid + down,
            ),
            blendMode = blend,
        )
        drawPath(
            path = path,
            color = lerpColor(lobeColor, sheen, if (light) 0.15f else 0.3f).copy(alpha = 0.7f * open),
            style = Stroke(width = borderPx),
            blendMode = blend,
        )
    }
    // The bright thread the lobes hang from.
    drawLine(
        brush = Brush.horizontalGradient(colors.map { it.copy(alpha = 0.55f * open) }.let { if (it.size < 2) it + it else it }),
        start = Offset(w * 0.08f, mid),
        end = Offset(w * 0.92f, mid),
        strokeWidth = borderPx,
    )
    // Sparks: drift up and sideways, twinkle.
    ToggleSparks.forEach { (sx, sy, rate, tint) ->
        val x = ((sx + seconds * 0.015f * rate) % 1f) * w
        val y = (((sy - seconds * 0.04f * rate) % 1f + 1f) % 1f) * h
        val twinkle = (0.5f + 0.5f * sin(seconds * rate * 2.2f + tint)).let { it * it }
        val color = lerpColor(colors[tint.toInt() % colors.size], sheen, if (light) 0.2f else 0.6f)
        drawCircle(color.copy(alpha = 0.25f * twinkle * open), radius = h * 0.06f, center = Offset(x, y))
        drawCircle(color.copy(alpha = 0.95f * twinkle * open), radius = h * 0.018f, center = Offset(x, y))
    }
    // The ring of colours running round the edge, with a soft glow under it.
    val shift = (seconds * 0.11f) % 1f
    val stops = Array(17) { k ->
        val position = k / 16f
        position to cyclicBrandColor(position - shift, colors)
    }
    val ring = Brush.sweepGradient(*stops, center = Offset(w / 2f, mid))
    val radius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)
    // 100: the same edge as the buttons beside it: a 2 dp ring inset by half, a glow three times as wide.
    val stroke = borderPx * 2f
    drawRoundRect(brush = ring, alpha = 0.35f * open, style = Stroke(width = stroke * 3f), cornerRadius = radius)
    drawRoundRect(
        brush = ring,
        alpha = open,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = androidx.compose.ui.geometry.Size(w - stroke, h - stroke),
        style = Stroke(width = stroke),
        cornerRadius = radius,
    )
}

/** The four colours around a circle, blended between neighbours; [position] wraps. */
internal fun cyclicBrandColor(position: Float, colors: List<Color> = BrandColors): Color {
    val palette = colors.ifEmpty { BrandColors }
    val p = ((position % 1f) + 1f) % 1f * palette.size
    val index = p.toInt() % palette.size
    return lerpColor(palette[index], palette[(index + 1) % palette.size], p - p.toInt())
}

/** M3 emphasized easing, cubic-bezier(0.2, 0, 0, 1). */
private val EmphasizedEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t)
