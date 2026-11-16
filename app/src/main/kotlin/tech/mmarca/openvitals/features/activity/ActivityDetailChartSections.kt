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
import tech.mmarca.openvitals.domain.insights.showsSpeedChart
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
 * 113: the charts of one activity, each a section the detail screen lets the visitor arrange,
 * with the same slider icon, drag and Save or Discard as the metric screens. The summary above
 * and the details, export and route cards below stay where they are.
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
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    val pauses = workout.segments.toSessionPauses()
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
    // 88–93: the chart each kind of activity gets, the zones, and the recovery after the end.
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
    )
    // Recorded speed, or the split-derived reconstruction, never both. 112: not on a treadmill.
    val speedChartShown = showsSpeedChart(workout.exerciseType)
    section(
        MetricDetailSectionId.ACTIVITY_DETAIL_SPEED,
        speedChartShown && (speedSamples.isNotEmpty() || splitSpeedTrace != null),
    ) {
        if (speedSamples.isNotEmpty()) {
            ActivitySpeedChartCard(
                samples = speedSamples,
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
    val cadenceKinds = activityCadenceKinds(cadenceSamples, workout.exerciseType)
    section(MetricDetailSectionId.ACTIVITY_DETAIL_CADENCE, cadenceKinds.isNotEmpty()) {
        Column {
            cadenceKinds.forEach { cadenceKind ->
                ActivityCadenceChartCard(
                    samples = cadenceSamples,
                    kind = cadenceKind,
                    sessionStart = workout.startTime,
                    sessionEnd = workout.endTime,
                    pauses = pauses,
                    unitFormatter = unitFormatter,
                    modifier = cardModifier,
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

/** 113: the drag wiring the metric screens get from their scaffold, for the detail's own list. */
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
