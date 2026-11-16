package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.WorkoutTypeChart
import tech.mmarca.openvitals.domain.insights.heartRateZoneSeconds
import tech.mmarca.openvitals.domain.insights.paceFromRoute
import tech.mmarca.openvitals.domain.insights.paceFromSpeed
import tech.mmarca.openvitals.domain.insights.workoutTypeChart
import tech.mmarca.openvitals.domain.insights.recoveryChartWindow
import tech.mmarca.openvitals.domain.insights.recoveryTrace
import tech.mmarca.openvitals.domain.insights.strengthSetSpans
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.ui.components.ChartZoom
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
    xLabels: List<String>,
    keys: Any,
    modifier: Modifier = Modifier,
    spans: List<ClosedFloatingPointRange<Float>> = emptyList(),
    spanColor: Color = MaterialTheme.colorScheme.primary,
    spanLabel: String? = null,
) {
    val first = remember(primary.points) { overlayScale(primary.points) }
    val second = remember(secondary?.points) { secondary?.let { overlayScale(it.points) } }
    val described = "$title: ${primary.label} ${primary.format(first.low)}–${primary.format(first.high)}" +
        (secondary?.let { s -> second?.let { "; ${s.label} ${s.format(it.low)}–${s.format(it.high)}" } } ?: "")
    ChartZoom(keys, modifier = modifier) { _ ->
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(Modifier.fillMaxWidth().height(OverlayChartHeight)) {
                AxisLabels(first, primary, TextAlign.End)
                Canvas(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = Spacing.xs)
                        .chartSemantics(described),
                ) {
                    spans.forEach { span ->
                        drawRect(
                            color = spanColor.copy(alpha = Emphasis.wash),
                            topLeft = Offset(span.start * size.width, 0f),
                            size = androidx.compose.ui.geometry.Size((span.endInclusive - span.start) * size.width, size.height),
                        )
                    }
                    if (secondary != null && second != null) drawSmoothLine(secondary, second, filled = false)
                    drawSmoothLine(primary, first, filled = true)
                }
                if (secondary != null && second != null) AxisLabels(second, secondary, TextAlign.Start) else Box(Modifier.width(Spacing.xs))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                xLabels.forEach {
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
                text = series.format(value),
                style = MaterialTheme.typography.labelSmall,
                color = series.color,
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

private fun DrawScope.drawSmoothLine(series: OverlaySeries, scale: OverlayScale, filled: Boolean) {
    val step = (series.points.size / MaxDrawnPoints).coerceAtLeast(1)
    val drawn = series.points.filterIndexed { index, _ -> index % step == 0 || index == series.points.lastIndex }
    if (drawn.isEmpty()) return
    val span = scale.high - scale.low
    val offsets = drawn.map { (x, value) ->
        val share = ((value - scale.low) / span).toFloat().coerceIn(0f, 1f)
        val y = if (series.inverted) share * size.height else (1f - share) * size.height
        Offset(x.coerceIn(0f, 1f) * size.width, y)
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
    drawPath(line, series.color, style = Stroke(width = Spacing.xs.toPx() / 2 * if (filled) 1.25f else 1f, cap = StrokeCap.Round))
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
            if (figures.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                figures.forEach { (label, value) ->
                    Column(Modifier.weight(1f)) {
                        Text(value, style = MaterialTheme.typography.titleMedium, color = accent)
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            chart()
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun sessionXLabels(axis: SessionAxis): List<String> =
    listOf(0f, 0.5f, 1f).map { formatElapsedChartLabel(axis.elapsedAt(it)) }

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
) {
    if (paceSecondsPerKm.size < 2) return
    val axis = remember(sessionStart, sessionEnd, pauses) { SessionAxis(sessionStart, sessionEnd, pauses) }
    val formatPace: (Double) -> String = { seconds -> unitFormatter.averagePace(1_000.0, (seconds * 1_000).toLong())?.text ?: "–" }
    val points = remember(paceSecondsPerKm, axis) { paceSecondsPerKm.across(axis) }
    val title = stringResource(R.string.activity_chart_pace_title)
    val values = remember(paceSecondsPerKm) { paceSecondsPerKm.map { it.second } }
    ActivityTypeChartFrame(
        title = title,
        figures = listOf(
            stringResource(R.string.summary_average) to formatPace(values.average()),
            stringResource(R.string.activity_chart_fastest) to formatPace(values.min()),
            stringResource(R.string.activity_chart_slowest) to formatPace(values.max()),
        ),
        accent = DistanceColor,
        modifier = modifier,
    ) {
        ActivityOverlayChart(
            title = title,
            primary = OverlaySeries(points, DistanceColor, title, formatPace, inverted = true),
            secondary = null,
            xLabels = sessionXLabels(axis),
            keys = paceSecondsPerKm,
        )
    }
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
    val longest = zones.max().coerceAtLeast(1)
    val title = stringResource(R.string.activity_chart_zones_title)
    ActivityTypeChartFrame(
        title = title,
        figures = emptyList(),
        accent = HeartColor,
        modifier = modifier,
        note = stringResource(R.string.activity_chart_zones_note, maxHeartRateBpm),
    ) {
        val spoken = zones.mapIndexed { index, seconds -> "Z${index + 1} ${formatElapsedChartLabel(Duration.ofSeconds(seconds))}" }
            .joinToString(", ")
        Column(
            modifier = Modifier.chartSemantics("$title: $spoken"),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            zones.forEachIndexed { index, seconds ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        stringResource(R.string.activity_chart_zone_label, index + 1),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(Spacing.giant + Spacing.lg),
                    )
                    Box(Modifier.weight(1f).height(Spacing.lg)) {
                        Box(
                            Modifier
                                .fillMaxWidth((seconds.toFloat() / longest).coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(ZoneColors[index], RoundedCornerShape(Spacing.xs)),
                        )
                    }
                    Text(
                        "${formatElapsedChartLabel(Duration.ofSeconds(seconds))} · ${seconds * 100 / total}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(Spacing.giant * 2),
                    )
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
            xLabels = sessionXLabels(axis),
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
    val sets = remember(segments, axis) { strengthSetSpans(segments).map { (from, to) -> axis.fractionOf(from)..axis.fractionOf(to) } }
    val title = stringResource(R.string.activity_chart_sets_title)
    val bpm: (Double) -> String = { "${it.toInt()} bpm" }
    ActivityTypeChartFrame(
        title = title,
        figures = listOf(
            stringResource(R.string.activity_chart_sets_count) to unitFormatter.count(sets.size),
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
            xLabels = sessionXLabels(axis),
            keys = samples to segments,
            spans = sets,
            spanColor = MaterialTheme.colorScheme.primary,
            spanLabel = stringResource(R.string.activity_chart_sets_legend),
        )
    }
}

/** 93: the heart rate in the two minutes after the end, and how far it fell. */
@Composable
internal fun ActivityRecoveryChartCard(
    samples: List<HeartRateSample>,
    sessionEnd: Instant,
    modifier: Modifier = Modifier,
) {
    val trace = remember(samples, sessionEnd) { recoveryTrace(samples, sessionEnd) }
    if (trace.size < 2) return
    val window = recoveryChartWindow.seconds.toFloat()
    val points = remember(trace) { trace.map { (seconds, bpm) -> (seconds / window) to bpm.toDouble() } }
    val atEnd = trace.first().second
    fun near(seconds: Long): Long? = trace.minByOrNull { kotlin.math.abs(it.first - seconds) }
        ?.takeIf { kotlin.math.abs(it.first - seconds) <= 20 }?.second
    val oneMinute = near(60)
    val twoMinutes = near(120)
    val bpm: (Double) -> String = { "${it.toInt()} bpm" }
    fun drop(value: Long?): String = value?.let { "−${(atEnd - it).coerceAtLeast(0)} bpm" } ?: "–"
    val title = stringResource(R.string.activity_chart_recovery_title)
    ActivityTypeChartFrame(
        title = title,
        figures = listOf(
            stringResource(R.string.activity_chart_recovery_end) to bpm(atEnd.toDouble()),
            stringResource(R.string.activity_chart_recovery_one) to drop(oneMinute),
            stringResource(R.string.activity_chart_recovery_two) to drop(twoMinutes),
        ),
        accent = HeartColor,
        modifier = modifier,
    ) {
        ActivityOverlayChart(
            title = title,
            primary = OverlaySeries(points, HeartColor, stringResource(R.string.activity_recording_live_heart_rate), bpm),
            secondary = null,
            xLabels = listOf("0:00", "1:00", "2:00"),
            keys = trace,
        )
    }
}

/** 88–93: the kind-specific chart of [workout], and the recovery chart, as list items. */
internal fun LazyListScope.activityTypeChartItems(
    workout: ExerciseData,
    heartRateSamples: List<HeartRateSample>,
    speedSamples: List<SpeedSample>,
    splitSpeedTrace: ActivitySplitSpeedTrace?,
    cadenceSamples: List<ActivityCadenceSample>,
    elevationSamples: List<ActivityElevationSample>,
    recoverySamples: List<HeartRateSample>,
    maxHeartRateBpm: Int?,
    unitFormatter: UnitFormatter,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    val pauses = workout.segments.toSessionPauses()
    val heartRate = heartRateSamples.map { it.time to it.beatsPerMinute.toDouble() }
    val speed = speedSamples.map { it.time to it.metersPerSecond }.ifEmpty {
        splitSpeedTrace?.samples.orEmpty().map { it.time to it.metersPerSecond }
    }
    when (workoutTypeChart(workout.exerciseType)) {
        WorkoutTypeChart.PACE -> item(key = "type-chart-pace") {
            val pace = remember(speedSamples, workout.route) {
                paceFromSpeed(speedSamples).ifEmpty { paceFromRoute(workout.route.points) }
            }
            ActivityPaceChartCard(pace, workout.startTime, workout.endTime, pauses, unitFormatter, cardModifier)
        }
        WorkoutTypeChart.HEART_RATE_ZONES -> if (maxHeartRateBpm != null) item(key = "type-chart-zones") {
            ActivityHeartRateZonesCard(heartRateSamples, workout.startTime, workout.endTime, maxHeartRateBpm, cardModifier)
        }
        WorkoutTypeChart.ELEVATION_WITH_SPEED -> item(key = "type-chart-climbs") {
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
        WorkoutTypeChart.CADENCE_WITH_HEART_RATE -> item(key = "type-chart-cadence-heart") {
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
        WorkoutTypeChart.HEART_RATE_WITH_SETS -> item(key = "type-chart-sets") {
            ActivityStrengthSetsChartCard(
                heartRateSamples, workout.segments, workout.startTime, workout.endTime, pauses, unitFormatter, cardModifier,
            )
        }
        null -> Unit
    }
    if (heartRateSamples.isNotEmpty()) item(key = "type-chart-recovery") {
        ActivityRecoveryChartCard(recoverySamples, workout.endTime, cardModifier)
    }
}
