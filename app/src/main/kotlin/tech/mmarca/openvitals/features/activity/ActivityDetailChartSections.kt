package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.GradePoint
import tech.mmarca.openvitals.domain.insights.GradientDistribution
import tech.mmarca.openvitals.domain.insights.gradientDistribution
import tech.mmarca.openvitals.domain.insights.heartRateZoneSeconds
import tech.mmarca.openvitals.domain.insights.paceFromRoute
import tech.mmarca.openvitals.domain.insights.showsSpeedChart
import tech.mmarca.openvitals.domain.insights.strideLengthSeries
import tech.mmarca.openvitals.domain.insights.trainingEffect
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.insights.ActivitySplits
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryReading
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource
import tech.mmarca.openvitals.ui.components.MetricDetailSectionBuilder
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * The charts of one activity, each a section the detail screen lets the visitor arrange,
 * with the same slider icon, drag and Save or Discard as the metric screens. The cards
 * around them are sections as well ([activityDetailCardSections]); only the summary heading
 * and the edit and delete buttons stay where they are.
 */
internal fun MetricDetailSectionBuilder.activityDetailChartSections(
    workout: ExerciseData,
    heartRateSamples: List<HeartRateSample>,
    heartRateAccessMissing: Boolean,
    onGrantHeartRate: () -> Unit,
    heartRateRecovery: HeartRateRecoveryReading?,
    recoverySamples: List<HeartRateSample>,
    maxHeartRateBpm: Int?,
    restingHeartRateBpm: Int?,
    speedSamples: List<SpeedSample>,
    cadenceSamples: List<ActivityCadenceSample>,
    sessionCalories: tech.mmarca.openvitals.domain.insights.SessionCalories?,
    splits: ActivitySplits,
    splitDistanceMeters: Double,
    slowestSplitPaceSeconds: Double?,
    fastestSplitPaceSeconds: Double?,
    splitSpeedTrace: ActivitySplitSpeedTrace?,
    elevationSamples: List<ActivityElevationSample>,
    elevationSource: RouteElevationSource,
    unitFormatter: UnitFormatter,
    sessionCardioLoad: Int? = null,
    personalRecords: List<tech.mmarca.openvitals.domain.insights.PersonalRecord> = emptyList(),
    recordsComparedWith: Int = 0,
    /** Speed and step cadence checked against the distance and the steps. */
    movement: tech.mmarca.openvitals.domain.insights.MovementSeries = workout.movementSeries(speedSamples, cadenceSamples),
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    val pauses = workout.segments.toSessionPauses()
    // The records this activity holds among those of its kind.
    section(MetricDetailSectionId.ACTIVITY_DETAIL_RECORDS, personalRecords.isNotEmpty()) {
        ActivityPersonalRecordsCard(personalRecords, recordsComparedWith, unitFormatter, cardModifier)
    }
    // A jump rope session's jumps, from its segments.
    val jumps = tech.mmarca.openvitals.domain.insights.jumpRopeSummary(workout.segments)
    section(MetricDetailSectionId.ACTIVITY_DETAIL_JUMPS, jumps != null) {
        jumps?.let { ActivityJumpRopeCard(it, unitFormatter, cardModifier) }
    }
    section(MetricDetailSectionId.ACTIVITY_DETAIL_SPLITS, splits.isNotEmpty) {
        ActivitySplitsCard(
            splits = splits,
            useSpeed = workout.exerciseType !in tech.mmarca.openvitals.domain.model.prefersPaceExercises,
            splitDistanceMeters = splitDistanceMeters,
            slowestPaceSeconds = slowestSplitPaceSeconds,
            fastestPaceSeconds = fastestSplitPaceSeconds,
            unitFormatter = unitFormatter,
            modifier = cardModifier,
        )
    }
    section(
        MetricDetailSectionId.ACTIVITY_DETAIL_HEART_RATE,
        heartRateSamples.isNotEmpty() || heartRateAccessMissing,
    ) {
        if (heartRateSamples.isNotEmpty()) {
            ActivityHeartRateChartCard(
                samples = heartRateSamples,
                sessionStart = workout.startTime,
                sessionEnd = workout.endTime,
                pauses = pauses,
                unitFormatter = unitFormatter,
                modifier = cardModifier,
            )
        } else {
            ActivityHeartRateAccessCard(onGrant = onGrantHeartRate, modifier = cardModifier)
        }
    }
    sessionCalories?.let { calories ->
        section(MetricDetailSectionId.ACTIVITY_DETAIL_CALORIES) {
            ActivityCaloriesChartCard(
                calories = calories,
                sessionStart = workout.startTime,
                sessionEnd = workout.endTime,
                pauses = pauses,
                unitFormatter = unitFormatter,
                modifier = cardModifier,
            )
        }
    }
    // Only a reading with a measured peak is worth a card.
    if (heartRateRecovery != null && heartRateRecovery.peakBpm != null) {
        section(MetricDetailSectionId.ACTIVITY_DETAIL_HEART_RATE_DROP) {
            HeartRateRecoveryCard(reading = heartRateRecovery, modifier = cardModifier)
        }
    }
    // The chart each kind of activity gets, the zones, and the recovery after the end.
    activityTypeChartSections(
        workout = workout,
        heartRateSamples = heartRateSamples,
        speedSamples = speedSamples,
        splitSpeedTrace = splitSpeedTrace,
        cadenceSamples = cadenceSamples,
        elevationSamples = elevationSamples,
        recoverySamples = recoverySamples,
        maxHeartRateBpm = maxHeartRateBpm,
        unitFormatter = unitFormatter,
        restingHeartRateBpm = restingHeartRateBpm,
        movement = movement,
    )
    // Recorded speed, or the split-derived reconstruction, never both. Not on a treadmill.
    val speedChartShown = showsSpeedChart(workout.exerciseType)
    section(
        MetricDetailSectionId.ACTIVITY_DETAIL_SPEED,
        speedChartShown && (movement.speed.size >= 2 || splitSpeedTrace != null),
    ) {
        if (movement.speed.size >= 2) {
            ActivitySpeedChartCard(
                series = movement.speed,
                sessionStart = workout.startTime,
                sessionEnd = workout.endTime,
                pauses = pauses,
                unitFormatter = unitFormatter,
                modifier = cardModifier,
            )
        } else if (splitSpeedTrace != null) {
            ActivitySplitSpeedChartCard(
                trace = splitSpeedTrace,
                source = splits.source,
                splitDistanceMeters = splitDistanceMeters,
                sessionStart = workout.startTime,
                sessionEnd = workout.endTime,
                pauses = pauses,
                unitFormatter = unitFormatter,
                modifier = cardModifier,
            )
        }
    }
    section(MetricDetailSectionId.ACTIVITY_DETAIL_ELEVATION, elevationSamples.isNotEmpty()) {
        ActivityElevationChartCard(
            samples = elevationSamples,
            source = elevationSource,
            sessionStart = workout.startTime,
            sessionEnd = workout.endTime,
            pauses = pauses,
            unitFormatter = unitFormatter,
            modifier = cardModifier,
        )
    }
    // The time uphill, flat and downhill, from the route and its heights.
    val gradient = activityGradient(workout, elevationSamples)
    section(MetricDetailSectionId.ACTIVITY_DETAIL_GRADIENT, gradient != null) {
        gradient?.let { ActivityGradientCard(it, cardModifier) }
    }
    // The aerobic and anaerobic effect from the zones, and the session's load.
    val effect = if (maxHeartRateBpm != null && heartRateSamples.isNotEmpty()) {
        trainingEffect(heartRateZoneSeconds(heartRateSamples, workout.startTime, workout.endTime, maxHeartRateBpm))
    } else {
        null
    }
    section(MetricDetailSectionId.ACTIVITY_DETAIL_TRAINING_EFFECT, effect != null) {
        effect?.let { ActivityTrainingEffectCard(it, sessionCardioLoad, cardModifier) }
    }
    // The length of each step, for the kinds that count steps.
    val strides = if (workout.exerciseType in tech.mmarca.openvitals.domain.model.stepBasedExercises) {
        // The same checked cadence and speed the charts draw.
        strideLengthSeries(
            cadence = movement.stepCadence,
            speed = movement.speed
                .ifEmpty { splitSpeedTrace?.samples.orEmpty().map { it.time to it.metersPerSecond } },
        )
    } else {
        emptyList()
    }
    section(MetricDetailSectionId.ACTIVITY_DETAIL_STRIDE, strides.size >= 2) {
        ActivityStrideChartCard(strides, workout.startTime, workout.endTime, unitFormatter, cardModifier, pauses)
    }
    // The laps the watch wrote: a pool swim's lengths, a track's laps.
    section(MetricDetailSectionId.ACTIVITY_DETAIL_LAPS, workout.laps.isNotEmpty()) {
        ActivityLapsCard(
            laps = workout.laps,
            swim = workout.exerciseType == androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL ||
                workout.exerciseType == androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
            unitFormatter = unitFormatter,
            modifier = cardModifier,
        )
    }
    val cadenceKinds = activityCadenceKinds(cadenceSamples, workout.exerciseType)
    section(MetricDetailSectionId.ACTIVITY_DETAIL_CADENCE, cadenceKinds.isNotEmpty()) {
        Column {
            cadenceKinds.forEach { cadenceKind ->
                ActivityCadenceChartCard(
                    samples = cadenceSamples,
                    kind = cadenceKind,
                    stepSeries = movement.stepCadence,
                    sessionStart = workout.startTime,
                    sessionEnd = workout.endTime,
                    pauses = pauses,
                    unitFormatter = unitFormatter,
                    modifier = cardModifier,
                    exerciseType = workout.exerciseType,
                )
            }
        }
    }
}

