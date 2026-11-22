package tech.mmarca.openvitals.features.activity

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.performance.LoadCoordinator
import tech.mmarca.openvitals.core.performance.runCatchingCancellable
import tech.mmarca.openvitals.data.repository.ActivityMarkerRepository
import tech.mmarca.openvitals.data.repository.contract.ActivitySplitPreferences
import tech.mmarca.openvitals.data.repository.contract.BodyProfilePreferences
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.data.repository.contract.CoMapsNavigationRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.domain.insights.ActivitySplits
import tech.mmarca.openvitals.domain.insights.HeartRateRecoveryReading
import tech.mmarca.openvitals.domain.insights.buildActivitySplits
import tech.mmarca.openvitals.domain.insights.calculateHeartRateRecovery
import tech.mmarca.openvitals.domain.insights.heartRateRecoveryWindowFor
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ActivityCalorieSample
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.domain.model.withSampleBackfilledMetrics
import tech.mmarca.openvitals.domain.preferences.ActivitySplitDistance
import tech.mmarca.openvitals.navigation.ACTIVITY_DETAIL_ID_ARG
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
internal data class ActivityDetailUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val workout: ExerciseData? = null,
    val heartRateSamples: List<HeartRateSample> = emptyList(),
    val speedSamples: List<SpeedSample> = emptyList(),
    val cadenceSamples: List<ActivityCadenceSample> = emptyList(),

    /** Running energy total over the session; empty without calorie data. */
    val calorieSamples: List<ActivityCalorieSample> = emptyList(),

    /** Energy per slice of the session, from heart rate when it can be, else from recorded calories. */
    val sessionCalories: tech.mmarca.openvitals.domain.insights.SessionCalories? = null,

    /** Heart rate cannot be read: the chart slot offers the grant instead. */
    val heartRateAccessMissing: Boolean = false,
    val markers: List<ActivityRecordingMarker> = emptyList(),

    /** CoMaps guidance saved beside the activity; app-local, empty for most. */
    val coMapsSamples: List<CoMapsNavigationSnapshot> = emptyList(),

    /** Splits cut against the user's split-distance preference. */
    val splits: ActivitySplits = ActivitySplits.none(),

    /** The distance the derived splits were cut at, for the card headers. */
    val splitDistanceMeters: Double = ActivitySplitDistance.defaultMeters,

    /** The slowest and fastest split in s/km, the pace-bar scale. Null leaves the bars unpainted. */
    val slowestSplitPaceSeconds: Double? = null,
    val fastestSplitPaceSeconds: Double? = null,

    /** Speed rebuilt from the splits, for a session that recorded none. */
    val splitSpeedTrace: ActivitySplitSpeedTrace? = null,

    /** The height profile of the session, oldest first. */
    val elevationSamples: List<ActivityElevationSample> = emptyList(),

    /** Where [elevationSamples] came from. */
    val elevationSource: tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource =
        tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource.GPS,

    /** How the heart rate fell after the effort stopped, for a session that carries a stop mark. */
    val heartRateRecovery: HeartRateRecoveryReading? = null,

    /** The heart rate read around the end, for the recovery chart. */
    val recoverySamples: List<tech.mmarca.openvitals.domain.model.HeartRateSample> = emptyList(),

    /** The heart rate the zones are shares of, from the profile or the age. */
    val maxHeartRateBpm: Int? = null,

    /** The rate an estimated recovery falls toward, from the profile. */
    val restingHeartRateBpm: Int? = null,

    /** The session's cardio load (Banister TRIMP), as the Cardio load screen counts it. */
    val sessionCardioLoad: Int? = null,

    /** The records this session holds among those of its kind in the year up to it. */
    val personalRecords: List<tech.mmarca.openvitals.domain.insights.PersonalRecord> = emptyList(),

    /** How many earlier sessions of the kind the records were measured against. */
    val recordsComparedWith: Int = 0,

    /** The plan this session was logged against, when it still exists and can be read. */
    val linkedPlan: PlannedExerciseData? = null,

    /** The session is shown and its samples are still being read: the charts' place is held. */
    val samplesLoading: Boolean = false,

    val error: ScreenError? = null,
)

private typealias TerrainRead = Pair<tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource, List<Double>?>

/** The route's terrain heights as a profile; null when the terrain did not cover it. */
private fun terrainSamples(workout: ExerciseData, read: TerrainRead?): List<ActivityElevationSample>? =
    read?.second?.let { heights ->
        workout.route.points.zip(heights) { point, meters -> ActivityElevationSample(point.time, meters) }.sortedBy { it.time }
    }

/** How far back the records look. */
private const val RecordsWindowDays = 365L

