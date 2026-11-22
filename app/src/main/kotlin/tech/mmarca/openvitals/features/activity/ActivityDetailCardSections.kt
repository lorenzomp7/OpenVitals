package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.MetricDetailSectionBuilder
import tech.mmarca.openvitals.ui.theme.Spacing

/** What the export card's buttons do: save or share in each format. */
internal class ActivityExportActions(
    val onSaveWorkoutAsTcx: () -> Unit = {},
    val onSaveWorkoutAsCsv: () -> Unit = {},
    val onSaveWorkoutAsFit: () -> Unit = {},
    val onShareWorkoutAsTcx: () -> Unit = {},
    val onShareWorkoutAsCsv: () -> Unit = {},
    val onShareWorkoutAsFit: () -> Unit = {},
    val onSaveWorkoutAsFitWithRoute: () -> Unit = {},
    val onShareWorkoutAsFitWithRoute: () -> Unit = {},
    val onSaveRouteAsGpx: () -> Unit = {},
    val onSaveRouteAsKmz: () -> Unit = {},
    val onShareRouteAsGpx: () -> Unit = {},
    val onShareRouteAsKmz: () -> Unit = {},
)

/**
 * 131: the cards around an activity's charts (figures, plan, route, its analysis and navigation,
 * segments, session details, export) are sections too: moved, hidden and put back as the charts
 * are, in the general order (132). The summary heading stays on top and the edit and delete
 * buttons at the bottom. [only], when given, limits the build to those sections (134: what can
 * show while the samples load).
 */
internal fun MetricDetailSectionBuilder.activityDetailCardSections(
    workout: ExerciseData,
    markers: List<ActivityRecordingMarker>,
    coMapsSamples: List<CoMapsNavigationSnapshot>,
    linkedPlanTitle: String?,
    onOpenPlan: (String) -> Unit,
    onOpenRouteInMap: () -> Unit,
    export: ActivityExportActions,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    only: Set<MetricDetailSectionId>? = null,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    fun wanted(id: MetricDetailSectionId) = only == null || id in only
    val hasRoute = workout.route.status == ExerciseRouteStatus.DATA && workout.route.points.isNotEmpty()

    section(MetricDetailSectionId.ACTIVITY_DETAIL_METRICS, wanted(MetricDetailSectionId.ACTIVITY_DETAIL_METRICS)) {
        MetricsCard(workout = workout, unitFormatter = unitFormatter, modifier = cardModifier)
    }
    val planId = workout.plannedExerciseSessionId
    section(MetricDetailSectionId.ACTIVITY_DETAIL_PLAN, planId != null && wanted(MetricDetailSectionId.ACTIVITY_DETAIL_PLAN)) {
        if (planId != null) {
            ActivityLinkedPlanDetailCard(
                planTitle = linkedPlanTitle,
                onOpenPlan = { onOpenPlan(planId) },
                modifier = cardModifier,
            )
        }
    }
    // 53: no route data, no route card.
    section(MetricDetailSectionId.ACTIVITY_DETAIL_ROUTE, hasRoute && wanted(MetricDetailSectionId.ACTIVITY_DETAIL_ROUTE)) {
        RouteCard(
            route = workout.route,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onOpenRouteInMap = onOpenRouteInMap,
            modifier = cardModifier,
        )
    }
    // The analysis needs two route points or a marker, else the card draws nothing.
    val hasAnalysis = (workout.route.status == ExerciseRouteStatus.DATA && workout.route.points.size >= 2) ||
        markers.isNotEmpty()
    section(
        MetricDetailSectionId.ACTIVITY_DETAIL_ROUTE_ANALYSIS,
        hasAnalysis && wanted(MetricDetailSectionId.ACTIVITY_DETAIL_ROUTE_ANALYSIS),
    ) {
        ActivityRouteAnalysisCard(
            workout = workout,
            markers = markers,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = cardModifier,
        )
    }
    section(
        MetricDetailSectionId.ACTIVITY_DETAIL_NAVIGATION,
        coMapsSamples.isNotEmpty() && wanted(MetricDetailSectionId.ACTIVITY_DETAIL_NAVIGATION),
    ) {
        ActivityCoMapsNavigationCard(samples = coMapsSamples, modifier = cardModifier)
    }
    section(
        MetricDetailSectionId.ACTIVITY_DETAIL_SEGMENTS,
        workout.segments.isNotEmpty() && wanted(MetricDetailSectionId.ACTIVITY_DETAIL_SEGMENTS),
    ) {
        SegmentsCard(
            segments = workout.segments,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = cardModifier,
        )
    }
    section(MetricDetailSectionId.ACTIVITY_DETAIL_SESSION, wanted(MetricDetailSectionId.ACTIVITY_DETAIL_SESSION)) {
        SessionDetailsCard(
            workout = workout,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            modifier = cardModifier,
        )
    }
    section(MetricDetailSectionId.ACTIVITY_DETAIL_EXPORT, wanted(MetricDetailSectionId.ACTIVITY_DETAIL_EXPORT)) {
        // Route formats only with a route; metric formats always.
        WorkoutExportCard(
            onSaveAsTcx = export.onSaveWorkoutAsTcx,
            onSaveAsCsv = export.onSaveWorkoutAsCsv,
            onSaveAsFit = export.onSaveWorkoutAsFit,
            onShareAsTcx = export.onShareWorkoutAsTcx,
            onShareAsCsv = export.onShareWorkoutAsCsv,
            onShareAsFit = export.onShareWorkoutAsFit,
            onSaveRouteAsGpx = export.onSaveRouteAsGpx.takeIf { hasRoute },
            onSaveRouteAsKmz = export.onSaveRouteAsKmz.takeIf { hasRoute },
            onShareRouteAsGpx = export.onShareRouteAsGpx.takeIf { hasRoute },
            onShareRouteAsKmz = export.onShareRouteAsKmz.takeIf { hasRoute },
            onSaveRouteAsFit = export.onSaveWorkoutAsFitWithRoute.takeIf { hasRoute },
            onShareRouteAsFit = export.onShareWorkoutAsFitWithRoute.takeIf { hasRoute },
            modifier = cardModifier,
        )
    }
}
