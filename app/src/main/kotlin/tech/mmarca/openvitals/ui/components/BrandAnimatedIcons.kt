package tech.mmarca.openvitals.ui.components

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.luminance
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
    val animate = loopingMotionAllowed() && LocalBrandLook.current.animated
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
 * A header button in the style of the animated OpenVitals toggle: a night
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
    val colors = LocalBrandLook.current.colors
    val inkColors = brandInkColors(colors)
    Box(
        modifier = modifier
            .size(BrandIconFrameSize)
            .clip(CircleShape)
            .background(brandButtonSurface())
            .drawWithContent {
                drawContent()
                val seconds = timeline.secondsAt(clock.value)
                val shift = (seconds * 0.11f) % 1f
                val stops = Array(17) { k -> (k / 16f) to cyclicBrandColor(k / 16f - shift, colors) }
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
                                colors = List(5) { cyclicBrandColor(it / 4f + drift, inkColors) },
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

/**
 * A button in the style of the animated OpenVitals toggle: a night pill with the four
 * Google colours running round its edge, and its icon and label painted with the same
 * colours, drifting. It shares the toggle's clock, so the two move as one.
 */
@Composable
fun BrandPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    timeline: FluidBrandTimeline? = LocalFluidBrandTimeline.current,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val clockTimeline = timeline ?: remember { FluidBrandTimeline() }
    val clock = rememberBrandClock(clockTimeline)
    val colors = LocalBrandLook.current.colors
    val inkColors = brandInkColors(colors)
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .clip(shape)
            .background(brandButtonSurface())
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .drawWithContent {
                drawContent()
                val seconds = clockTimeline.secondsAt(clock.value)
                val shift = (seconds * 0.11f) % 1f
                val stops = Array(17) { k -> (k / 16f) to cyclicBrandColor(k / 16f - shift, colors) }
                val ring = Brush.sweepGradient(*stops, center = center)
                val stroke = Spacing.xs.toPx() / 2f
                val radius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(ring, alpha = PillGlowAlpha, style = Stroke(stroke * 3f), cornerRadius = radius)
                drawRoundRect(
                    ring,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke),
                    cornerRadius = radius,
                )
            }
            .padding(horizontal = Spacing.md),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val seconds = clockTimeline.secondsAt(clock.value)
                    val drift = (seconds * 0.15f) % 1f
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = List(5) { cyclicBrandColor(it / 4f + drift, inkColors) },
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        ),
                        blendMode = BlendMode.SrcIn,
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** The glow under the pill's ring, as the toggle's. */
private const val PillGlowAlpha = 0.35f

/**
 * What the brand buttons are painted on: the toggle's night in a dark theme, the theme's
 * own light surface in a light one, where a row of black pills looked like a mistake.
 */
@Composable
internal fun brandButtonSurface(): androidx.compose.ui.graphics.Color {
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    return if (isLightBrandTheme()) scheme.surfaceContainerHighest else ToggleNight
}

/**
 * The colours an icon or label is painted with. On a light surface they are darkened a
 * little, or the Google yellow and green would vanish into it.
 */
@Composable
internal fun brandInkColors(colors: List<androidx.compose.ui.graphics.Color>): List<androidx.compose.ui.graphics.Color> {
    val light = isLightBrandTheme()
    return remember(colors, light) {
        if (light) colors.map { androidx.compose.ui.graphics.lerp(it, androidx.compose.ui.graphics.Color.Black, LightInkDarkening) } else colors
    }
}

@Composable
internal fun isLightBrandTheme(): Boolean =
    androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() > 0.5f

private const val LightInkDarkening = 0.3f
