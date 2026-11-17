package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.WorkoutTypeChart
import tech.mmarca.openvitals.domain.insights.averagePaceSecondsPerKm
import tech.mmarca.openvitals.domain.insights.bestPaceSeries
import tech.mmarca.openvitals.domain.insights.heartRateZoneBounds
import tech.mmarca.openvitals.domain.insights.heartRateZoneSeconds
import tech.mmarca.openvitals.domain.insights.paceFromRoute
import tech.mmarca.openvitals.domain.insights.paceFromSpeed
import tech.mmarca.openvitals.domain.insights.paceFromLaps
import tech.mmarca.openvitals.domain.insights.SlowestSwimPaceSecondsPerKm
import tech.mmarca.openvitals.domain.insights.workoutTypeChart
import tech.mmarca.openvitals.domain.insights.RecoveryMark
import tech.mmarca.openvitals.domain.insights.recoveryChartWindow
import tech.mmarca.openvitals.domain.insights.recoveryCurve
import tech.mmarca.openvitals.domain.insights.recoveryTrace
import tech.mmarca.openvitals.domain.insights.strengthSetSpans
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.ChartZoom
import tech.mmarca.openvitals.ui.components.MetricDetailSectionBuilder
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.SessionAxis
import tech.mmarca.openvitals.ui.components.SessionPause
import tech.mmarca.openvitals.ui.components.chartSemantics
import tech.mmarca.openvitals.ui.components.formatElapsedChartLabel
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.Spacing

/** One line of an overlay chart: points across `0..1`, in its own units and scale. */
internal data class OverlaySeries(
    val points: List<Pair<Float, Double>>,
    val color: Color,
    val label: String,
    val format: (Double) -> String,
    /** Smaller is better and drawn higher, as a pace is. */
    val inverted: Boolean = false,
    /** Drawn dashed: an estimate, not a reading (103). */
    val dashed: Boolean = false,
    /** A shorter form for the axis, where the unit would not fit (88). */
    val axisFormat: ((Double) -> String)? = null,
)

private val OverlayChartHeight = Spacing.giant * 4
private const val MaxDrawnPoints = 300

/**
 * 88–93: a chart of one or two series over the same stretch of time, each on its own scale
 * (the first labelled on the left, the second on the right), with optional shaded spans.
 * Lines are smooth, as the heart rate chart's are. The frame gives it the full-screen button.
 */
@Composable
internal fun ActivityOverlayChart(
    title: String,
    primary: OverlaySeries,
    secondary: OverlaySeries?,
    xLabel: (Float) -> String,
    keys: Any,
    modifier: Modifier = Modifier,
    spans: List<ClosedFloatingPointRange<Float>> = emptyList(),
    spanColor: Color = MaterialTheme.colorScheme.primary,
    spanLabel: String? = null,
    /** The second series continues the first, on the first's scale (103). */
    sharedScale: Boolean = false,
) {
    // 137: a line that starts after the session's first second is carried back to it.
    @Suppress("NAME_SHADOWING")
    val primary = remember(primary) { primary.copy(points = anchoredAtStart(primary.points)) }
    val first = remember(primary.points, secondary?.points, sharedScale) {
        overlayScale(if (sharedScale && secondary != null) primary.points + secondary.points else primary.points)
    }
    val second = remember(secondary?.points, sharedScale, first) {
        secondary?.let { if (sharedScale) first else overlayScale(it.points) }
    }
    val rightAxis = secondary != null && !sharedScale
    val described = "$title: ${primary.label} ${primary.format(first.low)}–${primary.format(first.high)}" +
        (secondary?.let { s -> second?.let { "; ${s.label} ${s.format(it.low)}–${s.format(it.high)}" } } ?: "")
    ChartZoom(keys, modifier = modifier) { zoom ->
        val viewport = zoom.viewport
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            // 140: hold and slide across the chart to read the reading under the finger.
            val targets = remember(primary, secondary, first, viewport) {
                overlayScrubTargets(primary, secondary, first, viewport, xLabel)
            }
            Row(Modifier.fillMaxWidth().height(OverlayChartHeight)) {
                AxisLabels(first, primary, TextAlign.End)
                tech.mmarca.openvitals.ui.components.ChartScrubber(
                    targets = targets,
                    accentColor = primary.color,
                    multiTouch = zoom.multiTouch,
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = Spacing.xs),
                ) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .chartSemantics(described),
                ) {
                    // 101: in full screen the viewport is what the fingers zoomed to.
                    clipRect {
                        spans.forEach { span ->
                            val from = viewport.visibleFraction(span.start)
                            val to = viewport.visibleFraction(span.endInclusive)
                            drawRect(
                                color = spanColor.copy(alpha = Emphasis.wash),
                                topLeft = Offset(from * size.width, 0f),
                                size = androidx.compose.ui.geometry.Size((to - from) * size.width, size.height),
                            )
                        }
                        if (secondary != null && second != null) drawSmoothLine(secondary, second, filled = false, viewport)
                        drawSmoothLine(primary, first, filled = true, viewport)
                    }
                }
                }
                if (rightAxis && second != null) AxisLabels(second, secondary!!, TextAlign.Start) else Box(Modifier.width(Spacing.xs))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0f, 0.5f, 1f).map { xLabel(viewport.dataFraction(it)) }.forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(primary.color, primary.label)
                secondary?.let { LegendDot(it.color, it.label) }
                if (spans.isNotEmpty() && spanLabel != null) LegendDot(spanColor.copy(alpha = Emphasis.subtle), spanLabel)
            }
        }
    }
}

