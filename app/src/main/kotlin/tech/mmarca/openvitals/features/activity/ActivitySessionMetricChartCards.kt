package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlin.math.roundToInt
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.SplitSource
import tech.mmarca.openvitals.domain.insights.CalorieIntervalSource
import tech.mmarca.openvitals.domain.insights.SessionCalories
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.ChartZoom
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlotPoint
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.SessionAxis
import tech.mmarca.openvitals.ui.components.SessionAxisLabels
import tech.mmarca.openvitals.ui.components.SessionPause
import tech.mmarca.openvitals.ui.components.formatElapsedChartLabel
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.StepsColor

@Composable
internal fun ActivitySpeedChartCard(
    /** The speed as [movementSeries] checked it: cleaned, outside the pauses, adding up to the distance. */
    series: List<Pair<Instant, Double>>,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    pauses: List<SessionPause> = emptyList(),
) {
    if (series.size < 2) return

    ActivitySessionMetricChartCard(
        title = stringResource(R.string.activity_recording_live_speed),
        sortedValues = series,
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        unitFormatter = unitFormatter,
        accentColor = DistanceColor,
        valueFormatter = { unitFormatter.speed(it).text },
        modifier = modifier,
        pauses = pauses,
    )
}

/**
 * Speed rebuilt from the splits for a device that recorded none. The trace
 * steps, one flat run per split, and the stat row counts splits. The
 * trace states its own average: the chart's mean would be wrong.
 */
@Composable
internal fun ActivitySplitSpeedChartCard(
    trace: ActivitySplitSpeedTrace,
    source: SplitSource,
    splitDistanceMeters: Double,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    pauses: List<SessionPause> = emptyList(),
) {
    if (trace.samples.isEmpty()) return

    // ESTIMATED never reaches here (flat by construction), nor SPEED_SAMPLES (the recorded card wins).
    val title: String
    val countLabel: String
    when (source) {
        SplitSource.DEVICE_LAPS -> {
            title = stringResource(R.string.activity_speed_per_lap_title)
            countLabel = stringResource(R.string.activity_speed_laps_label)
        }
        else -> {
            title = stringResource(
                R.string.activity_speed_per_split_title,
                splitDistanceLabel(unitFormatter, splitDistanceMeters),
            )
            countLabel = stringResource(R.string.activity_speed_splits_label)
        }
    }

    ActivitySessionMetricChartCard(
        title = title,
        sortedValues = remember(trace) { trace.samples.map { it.time to it.metersPerSecond } },
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        unitFormatter = unitFormatter,
        accentColor = DistanceColor,
        valueFormatter = { unitFormatter.speed(it).text },
        countText = unitFormatter.count(trace.splitCount),
        countLabel = countLabel,
        averageOverride = trace.averageMetersPerSecond,
        modifier = modifier,
        pauses = pauses,
    )
}

/**
 * The height profile. Health Connect has no elevation series, so this is
 * drawn from the route's altitudes. No route or altitude, no card.
 */
/**
 * The energy burned minute by minute, drawn like the heart rate and
 * elevation charts. Each point is the kcal per minute of one slice of the
 * session, from heart rate (Keytel) when the body profile allows it.
 */
@Composable
internal fun ActivityCaloriesChartCard(
    calories: SessionCalories,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    pauses: List<SessionPause> = emptyList(),
) {
    if (calories.intervals.isEmpty() || calories.totalKcal < 1.0) return
    val perMinute = remember(calories) {
        calories.intervals.map { interval ->
            val seconds = java.time.Duration.between(interval.start, interval.end).seconds.coerceAtLeast(1L)
            interval.start.plusSeconds(seconds / 2) to interval.kcal / (seconds / 60.0)
        }
    }
    val sessionMinutes = java.time.Duration.between(sessionStart, sessionEnd).seconds / 60.0
    val average = if (sessionMinutes > 0.0) calories.totalKcal / sessionMinutes else 0.0
    ActivitySessionMetricChartCard(
        title = stringResource(R.string.activity_calories_per_interval_title),
        sortedValues = perMinute,
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        unitFormatter = unitFormatter,
        accentColor = CaloriesColor,
        valueFormatter = { kcalPerMinuteText(it) },
        statsOverride = listOf(
            stringResource(R.string.stat_total) to unitFormatter.energy(calories.totalKcal).text,
            stringResource(R.string.summary_average) to kcalPerMinuteText(average),
            stringResource(R.string.activity_calories_peak) to kcalPerMinuteText(perMinute.maxOf { it.second }),
        ),
        footnote = stringResource(
            if (calories.source == CalorieIntervalSource.HEART_RATE) {
                R.string.activity_calories_from_heart_rate
            } else {
                R.string.activity_calories_from_recorded
            },
        ),
        modifier = modifier,
        pauses = pauses,
    )
}

private fun kcalPerMinuteText(value: Double): String =
    String.format(java.util.Locale.getDefault(), "%.1f kcal/min", value)

