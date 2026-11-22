package tech.mmarca.openvitals.features.dashboard

import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher
import tech.mmarca.openvitals.ui.components.shouldShowDashboardHealthConnectPromo
import java.time.LocalDate
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    refreshRequest: Int = 0,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit = {},
    onOpenLog: () -> Unit,
    onStartActivity: () -> Unit,
    onOpenBodyProfile: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    brandTimeline: tech.mmarca.openvitals.ui.components.FluidBrandTimeline? = null,
) {
    // Seeded from the flow's current value: a placeholder frame on return would
    // reset the carousel page and scroll position.
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.scrollPosition.first,
        initialFirstVisibleItemScrollOffset = viewModel.scrollPosition.second,
    )
    androidx.compose.runtime.DisposableEffect(viewModel, listState) {
        onDispose { viewModel.rememberScrollPosition(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmEdit by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    // 70: an edit that ends where it began closes without asking.
    val requestFinishEdit: () -> Unit = {
        if (viewModel.hasDashboardEdits()) confirmEdit = true else viewModel.finishDashboardEdit(false)
    }
    androidx.activity.compose.BackHandler(enabled = state.isEditingDashboard) { requestFinishEdit() }
    // 120: leaving the editor, saved or not, lands at the top of the home screen, not where the
    // editing had scrolled to.
    var wasEditingDashboard by remember { mutableStateOf(state.isEditingDashboard) }
    androidx.compose.runtime.LaunchedEffect(state.isEditingDashboard) {
        if (wasEditingDashboard && !state.isEditingDashboard) {
            // 99: wait for the editor to finish fading out where it stands; scrolling at once
            // brought its top into view for a frame before the dashboard took its place.
            kotlinx.coroutines.delay(tech.mmarca.openvitals.ui.theme.ScreenMotion.FadeThroughOutMillis.toLong())
            listState.scrollToItem(0)
        }
        wasEditingDashboard = state.isEditingDashboard
    }
    if (confirmEdit && state.isEditingDashboard) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmEdit = false },
            title = { androidx.compose.material3.Text(stringResource(R.string.dashboard_save_changes)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.finishDashboardEdit(true); confirmEdit = false }) {
                    androidx.compose.material3.Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.finishDashboardEdit(false); confirmEdit = false }) {
                    androidx.compose.material3.Text(stringResource(R.string.action_discard))
                }
            },
        )
    }
    val history by viewModel.workoutHistory.collectAsStateWithLifecycle()
    val isLoading = state.isLoading
    val error = state.error
    val display = state.display
    val selectedDate = state.selectedDate
    val loadedData = state.data
    var showDatePicker by remember { mutableStateOf(false) }
    val showPromo = shouldShowDashboardHealthConnectPromo(
        availability = state.healthConnectAvailability,
        syncEnabled = state.healthConnectSyncEnabled,
        minimumPermissionsGranted = state.minimumPermissionsGranted,
    )
    var permissionReloadKey by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberHealthConnectPermissionLauncher(
        onResult = {
            permissionReloadKey++
            viewModel.refresh()
        },
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentDay()
    }
    androidx.compose.runtime.LaunchedEffect(refreshRequest) {
        viewModel.onRefreshRequest(refreshRequest)
    }
    val context = LocalContext.current
    val errorMessage = error?.resolve()
    androidx.compose.runtime.LaunchedEffect(errorMessage) {
        // A refresh failure with data on screen is a toast, not the error state.
        if (errorMessage != null && loadedData != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    val profileViewModel: tech.mmarca.openvitals.features.settings.UserProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val profile by profileViewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { profileViewModel.refresh() }
    val profilePhoto = remember(profile.photo) { profile.photo?.let { it.asImageBitmap() } }
    val timeline = brandTimeline ?: tech.mmarca.openvitals.ui.components.rememberFluidBrandTimeline()
    // 94: the toggle greets, fills with today's goals, shows the sync, and opens today's summary.
    // 97: the palette and motion chosen in Display settings.
    val brandLookViewModel: tech.mmarca.openvitals.features.settings.BrandLookViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val paletteSource by brandLookViewModel.palette.collectAsStateWithLifecycle()
    val brandAnimated by brandLookViewModel.animated.collectAsStateWithLifecycle()
    // 149, 150: the tiles the home screen shows, reported by the content once it has drawn them.
    var paletteIds by remember { mutableStateOf<DashboardPaletteIds?>(null) }
    val brandColors = dashboardBrandColors(
        paletteSource,
        state.dashboardWidgets.filter { it in state.placedDashboardWidgets },
        paletteIds,
    )
    // 94: the toggle follows the selected period: its sleep, steps, active calories, and
    // whether it holds a workout.
    // 143: while another period loads, the toggle and its summary wait for it rather than show
    // the period before under the new one's title.
    val awaitingPeriod = loadedData != null && (
        loadedData.date != selectedDate ||
            loadedData.period != state.dashboardPeriod().takeIf {
                state.selectedRange != tech.mmarca.openvitals.core.period.TimeRange.DAY
            }
        )
    val hadWorkout = loadedData?.takeUnless { awaitingPeriod }?.let { it.workouts.isNotEmpty() || it.workout != null }
    val goalDisplay = if (awaitingPeriod) {
        display.copy(widgets = display.widgets.mapValues { (_, model) -> model.copy(isLoading = true) })
    } else {
        display
    }
    val dayGoals = dashboardDayGoals(goalDisplay, brandColors, hadWorkout)
    // 100: no day until the dashboard has loaded, so the greeting starts when the home screen
    // is there to be seen, not behind the opening and the loading.
    val day = brandDay(name = profile.name, goals = dayGoals).takeIf { loadedData != null }
    var showDaySummary by remember { mutableStateOf(false) }
    if (showDaySummary) {
        val period = state.dashboardPeriod()
        val summaryTitle = state.customPeriod?.let { "${it.start} – ${it.end}" }
            ?: tech.mmarca.openvitals.ui.components.localizedPeriodTitle(
                range = state.selectedRange,
                period = period,
                weekPeriodMode = state.activityWeekMode.toWeekPeriodMode(),
            )
        DashboardDaySummarySheet(summaryTitle, dayGoals, onOpenMetric = onOpenMetric, onDismiss = { showDaySummary = false })
    }
    val header: @Composable () -> Unit = {
        DashboardHeader(timeline, profilePhoto,
            onOpenBodyProfile, onOpenAchievements, onOpenSettings,
            day = day, onOpenDay = { showDaySummary = true },
            isEditingDashboard = state.isEditingDashboard,
            // 106: the pencil lives in the header now; it needs a loaded dashboard to edit.
            onToggleDashboardEdit = if (loadedData != null) {
                { if (state.isEditingDashboard) requestFinishEdit() else viewModel.toggleDashboardEdit() }
            } else null,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        tech.mmarca.openvitals.ui.components.LocalFluidBrandTimeline provides timeline,
        tech.mmarca.openvitals.ui.components.LocalBrandLook provides
            tech.mmarca.openvitals.ui.components.BrandLook(brandColors, brandAnimated),
    ) {
    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.DASHBOARD,
        accessGateHeader = {
            androidx.compose.foundation.layout.Box(Modifier.statusBarsPadding()) { header() }
        },
        isLoading = isLoading && loadedData != null,
        refreshKey = refreshRequest to permissionReloadKey,
        // The body renders the sync banner itself; the shell must not add a second.
        showInlineSyncBanner = false,
    ) { hcUx ->
        // One reload, one indicator: the spinner answers the pull only.
        var pullRefreshRequested by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(state.isRefreshing) {
            if (!state.isRefreshing) pullRefreshRequested = false
        }
        PullToRefreshBox(
            isRefreshing = pullRefreshRequested && state.isRefreshing && loadedData != null,
            onRefresh = {
                pullRefreshRequested = true
                viewModel.refresh()
            },
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) {
            when {
                isLoading && loadedData == null -> DashboardPlaceholder(header) { FullScreenLoading() }
                error != null && loadedData == null ->
                    DashboardPlaceholder(header) { ErrorMessage(error?.resolve() ?: stringResource(R.string.unknown_error)) }
                loadedData != null -> DashboardContent(
                    data = loadedData,
                    listState = listState,
                    header = header,
                    earlierWorkouts = history.workouts.takeIf { history.date == loadedData.date && state.selectedRange == tech.mmarca.openvitals.core.period.TimeRange.DAY }.orEmpty(),
                    historyState = history.takeIf { it.date == selectedDate && state.selectedRange == tech.mmarca.openvitals.core.period.TimeRange.DAY },
                    periodState = state,
                    onSelectRange = viewModel::selectRange,
                    onLoadMoreWorkouts = {
                        if (history.error == tech.mmarca.openvitals.core.presentation.ScreenError.PermissionDenied) {
                            permissionLauncher.launch(viewModel.workoutHistoryPermissions)
                        } else viewModel.loadMoreWorkouts()
                    },
                    selectedDate = selectedDate,
                    display = display,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    canGoForward = state.canAdvancePeriod(),
                    isRefreshing = state.isRefreshing && !pullRefreshRequested,
                    syncPaused = hcUx.syncPaused,
                    // Rate-limited is not "no data": the banner counts down to the retry.
                    rateLimitDeadlineEpochMillis = state.rateLimitDeadlineEpochMillis,
                    dashboardWidgets = state.dashboardWidgets,
                    isEditingDashboard = state.isEditingDashboard,
                    sortEmptyTilesLast = state.sortEmptyTilesLast,
                    onPreviousDay = viewModel::previousDay,
                    onNextDay = viewModel::nextDay,
                    onOpenCalendar = { showDatePicker = true },
                    onMoveWidgetToTarget = viewModel::moveDashboardWidgetToTarget,
                    onReorderWidgets = viewModel::setDashboardVisibleOrder,
                    onRemoveWidget = viewModel::removeDashboardWidget,
                    widgetSizes = state.widgetSizes,
                    widgetDetailCounts = state.widgetDetailCounts,
                    onSetWidgetDetailCount = viewModel::setWidgetDetailCount,
                    pagedLayout = state.pagedLayout,
                    onSetWidgetSize = viewModel::setWidgetSize,
                    onSetPagedLayout = viewModel::setPagedLayout,
                    onAddWidget = viewModel::addDashboardWidget,
                    // The OpenWatch tile opens OpenWatch, not a metric screen.
                    onOpenMetric = { id ->
                        if (id == DashboardWidgetId.OPENWATCH) viewModel.openOpenWatch(context) else onOpenMetric(id)
                    },
                    onSyncWatch = viewModel::syncWatchNow,
                    onOpenActivities = onOpenActivities,
                    onOpenActivity = onOpenActivity,
                    onEditActivity = onEditActivity,
                    onDeleteActivity = viewModel::deleteActivityEntry,
                    onOpenLog = onOpenLog,
                    onStartActivity = onStartActivity,
                    onToggleDashboardEdit = { if (state.isEditingDashboard) requestFinishEdit() else viewModel.toggleDashboardEdit() },
                    placedWidgetIds = state.placedDashboardWidgets,
                    onPaletteIds = { paletteIds = it },
                )
                else -> DashboardPlaceholder(header) { ErrorMessage(stringResource(R.string.message_no_dashboard_data)) }
            }
        }
    }
    }

    if (showDatePicker) {
        DashboardPeriodDialog(
            state = state,
            onDismiss = { showDatePicker = false },
            onRange = viewModel::selectRange,
            onDate = { date -> showDatePicker = false; viewModel.selectDate(date) },
            onCustom = { start, end -> showDatePicker = false; viewModel.selectCustomPeriod(start, end) },
        )
    }
}