private data class OverlayScale(val low: Double, val high: Double)

/** At most this many points a hold can land on: the nearest reading is still found. */
private const val MaxScrubTargets = 600

/**
 * 140: the readings of [primary] on show, each with the time under it and, when there is one,
 * the second series' reading nearest in time.
 */
private fun overlayScrubTargets(
    primary: OverlaySeries,
    secondary: OverlaySeries?,
    scale: OverlayScale,
    viewport: tech.mmarca.openvitals.ui.components.ChartViewport,
    xLabel: (Float) -> String,
): List<tech.mmarca.openvitals.ui.components.ScrubTarget> {
    val shown = primary.points.filter { (x, _) -> viewport.visibleFraction(x) in 0f..1f }
    val step = (shown.size / MaxScrubTargets).coerceAtLeast(1)
    val span = (scale.high - scale.low).takeIf { it > 0.0 } ?: 1.0
    return shown.filterIndexed { index, _ -> index % step == 0 }.map { (x, value) ->
        val share = ((value - scale.low) / span).toFloat().coerceIn(0f, 1f)
        tech.mmarca.openvitals.ui.components.ScrubTarget(
            xFraction = viewport.visibleFraction(x),
            yFraction = if (primary.inverted) 1f - share else share,
            label = {
                val other = secondary?.points?.minByOrNull { kotlin.math.abs(it.first - x) }
                    ?.let { (_, otherValue) -> " · ${secondary.format(otherValue)}" }.orEmpty()
                primary.format(value) + other to xLabel(x)
            },
        )
    }
}

/**
 * 137: a series whose first reading comes after the start is drawn from the start: its first
 * value there is estimated as the median of its first few readings, so one odd reading at the
 * start does not set it.
 */
internal fun anchoredAtStart(points: List<Pair<Float, Double>>, epsilon: Float = 0.002f): List<Pair<Float, Double>> {
    val firstX = points.firstOrNull()?.first ?: return points
    if (firstX <= epsilon) return points
    val head = points.take(StartEstimateReadings).map { it.second }.sorted()
    return listOf(0f to head[head.size / 2]) + points
}

/** 137: how many of the first readings the estimated start is the median of. */
internal const val StartEstimateReadings = 5

private fun overlayScale(points: List<Pair<Float, Double>>): OverlayScale {
    val low = points.minOf { it.second }
    val high = points.maxOf { it.second }
    val pad = ((high - low).takeIf { it > 1e-6 } ?: maxOf(kotlin.math.abs(high) * 0.2, 1.0)) * 0.1
    return OverlayScale(low - pad, high + pad)
}