/** Speed worth charting: at least one sample moved. A trainer without a wheel sensor writes zeros. */
internal fun hasMeaningfulSpeed(samples: List<SpeedSample>): Boolean =
    samples.any { it.metersPerSecond.isFinite() && it.metersPerSecond > 0.05 }

@Composable
internal fun ActivityElevationChartCard(
    samples: List<ActivityElevationSample>,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    pauses: List<SessionPause> = emptyList(),
    source: tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource =
        tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource.GPS,
) {
    if (samples.isEmpty()) return
    // The ascent and descent of the profile drawn, filtered of GPS wander as the splits
    // count them, beside the lowest and highest point.
    val change = remember(samples) {
        tech.mmarca.openvitals.domain.insights.RouteElevation.elevationChangeFromAltitudes(samples.map { it.meters })
    }
    val low = samples.minOf { it.meters }
    val high = samples.maxOf { it.meters }

    ActivitySessionMetricChartCard(
        statsOverride = listOf(
            stringResource(R.string.activity_elevation_ascent) to "↑ ${unitFormatter.elevation(change.gain).text}",
            stringResource(R.string.activity_elevation_descent) to "↓ ${unitFormatter.elevation(change.loss).text}",
            stringResource(R.string.summary_range) to
                activityValueRange(unitFormatter.elevation(low).text, unitFormatter.elevation(high).text),
        ),
        // Where the heights came from: a terrain model, or the GPS, which drifts.
        footnote = stringResource(
            when (source) {
                tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource.TERRAIN_TILES ->
                    R.string.activity_elevation_source_tiles
                tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource.MAPBOX_TERRAIN ->
                    R.string.activity_elevation_source_mapbox
                tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource.GPS ->
                    R.string.activity_elevation_source_gps
                tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource.GPS_MAPBOX_FAILED ->
                    R.string.activity_elevation_source_mapbox_failed
                tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource.TERRARIUM_TERRAIN ->
                    R.string.activity_elevation_source_terrarium
                tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource.COMBINED_TERRAIN ->
                    R.string.activity_elevation_source_combined
                tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource.GPS_TERRAIN_FAILED ->
                    R.string.activity_elevation_source_terrain_failed
            },
        ),
        title = stringResource(R.string.metric_elevation),
        sortedValues = remember(samples) { samples.map { it.time to it.meters } },
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        unitFormatter = unitFormatter,
        accentColor = ElevationColor,
        valueFormatter = { unitFormatter.elevation(it).text },
        floorAtZero = false,
        modifier = modifier,
        pauses = pauses,
    )
}

@Composable
internal fun ActivityCadenceChartCard(
    samples: List<ActivityCadenceSample>,
    kind: ActivityCadenceKind,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    pauses: List<SessionPause> = emptyList(),
    /** A run's cadence written per foot is counted in steps. */
    exerciseType: Int? = null,
    /** The step cadence as [movementSeries] checked it against the steps; read here when null. */
    stepSeries: List<Pair<Instant, Double>>? = null,
) {
    val filtered = remember(samples, kind) { samples.filter { it.kind == kind } }
    // All zeros is a sensor that was not there, not a cadence.
    if (filtered.none { it.rate > 0.0 }) return
    // One app's readings inside the session, cleaned, and a run's counted in steps.
    val series = remember(filtered, sessionStart, sessionEnd, exerciseType, stepSeries) {
        if (kind == ActivityCadenceKind.STEPS && stepSeries != null) return@remember stepSeries
        val cleaned = tech.mmarca.openvitals.domain.insights.sessionSeries(
            filtered.map { tech.mmarca.openvitals.domain.insights.SessionReading(it.time, it.rate, it.source) },
            sessionStart,
            sessionEnd,
        )
        if (kind == ActivityCadenceKind.STEPS) {
            tech.mmarca.openvitals.domain.insights.runningCadenceInSteps(exerciseType, cleaned)
        } else {
            cleaned
        }
    }
    if (series.size < 2) return

    val title = when (kind) {
        ActivityCadenceKind.CYCLING -> stringResource(R.string.metric_cycling_cadence)
        ActivityCadenceKind.STEPS -> stringResource(R.string.metric_steps_cadence)
    }
    val accentColor = when (kind) {
        ActivityCadenceKind.CYCLING -> CycleColor
        ActivityCadenceKind.STEPS -> StepsColor
    }

    ActivitySessionMetricChartCard(
        title = title,
        sortedValues = series,
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        unitFormatter = unitFormatter,
        accentColor = accentColor,
        valueFormatter = {
            when (kind) {
                ActivityCadenceKind.STEPS -> unitFormatter.stepsCadence(it).text
                ActivityCadenceKind.CYCLING -> unitFormatter.cadence(it).text
            }
        },
        modifier = modifier,
        pauses = pauses,
    )
}

