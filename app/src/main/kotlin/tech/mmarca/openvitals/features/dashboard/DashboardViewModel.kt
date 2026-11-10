package tech.mmarca.openvitals.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.AppForegroundGate
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.BleRecordingMetrics
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.dashboardMetricLoadGroups
import tech.mmarca.openvitals.domain.model.mergeLoaded
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.usecase.LoadDashboardDayUseCase
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.sync.BodyEnergyChainSyncService
import tech.mmarca.openvitals.data.sync.HistorySyncScheduler
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler
import java.time.LocalDate
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.healthconnect.withHealthConnectReadOutcome
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val loadDashboardDayUseCase: LoadDashboardDayUseCase,
    private val repository: HealthRepository,
    private val prefs: PreferencesRepository,
    private val unitFormatter: UnitFormatter,
    private val dateTimeFormatterProvider: DateTimeFormatterProvider,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val activityRepository: ActivityRepository? = null,
    private val bodyEnergyRepository: BodyEnergyRepository? = null,
    private val bleDeviceRepository: BleDeviceRepository? = null,
    private val bleSensorCoordinator: BleSensorCoordinator? = null,
    private val historySyncScheduler: HistorySyncScheduler? = null,
    private val homeWidgetRefreshScheduler: HomeWidgetRefreshScheduler? = null,
    private val chainSyncService: BodyEnergyChainSyncService? = null,
    private val medicalRecords: MedicalRecordsRepository? = null,
    private val foregroundGate: AppForegroundGate? = null,
    private val openWatchLink: tech.mmarca.openvitals.openwatch.OpenWatchLink? = null,
) : ViewModel() {

    val minimumOnboardingPermissions get() = repository.minimumOnboardingPermissions

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            selectedRange = prefs.timeRangeFor(tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey.DASHBOARD),
            // Appends widgets added by an update to a layout saved before them.
            dashboardWidgets = dashboardWidgetIdsWithNewOnesAppended(
                storedIds = prefs.dashboardWidgetOrder(),
                knownIds = prefs.dashboardKnownWidgetIds(),
                persist = { order, known ->
                    order?.let(prefs::setDashboardWidgetOrder)
                    prefs.setDashboardKnownWidgetIds(known)
                },
            ),
            widgetSizes = prefs.dashboardWidgetSizes().mapNotNull { (name, spaces) ->
                DashboardWidgetId.entries.firstOrNull { it.name == name }?.let { it to spaces }
            }.toMap(),
            pagedLayout = prefs.dashboardPagedLayout(),
            placedDashboardWidgets = prefs.dashboardWidgetOrder()
                ?.let { stored -> dashboardWidgetIdsFromStored(stored).toSet() }
                .orEmpty(),
            dailyGoals = prefs.dashboardDailyGoals(),
            sleepWindow = prefs.sleepWindow,
            activityWeekMode = prefs.activityWeekMode,
            showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories,
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val periodDriver = tech.mmarca.openvitals.core.period.PeriodSelectionDriver(
        initialRange = _uiState.value.selectedRange,
        initialWeekPeriodMode = prefs.activityWeekMode.toWeekPeriodMode(),
        onRangeSelected = { prefs.setTimeRangeFor(tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey.DASHBOARD, it) },
    )
    private val loadCoordinator = LoadCoordinator()
    private var userPinnedPastDay = false
    private var permissionPromptDismissedForLoad = false
    private val history = DashboardWorkoutHistory { start, end ->
        activityRepository?.loadWorkouts(start, end).orEmpty()
    }
    internal val workoutHistory = history.state
    val workoutHistoryPermissions get() = repository.corePermissions + repository.additionalDataAccessPermissions.filter { it.endsWith("READ_HEALTH_DATA_HISTORY") }
    fun loadMoreWorkouts() { viewModelScope.launch { history.loadMore() } }
    private var loadGeneration = 0L

    /** Serialises publishing: each merge is a read-modify-write of the shared state. */
    private val publishMutex = Mutex()

    /**
     * The day being loaded, or null once settled. A NORMAL request for the
     * same day is absorbed; FORCE and date changes restart.
     */
    private var inFlightLoadDate: LocalDate? = null
    private var lastSettledKey: Pair<LocalDate, TimeRange>? = null
    private var lastSettledAt = 0L


    init {
        observeOpenWatch()
        observeSensorStatus()
        observeBodyEnergyRebuild()
        observeRateLimit()
        load(_uiState.value.selectedDate)
    }

    /** The banner counts down from the deadline; when it passes, the degraded tiles are asked for again. */
    private fun observeRateLimit() {
        viewModelScope.launch {
            repository.rateLimitDeadlineEpochMillis.collect { deadline ->
                _uiState.update { state -> state.copy(rateLimitDeadlineEpochMillis = deadline.takeIf { it > 0L }) }
            }
        }
        DashboardRateLimitRecovery(
            deadlines = repository.rateLimitDeadlineEpochMillis,
            canReload = {
                _uiState.value.degradedWidgets.isNotEmpty() &&
                    inFlightLoadDate == null &&
                    foregroundGate?.isForeground != false
            },
            reload = ::reloadDegradedWidgets,
        ).start(viewModelScope)
    }

    /** Reloads only the tiles whose reads were refused; the others keep what they have. */
    private fun reloadDegradedWidgets() {
        val degraded = _uiState.value.degradedWidgets
        if (degraded.isEmpty()) return
        forceLoad(_uiState.value.selectedDate, RefreshMode.NORMAL, onlyWidgets = degraded)
    }

    private fun observeBodyEnergyRebuild() {
        val service = chainSyncService ?: return
        viewModelScope.launch {
            // The rebuild dropped today's Body Energy row; reload so the card re-chains.
            service.chainRebuilt.collect {
                forceLoad(_uiState.value.selectedDate, RefreshMode.NORMAL)
            }
        }
    }

    private var consumedRefreshRequest = 0
    fun onRefreshRequest(request: Int) {
        if (request <= consumedRefreshRequest) return
        consumedRefreshRequest = request
        refresh()
    }

    fun refresh() {
        load(_uiState.value.selectedDate, RefreshMode.FORCE)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** The OpenWatch tile follows OpenLink's state; a change rebuilds the grid around it. */
    private fun observeOpenWatch() {
        val link = openWatchLink ?: return
        _uiState.update { it.copy(openWatch = link.tile.value) }
        viewModelScope.launch {
            link.tile.collect { tile ->
                if (tile != _uiState.value.openWatch) {
                    _uiState.update { it.copy(openWatch = tile) }
                    rebuildDisplay()
                }
            }
        }
    }

    /** The tile's sync button: the watch it shows, through OpenWatch. */
    fun syncWatchNow() {
        val deviceId = _uiState.value.openWatch?.deviceId ?: return
        openWatchLink?.requestSync(deviceId)
    }

    /** The tile itself: OpenWatch, or its consent screen while it waits. */
    fun openOpenWatch(context: android.content.Context) {
        openWatchLink?.open(context)
    }

    private fun observeSensorStatus() {
        val deviceRepository = bleDeviceRepository ?: return
        val metricsFlow = bleSensorCoordinator?.metrics ?: flowOf(BleRecordingMetrics())
        viewModelScope.launch {
            combine(
                deviceRepository.devicesFlow,
                metricsFlow,
            ) { devices, metrics ->
                devices.toDashboardSensorStatus(metrics.deviceStatuses)
            }.collect { sensorStatus ->
                _uiState.update { it.copy(sensorStatus = sensorStatus) }
            }
        }
    }

    fun deleteActivityEntry(entryId: String) {
        if (entryId.isBlank()) return
        val activityRepository = activityRepository ?: return
        val entry = _uiState.value.data?.workouts.orEmpty()
            .plus(_uiState.value.data?.workout)
            .filterNotNull()
            .firstOrNull { it.id == entryId } ?: return
        if (!entry.isOpenVitalsEntry) return

        viewModelScope.launch {
            runCatching {
                activityRepository.deleteActivityEntry(entryId)
            }.onSuccess {
                refresh()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.toScreenError(R.string.screen_error_delete_activity),
                )
            }
        }
    }

    fun refreshPreferences() {
        val sleepWindow = prefs.sleepWindow
        val activityWeekMode = prefs.activityWeekMode
        val showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories
        val dailyGoals = prefs.dashboardDailyGoals()
        val current = _uiState.value
        val sleepRangeChanged = current.sleepWindow != sleepWindow
        val activityWeekModeChanged = current.activityWeekMode != activityWeekMode
        val calorieModeChanged = current.showOpenVitalsCalculatedCalories != showOpenVitalsCalculatedCalories
        if (
            sleepRangeChanged ||
            activityWeekModeChanged ||
            calorieModeChanged ||
            current.dailyGoals != dailyGoals
        ) {
            viewModelScope.launch {
                val display = current.data?.let { data ->
                    buildDisplay(data, dailyGoals, current.loadingWidgets)
                } ?: current.display
                _uiState.value = current.copy(
                    sleepWindow = sleepWindow,
                    activityWeekMode = activityWeekMode,
                    showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
                    dailyGoals = dailyGoals,
                    display = display,
                )
            }
        }
        if (sleepRangeChanged || activityWeekModeChanged || calorieModeChanged) {
            // Bypasses the in-flight dedupe: the running load read old preferences.
            forceLoad(current.selectedDate, RefreshMode.NORMAL)
        }
    }

    fun resumeCurrentDay() {
        refreshPreferences()
        // Reload whenever the user returns to today: ON_RESUME is the only signal
        // that Health Connect data may have changed.
        if (!userPinnedPastDay) {
            load(LocalDate.now())
        }
    }

    fun load(date: LocalDate, refreshMode: RefreshMode = RefreshMode.NORMAL) {
        val clampedDate = date.coerceAtMost(LocalDate.now())
        if (refreshMode == RefreshMode.NORMAL && clampedDate == inFlightLoadDate) return
        if (refreshMode == RefreshMode.NORMAL &&
            lastSettledKey == (clampedDate to _uiState.value.selectedRange) &&
            System.currentTimeMillis() - lastSettledAt < 300_000L &&
            _uiState.value.data != null && _uiState.value.degradedWidgets.isEmpty()
        ) return
        forceLoad(date = clampedDate, refreshMode = refreshMode)
    }

    private fun forceLoad(
        date: LocalDate,
        refreshMode: RefreshMode,
        /** A partial pass: only these tiles are read and marked loading. Null reads the whole day. */
        onlyWidgets: Set<DashboardWidgetId>? = null,
    ) {
        val clampedDate = date.coerceAtMost(LocalDate.now())
        periodDriver.weekPeriodMode = prefs.activityWeekMode.toWeekPeriodMode()
        periodDriver.selectDate(clampedDate)
        // The date flips before any read, so the navigator answers instantly.
        _uiState.value = _uiState.value.copy(
            selectedDate = clampedDate,
            sortEmptyTilesLast = prefs.dashboardSortEmptyTilesLast,
        )
        if (onlyWidgets == null && _uiState.value.selectedRange == TimeRange.DAY) history.reset(clampedDate)
        val generation = ++loadGeneration
        inFlightLoadDate = clampedDate
        loadCoordinator.launch(viewModelScope) load@{
            try {
                runLoadPass(
                    clampedDate = clampedDate,
                    refreshMode = refreshMode,
                    generation = generation,
                    onlyWidgets = onlyWidgets,
                )
            } finally {
                // A superseded job must not clear the newer load's marker.
                if (isCurrent) inFlightLoadDate = null
            }
        }
    }

    private suspend fun LoadCoordinator.LoadScope.runLoadPass(
        clampedDate: LocalDate,
        refreshMode: RefreshMode,
        generation: Long,
        onlyWidgets: Set<DashboardWidgetId>? = null,
    ) {
        val sleepWindow = prefs.sleepWindow
        val activityWeekMode = prefs.activityWeekMode
        val showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories
        val dailyGoals = prefs.dashboardDailyGoals()
        permissionPromptDismissedForLoad = false
        val current = _uiState.value
        val availability = repository.availability()
        val granted = if (availability == HealthConnectAvailability.AVAILABLE) {
            repository.grantedPermissions()
        } else {
            emptySet()
        }
        _uiState.value = current.copy(
            selectedDate = clampedDate,
            isRefreshing = true,
            error = null,
            sleepWindow = sleepWindow,
            activityWeekMode = activityWeekMode,
            showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
            dailyGoals = dailyGoals,
            healthConnectSyncEnabled = prefs.healthConnectSyncEnabled,
            healthConnectAvailability = availability,
            minimumPermissionsGranted = repository.minimumOnboardingPermissions.all { it in granted },
            loadingWidgets = emptySet(),
        )
        // A partial pass reads the tiles it was given and leaves the others as they are.
        val widgets = _uiState.value.dashboardWidgets.filter { onlyWidgets == null || it in onlyWidgets }
        // Widget order is load order: first-screen tiles read first.
        val orderedMetrics = widgets.mapNotNull { it.toDashboardMetricOrNull() }.distinct()
        val groups = if (current.selectedRange == TimeRange.DAY) dashboardMetricLoadGroups(orderedMetrics)
            else listOf(orderedMetrics.toSet()).filter { it.isNotEmpty() }
        val metricWidgetIds = widgets.filter { it.toDashboardMetricOrNull() != null }.toSet()
        val wantsBodyEnergy = bodyEnergyRepository != null &&
            DashboardWidgetId.BODY_ENERGY in widgets

        // Nothing here may await a metric: the screen shows "loading" tiles at once.
        val requestedPeriod = current.dashboardPeriod().takeIf { current.selectedRange != TimeRange.DAY }
        val existingData = current.data?.takeIf { it.date == clampedDate && it.period == requestedPeriod }
        val hadExistingData = existingData != null
        val seed = (existingData ?: DashboardData(date = clampedDate, period = requestedPeriod))
            // A reload re-establishes these; the merge can only add to the set. A partial pass keeps them.
            .let { if (onlyWidgets == null) it.copy(missingPermissions = emptySet()) else it }
        publishDashboardData(
            data = seed,
            // Keep the loading/sort contract even when a previous answer is cached.
            loadingWidgets = metricWidgetIds + listOfNotNull(
                DashboardWidgetId.BODY_ENERGY.takeIf { wantsBodyEnergy },
            ),
            sleepWindow = sleepWindow,
            activityWeekMode = activityWeekMode,
            goals = dailyGoals,
        )

        // Children of the coordinator's job, so the next load cancels them.
        val failures = coroutineScope {
            val metricPasses = groups.map { group ->
                async {
                    loadMetricGroup(
                        metrics = group,
                        date = clampedDate,
                        sleepWindow = sleepWindow,
                        activityWeekMode = activityWeekMode,
                        generation = generation,
                    )
                }
            }
            if (onlyWidgets == null && current.selectedRange == TimeRange.DAY) {
                launch { history.loadMore() }
            }
            if (wantsBodyEnergy) {
                launch {
                    loadBodyEnergy(
                        date = clampedDate,
                        refreshMode = refreshMode,
                        generation = generation,
                    )
                }
            }
            metricPasses.awaitAll()
        }

        if (!isCurrent) return
        // One metric failing is that tile's problem. Every metric failing is the screen's.
        val firstFailure = failures.filterNotNull().firstOrNull()
        if (onlyWidgets == null && firstFailure != null && failures.size == groups.size && failures.none { it == null }) {
            _uiState.value = _uiState.value.copy(
                error = firstFailure.toScreenError(R.string.screen_error_generic),
                // Nothing shown before: the error is the whole screen. Otherwise a toast.
                data = if (hadExistingData) _uiState.value.data else null,
                isLoading = false,
                isRefreshing = false,
            )
        }
        if (failures.all { it == null } && _uiState.value.degradedWidgets.isEmpty()) {
            lastSettledKey = clampedDate to _uiState.value.selectedRange
            lastSettledAt = System.currentTimeMillis()
        }
        // Once per app open, after the load settles: the background passes. Not after a
        // load Health Connect refused; those reads come first when it answers again.
        historySyncScheduler?.let { scheduler ->
            val cleanLoad = failures.all { it == null } && repository.rateLimitRetryAfterMillis() == 0L
            viewModelScope.launch {
                // Three services in a row. Their bookkeeping stays off Main.
                runCatching { withContext(dispatchers.default) { scheduler.drainIncrementalOnce(cleanLoad) } }
            }
        }
        // The home screen shows today, so a settled read of today is its cue.
        if (clampedDate == LocalDate.now()) homeWidgetRefreshScheduler?.refreshNow()
    }

    /**
     * Loads one group of metrics and merges it in. A failed group is not an
     * error state; [rateLimitedRetryAfterMinutes] separates "no data" from
     * "Health Connect will not answer".
     */
    private suspend fun loadMetricGroup(
        metrics: Set<DashboardMetric>,
        date: LocalDate,
        sleepWindow: SleepWindow,
        activityWeekMode: ActivityWeekMode,
        generation: Long,
    ): Throwable? {
        val widgetIds = _uiState.value.dashboardWidgets
            .filter { it.toDashboardMetricOrNull() in metrics }
            .toSet()
        val query = DashboardQuery(
            date = date,
            period = _uiState.value.dashboardPeriod().takeIf { _uiState.value.selectedRange != TimeRange.DAY },
            sleepWindow = sleepWindow,
            activityWeekMode = activityWeekMode,
            visibleMetrics = metrics,
            // Nothing is gated on a pass any more, so the tile can afford these reads.
            includeHistoricalBaselines = _uiState.value.selectedRange == TimeRange.DAY,
            includeWeeklyTrainingSignals = DashboardMetric.WEEKLY_CARDIO_LOAD in metrics,
        )
        var failure: Throwable? = null
        var degraded = false
        val loaded = try {
            withHealthConnectReadOutcome { loadDashboardDayUseCase(query) }
                .also { degraded = it.degraded }
                .value
        } catch (error: CancellationException) {
            if (!currentCoroutineContext().isActive) throw error
            failure = error
            null
        } catch (error: Throwable) {
            failure = error
            null
        }
        publishMerged(generation = generation, date = date, clearing = widgetIds, degraded = degraded) { current ->
            if (loaded == null) current else current.mergeLoaded(loaded)
        }
        return failure
    }

    private suspend fun loadBodyEnergy(
        date: LocalDate,
        refreshMode: RefreshMode,
        generation: Long,
    ) {
        val repository = bodyEnergyRepository ?: return
        var degraded = false
        val timeline = try {
            withHealthConnectReadOutcome {
                repository.loadTimeline(
                    BodyEnergyTimelineQuery(
                        period = _uiState.value.dashboardPeriod().end.let { DatePeriod(it, it) },
                        range = TimeRange.DAY,
                        refreshMode = refreshMode,
                    )
                ).latestDay
            }.also { degraded = it.degraded }.value
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            null
        }
        publishMerged(
            generation = generation,
            date = date,
            clearing = setOf(DashboardWidgetId.BODY_ENERGY),
            degraded = degraded,
        ) { current ->
            if (timeline == null) current else current.copy(bodyEnergyTimeline = timeline)
        }
    }

    /** Merges one finished pass into the state under [publishMutex]. [degraded]: its reads were refused. */
    private suspend fun publishMerged(
        generation: Long,
        date: LocalDate,
        clearing: Set<DashboardWidgetId>,
        degraded: Boolean = false,
        merge: (DashboardData) -> DashboardData,
    ) {
        publishMutex.withLock {
            if (generation != loadGeneration) return
            val state = _uiState.value
            if (state.selectedDate != date) return
            val currentData = state.data?.takeIf { it.date == date } ?: return
            _uiState.update {
                it.copy(degradedWidgets = if (degraded) it.degradedWidgets + clearing else it.degradedWidgets - clearing)
            }
            publishDashboardData(
                data = merge(currentData),
                loadingWidgets = state.loadingWidgets - clearing,
                sleepWindow = state.sleepWindow,
                activityWeekMode = state.activityWeekMode,
                goals = state.dailyGoals,
            )
        }
    }

    private suspend fun publishDashboardData(
        data: DashboardData,
        loadingWidgets: Set<DashboardWidgetId>,
        sleepWindow: SleepWindow,
        activityWeekMode: ActivityWeekMode,
        goals: DashboardDailyGoals,
    ) {
        // A reload that found nothing new keeps the old instances, or every
        // animation replays on every sync.
        val current = _uiState.value
        val stableData = if (data == current.data) current.data ?: data else data
        val builtDisplay = buildDisplay(stableData, goals, loadingWidgets)
        val display = if (builtDisplay == current.display) current.display else builtDisplay
        _uiState.value = _uiState.value.copy(
            data = stableData,
            isLoading = false,
            // Done when the last tile stops loading.
            isRefreshing = loadingWidgets.isNotEmpty(),
            unacknowledgedWidgetPermissions = unacknowledgedWidgetPermissions(data.missingPermissions),
            sleepWindow = sleepWindow,
            activityWeekMode = activityWeekMode,
            showOpenVitalsCalculatedCalories = prefs.showOpenVitalsCalculatedCalories,
            dailyGoals = goals,
            display = display,
            loadingWidgets = loadingWidgets,
        )
    }

    fun selectRange(range: TimeRange) {
        if (range == _uiState.value.selectedRange) return
        periodDriver.selectRange(range)
        _uiState.update { it.copy(selectedRange = range) }
        userPinnedPastDay = _uiState.value.dashboardPeriod().end.isBefore(LocalDate.now())
        forceLoad(_uiState.value.selectedDate, RefreshMode.NORMAL)
    }

    fun previousDay() = selectDate(periodDriver.previousPeriod().selectedDate)

    fun nextDay() {
        periodDriver.nextPeriod()?.let { selectDate(it.selectedDate) }
    }

    fun selectDate(date: LocalDate) {
        val clampedDate = periodDriver.selectDate(date).selectedDate
        userPinnedPastDay = tech.mmarca.openvitals.core.period.periodFor(
            _uiState.value.selectedRange, clampedDate, weekPeriodMode = prefs.activityWeekMode.toWeekPeriodMode(),
        ).end.isBefore(LocalDate.now())
        load(clampedDate)
    }

    fun acknowledgeWidgetMissingPermissions() {
        val missing = _uiState.value.unacknowledgedWidgetPermissions
        if (missing.isEmpty()) return
        prefs.acknowledgePermissionsFor(HealthConnectFeature.DASHBOARD, missing)
        permissionPromptDismissedForLoad = true
        _uiState.value = _uiState.value.copy(unacknowledgedWidgetPermissions = emptySet())
    }

    private fun unacknowledgedWidgetPermissions(missingPermissions: Set<String>): Set<String> =
        if (permissionPromptDismissedForLoad) emptySet()
        else missingPermissions - prefs.acknowledgedPermissionsFor(HealthConnectFeature.DASHBOARD)

    fun setWidgetSize(widgetId: DashboardWidgetId, spaces: Int) {
        if (spaces !in listOf(1, 2, 4)) return
        val sizes = _uiState.value.widgetSizes + (widgetId to spaces)
        prefs.setDashboardWidgetSizes(sizes.mapKeys { it.key.name })
        _uiState.value = _uiState.value.copy(widgetSizes = sizes)
    }

    fun setPagedLayout(paged: Boolean) {
        prefs.setDashboardPagedLayout(paged)
        _uiState.value = _uiState.value.copy(pagedLayout = paged)
    }

    fun toggleDashboardEdit() {
        val editing = !_uiState.value.isEditingDashboard
        _uiState.value = _uiState.value.copy(isEditingDashboard = editing)
        // Edit mode materialises unsupported metrics; rebuild the display, same data.
        rebuildDisplay()
    }

    private fun rebuildDisplay() {
        val current = _uiState.value
        val data = current.data ?: return
        viewModelScope.launch {
            val display = buildDisplay(data, current.dailyGoals, current.loadingWidgets)
            _uiState.update { it.copy(display = display) }
        }
    }

    fun removeDashboardWidget(widgetId: DashboardWidgetId) {
        updateDashboardWidgets(_uiState.value.dashboardWidgets - widgetId)
    }

    fun addDashboardWidget(widgetId: DashboardWidgetId) {
        val current = _uiState.value.dashboardWidgets
        // Recording the placement keeps an unsupported metric in the grid.
        updateDashboardWidgets(if (widgetId in current) current else current + widgetId)
    }

    fun moveDashboardWidget(widgetId: DashboardWidgetId, offset: Int) {
        val current = _uiState.value.dashboardWidgets
        val fromIndex = current.indexOf(widgetId)
        if (fromIndex == -1) return

        val toIndex = (fromIndex + offset).coerceIn(current.indices)
        if (fromIndex == toIndex) return

        updateDashboardWidgets(
            current.toMutableList().apply {
                removeAt(fromIndex)
                add(toIndex, widgetId)
            }
        )
    }

    fun moveDashboardWidgetToTarget(widgetId: DashboardWidgetId, targetWidgetId: DashboardWidgetId) {
        val current = _uiState.value.dashboardWidgets
        val fromIndex = current.indexOf(widgetId)
        val targetIndex = current.indexOf(targetWidgetId)
        if (fromIndex == -1 || targetIndex == -1 || fromIndex == targetIndex) return

        // The same list the grid lays out: an unsupported metric the user never placed is in the
        // tray, not the rows. Counting it here put the section boundary one tile off the screen's.
        val state = _uiState.value
        val fixedWidgetIds = current.filterNot {
            it == DashboardWidgetId.WORKOUT ||
                (it in state.display.unsupportedIds && it !in state.placedDashboardWidgets)
        }.take(3)
        val fromFixedSection = widgetId in fixedWidgetIds
        val targetFixedSection = targetWidgetId in fixedWidgetIds
        val updated = current.toMutableList().apply {
            this[fromIndex] = targetWidgetId
            this[targetIndex] = widgetId
        }

        updateDashboardWidgets(updated)
    }

    private fun updateDashboardWidgets(widgets: List<DashboardWidgetId>) {
        val customizableWidgets = customizableDashboardWidgetIds(widgets)
        prefs.setDashboardWidgetOrder(customizableWidgets.map { it.name })
        _uiState.value = _uiState.value.copy(
            dashboardWidgets = customizableWidgets,
            placedDashboardWidgets = customizableWidgets.toSet(),
        )
    }

    private suspend fun buildDisplay(
        data: DashboardData,
        dailyGoals: DashboardDailyGoals,
        loadingWidgets: Set<DashboardWidgetId> = emptySet(),
    ): DashboardDisplayState = withContext(dispatchers.default) {
        DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals.forPeriod(data.period),
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            loadingWidgets = loadingWidgets,
            degradedWidgets = _uiState.value.degradedWidgets,
            bodyEnergySetupCompleted = prefs.bodyEnergyCalibration().setupCompleted,
            // Only edit mode shows unsupported metrics, in the add tray.
            includeUnsupported = _uiState.value.isEditingDashboard,
            medicalRecordsAvailable = medicalRecords?.isAvailable() == true,
            openWatch = _uiState.value.openWatch,
        )
            // A reload marks every tile loading. Without the last answers, the sort would reshuffle.
            .withLastDemoted(_uiState.value.display.lastDemoted)
    }
}
