package tech.mmarca.openvitals.features.activity

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import tech.mmarca.openvitals.ui.components.renderOrderedMetricDetailSections
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.domain.insights.ActivitySplits
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryReading
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import kotlinx.coroutines.launch

@Composable
internal fun ActivityDetailScreen(
    viewModel: ActivityDetailViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditActivity: (String) -> Unit = {},
    onDeleteActivity: () -> Unit = {},
    onOpenPlan: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportScope = rememberCoroutineScope()
    val error = state.error
    val workout = state.workout
    val latestWorkout by rememberUpdatedState(workout)
    val heartRatePermissionLauncher = rememberHealthConnectPermissionLauncher(onResult = viewModel::load)
    fun showRouteExportFailure() {
        Toast.makeText(
            context,
            R.string.activity_route_export_failed,
            Toast.LENGTH_LONG,
        ).show()
    }
    // The picker answers on the main thread. Encoding a long route there froze the screen.
    fun saveRouteExport(format: ActivityRouteExportFormat, destination: Uri?) {
        val currentWorkout = latestWorkout ?: return
        if (destination == null) return
        exportScope.launch {
            context.saveActivityRouteExport(
                workout = currentWorkout,
                format = format,
                destination = destination,
            )
                .onSuccess {
                    Toast.makeText(
                        context,
                        R.string.activity_route_export_saved,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                .onFailure { showRouteExportFailure() }
        }
    }
    val saveGpxRoute = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ActivityRouteExportFormat.GPX.mimeType),
    ) { uri ->
        saveRouteExport(ActivityRouteExportFormat.GPX, uri)
    }
    val saveKmzRoute = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ActivityRouteExportFormat.KMZ.mimeType),
    ) { uri ->
        saveRouteExport(ActivityRouteExportFormat.KMZ, uri)
    }
    fun launchRouteExport(format: ActivityRouteExportFormat) {
        val currentWorkout = latestWorkout ?: return
        val fileName = currentWorkout.routeExportFileName(format)
        when (format) {
            ActivityRouteExportFormat.GPX -> saveGpxRoute.launch(fileName)
            ActivityRouteExportFormat.KMZ -> saveKmzRoute.launch(fileName)
        }
    }
    val latestSeries by rememberUpdatedState(
        ActivityWorkoutSeries(
            heartRate = state.heartRateSamples,
            speed = state.speedSamples,
            cadence = state.cadenceSamples,
        ),
    )
    fun showWorkoutExportFailure() {
        Toast.makeText(
            context,
            R.string.activity_workout_export_failed,
            Toast.LENGTH_LONG,
        ).show()
    }
    fun saveWorkoutExport(format: ActivityWorkoutExportFormat, destination: Uri?) {
        val currentWorkout = latestWorkout ?: return
        if (destination == null) return
        val series = latestSeries
        exportScope.launch {
            context.saveActivityWorkoutExport(
                workout = currentWorkout,
                series = series,
                format = format,
                destination = destination,
            )
                .onSuccess {
                    Toast.makeText(
                        context,
                        R.string.activity_workout_export_saved,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                .onFailure { showWorkoutExportFailure() }
        }
    }
    val saveTcxWorkout = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ActivityWorkoutExportFormat.TCX.mimeType),
    ) { uri -> saveWorkoutExport(ActivityWorkoutExportFormat.TCX, uri) }
    val saveCsvWorkout = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ActivityWorkoutExportFormat.CSV.mimeType),
    ) { uri -> saveWorkoutExport(ActivityWorkoutExportFormat.CSV, uri) }
    val saveFitWorkout = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ActivityWorkoutExportFormat.FIT.mimeType),
    ) { uri -> saveWorkoutExport(ActivityWorkoutExportFormat.FIT, uri) }
    val saveFitWithRouteWorkout = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ActivityWorkoutExportFormat.FIT_WITH_ROUTE.mimeType),
    ) { uri -> saveWorkoutExport(ActivityWorkoutExportFormat.FIT_WITH_ROUTE, uri) }
    fun launchWorkoutExport(format: ActivityWorkoutExportFormat) {
        val currentWorkout = latestWorkout ?: return
        val fileName = currentWorkout.workoutExportFileName(format)
        when (format) {
            ActivityWorkoutExportFormat.TCX -> saveTcxWorkout.launch(fileName)
            ActivityWorkoutExportFormat.CSV -> saveCsvWorkout.launch(fileName)
            ActivityWorkoutExportFormat.FIT -> saveFitWorkout.launch(fileName)
            ActivityWorkoutExportFormat.FIT_WITH_ROUTE -> saveFitWithRouteWorkout.launch(fileName)
        }
    }
    fun shareWorkout(format: ActivityWorkoutExportFormat) {
        val currentWorkout = latestWorkout ?: return
        val series = latestSeries
        exportScope.launch {
            context.shareActivityWorkout(
                workout = currentWorkout,
                series = series,
                format = format,
            )
                .onFailure {
                    Toast.makeText(
                        context,
                        R.string.activity_workout_share_failed,
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }
    }
    fun shareRoute(format: ActivityRouteExportFormat) {
        val currentWorkout = latestWorkout ?: return
        exportScope.launch {
            context.shareActivityRoute(workout = currentWorkout, format = format)
                .onFailure {
                    Toast.makeText(
                        context,
                        R.string.activity_route_share_failed,
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }
    }

    // 113: the slider icon in the app bar arranges the charts, as on the metric screens.
    val chartOrdering = tech.mmarca.openvitals.core.presentation.rememberMetricDetailSectionOrdering()
    when {
        state.isLoading -> FullScreenLoading()
        error != null -> ErrorMessage(message = error.resolve().orEmpty())
        workout != null -> ActivityDetailContent(
            workout = workout,
            heartRateSamples = state.heartRateSamples,
            heartRateRecovery = state.heartRateRecovery,
            recoverySamples = state.recoverySamples,
            maxHeartRateBpm = state.maxHeartRateBpm,
            restingHeartRateBpm = state.restingHeartRateBpm,
            speedSamples = state.speedSamples,
            cadenceSamples = state.cadenceSamples,
            sessionCalories = state.sessionCalories,
            heartRateAccessMissing = state.heartRateAccessMissing,
            onGrantHeartRate = {
                heartRatePermissionLauncher.launch(
                    setOf(androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                        androidx.health.connect.client.records.HeartRateRecord::class,
                    )),
                )
            },
            markers = state.markers,
            coMapsSamples = state.coMapsSamples,
            splits = state.splits,
            splitDistanceMeters = state.splitDistanceMeters,
            slowestSplitPaceSeconds = state.slowestSplitPaceSeconds,
            fastestSplitPaceSeconds = state.fastestSplitPaceSeconds,
            splitSpeedTrace = state.splitSpeedTrace,
            elevationSamples = state.elevationSamples,
            elevationSource = state.elevationSource,
            isDeleting = state.isDeleting,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onEditActivity = onEditActivity,
            onDeleteActivity = { viewModel.deleteActivity(onDeleteActivity) },
            linkedPlanTitle = state.linkedPlan?.title,
            onOpenPlan = onOpenPlan,
            onOpenRouteInMap = {
                exportScope.launch {
                    context.openActivityRouteInMap(workout)
                        .onFailure {
                            Toast.makeText(
                                context,
                                R.string.activity_route_open_failed,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                }
            },
            onSaveRouteAsGpx = { launchRouteExport(ActivityRouteExportFormat.GPX) },
            onSaveRouteAsKmz = { launchRouteExport(ActivityRouteExportFormat.KMZ) },
            onShareRouteAsGpx = { shareRoute(ActivityRouteExportFormat.GPX) },
            onShareRouteAsKmz = { shareRoute(ActivityRouteExportFormat.KMZ) },
            onSaveWorkoutAsTcx = { launchWorkoutExport(ActivityWorkoutExportFormat.TCX) },
            onSaveWorkoutAsCsv = { launchWorkoutExport(ActivityWorkoutExportFormat.CSV) },
            onSaveWorkoutAsFit = { launchWorkoutExport(ActivityWorkoutExportFormat.FIT) },
            onShareWorkoutAsTcx = { shareWorkout(ActivityWorkoutExportFormat.TCX) },
            onShareWorkoutAsCsv = { shareWorkout(ActivityWorkoutExportFormat.CSV) },
            onShareWorkoutAsFit = { shareWorkout(ActivityWorkoutExportFormat.FIT) },
            onSaveWorkoutAsFitWithRoute = { launchWorkoutExport(ActivityWorkoutExportFormat.FIT_WITH_ROUTE) },
            onShareWorkoutAsFitWithRoute = { shareWorkout(ActivityWorkoutExportFormat.FIT_WITH_ROUTE) },
            sectionContext = chartOrdering,
            sessionCardioLoad = state.sessionCardioLoad,
            personalRecords = state.personalRecords,
            recordsComparedWith = state.recordsComparedWith,
        )
    }
}

@Composable
internal fun ActivityDetailContent(
    workout: ExerciseData,
    heartRateSamples: List<HeartRateSample>,
    heartRateRecovery: HeartRateRecoveryReading?,
    recoverySamples: List<tech.mmarca.openvitals.domain.model.HeartRateSample> = emptyList(),
    maxHeartRateBpm: Int? = null,
    restingHeartRateBpm: Int? = null,
    speedSamples: List<SpeedSample>,
    cadenceSamples: List<ActivityCadenceSample>,
    markers: List<ActivityRecordingMarker>,
    sessionCalories: tech.mmarca.openvitals.domain.insights.SessionCalories? = null,
    heartRateAccessMissing: Boolean = false,
    onGrantHeartRate: () -> Unit = {},
    coMapsSamples: List<CoMapsNavigationSnapshot> = emptyList(),
    splits: ActivitySplits,
    splitDistanceMeters: Double,
    slowestSplitPaceSeconds: Double?,
    fastestSplitPaceSeconds: Double?,
    splitSpeedTrace: ActivitySplitSpeedTrace?,
    elevationSamples: List<ActivityElevationSample>,
    elevationSource: tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource =
        tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource.GPS,
    isDeleting: Boolean,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditActivity: (String) -> Unit,
    onDeleteActivity: () -> Unit,
    linkedPlanTitle: String? = null,
    onOpenPlan: (String) -> Unit = {},
    onOpenRouteInMap: () -> Unit,
    onSaveRouteAsGpx: () -> Unit,
    onSaveRouteAsKmz: () -> Unit,
    onShareRouteAsGpx: () -> Unit,
    onShareRouteAsKmz: () -> Unit,
    onSaveWorkoutAsTcx: () -> Unit = {},
    onSaveWorkoutAsCsv: () -> Unit = {},
    onSaveWorkoutAsFit: () -> Unit = {},
    onShareWorkoutAsTcx: () -> Unit = {},
    onShareWorkoutAsCsv: () -> Unit = {},
    onShareWorkoutAsFit: () -> Unit = {},
    onSaveWorkoutAsFitWithRoute: () -> Unit = {},
    onShareWorkoutAsFitWithRoute: () -> Unit = {},
    sessionCardioLoad: Int? = null,
    personalRecords: List<tech.mmarca.openvitals.domain.insights.PersonalRecord> = emptyList(),
    recordsComparedWith: Int = 0,
    /** 113: the arrangement of the charts; null shows them in the default order, unarrangeable. */
    sectionContext: tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext? = null,
) {
    val context = LocalContext.current
    val sections = sectionContext ?: rememberFixedActivityChartSections()
    val reorderableCharts = rememberActivityChartReordering(sections)
    LazyColumn(
        state = sections.listState.lazyListState,
        contentPadding = PaddingValues(vertical = 8.dp),
        // 113: while a chart is dragged the list must not scroll under the finger by itself.
        userScrollEnabled = !reorderableCharts.isAnyItemDragging,
    ) {
        item {
            WorkoutSummaryCard(
                workout = workout,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onManageDataSources = { openHealthConnectPermissionSettings(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            MetricsCard(
                workout = workout,
                unitFormatter = unitFormatter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        workout.plannedExerciseSessionId?.let { planId ->
            item {
                ActivityLinkedPlanDetailCard(
                    planTitle = linkedPlanTitle,
                    onOpenPlan = { onOpenPlan(planId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        // 113: the charts, in the order the visitor arranged them (the slider icon at the top).
        renderOrderedMetricDetailSections(sections) {
            activityDetailChartSections(
                workout = workout,
                heartRateSamples = heartRateSamples,
                heartRateAccessMissing = heartRateAccessMissing,
                onGrantHeartRate = onGrantHeartRate,
                heartRateRecovery = heartRateRecovery,
                recoverySamples = recoverySamples,
                maxHeartRateBpm = maxHeartRateBpm,
                restingHeartRateBpm = restingHeartRateBpm,
                speedSamples = speedSamples,
                cadenceSamples = cadenceSamples,
                sessionCalories = sessionCalories,
                splits = splits,
                splitDistanceMeters = splitDistanceMeters,
                slowestSplitPaceSeconds = slowestSplitPaceSeconds,
                fastestSplitPaceSeconds = fastestSplitPaceSeconds,
                splitSpeedTrace = splitSpeedTrace,
                elevationSamples = elevationSamples,
                elevationSource = elevationSource,
                unitFormatter = unitFormatter,
                sessionCardioLoad = sessionCardioLoad,
                personalRecords = personalRecords,
                recordsComparedWith = recordsComparedWith,
            )
        }
        item {
            SessionDetailsCard(
                workout = workout,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (workout.segments.isNotEmpty()) item {
            SegmentsCard(
                segments = workout.segments,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        // 53: no route data, no route card.
        if (workout.route.status == ExerciseRouteStatus.DATA && workout.route.points.isNotEmpty()) item {
            RouteCard(
                route = workout.route,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenRouteInMap = onOpenRouteInMap,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            // Route formats only with a route; metric formats always.
            val hasExportableRoute = workout.route.status == ExerciseRouteStatus.DATA &&
                workout.route.points.isNotEmpty()
            WorkoutExportCard(
                onSaveAsTcx = onSaveWorkoutAsTcx,
                onSaveAsCsv = onSaveWorkoutAsCsv,
                onSaveAsFit = onSaveWorkoutAsFit,
                onShareAsTcx = onShareWorkoutAsTcx,
                onShareAsCsv = onShareWorkoutAsCsv,
                onShareAsFit = onShareWorkoutAsFit,
                onSaveRouteAsGpx = onSaveRouteAsGpx.takeIf { hasExportableRoute },
                onSaveRouteAsKmz = onSaveRouteAsKmz.takeIf { hasExportableRoute },
                onShareRouteAsGpx = onShareRouteAsGpx.takeIf { hasExportableRoute },
                onShareRouteAsKmz = onShareRouteAsKmz.takeIf { hasExportableRoute },
                onSaveRouteAsFit = onSaveWorkoutAsFitWithRoute.takeIf { hasExportableRoute },
                onShareRouteAsFit = onShareWorkoutAsFitWithRoute.takeIf { hasExportableRoute },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            )
        }
        item {
            ActivityRouteAnalysisCard(
                workout = workout,
                markers = markers,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (coMapsSamples.isNotEmpty()) {
            item {
                ActivityCoMapsNavigationCard(
                    samples = coMapsSamples,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        if (workout.isOpenVitalsEntry && workout.id.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OpenVitalsButton(
                        onClick = { onEditActivity(workout.id) },
                        enabled = !isDeleting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.cd_edit_entry),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    OpenVitalsOutlinedButton(
                        onClick = onDeleteActivity,
                        enabled = !isDeleting,
                        buttonColors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.action_delete),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** Heart rate access is off: the BPM chart's place offers the grant. */
@Composable
internal fun ActivityHeartRateAccessCard(
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    tech.mmarca.openvitals.ui.components.OpenVitalsCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.activity_recording_live_heart_rate),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.activity_heart_rate_access_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OpenVitalsButton(onClick = onGrant) {
                Text(text = stringResource(R.string.action_grant_permission))
            }
        }
    }
}
