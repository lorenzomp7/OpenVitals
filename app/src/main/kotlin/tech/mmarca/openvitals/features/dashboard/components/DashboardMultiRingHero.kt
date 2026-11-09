package tech.mmarca.openvitals.features.dashboard.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.features.dashboard.DashboardHeroRing
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Radii

private val HeroPadding = 10.dp
private val HeroRingStroke = 14.dp
private val HeroSideStroke = 10.dp

/** How far the side arcs sit outside the centre ring, as a multiple of its radius. */
private const val HeroSideRadiusFactor = 1.34f

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
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(HeroRevealMillis)) }

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
            val byHeight = maxHeight / 2 - HeroRingStroke / 2
            val byWidth = if (hasSides) (maxWidth / 2 - 76.dp) / HeroSideRadiusFactor else maxWidth / 2
            val ringRadius: Dp = minOf(byHeight, byWidth)
            val sideRadius = ringRadius * HeroSideRadiusFactor
            val sideWidth = (maxWidth / 2 - sideRadius - HeroSideStroke).coerceAtLeast(0.dp)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val middle = Offset(size.width / 2f, size.height / 2f)
                drawRing(
                    center = middle,
                    radius = ringRadius.toPx(),
                    stroke = HeroRingStroke.toPx(),
                    startAngle = -90f,
                    sweep = 360f,
                    fraction = center.fraction * reveal.value,
                    track = centerColors.ring.copy(alpha = 0.22f),
                    fill = centerColors.ring,
                )
                if (left != null && leftColors != null) {
                    // Fills from the bottom of the arc upwards.
                    drawRing(
                        center = middle,
                        radius = sideRadius.toPx(),
                        stroke = HeroSideStroke.toPx(),
                        startAngle = 180f - HeroSideSweep / 2f,
                        sweep = HeroSideSweep,
                        fraction = left.fraction * reveal.value,
                        track = leftColors.ring.copy(alpha = 0.22f),
                        fill = leftColors.ring,
                    )
                }
                if (right != null && rightColors != null) {
                    drawRing(
                        center = middle,
                        radius = sideRadius.toPx(),
                        stroke = HeroSideStroke.toPx(),
                        startAngle = HeroSideSweep / 2f,
                        sweep = -HeroSideSweep,
                        fraction = right.fraction * reveal.value,
                        track = rightColors.ring.copy(alpha = 0.22f),
                        fill = rightColors.ring,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
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
        )
        AutoResizeText(
            text = ring.title.uppercase(),
            style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 2,
        )
    }
}

private fun DrawScope.drawRing(
    center: Offset,
    radius: Float,
    stroke: Float,
    startAngle: Float,
    sweep: Float,
    fraction: Float,
    track: Color,
    fill: Color,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    val style = Stroke(width = stroke, cap = StrokeCap.Round)
    drawArc(track, startAngle, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
    val filled = sweep * fraction.coerceIn(0f, 1f)
    if (filled != 0f) {
        drawArc(fill, startAngle, filled, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
    }
}