@HiltViewModel
internal class ActivityDetailViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val heartRepository: HeartRepository? = null,
    private val markerRepository: ActivityMarkerRepository? = null,
    private val bodyProfilePreferences: BodyProfilePreferences,
    private val splitPreferences: ActivitySplitPreferences,
    private val coMapsNavigationRepository: CoMapsNavigationRepository? = null,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    savedStateHandle: SavedStateHandle,
    private val terrainElevation: tech.mmarca.openvitals.features.activity.elevation.RouteTerrainElevation? = null,
    private val recordsCache: ActivityRecordsCache = ActivityRecordsCache(),
) : ViewModel() {

    /** The workout the route opened. Blank when it named none: the not-found state. */
    private val activityId: String = savedStateHandle[ACTIVITY_DETAIL_ID_ARG] ?: ""

    private val _uiState = MutableStateFlow(ActivityDetailUiState())
    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()
    private val loadCoordinator = LoadCoordinator()
    private var pendingTerrain: kotlinx.coroutines.Deferred<TerrainRead?>? = null

    init {
        load()
        observeSplitDistance()
    }

    fun load() {
        if (activityId.isBlank()) {
            _uiState.value = ActivityDetailUiState(
                isLoading = false,
                error = ScreenError.MissingArgument,
            )
            return
        }

        loadCoordinator.launch(viewModelScope) load@{
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatchingCancellable { loadDetail() }
                .onSuccess { loaded ->
                    if (!isCurrent) return@load
                    _uiState.value = loaded
                    val workout = loaded.workout ?: return@load
                    coroutineScope {
                        // A terrain read that outlasted the charts swaps their heights in place.
                        pendingTerrain?.let { pending ->
                            launch {
                                val read = pending.await()
                                pendingTerrain = null
                                if (isCurrent && read != null) _uiState.update {
                                    it.copy(
                                        elevationSamples = terrainSamples(workout, read) ?: it.elevationSamples,
                                        elevationSource = read.first,
                                    )
                                }
                            }
                        }
                        // The records come after, from a year of the kind; the screen does not wait.
                        launch {
                            runCatchingCancellable { loadRecords(workout) }.getOrNull()?.let { (records, compared) ->
                                if (isCurrent) {
                                    _uiState.update { it.copy(personalRecords = records, recordsComparedWith = compared) }
                                }
                            }
                        }
                    }
                }
                .onFailure {
                    if (!isCurrent) return@load
                    _uiState.value = ActivityDetailUiState(
                        isLoading = false,
                        error = it.toScreenError(R.string.screen_error_load_activity),
                    )
                }
        }
    }

    /**
     * The records [workout] holds among the sessions of its kind in the year up to it. A
     * refused or skipped read leaves the card out, as any other missing figure.
     */
    private suspend fun loadRecords(
        workout: ExerciseData,
    ): Pair<List<tech.mmarca.openvitals.domain.insights.PersonalRecord>, Int>? {
        val zone = java.time.ZoneId.systemDefault()
        val end = workout.endTime.atZone(zone).toLocalDate()
        val start = end.minusDays(RecordsWindowDays)
        // Read once from the year before this session up to today, so every later session
        // of the kind opened next is answered from memory instead of a year of reads each.
        val sessions = recordsCache.get(workout.exerciseType, start, end) ?: run {
            val readUntil = maxOf(end, LocalDate.now(zone))
            repository.loadWorkoutsOfType(workout.exerciseType, start, readUntil)
                .also { recordsCache.put(workout.exerciseType, start, readUntil, it) }
                .filter { !it.startTime.atZone(zone).toLocalDate().isAfter(end) }
        }
        val earlier = sessions.filter { it.id != workout.id && !it.startTime.isAfter(workout.startTime) }
        if (earlier.isEmpty()) return null
        return withContext(dispatchers.default) {
            tech.mmarca.openvitals.domain.insights.personalRecords(workout, earlier) to earlier.size
        }
    }

    /** The session first. The reads that hang on it then run together. */
    private suspend fun loadDetail(): ActivityDetailUiState {
        val splitDistanceMeters = currentSplitDistanceMeters()
        val loadedWorkout = repository.loadWorkout(activityId)
            ?: return ActivityDetailUiState(isLoading = false, error = ScreenError.NotFound)
                .withSplitsCutAt(splitDistanceMeters)
        val workout = activityDetailWorkout(loadedWorkout)
        // The summary at once, with the charts' place held so nothing moves when they come.
        _uiState.value = ActivityDetailUiState(isLoading = false, workout = workout, samplesLoading = true)
            .withSplitsCutAt(splitDistanceMeters)
        // The terrain is a network read; it runs beside the Health Connect reads and
        // the charts never wait for it: the cleaned GPS draws until it lands.
        pendingTerrain = if (workout.route.status == tech.mmarca.openvitals.domain.model.ExerciseRouteStatus.DATA) {
            viewModelScope.async { runCatchingCancellable { terrainElevation?.altitudes(workout.route.points) }.getOrNull() }
        } else {
            null
        }
        return coroutineScope {
            // Speed, cadence, markers and recovery degrade to empty on failure.
            // The session and the heart-rate read do not.
            val heartRateSamples = async {
                heartRepository?.loadHeartRateSamples(workout.startTime, workout.endTime).orEmpty()
            }
            val speedSamples = async {
                runCatchingCancellable {
                    repository.loadSpeedSamples(workout.startTime, workout.endTime)
                }.getOrDefault(emptyList())
                    // A trainer without a wheel sensor writes zeros: that is no speed, not 0 km/h.
                    .takeIf { hasMeaningfulSpeed(it) }
                    .orEmpty()
            }
            val calorieSamples = async {
                runCatchingCancellable {
                    repository.loadCalorieSamples(workout.startTime, workout.endTime)
                }.getOrDefault(emptyList())
            }
            val heartRateAccess = async {
                runCatchingCancellable { heartRepository?.hasHeartRateReadAccess() ?: true }
                    .getOrDefault(true)
            }
            val cadenceSamples = async {
                runCatchingCancellable {
                    repository.loadActivityCadenceSamples(workout.startTime, workout.endTime)
                }.getOrDefault(emptyList())
            }
            val markers = async {
                runCatchingCancellable { loadMarkers(workout) }.getOrNull().orEmpty()
            }
            val coMapsSamples = async(dispatchers.io) {
                runCatchingCancellable { loadCoMapsSamples(workout) }.getOrNull().orEmpty()
            }
            val heartRateRecovery = async {
                runCatchingCancellable { loadHeartRateRecovery(workout) }.getOrNull()
            }
            val linkedPlan = async {
                workout.plannedExerciseSessionId?.let { planId ->
                    runCatchingCancellable { repository.loadPlannedWorkout(planId) }.getOrNull()
                }
            }
            val heartRate = heartRateSamples.await()
            val speed = speedSamples.await()
            val cadence = cadenceSamples.await()
            val calories = calorieSamples.await()
            val hasHeartRateAccess = heartRateAccess.await()
            val loadedMarkers = markers.await()
            val loadedCoMapsSamples = coMapsSamples.await()
            val recovery = heartRateRecovery.await()
            // The two minutes after the end, read only for a session with a heart rate.
            val recoverySamples = if (heartRate.isEmpty()) emptyList() else runCatchingCancellable {
                heartRepository?.loadHeartRateSamples(
                    workout.endTime.minusSeconds(15),
                    workout.endTime.plus(tech.mmarca.openvitals.domain.insights.recoveryChartWindow).plusSeconds(10),
                )
            }.getOrNull().orEmpty()
            val plan = linkedPlan.await()
            // The terrain's heights if they are already here; else they replace the GPS later.
            val terrainRead = pendingTerrain?.takeIf { it.isCompleted }?.await()
                ?.also { pendingTerrain = null }
            // Backfill, height profile and splits walk every sample.
            val profile = runCatchingCancellable { bodyProfilePreferences.bodyProfile() }.getOrNull()
            withContext(dispatchers.default) {
                val sessionCalories = tech.mmarca.openvitals.domain.insights.sessionCalories(
                    start = workout.startTime,
                    end = workout.endTime,
                    heartRate = heartRate,
                    recorded = calories,
                    weightKg = profile?.weightKg,
                    ageYears = profile?.ageYears(),
                    sex = profile?.sex,
                )
                val backfilledWorkout = workout.withSampleBackfilledMetrics(
                    heartRateSamples = heartRate,
                    speedSamples = speed,
                    cadenceSamples = cadence,
                )
                ActivityDetailUiState(
                    isLoading = false,
                    workout = backfilledWorkout,
                    heartRateSamples = heartRate,
                    speedSamples = speed,
                    cadenceSamples = cadence,
                    calorieSamples = calories,
                    sessionCalories = sessionCalories,
                    heartRateAccessMissing = heartRate.isEmpty() && !hasHeartRateAccess,
                    markers = loadedMarkers,
                    coMapsSamples = loadedCoMapsSamples,
                    elevationSamples = terrainSamples(workout, terrainRead) ?: cleanGpsElevation(elevationProfile(backfilledWorkout.route)),
                    elevationSource = terrainRead?.first
                        ?: tech.mmarca.openvitals.features.activity.elevation.RouteElevationSource.GPS,
                    heartRateRecovery = recovery,
                    recoverySamples = recoverySamples,
                    maxHeartRateBpm = tech.mmarca.openvitals.domain.insights.workoutMaxHeartRate(
                        profile?.maxHeartRateBpm,
                        profile?.ageYears(),
                    ),
                    restingHeartRateBpm = profile?.restingHeartRateBpm,
                    sessionCardioLoad = tech.mmarca.openvitals.domain.insights.calculateCardioLoad(
                        steps = null,
                        samples = heartRate,
                        restingHeartRate = profile?.restingHeartRateBpm?.toLong(),
                        baselineRestingHeartRate = null,
                        observedMaxHeartRate = null,
                        activityWindows = listOf(
                            tech.mmarca.openvitals.domain.insights.CardioLoadTimeWindow(workout.startTime, workout.endTime),
                        ),
                        ageYears = profile?.ageYears(),
                        explicitMaxHeartRate = profile?.maxHeartRateBpm?.toLong(),
                        sex = profile?.sex,
                    ).takeIf { it.usedTrimp }?.score,
                    linkedPlan = plan,
                ).withSplitsCutAt(splitDistanceMeters)
            }
        }
    }

    fun deleteActivity(onDeleted: () -> Unit = {}) {
        val workout = _uiState.value.workout ?: return
        if (!workout.isOpenVitalsEntry || workout.id.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            runCatching {
                repository.deleteActivityEntry(workout.id)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isDeleting = false, workout = null)
                onDeleted()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = error.toScreenError(R.string.screen_error_delete_activity),
                )
            }
        }
    }

    /** The recording marks of [workout], looked up by id and then by client id. */
    private suspend fun loadMarkers(workout: ExerciseData): List<ActivityRecordingMarker> =
        markerRepository?.markersForActivity(workout.id).orEmpty()
            .ifEmpty {
                workout.clientRecordId
                    ?.let { markerRepository?.markersForActivity(it) }
                    .orEmpty()
            }

    /** The saved CoMaps guidance of [workout], looked up like the markers are. */
    private fun loadCoMapsSamples(workout: ExerciseData): List<CoMapsNavigationSnapshot> =
        coMapsNavigationRepository?.loadSamples(workout.id).orEmpty()
            .ifEmpty {
                workout.clientRecordId
                    ?.let { coMapsNavigationRepository?.loadSamples(it) }
                    .orEmpty()
            }

    /** The recovery reading, or null without a stop mark, in which case no read is issued. */
    private suspend fun loadHeartRateRecovery(workout: ExerciseData): HeartRateRecoveryReading? {
        val heartRepository = heartRepository ?: return null
        val window = heartRateRecoveryWindowFor(workout) ?: return null
        val samples = heartRepository.loadHeartRateSamples(window.readStart, window.readEnd)
        val profile = bodyProfilePreferences.bodyProfile()
        return calculateHeartRateRecovery(
            recoveryStart = window.recoveryStart,
            samples = samples,
            restingHeartRateBpm = profile.restingHeartRateBpm,
            ageYears = profile.ageYears(),
            // The 90-day observed maximum is a trend-screen concern.
            observedMaxHeartRateBpm = null,
            explicitMaxHeartRateBpm = profile.maxHeartRateBpm,
            sex = profile.sex,
        )
    }

    private fun currentSplitDistanceMeters(): Double =
        ActivitySplitDistance.normalize(
            splitPreferences.activitySplitDistanceMeters,
        )

    /** Re-cuts the splits when the preference changes. A state update only. */
    private fun observeSplitDistance() {
        viewModelScope.launch {
            splitPreferences.activitySplitDistanceMetersFlow.collect { meters ->
                val normalized = ActivitySplitDistance.normalize(meters)
                val state = _uiState.value
                if (state.splitDistanceMeters == normalized && state.workout == null) return@collect
                val recut = withContext(dispatchers.default) { state.withSplitsCutAt(normalized) }
                // A load that finished meanwhile already cut its own splits at this distance.
                _uiState.update { current -> if (current === state) recut else current }
            }
        }
    }

    /** The state with its splits re-cut at [splitDistanceMeters]. */
    private fun ActivityDetailUiState.withSplitsCutAt(
        splitDistanceMeters: Double,
    ): ActivityDetailUiState {
        val workout = workout ?: return copy(
            splitDistanceMeters = splitDistanceMeters,
            splits = ActivitySplits.none(),
            slowestSplitPaceSeconds = null,
            fastestSplitPaceSeconds = null,
            splitSpeedTrace = null,
        )
        val routePoints = workout.route
            .takeIf { it.status == ExerciseRouteStatus.DATA }
            ?.points
            .orEmpty()
        val splits = buildActivitySplits(
            workout = workout,
            routePoints = routePoints,
            speedSamples = speedSamples,
            heartRateSamples = heartRateSamples,
            splitDistanceMeters = splitDistanceMeters,
        )
        return copy(
            splitDistanceMeters = splitDistanceMeters,
            splits = splits,
            slowestSplitPaceSeconds = slowestSplitPaceSeconds(splits),
            fastestSplitPaceSeconds = fastestSplitPaceSeconds(splits),
            splitSpeedTrace = splitSpeedTrace(recordedSpeed = speedSamples, splits = splits),
        )
    }
}