/** The charts in their default order, for a caller with no arrangement to offer (a test). */
@Composable
internal fun rememberFixedActivityChartSections(): MetricDetailSectionContext {
    val listState = rememberMetricDetailSectionListState()
    return remember(listState) {
        MetricDetailSectionContext(
            listState = listState,
            order = DefaultMetricDetailSectionOrder,
            isEditingSections = false,
            onMoveSectionToTarget = { _, _ -> },
            onMoveSection = { _, _ -> },
        )
    }
}

/** The drag wiring the metric screens get from their scaffold, for the detail's own list. */
@Composable
internal fun rememberActivityChartReordering(sections: MetricDetailSectionContext): ReorderableLazyListState {
    val listState = sections.listState
    val reorderable = rememberReorderableLazyListState(listState.lazyListState) { from, to ->
        val fromId = from.key as? MetricDetailSectionId
        val toId = to.key as? MetricDetailSectionId
        if (fromId != null && toId != null && fromId != toId) listState.onMoveSectionToTarget?.invoke(fromId, toId)
    }
    SideEffect { listState.reorderableState = reorderable }
    return reorderable
}

/** The route's points with the heights the chart draws (terrain or GPS), matched by time. */
private fun activityGradient(workout: ExerciseData, elevation: List<ActivityElevationSample>): GradientDistribution? {
    if (elevation.size < 2) return null
    val heights = elevation.associate { it.time to it.meters }
    val points = workout.route.points.mapNotNull { point ->
        (heights[point.time] ?: point.altitudeMeters)?.let { GradePoint(point.time, point.latitude, point.longitude, it) }
    }
    return gradientDistribution(points)
}

/**
 * The speed and step cadence of [this] session as its charts draw them: the session app's
 * readings outside the pauses, checked against the distance and the steps ([movementSeries]).
 */
internal fun ExerciseData.movementSeries(
    speedSamples: List<SpeedSample>,
    cadenceSamples: List<ActivityCadenceSample>,
): tech.mmarca.openvitals.domain.insights.MovementSeries =
    tech.mmarca.openvitals.domain.insights.movementSeries(
        exerciseType = exerciseType,
        start = startTime,
        end = endTime,
        speedReadings = speedSamples.map { tech.mmarca.openvitals.domain.insights.SessionReading(it.time, it.metersPerSecond, it.source) },
        stepCadenceReadings = cadenceSamples.filter { it.kind == ActivityCadenceKind.STEPS }
            .map { tech.mmarca.openvitals.domain.insights.SessionReading(it.time, it.rate, it.source) },
        preferredSource = source.takeIf { it.isNotBlank() },
        route = if (route.status == tech.mmarca.openvitals.domain.model.ExerciseRouteStatus.DATA) route.points else emptyList(),
        distanceMeters = totalDistanceMeters,
        steps = steps,
        pauses = segments.toSessionPauses().map { it.start to it.end },
    )
