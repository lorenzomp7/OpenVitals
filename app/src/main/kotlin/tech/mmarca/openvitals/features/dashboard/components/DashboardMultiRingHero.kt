package tech.mmarca.openvitals.features.dashboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.mmarca.openvitals.features.dashboard.DashboardHeroRing
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Radii

private val HeroPadding = 10.dp
private val HeroRingStroke = 12.dp
private val HeroSideStroke = 10.dp
private val HeroValueFloor = 13.sp

/** Room under the rings for their reflection, as on a glossy floor. */
private val HeroReflectionRoom = 22.dp

/** How far the glow spreads round a lit arc. */
private val HeroGlowBlur = 7.dp
private const val HeroGlowAlpha = 0.55f
private const val HeroReflectionAlpha = 0.30f

/**
 * How far the side arcs sit outside the centre ring, as a multiple of its radius. Wider,
 * so a clear band of dark runs between each arc and the ring.
 */
private const val HeroSideRadiusFactor = 1.42f

/** Each side arc spans this many degrees, centred on the horizontal. */
private const val HeroSideSweep = 100f
private const val HeroRevealMillis = 700

/**
 * Three metrics in one card: a full ring in the middle and an arc on each
 * side, each with its own number. A missing side leaves its space empty.
 */
@Composable
internal fun DashboardMultiRingHero(
    center: DashboardHeroRing,
    left: DashboardHeroRing?,
    right: DashboardHeroRing?,
    modifier: Modifier = Modifier,
) {
    val centerColors = dashboardTileColors(center.accentColor)
    val leftColors = left?.let { dashboardTileColors(it.accentColor) }
    val rightColors = right?.let { dashboardTileColors(it.accentColor) }
    val centerFraction by animateFloatAsState(center.fraction, tween(240), label = "centerRing")
    val leftFraction by animateFloatAsState(left?.fraction ?: 0f, tween(240), label = "leftRing")
    val rightFraction by animateFloatAsState(right?.fraction ?: 0f, tween(240), label = "rightRing")

    OpenVitalsCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(Radii.xl),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(HeroPadding),
        ) {
            val hasSides = left != null || right != null
            // The centre ring is as large as the height allows, less when the
            // sides need room for their arcs and numbers.
            val byHeight = (maxHeight - HeroReflectionRoom) / 2 - HeroRingStroke / 2
            val byWidth = if (hasSides) (maxWidth / 2 - 62.dp) / HeroSideRadiusFactor else maxWidth / 2
            val ringRadius: Dp = minOf(byHeight, byWidth)
            val sideRadius = ringRadius * HeroSideRadiusFactor
            val sideWidth = (maxWidth / 2 - sideRadius - HeroSideStroke).coerceAtLeast(0.dp)
            val glowPaint = remember { Paint() }
            // The glow and the floor's reflection are light on a dark card; on a light one
            // they read as a muddy smudge round the arcs, so a light theme draws the rings plain.
            val glowing = MaterialTheme.colorScheme.surfaceContainer.luminance() < 0.5f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val middle = Offset(size.width / 2f, (size.height - HeroReflectionRoom.toPx()) / 2f)
                val floor = size.height - HeroReflectionRoom.toPx() / 2f
                val ring = HeroRing(
                    center = middle,
                    radius = ringRadius.toPx(),
                    stroke = HeroRingStroke.toPx(),
                    startAngle = -90f,
                    sweep = 360f,
                    fraction = centerFraction,
                    color = centerColors.ring,
                )
                val sides = listOfNotNull(
                    // Fills from the bottom of the arc upwards.
                    if (left != null && leftColors != null) HeroRing(
                        middle, sideRadius.toPx(), HeroSideStroke.toPx(),
                        180f - HeroSideSweep / 2f, HeroSideSweep, leftFraction, leftColors.ring,
                    ) else null,
                    if (right != null && rightColors != null) HeroRing(
                        middle, sideRadius.toPx(), HeroSideStroke.toPx(),
                        HeroSideSweep / 2f, -HeroSideSweep, rightFraction, rightColors.ring,
                    ) else null,
                )
                // Each ring's light falls on the floor under it, soft and faint.
                if (glowing) {
                    drawReflection(glowPaint, ring, floor, width = ring.radius * 1.3f)
                    sides.forEach { drawReflection(glowPaint, it, floor, width = it.stroke * 3f) }
                }
                (listOf(ring) + sides).forEach { drawRing(it, glowPaint, glowing) }
            }

            // The numbers sit level with the rings, above the reflection.
            Row(modifier = Modifier.fillMaxSize().padding(bottom = HeroReflectionRoom)) {
                HeroLabel(
                    ring = left,
                    color = leftColors?.ring,
                    large = false,
                    alignment = Alignment.End,
                    modifier = Modifier
                        .width(sideWidth)
                        .fillMaxHeight(),
                )
                HeroLabel(
                    ring = center,
                    color = centerColors.ring,
                    large = true,
                    alignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = sideRadius - ringRadius + HeroRingStroke + 6.dp),
                )
                HeroLabel(
                    ring = right,
                    color = rightColors?.ring,
                    large = false,
                    alignment = Alignment.Start,
                    modifier = Modifier
                        .width(sideWidth)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun HeroLabel(
    ring: DashboardHeroRing?,
    color: Color?,
    large: Boolean,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    if (ring == null || color == null) {
        Box(modifier = modifier)
        return
    }
    val onClick = ring.onClick
    Column(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = alignment,
    ) {
        AutoResizeText(
            text = ring.value,
            style = if (large) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            // A number with its unit shrinks to fit beside the arc instead of being cut.
            minFontSize = HeroValueFloor,
        )
        AutoResizeText(
            text = ring.title,
            style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 2,
        )
    }
}

private class HeroRing(
    val center: Offset,
    val radius: Float,
    val stroke: Float,
    val startAngle: Float,
    val sweep: Float,
    val fraction: Float,
    val color: Color,
)

private fun DrawScope.drawRing(ring: HeroRing, glowPaint: Paint, glowing: Boolean = true) {
    val topLeft = Offset(ring.center.x - ring.radius, ring.center.y - ring.radius)
    val arcSize = Size(ring.radius * 2f, ring.radius * 2f)
    val arcStyle = Stroke(width = ring.stroke, cap = StrokeCap.Round)
    drawArc(ring.color.copy(alpha = 0.22f), ring.startAngle, ring.sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = arcStyle)
    val filled = ring.sweep * ring.fraction.coerceIn(0f, 1f)
    if (filled == 0f) return
    // The lit part glows, a blurred copy of itself under it.
    if (glowing) drawIntoCanvas { canvas ->
        glowPaint.asFrameworkPaint().apply {
            reset()
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = ring.stroke
            strokeCap = android.graphics.Paint.Cap.ROUND
            color = ring.color.copy(alpha = HeroGlowAlpha).toArgb()
            maskFilter = android.graphics.BlurMaskFilter(HeroGlowBlur.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawArc(
            topLeft.x, topLeft.y, topLeft.x + arcSize.width, topLeft.y + arcSize.height,
            ring.startAngle, filled, false, glowPaint,
        )
    }
    drawArc(ring.color, ring.startAngle, filled, useCenter = false, topLeft = topLeft, size = arcSize, style = arcStyle)
}

/** A soft oval of the ring's colour on the floor under the lowest point of its arc. */
private fun DrawScope.drawReflection(paint: Paint, ring: HeroRing, floor: Float, width: Float) {
    val end = ring.startAngle + ring.sweep
    // The lowest point the ring reaches: its bottom, or the arc's lower end.
    val lowest = if (ring.sweep >= 360f) 90.0 else {
        listOf(ring.startAngle, end).maxBy { kotlin.math.sin(Math.toRadians(it.toDouble())) }.toDouble()
    }
    val x = ring.center.x + ring.radius * kotlin.math.cos(Math.toRadians(lowest)).toFloat()
    val height = HeroReflectionRoom.toPx() * 0.6f
    drawIntoCanvas { canvas ->
        paint.asFrameworkPaint().apply {
            reset()
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            color = ring.color.copy(alpha = HeroReflectionAlpha * (0.4f + 0.6f * ring.fraction.coerceIn(0f, 1f))).toArgb()
            maskFilter = android.graphics.BlurMaskFilter(height, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawOval(x - width / 2f, floor - height / 2f, x + width / 2f, floor + height / 2f, paint)
    }
}
