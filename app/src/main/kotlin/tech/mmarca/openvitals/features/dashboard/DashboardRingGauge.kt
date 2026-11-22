package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** sin 45°: the ring's two ends sit this far below its centre, as a share of the radius. */
private const val RingOpening = 0.70710677f

/** Where a 270° ring is drawn in its tile, and how big. */
internal data class DashboardRingGeometry(val centerX: Float, val centerY: Float, val radius: Float)

/**
 * The ring as large as the tile allows and centred on what is drawn, not on the full
 * circle. A 270° ring is open at the bottom, so centring the circle left the drawing high
 * in its tile with an empty band below; here the band from the ring's top to the bottom
 * of its two round ends sits in the middle.
 */
internal fun dashboardRingGeometry(width: Float, height: Float, stroke: Float): DashboardRingGeometry {
    val radius = min((width - stroke) / 2f, (height - stroke) / (1f + RingOpening)).coerceAtLeast(0f)
    val drawnHeight = radius * (1f + RingOpening) + stroke
    val top = (height - drawnHeight) / 2f
    return DashboardRingGeometry(centerX = width / 2f, centerY = top + stroke / 2f + radius, radius = radius)
}

/**
 * Half the width a line may take when it spans [top] to [bottom] (measured downwards from
 * the centre) inside a ring whose hole has radius [hole]. Above the ends the hole's chord
 * limits it; below them the ring is open and the line may run as wide as the gap between
 * the ends.
 */
internal fun insideRingHalfWidth(hole: Float, top: Float, bottom: Float): Float {
    val reach = max(max(-top, min(bottom, hole * RingOpening)), 0f)
    return if (reach >= hole) 0f else sqrt(hole * hole - reach * reach)
}

/** The tops of lines of [heights] stacked as one block centred on the ring's centre. */
internal fun ringLineTops(heights: List<Float>): List<Float> {
    var top = -heights.sum() / 2f
    return heights.map { height -> top.also { top += height } }
}

/** Room left between the text and the inner edge of the ring's track. */
private val RingTextMargin: Dp = 6.dp

private const val SizeStepSp = 0.5f

private class RingLine(val text: String, val style: TextStyle, val maxSize: TextUnit, val minSize: TextUnit, val shared: Boolean)

/**
 * A four-space tile's ring: the title, the reading and the [details] inside it. 154, 155,
 * Each line is as wide as the ring is at its height, and the details share one size,
 * the largest at which all of them fit, so no line is cut short or smaller than the rest.
 */
@Composable
internal fun DashboardRingGauge(
    ring: DashboardHeroRing,
    details: List<String>,
    trackColor: Color,
    ringColor: Color,
    labelColor: Color,
    stroke: Dp,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography
    val valueColor = MaterialTheme.colorScheme.onSurface
    val lines = buildList {
        add(RingLine(ring.title, typography.labelLarge.copy(color = labelColor), typography.labelLarge.fontSize, 10.sp, shared = false))
        add(
            RingLine(
                ring.value,
                typography.headlineMedium.copy(color = valueColor, fontWeight = FontWeight.Bold),
                typography.headlineMedium.fontSize,
                14.sp,
                shared = false,
            ),
        )
        details.forEach { add(RingLine(it, typography.bodyMedium.copy(color = labelColor), typography.bodyMedium.fontSize, 10.sp, shared = true)) }
    }
    val filled = 270f * ring.fraction.coerceIn(0f, 1f)
    SubcomposeLayout(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val strokePx = stroke.toPx()
                val geometry = dashboardRingGeometry(size.width, size.height, strokePx)
                if (geometry.radius <= 0f) return@drawBehind
                val arcStyle = Stroke(width = strokePx, cap = StrokeCap.Round)
                val topLeft = Offset(geometry.centerX - geometry.radius, geometry.centerY - geometry.radius)
                val arcSize = Size(geometry.radius * 2f, geometry.radius * 2f)
                drawArc(trackColor, 135f, 270f, useCenter = false, topLeft = topLeft, size = arcSize, style = arcStyle)
                if (filled > 0f) {
                    drawArc(ringColor, 135f, filled, useCenter = false, topLeft = topLeft, size = arcSize, style = arcStyle)
                }
            },
    ) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val strokePx = stroke.toPx()
        val geometry = dashboardRingGeometry(width.toFloat(), height.toFloat(), strokePx)
        val hole = geometry.radius - strokePx / 2f - RingTextMargin.toPx()
        if (hole <= 0f) return@SubcomposeLayout layout(width, height) {}
        val laid = layoutRingLines(measurer, lines, hole)
        val placeables = subcompose(Unit) {
            lines.forEachIndexed { index, line ->
                Text(
                    text = line.text,
                    style = line.style.copy(fontSize = laid[index].size),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }.mapIndexed { index, measurable ->
            measurable.measure(Constraints(maxWidth = laid.getOrNull(index)?.width ?: width))
        }
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val top = laid.getOrNull(index)?.top ?: 0f
                placeable.place(
                    x = (geometry.centerX - placeable.width / 2f).roundToInt(),
                    y = (geometry.centerY + top).roundToInt(),
                )
            }
        }
    }
}

/** One line's size, its top relative to the ring's centre, and the width it may take. */
private class RingLineLayout(val size: TextUnit, val top: Float, val width: Int)

private fun layoutRingLines(measurer: TextMeasurer, lines: List<RingLine>, hole: Float): List<RingLineLayout> {
    var sizes = lines.map { it.maxSize }
    // Two passes: smaller text makes a shorter block, whose lines then sit where the ring is wider.
    repeat(2) {
        val heights = lines.mapIndexed { index, line -> measureLine(measurer, line, sizes[index]).size.height.toFloat() }
        val widths = ringLineTops(heights).mapIndexed { index, top ->
            2f * insideRingHalfWidth(hole, top, top + heights[index])
        }
        val fitted = lines.mapIndexed { index, line -> fitSize(measurer, line, widths[index]) }
        val sharedSize = lines.indices.filter { lines[it].shared }.minOfOrNull { fitted[it].value }
        sizes = lines.mapIndexed { index, line -> if (line.shared && sharedSize != null) sharedSize.sp else fitted[index] }
    }
    val heights = lines.mapIndexed { index, line -> measureLine(measurer, line, sizes[index]).size.height.toFloat() }
    return ringLineTops(heights).mapIndexed { index, top ->
        val width = 2f * insideRingHalfWidth(hole, top, top + heights[index])
        RingLineLayout(sizes[index], top, width.roundToInt().coerceAtLeast(0))
    }
}

private fun measureLine(measurer: TextMeasurer, line: RingLine, size: TextUnit): TextLayoutResult =
    measurer.measure(
        text = line.text,
        style = line.style.copy(fontSize = size),
        softWrap = false,
        maxLines = 1,
    )

/** The largest size, in half-sp steps, at which [line] fits [width]; its smallest if none does. */
private fun fitSize(measurer: TextMeasurer, line: RingLine, width: Float): TextUnit {
    val steps = ((line.maxSize.value - line.minSize.value) / SizeStepSp).toInt().coerceAtLeast(0)
    fun sizeAt(step: Int) = (line.minSize.value + step * SizeStepSp).sp
    fun fits(step: Int) = measureLine(measurer, line, sizeAt(step)).size.width <= width
    if (fits(steps)) return line.maxSize
    var low = 0
    var high = steps - 1
    var best = 0
    while (low <= high) {
        val mid = (low + high) / 2
        if (fits(mid)) { best = mid; low = mid + 1 } else high = mid - 1
    }
    return sizeAt(best)
}
