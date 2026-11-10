package tech.mmarca.openvitals.data.repository.dashboard

import java.time.ZoneId
import kotlin.math.roundToLong
import tech.mmarca.openvitals.domain.model.*
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/** Range reads, never one dashboard request per day. Snapshots retain their explicit latest/weekly meaning. */
internal class DashboardPeriodLoader(private val hc: HealthConnectManager, private val estimatedCalories: Boolean = false) {
    suspend fun load(
        query: DashboardQuery,
        granted: Set<String>,
        loadDay: suspend (DashboardQuery) -> DashboardData,
    ): DashboardData {
        val period = requireNotNull(query.period)
        val wanted = query.visibleMetrics
        val permitted = wanted.filterTo(mutableSetOf()) { metric ->
            MetricReadPermissions.forMetric(metric, estimatedCalories).all { it in granted }
        }
        val base = loadDay(query.copy(date = period.end, period = null, visibleMetrics = wanted - rangeMetrics))
        val totals = readTotals(query.copy(visibleMetrics = permitted), base)
        return totals.copy(
            date = query.date,
            period = period,
            loadedMetrics = wanted,
            missingPermissions = base.missingPermissions + (wanted intersect rangeMetrics)
                .flatMap { MetricReadPermissions.forMetric(it, estimatedCalories) }
                .filter { it in hc.managedPermissions && it !in granted },
        )
    }

    private suspend fun readTotals(query: DashboardQuery, base: DashboardData): DashboardData {
        val period = requireNotNull(query.period)
        val start = period.start
        val end = period.end
        val metrics = query.visibleMetrics
        fun wants(metric: DashboardMetric) = metric in metrics
        val movement = metrics.any { it in movementMetrics }
        val days = if (movement) hc.readDailySteps(
            start, end,
            includeSteps = wants(DashboardMetric.STEPS),
            includeDistance = wants(DashboardMetric.DISTANCE),
            includeWheelchairPushes = wants(DashboardMetric.WHEELCHAIR_PUSHES),
            includeFloors = wants(DashboardMetric.FLOORS),
            includeActiveCalories = wants(DashboardMetric.ACTIVE_CALORIES),
            includeElevation = wants(DashboardMetric.ELEVATION),
        ) else emptyList()
        val nutrition = if (wants(DashboardMetric.CALORIES_OUT)) hc.readDailyNutrition(
            start, end, includeHydration = false, includeEstimatedCalories = estimatedCalories,
        ) else emptyList()
        val macros = if (metrics.any { it in macroMetrics }) hc.readDailyMacros(start, end) else emptyList()
        val hydration = if (wants(DashboardMetric.HYDRATION)) hc.readDailyHydration(start, end) else emptyList()
        val zone = ZoneId.systemDefault()
        val workouts = if (wants(DashboardMetric.WORKOUT)) hc.readExerciseSessions(
            start.atStartOfDay(zone).toInstant(), end.plusDays(1).atStartOfDay(zone).toInstant(),
        ).sortedByDescending { it.startTime } else emptyList()
        val sleep = if (wants(DashboardMetric.SLEEP)) hc.readDailySleepDurations(start, end, query.sleepWindow)
            .filter { it.durationMs > 0 } else emptyList()
        val heart = if (wants(DashboardMetric.AVG_HEART_RATE)) hc.readDailyHeartRateAverages(start, end)
            .values.filter { it > 0 } else emptyList()
        val resting = if (wants(DashboardMetric.RESTING_HEART_RATE)) hc.readDailyRestingHR(start, end)
            .map { it.bpm }.filter { it > 0 } else emptyList()
        val hrv = if (wants(DashboardMetric.HRV)) hc.readDailyHRV(start, end)
            .map { it.rmssdMs }.filter { it > 0 } else emptyList()
        val mindfulness = if (wants(DashboardMetric.MINDFULNESS)) hc.readMindfulnessSessions(
            start.atStartOfDay(zone).toInstant(), end.plusDays(1).atStartOfDay(zone).toInstant(),
        ).minutesByStartDate(zone).values.sum().toInt() else null
        return base.copy(
            steps = days.sumOf { it.steps },
            distanceMeters = days.sumOf { it.distanceMeters },
            activeCaloriesKcal = days.mapNotNull { it.activeCaloriesKcal }.takeIf { it.isNotEmpty() }?.sum(),
            floorsClimbed = days.mapNotNull { it.floorsClimbed }.takeIf { it.isNotEmpty() }?.sum(),
            elevationGainedMeters = days.mapNotNull { it.elevationGainedMeters }.takeIf { it.isNotEmpty() }?.sum(),
            wheelchairPushes = days.mapNotNull { it.wheelchairPushes }.takeIf { it.isNotEmpty() }?.sum(),
            caloriesKcal = nutrition.sumOf { it.caloriesBurnedKcal },
            caloriesKcalSource = nutrition.firstOrNull { it.hasCaloriesBurnedData }?.caloriesBurnedSource
                ?: CaloriesBurnedSource.NO_DATA,
            caloriesInKcal = macros.takeIf { it.isNotEmpty() }?.sumOf { it.energyKcal },
            proteinGrams = macros.takeIf { it.isNotEmpty() }?.sumOf { it.proteinGrams },
            carbsGrams = macros.takeIf { it.isNotEmpty() }?.sumOf { it.carbsGrams },
            fatGrams = macros.takeIf { it.isNotEmpty() }?.sumOf { it.fatGrams },
            caffeineGrams = macros.takeIf { it.isNotEmpty() }?.sumOf { it.nutrientValues[NutritionNutrient.CAFFEINE] ?: 0.0 },
            hydrationLiters = hydration.sumOf { it.liters },
            workout = workouts.firstOrNull(),
            workouts = workouts,
            sleep = sleep.takeIf { it.isNotEmpty() }?.let { nights ->
                SleepData(
                    id = "dashboard-period-average",
                    startTime = start.atStartOfDay(zone).toInstant(),
                    endTime = end.plusDays(1).atStartOfDay(zone).toInstant(),
                    durationMs = nights.map { it.durationMs }.average().roundToLong(),
                    source = "",
                )
            },
            avgHeartRateBpm = heart.takeIf { it.isNotEmpty() }?.average()?.roundToLong() ?: 0L,
            restingHeartRateBpm = resting.takeIf { it.isNotEmpty() }?.average()?.roundToLong() ?: 0L,
            hrvRmssdMs = hrv.takeIf { it.isNotEmpty() }?.average(),
            mindfulnessMinutes = mindfulness,
        )
    }

    companion object {
        private val movementMetrics = setOf(
            DashboardMetric.STEPS, DashboardMetric.DISTANCE, DashboardMetric.ACTIVE_CALORIES,
            DashboardMetric.FLOORS, DashboardMetric.ELEVATION, DashboardMetric.WHEELCHAIR_PUSHES,
        )
        private val macroMetrics = setOf(
            DashboardMetric.CALORIES_IN, DashboardMetric.PROTEIN, DashboardMetric.CARBS,
            DashboardMetric.FAT, DashboardMetric.CAFFEINE,
        )
        private val rangeMetrics = movementMetrics + macroMetrics + setOf(
            DashboardMetric.CALORIES_OUT, DashboardMetric.HYDRATION, DashboardMetric.WORKOUT,
            DashboardMetric.SLEEP, DashboardMetric.AVG_HEART_RATE, DashboardMetric.RESTING_HEART_RATE,
            DashboardMetric.HRV, DashboardMetric.MINDFULNESS,
        )
    }
}
