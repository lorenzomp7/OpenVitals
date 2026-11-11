package tech.mmarca.openvitals.features.dashboard

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepWindow

@Immutable
data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedRange: TimeRange = TimeRange.DAY,
    val data: DashboardData? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: ScreenError? = null,
    val unacknowledgedWidgetPermissions: Set<String> = emptySet(),
    val sleepWindow: SleepWindow = SleepWindow.Default,
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
    val showOpenVitalsCalculatedCalories: Boolean = false,
    val dashboardWidgets: List<DashboardWidgetId> = DefaultDashboardWidgetIds,
    /**
     * The widgets the user placed. Only these keep an unsupported metric in
     * the edit grid; a default-layout one belongs in the add tray.
     */
    val placedDashboardWidgets: Set<DashboardWidgetId> = emptySet(),
    val dailyGoals: DashboardDailyGoals = DashboardDailyGoals(),
    val isEditingDashboard: Boolean = false,
    /** The spaces (1, 2 or 4) chosen for a widget; one left out keeps its own size. */
    val widgetSizes: Map<DashboardWidgetId, Int> = emptyMap(),
    val widgetDetailCounts: Map<DashboardWidgetId, Int> = emptyMap(),
    val pagedLayout: Boolean = false,
    val sortEmptyTilesLast: Boolean = true,
    val healthConnectSyncEnabled: Boolean = true,
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val minimumPermissionsGranted: Boolean = true,
    val display: DashboardDisplayState = DashboardDisplayState(),
    val loadingWidgets: Set<DashboardWidgetId> = emptySet(),
    /** Tiles whose last read Health Connect refused or the backoff skipped. Reloaded alone when it ends. */
    val degradedWidgets: Set<DashboardWidgetId> = emptySet(),
    /** When Health Connect's refusal ends, epoch millis, or null while it is answering. */
    val rateLimitDeadlineEpochMillis: Long? = null,
    val sensorStatus: DashboardSensorStatus = DashboardSensorStatus(),
    /** The OpenWatch tile, from OpenLink; null hides it. */
    val openWatch: tech.mmarca.openvitals.openwatch.OpenWatchTileDisplay? = null,
)

@Immutable
data class DashboardSensorStatus(
    val devices: List<DashboardSensorDeviceStatus> = emptyList(),
) {
    val hasDevices: Boolean
        get() = devices.isNotEmpty()

    val enabledCount: Int
        get() = devices.count { it.enabled }

    val connectedCount: Int
        get() = devices.count { it.connectionStatus == BleConnectionStatus.CONNECTED }

    val lowestBatteryPercent: Int?
        get() = devices.mapNotNull { it.batteryPercent }.minOrNull()
}

@Immutable
data class DashboardSensorDeviceStatus(
    val id: String,
    val displayName: String,
    val enabled: Boolean,
    val connectionStatus: BleConnectionStatus,
    val batteryPercent: Int?,
)

@Immutable
data class DashboardDailyGoals(
    val steps: Double = MetricDailyGoalKey.STEPS.defaultValue,
    val distanceMeters: Double = MetricDailyGoalKey.DISTANCE_METERS.defaultValue,
    val caloriesOutKcal: Double = MetricDailyGoalKey.CALORIES_OUT_KCAL.defaultValue,
    val activeCaloriesKcal: Double = MetricDailyGoalKey.ACTIVE_CALORIES_KCAL.defaultValue,
    val floors: Double = MetricDailyGoalKey.FLOORS.defaultValue,
    val elevationMeters: Double = MetricDailyGoalKey.ELEVATION_METERS.defaultValue,
    val wheelchairPushes: Double = MetricDailyGoalKey.WHEELCHAIR_PUSHES.defaultValue,
    val sleepHours: Double = MetricDailyGoalKey.SLEEP_HOURS.defaultValue,
    val hydrationLiters: Double = 2.0,
    val caloriesInKcal: Double = MetricDailyGoalKey.CALORIES_IN_KCAL.defaultValue,
    val proteinGrams: Double = MetricDailyGoalKey.PROTEIN_GRAMS.defaultValue,
    val carbsGrams: Double = MetricDailyGoalKey.CARBS_GRAMS.defaultValue,
    val fatGrams: Double = MetricDailyGoalKey.FAT_GRAMS.defaultValue,
    val mindfulnessMinutes: Double = MetricDailyGoalKey.MINDFULNESS_MINUTES.defaultValue,
)
