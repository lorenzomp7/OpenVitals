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
import androidx.compose.ui.text.style.TextAlign
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

/**
 * Where a 270° ring is drawn in its tile, and how big. The title goes in the band under
 * the ring's open bottom, from [labelTop] down.
 */
internal data class DashboardRingGeometry(val centerX: Float, val centerY: Float, val radius: Float, val labelTop: Float)

/**
 * The ring as large as the tile allows, with a band of [labelHeight] under it for the
 * title, and the two centred as one drawing. A 270° ring is open at the bottom: centring
 * the full circle left it high in its tile with an empty band below, which now holds the
 * title at the tile's full width.
 */
internal fun dashboardRingGeometry(width: Float, height: Float, stroke: Float, labelHeight: Float = 0f): DashboardRingGeometry {
    val radius = min((width - stroke) / 2f, (height - stroke - labelHeight) / (1f + RingOpening)).coerceAtLeast(0f)
    val drawnHeight = radius * (1f + RingOpening) + stroke + labelHeight
    val top = (height - drawnHeight) / 2f
    val centerY = top + stroke / 2f + radius
    return DashboardRingGeometry(
        centerX = width / 2f,
        centerY = centerY,
        radius = radius,
        labelTop = centerY + radius * RingOpening + stroke / 2f,
    )
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
private val RingTextMargin: Dp = 4.dp

private const val SizeStepSp = 0.5f

/** Under this a detail is hard to read: it goes on two lines instead of shrinking further. */
private val DetailMinSize: TextUnit = 11.sp

private class RingLine(
    val text: String,
    val style: TextStyle,
    val maxSize: TextUnit,
    val minSize: TextUnit,
    val shared: Boolean,
    val maxLines: Int = 1,
)

/** One line's size, its top relative to the ring's centre, the width it may take and whether it wraps. */
private class RingLineLayout(val size: TextUnit, val top: Float, val width: Int, val wraps: Boolean)

/**
 * A four-space tile's ring: the reading and the [details] inside it, the title under its
 * open bottom across the tile's width. Each line inside is as wide as the ring is at its
 * height, and the details share one size, the largest at which all of them fit; a detail
 * that still does not fit at a readable size takes two lines rather than an ellipsis.
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
    val title = RingLine(ring.title, typography.titleSmall.copy(color = labelColor), typography.titleSmall.fontSize, 11.sp, shared = false)
    val inside = buildList {
        add(
            RingLine(
                ring.value,
                typography.headlineMedium.copy(color = valueColor, fontWeight = FontWeight.Bold),
                typography.headlineMedium.fontSize,
                14.sp,
                shared = false,
            ),
        )
        details.forEach {
            add(RingLine(it, typography.bodyMedium.copy(color = labelColor), typography.bodyMedium.fontSize, DetailMinSize, shared = true, maxLines = 2))
        }
    }
    val filled = 270f * ring.fraction.coerceIn(0f, 1f)
    SubcomposeLayout(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val strokePx = stroke.toPx()
                val labelHeight = measureLine(measurer, title, title.maxSize).size.height.toFloat()
                val geometry = dashboardRingGeometry(size.width, size.height, strokePx, labelHeight)
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
        val labelHeight = measureLine(measurer, title, title.maxSize).size.height.toFloat()
        val geometry = dashboardRingGeometry(width.toFloat(), height.toFloat(), strokePx, labelHeight)
        val hole = geometry.radius - strokePx / 2f - RingTextMargin.toPx()
        if (hole <= 0f) return@SubcomposeLayout layout(width, height) {}
        val laid = layoutRingLines(measurer, inside, hole)
        val titleSize = fitSize(measurer, title, width.toFloat())
        val titlePlaceable = subcompose("title") {
            RingText(title, titleSize, wraps = false)
        }.first().measure(Constraints(maxWidth = width))
        val placeables = subcompose("inside") {
            inside.forEachIndexed { index, line -> RingText(line, laid[index].size, laid[index].wraps) }
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
            titlePlaceable.place(
                x = (geometry.centerX - titlePlaceable.width / 2f).roundToInt(),
                y = geometry.labelTop.roundToInt(),
            )
        }
    }
}

@Composable
private fun RingText(line: RingLine, size: TextUnit, wraps: Boolean) {
    Text(
        text = line.text,
        style = line.style.copy(fontSize = size),
        textAlign = TextAlign.Center,
        maxLines = if (wraps) line.maxLines else 1,
        softWrap = wraps,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun layoutRingLines(measurer: TextMeasurer, lines: List<RingLine>, hole: Float): List<RingLineLayout> {
    var sizes = lines.map { it.maxSize }
    var wraps = lines.map { false }
    var widths = lines.map { 2f * hole }
    // Two passes: smaller text makes a shorter block, whose lines then sit where the ring is wider.
    repeat(2) {
        val heights = lines.mapIndexed { index, line -> lineHeight(measurer, line, sizes[index], wraps[index], widths[index]) }
        widths = ringLineTops(heights).mapIndexed { index, top -> 2f * insideRingHalfWidth(hole, top, top + heights[index]) }
        val fitted = lines.mapIndexed { index, line -> fitSize(measurer, line, widths[index]) }
        wraps = lines.mapIndexed { index, line ->
            line.maxLines > 1 && measureLine(measurer, line, fitted[index]).size.width > widths[index]
        }
        val sharedSize = lines.indices.filter { lines[it].shared }.minOfOrNull { fitted[it].value }
        sizes = lines.mapIndexed { index, line -> if (line.shared && sharedSize != null) sharedSize.sp else fitted[index] }
    }
    val heights = lines.mapIndexed { index, line -> lineHeight(measurer, line, sizes[index], wraps[index], widths[index]) }
    return ringLineTops(heights).mapIndexed { index, top ->
        val width = 2f * insideRingHalfWidth(hole, top, top + heights[index])
        RingLineLayout(sizes[index], top, width.roundToInt().coerceAtLeast(0), wraps[index])
    }
}

private fun lineHeight(measurer: TextMeasurer, line: RingLine, size: TextUnit, wraps: Boolean, width: Float): Float =
    if (wraps) {
        measurer.measure(
            text = line.text,
            style = line.style.copy(fontSize = size),
            maxLines = line.maxLines,
            constraints = Constraints(maxWidth = width.roundToInt().coerceAtLeast(0)),
        ).size.height.toFloat()
    } else {
        measureLine(measurer, line, size).size.height.toFloat()
    }

private fun measureLine(measurer: TextMeasurer, line: RingLine, size: TextUnit): TextLayoutResult =
    measurer.measure(
        text = line.text,
        style = line.style.copy(fontSize = size),
        softWrap = false,
        maxLines = 1,
    )

/** The largest size, in half-sp steps, at which [line] fits [width] on one line; its smallest if none does. */
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