@Composable
private fun ActivitySessionMetricChartCard(
    title: String,
    sortedValues: List<Pair<Instant, Double>>,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    accentColor: Color,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier,
    // Speed and cadence cannot be negative. Elevation is read against the
    // ground, not zero, and can be below it.
    floorAtZero: Boolean = true,
    // A stepped trace counts splits, not the corners of the steps.
    countText: String? = null,
    countLabel: String? = null,
    averageOverride: Double? = null,
    pauses: List<SessionPause> = emptyList(),
    // Label to value; replaces the average, range and sample stats when set.
    statsOverride: List<Pair<String, String>>? = null,
    // A line under the "recorded" time, such as where the numbers came from.
    footnote: String? = null,
) {
    if (sortedValues.isEmpty()) return

    // One pass each per list, not per recomposition.
    val minValue = remember(sortedValues) { sortedValues.minOf { it.second } }
    val maxValue = remember(sortedValues) { sortedValues.maxOf { it.second } }
    // Weighted by time, so a stretch read every second does not outweigh one read every minute.
    val avgValue = averageOverride ?: remember(sortedValues) {
        tech.mmarca.openvitals.domain.insights.timeWeightedMean(sortedValues) ?: sortedValues.map { it.second }.average()
    }
    // A flat trace still needs a readable axis: give it room around its value.
    val valueRange = (maxValue - minValue).takeIf { it > 1e-6 }
        ?: maxOf(kotlin.math.abs(maxValue) * 0.2, 1.0)
    val paddedMin = (minValue - valueRange * 0.1).let { if (floorAtZero) it.coerceAtLeast(0.0) else it }
    val paddedMax = maxValue + valueRange * 0.1
    // The axis counts only moving time: a pause gets none of the chart.
    val axis = remember(sessionStart, sessionEnd, pauses) {
        SessionAxis(start = sessionStart, end = sessionEnd, pauses = pauses)
    }
    val chartHeight = 180.dp
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    // Lines only, as the heart rate and elevation charts are.
    val drawPoints = false
    // Built once, identity-stable, so the geometry cache holds.
    val chartPoints = remember(sortedValues, axis) {
        // Drawn from the session's start; a first reading that came later is estimated there.
        anchoredAtStart(sortedValues.map { (time, value) -> axis.fractionOf(time) to value })
            .map { (x, value) -> MetricLinePlotPoint(xFraction = x, value = value) }
    }

    OpenVitalsCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            // Left, middle and right, the range's unit said once.
            ActivityChartFigures(
                figures = statsOverride ?: listOf(
                    stringResource(R.string.summary_average) to valueFormatter(avgValue),
                    stringResource(R.string.summary_range) to activityValueRange(valueFormatter(minValue), valueFormatter(maxValue)),
                    (countLabel ?: stringResource(R.string.summary_samples)) to
                        (countText ?: unitFormatter.count(sortedValues.size)),
                ),
                accent = accentColor,
            )
            // Plot and elapsed row share the one viewport.
            ChartZoom(
                sessionStart, sessionEnd, sortedValues,
                initialViewport = remember(sortedValues, axis) {
                    activitySampleViewport(sortedValues.map { axis.fractionOf(it.first) })
                },
            ) { zoom ->
                Column {
                    MetricLinePlot(
                        points = chartPoints,
                        minValue = paddedMin,
                        maxValue = paddedMax,
                        accentColor = accentColor,
                        chartHeight = chartHeight,
                        valueFormatter = valueFormatter,
                        pointRadius = if (drawPoints) 2.dp else 0.dp,
                        lineStrokeWidth = 2.dp,
                        drawPoints = drawPoints,
                        viewport = zoom.viewport,
                        multiTouch = zoom.multiTouch,
                        title = title,
                        // Moving elapsed, matching the labels under it.
                        scrubLabel = { point ->
                            valueFormatter(point.value) to
                                formatElapsedChartLabel(axis.elapsedAt(point.xFraction))
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    ChartXAxisWithYAxis {
                        SessionAxisLabels(axis = axis, viewport = zoom.viewport)
                    }
                }
            }
            Text(
                text = stringResource(
                    R.string.summary_recorded,
                    timeFormatter.format(sortedValues.first().first.atZone(zone)),
                    timeFormatter.format(sortedValues.last().first.atZone(zone)),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            footnote?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The length of each step over the session, the speed over the cadence. */
@Composable
internal fun ActivityStrideChartCard(
    stridesMeters: List<Pair<Instant, Double>>,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    pauses: List<SessionPause> = emptyList(),
) {
    if (stridesMeters.size < 2) return
    ActivitySessionMetricChartCard(
        title = stringResource(R.string.activity_chart_stride_title),
        sortedValues = stridesMeters.map { (time, meters) -> time to meters * 100.0 },
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        unitFormatter = unitFormatter,
        accentColor = StepsColor,
        valueFormatter = { "${it.roundToInt()} cm" },
        footnote = stringResource(R.string.activity_chart_stride_note),
        modifier = modifier,
        pauses = pauses,
    )
}