@Composable
private fun AxisLabels(scale: OverlayScale, series: OverlaySeries, align: TextAlign) {
    val top = if (series.inverted) scale.low else scale.high
    val bottom = if (series.inverted) scale.high else scale.low
    Column(
        modifier = Modifier.fillMaxHeight().width(Spacing.giant + Spacing.sm),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(top, (top + bottom) / 2, bottom).forEach { value ->
            Text(
                text = (series.axisFormat ?: series.format)(value),
                style = MaterialTheme.typography.labelSmall,
                // 138: the axes are read in the theme's neutral ink, never the series' colour.
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = align,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Box(Modifier.size(Spacing.sm).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun DrawScope.drawSmoothLine(
    series: OverlaySeries,
    scale: OverlayScale,
    filled: Boolean,
    viewport: tech.mmarca.openvitals.ui.components.ChartViewport,
) {
    val step = (series.points.size / MaxDrawnPoints).coerceAtLeast(1)
    val drawn = series.points.filterIndexed { index, _ -> index % step == 0 || index == series.points.lastIndex }
    if (drawn.isEmpty()) return
    val span = scale.high - scale.low
    val offsets = drawn.map { (x, value) ->
        val share = ((value - scale.low) / span).toFloat().coerceIn(0f, 1f)
        val y = if (series.inverted) share * size.height else (1f - share) * size.height
        Offset(viewport.visibleFraction(x.coerceIn(0f, 1f)) * size.width, y)
    }
    val line = Path().apply {
        moveTo(offsets.first().x, offsets.first().y)
        for (i in 1 until offsets.size) {
            val previous = offsets[i - 1]
            val mid = Offset((previous.x + offsets[i].x) / 2, (previous.y + offsets[i].y) / 2)
            quadraticTo(previous.x, previous.y, mid.x, mid.y)
        }
        lineTo(offsets.last().x, offsets.last().y)
    }
    if (filled) {
        val area = Path().apply {
            addPath(line)
            lineTo(offsets.last().x, size.height)
            lineTo(offsets.first().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(series.color.copy(alpha = Emphasis.subtle), Color.Transparent)))
    }
    drawPath(
        line,
        series.color,
        style = Stroke(
            width = Spacing.xs.toPx() / 2 * if (filled) 1.25f else 1f,
            cap = StrokeCap.Round,
            pathEffect = if (series.dashed) {
                androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(Spacing.sm.toPx(), Spacing.xs.toPx()))
            } else {
                null
            },
        ),
    )
}

/** The frame every card here shares: title, a row of figures, the chart, and a note. */
@Composable
private fun ActivityTypeChartFrame(
    title: String,
    figures: List<Pair<String, String>>,
    accent: Color,
    modifier: Modifier,
    note: String? = null,
    chart: @Composable () -> Unit,
) {
    OpenVitalsCard(modifier = modifier) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            // 114: left, middle and right, as on every activity chart.
            ActivityChartFigures(figures, accent)
            chart()
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun List<Pair<Instant, Double>>.across(axis: SessionAxis): List<Pair<Float, Double>> =
    map { (time, value) -> axis.fractionOf(time) to value }

/** 88: the pace over time, faster drawn higher. */
@Composable
internal fun ActivityPaceChartCard(
    paceSecondsPerKm: List<Pair<Instant, Double>>,
    sessionStart: Instant,
    sessionEnd: Instant,
    pauses: List<SessionPause>,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    /** 118: a swim's pace, per 100 m rather than per kilometre. */
    per100m: Boolean = false,
) {
    if (paceSecondsPerKm.size < 2) return
    val axis = remember(sessionStart, sessionEnd, pauses) { SessionAxis(sessionStart, sessionEnd, pauses) }
    // 88: the axis has room for the minutes and seconds only; the figures above carry the unit.
    val axisPace: (Double) -> String = { seconds ->
        if (per100m) minutesSeconds(seconds / 10.0) else unitFormatter.averagePace(1_000.0, (seconds * 1_000).toLong())?.value ?: "–"
    }
    val formatPace: (Double) -> String = { seconds ->
        if (per100m) "${minutesSeconds(seconds / 10.0)} /100 m"
        else unitFormatter.averagePace(1_000.0, (seconds * 1_000).toLong())?.text ?: "–"
    }
    val points = remember(paceSecondsPerKm, axis) { paceSecondsPerKm.across(axis) }
    val title = stringResource(R.string.activity_chart_pace_title)
    val values = remember(paceSecondsPerKm) { paceSecondsPerKm.map { it.second } }
    val average = remember(paceSecondsPerKm) { averagePaceSecondsPerKm(paceSecondsPerKm) ?: values.average() }
    ActivityTypeChartFrame(
        title = title,
        figures = listOf(
            stringResource(R.string.summary_average) to formatPace(average),
            stringResource(R.string.activity_chart_fastest) to formatPace(values.min()),
            stringResource(R.string.activity_chart_slowest) to formatPace(values.max()),
        ),
        accent = DistanceColor,
        modifier = modifier,
    ) {
        ActivityOverlayChart(
            title = title,
            primary = OverlaySeries(points, DistanceColor, title, formatPace, inverted = true, axisFormat = axisPace),
            secondary = null,
            xLabel = { f -> formatElapsedChartLabel(axis.elapsedAt(f)) },
            keys = paceSecondsPerKm,
        )
    }
}

private fun minutesSeconds(seconds: Double): String {
    val whole = seconds.roundToInt().coerceAtLeast(0)
    return String.format(java.util.Locale.US, "%d:%02d", whole / 60, whole % 60)
}

private val ZoneColors = listOf(
    Color(0xFF64B5F6), Color(0xFF4DB6AC), Color(0xFFFFD54F), Color(0xFFFF8A65), Color(0xFFE57373),
)

/** 89: the time spent in each heart rate zone, as bars. */
@Composable
internal fun ActivityHeartRateZonesCard(
    samples: List<HeartRateSample>,
    sessionStart: Instant,
    sessionEnd: Instant,
    maxHeartRateBpm: Int,
    modifier: Modifier = Modifier,
) {
    val zones = remember(samples, sessionStart, sessionEnd, maxHeartRateBpm) {
        heartRateZoneSeconds(samples, sessionStart, sessionEnd, maxHeartRateBpm)
    }
    val total = zones.sum()
    if (total <= 0L) return
    val title = stringResource(R.string.activity_chart_zones_title)
    ActivityTypeChartFrame(
        title = title,
        figures = emptyList(),
        accent = HeartColor,
        modifier = modifier,
        note = stringResource(R.string.activity_chart_zones_note, maxHeartRateBpm),
    ) {
        val names = listOf(
            stringResource(R.string.activity_chart_zone_name_1),
            stringResource(R.string.activity_chart_zone_name_2),
            stringResource(R.string.activity_chart_zone_name_3),
            stringResource(R.string.activity_chart_zone_name_4),
            stringResource(R.string.activity_chart_zone_name_5),
        )
        val bounds = remember(maxHeartRateBpm) { heartRateZoneBounds(maxHeartRateBpm) }
        val spoken = zones.mapIndexed { index, seconds -> "${names[index]} ${formatElapsedChartLabel(Duration.ofSeconds(seconds))}" }
            .joinToString(", ")
        // 117: as the watches show them: the hardest zone on top, each with its name, its bpm
        // and the share and time spent in it, over a bar as long as that share.
        Column(
            modifier = Modifier.chartSemantics("$title: $spoken"),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            zones.indices.reversed().forEach { index ->
                val seconds = zones[index]
                val (low, high) = bounds[index]
                val range = when {
                    low == null -> "< ${(high ?: 0) + 1} bpm"
                    high == null -> "≥ $low bpm"
                    else -> activityValueRange("$low bpm", "$high bpm")
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(names[index], style = MaterialTheme.typography.labelLarge)
                            Text(range, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "${seconds * 100 / total}%  ${formatElapsedChartLabel(Duration.ofSeconds(seconds))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(Spacing.sm)
                            .background(ZoneColors[index].copy(alpha = Emphasis.wash), RoundedCornerShape(Spacing.xs)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((seconds.toFloat() / total).coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(ZoneColors[index], RoundedCornerShape(Spacing.xs)),
                        )
                    }
                }
            }
        }
    }
}

/** 90, 91: two measures of the same session on one chart, each on its own scale. */
@Composable
internal fun ActivityPairedChartCard(
    title: String,
    primary: Pair<String, List<Pair<Instant, Double>>>,
    primaryColor: Color,
    primaryFormat: (Double) -> String,
    secondary: Pair<String, List<Pair<Instant, Double>>>,
    secondaryColor: Color,
    secondaryFormat: (Double) -> String,
    sessionStart: Instant,
    sessionEnd: Instant,
    pauses: List<SessionPause>,
    modifier: Modifier = Modifier,
) {
    if (primary.second.size < 2 || secondary.second.size < 2) return
    val axis = remember(sessionStart, sessionEnd, pauses) { SessionAxis(sessionStart, sessionEnd, pauses) }
    val first = remember(primary.second, axis) { primary.second.sortedBy { it.first }.across(axis) }
    val second = remember(secondary.second, axis) { secondary.second.sortedBy { it.first }.across(axis) }
    ActivityTypeChartFrame(
        title = title,
        figures = listOf(
            primary.first to primaryFormat(primary.second.map { it.second }.average()),
            secondary.first to secondaryFormat(secondary.second.map { it.second }.average()),
        ),
        accent = primaryColor,
        modifier = modifier,
        note = stringResource(R.string.activity_chart_averages_note),
    ) {
        ActivityOverlayChart(
            title = title,
            primary = OverlaySeries(first, primaryColor, primary.first, primaryFormat),
            secondary = OverlaySeries(second, secondaryColor, secondary.first, secondaryFormat),
            xLabel = { f -> formatElapsedChartLabel(axis.elapsedAt(f)) },
            keys = primary.second to secondary.second,
        )
    }
}

/** 92: the heart rate of a strength session with each set shaded, to see the recovery between sets. */
@Composable
internal fun ActivityStrengthSetsChartCard(
    samples: List<HeartRateSample>,
    segments: List<ExerciseSegmentData>,
    sessionStart: Instant,
    sessionEnd: Instant,
    pauses: List<SessionPause>,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    if (samples.size < 2) return
    val axis = remember(sessionStart, sessionEnd, pauses) { SessionAxis(sessionStart, sessionEnd, pauses) }
    val points = remember(samples, axis) { samples.sortedBy { it.time }.map { axis.fractionOf(it.time) to it.beatsPerMinute.toDouble() } }
    val sets = remember(segments, axis) { strengthSetSpans(segments, sessionStart, sessionEnd).map { (from, to) -> axis.fractionOf(from)..axis.fractionOf(to) } }
    val title = stringResource(R.string.activity_chart_sets_title)
    val bpm: (Double) -> String = { "${it.toInt()} bpm" }
    ActivityTypeChartFrame(
        title = title,
        figures = listOf(
            // 105: a strength session is at least one set, recorded or not.
            stringResource(R.string.activity_chart_sets_count) to unitFormatter.count(maxOf(1, sets.size)),
            stringResource(R.string.summary_average) to bpm(points.map { it.second }.average()),
        ),
        accent = HeartColor,
        modifier = modifier,
        note = if (sets.isEmpty()) stringResource(R.string.activity_chart_sets_none) else null,
    ) {
        ActivityOverlayChart(
            title = title,
            primary = OverlaySeries(points, HeartColor, stringResource(R.string.activity_recording_live_heart_rate), bpm),
            secondary = null,
            xLabel = { f -> formatElapsedChartLabel(axis.elapsedAt(f)) },
            keys = samples to segments,
            spans = sets,
            spanColor = MaterialTheme.colorScheme.primary,
            spanLabel = stringResource(R.string.activity_chart_sets_legend),
        )
    }
}

/**
 * 93: the heart rate in the two minutes after the end, and how far it fell. 103: past the last
 * reading the fall is estimated toward the resting rate, drawn dashed and marked "≈".
 */
@Composable
internal fun ActivityRecoveryChartCard(
    samples: List<HeartRateSample>,
    sessionSamples: List<HeartRateSample>,
    sessionEnd: Instant,
    restingHeartRateBpm: Int?,
    modifier: Modifier = Modifier,
) {
    val curve = remember(samples, sessionSamples, sessionEnd, restingHeartRateBpm) {
        val lastInSession = sessionSamples.filter { !it.time.isAfter(sessionEnd) }.maxByOrNull { it.time }?.beatsPerMinute
        recoveryCurve(recoveryTrace(samples, sessionEnd), lastInSession, restingHeartRateBpm)
    } ?: return
    val window = recoveryChartWindow.seconds.toFloat()
    val measured = remember(curve) { curve.measured.map { (seconds, bpm) -> (seconds / window) to bpm } }
    val estimated = remember(curve) { curve.estimated.map { (seconds, bpm) -> (seconds / window) to bpm } }
    val bpm: (Double) -> String = { "${it.roundToInt()} bpm" }
    fun drop(mark: RecoveryMark?): String = mark?.let {
        (if (it.estimated) "≈ " else "") + "−${(curve.endBpm - it.bpm).roundToInt().coerceAtLeast(0)} bpm"
    } ?: "–"
    val title = stringResource(R.string.activity_chart_recovery_title)
    val label = stringResource(R.string.activity_recording_live_heart_rate)
    val estimateLabel = stringResource(R.string.activity_chart_recovery_estimate)
    val lastReading = curve.measured.lastOrNull()?.first ?: 0L
    ActivityTypeChartFrame(
        title = title,
        figures = listOf(
            stringResource(R.string.activity_chart_recovery_end) to bpm(curve.endBpm),
            stringResource(R.string.activity_chart_recovery_one) to drop(curve.oneMinute),
            stringResource(R.string.activity_chart_recovery_two) to drop(curve.twoMinutes),
        ),
        accent = HeartColor,
        modifier = modifier,
        note = if (estimated.isNotEmpty()) {
            stringResource(
                R.string.activity_chart_recovery_estimate_note,
                formatElapsedChartLabel(Duration.ofSeconds(lastReading)),
                curve.restingBpm,
            )
        } else {
            null
        },
    ) {
        val estimateSeries = OverlaySeries(estimated, HeartColor, estimateLabel, bpm, dashed = true)
        ActivityOverlayChart(
            title = title,
            primary = if (measured.size >= 2 || estimated.isEmpty()) OverlaySeries(measured, HeartColor, label, bpm) else estimateSeries,
            secondary = if (measured.size >= 2 && estimated.isNotEmpty()) estimateSeries else null,
            xLabel = { f -> formatElapsedChartLabel(Duration.ofSeconds((f * recoveryChartWindow.seconds).toLong())) },
            keys = curve,
            sharedScale = true,
        )
    }
}

/**
 * 88–93: the kind-specific chart of [workout], the zones and the recovery chart, as sections the
 * detail screen arranges (113).
 */
internal fun MetricDetailSectionBuilder.activityTypeChartSections(
    workout: ExerciseData,
    heartRateSamples: List<HeartRateSample>,
    speedSamples: List<SpeedSample>,
    splitSpeedTrace: ActivitySplitSpeedTrace?,
    cadenceSamples: List<ActivityCadenceSample>,
    elevationSamples: List<ActivityElevationSample>,
    recoverySamples: List<HeartRateSample>,
    maxHeartRateBpm: Int?,
    unitFormatter: UnitFormatter,
    restingHeartRateBpm: Int? = null,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    val pauses = workout.segments.toSessionPauses()
    val heartRate = heartRateSamples.map { it.time to it.beatsPerMinute.toDouble() }
    val speed = speedSamples.map { it.time to it.metersPerSecond }.ifEmpty {
        splitSpeedTrace?.samples.orEmpty().map { it.time to it.metersPerSecond }
    }
    when (workoutTypeChart(workout.exerciseType)) {
        WorkoutTypeChart.PACE -> section(TypeChartSection) {
            // 88: whichever of the recorded speed, the route and the splits covers the session.
            val pace = remember(speedSamples, workout.route, splitSpeedTrace) {
                bestPaceSeries(
                    listOf(
                        paceFromSpeed(speedSamples),
                        paceFromRoute(workout.route.points),
                        splitSpeedTrace?.samples.orEmpty()
                            .filter { it.metersPerSecond > 0.0 }
                            .map { it.time to 1_000.0 / it.metersPerSecond },
                    ),
                    workout.startTime,
                    workout.endTime,
                )
            }
            ActivityPaceChartCard(pace, workout.startTime, workout.endTime, pauses, unitFormatter, cardModifier)
        }
        WorkoutTypeChart.SWIM_PACE -> section(TypeChartSection) {
            // 118: per 100 m, from the laps a pool watch writes, else the speed or the route.
            val pace = remember(workout.laps, speedSamples, workout.route) {
                bestPaceSeries(
                    listOf(
                        paceFromLaps(workout.laps),
                        paceFromSpeed(speedSamples, SlowestSwimPaceSecondsPerKm),
                        paceFromRoute(workout.route.points, SlowestSwimPaceSecondsPerKm),
                    ),
                    workout.startTime,
                    workout.endTime,
                )
            }
            ActivityPaceChartCard(pace, workout.startTime, workout.endTime, pauses, unitFormatter, cardModifier, per100m = true)
        }
        WorkoutTypeChart.ELEVATION_WITH_SPEED -> section(TypeChartSection) {
            ActivityPairedChartCard(
                title = stringResource(R.string.activity_chart_climbs_title),
                primary = stringResource(R.string.metric_elevation) to elevationSamples.map { it.time to it.meters },
                primaryColor = ElevationColor,
                primaryFormat = { unitFormatter.elevation(it).text },
                secondary = stringResource(R.string.activity_recording_live_speed) to speed,
                secondaryColor = DistanceColor,
                secondaryFormat = { unitFormatter.speed(it).text },
                sessionStart = workout.startTime,
                sessionEnd = workout.endTime,
                pauses = pauses,
                modifier = cardModifier,
            )
        }
        WorkoutTypeChart.CADENCE_WITH_HEART_RATE -> section(TypeChartSection) {
            ActivityPairedChartCard(
                title = stringResource(R.string.activity_chart_cadence_heart_title),
                primary = stringResource(R.string.activity_recording_live_heart_rate) to heartRate,
                primaryColor = HeartColor,
                primaryFormat = { "${it.toInt()} bpm" },
                secondary = stringResource(R.string.metric_cycling_cadence) to cadenceSamples
                    .filter { it.kind == ActivityCadenceKind.CYCLING && it.rate > 0.0 }
                    .map { it.time to it.rate },
                secondaryColor = CycleColor,
                secondaryFormat = { unitFormatter.cadence(it).text },
                sessionStart = workout.startTime,
                sessionEnd = workout.endTime,
                pauses = pauses,
                modifier = cardModifier,
            )
        }
        WorkoutTypeChart.HEART_RATE_WITH_SETS -> section(TypeChartSection) {
            ActivityStrengthSetsChartCard(
                heartRateSamples, workout.segments, workout.startTime, workout.endTime, pauses, unitFormatter, cardModifier,
            )
        }
        null -> Unit
    }
    // 110, 118: every kind with a heart rate gets the time in each zone: tennis, a walk, a swim.
    if (maxHeartRateBpm != null && heartRateSamples.isNotEmpty()) {
        section(MetricDetailSectionId.ACTIVITY_DETAIL_ZONES) {
            ActivityHeartRateZonesCard(heartRateSamples, workout.startTime, workout.endTime, maxHeartRateBpm, cardModifier)
        }
    }
    if (heartRateSamples.isNotEmpty()) section(MetricDetailSectionId.ACTIVITY_DETAIL_RECOVERY) {
        ActivityRecoveryChartCard(recoverySamples, heartRateSamples, workout.endTime, restingHeartRateBpm, cardModifier)
    }
}

private val TypeChartSection = MetricDetailSectionId.ACTIVITY_DETAIL_TYPE_CHART
