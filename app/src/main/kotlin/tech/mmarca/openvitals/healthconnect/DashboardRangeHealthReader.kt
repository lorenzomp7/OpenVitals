package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.ZoneId
import tech.mmarca.openvitals.domain.model.*

/** One bounded response for range headlines; no daily buckets or raw heart-rate samples. */
internal class DashboardRangeHealthReader(private val support: HealthConnectReaderSupport) {
    suspend fun read(query: DashboardQuery, base: DashboardData): DashboardData {
        val period = requireNotNull(query.period)
        val wanted = query.visibleMetrics
        val metrics = buildSet<AggregateMetric<*>> {
            if (DashboardMetric.STEPS in wanted) add(StepsRecord.COUNT_TOTAL)
            if (DashboardMetric.DISTANCE in wanted) add(DistanceRecord.DISTANCE_TOTAL)
            if (DashboardMetric.ACTIVE_CALORIES in wanted) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (DashboardMetric.FLOORS in wanted) add(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL)
            if (DashboardMetric.ELEVATION in wanted) add(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)
            if (DashboardMetric.WHEELCHAIR_PUSHES in wanted) add(WheelchairPushesRecord.COUNT_TOTAL)
            if (DashboardMetric.CALORIES_OUT in wanted) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            if (DashboardMetric.HYDRATION in wanted) add(HydrationRecord.VOLUME_TOTAL)
            if (DashboardMetric.AVG_HEART_RATE in wanted) add(HeartRateRecord.BPM_AVG)
            if (DashboardMetric.RESTING_HEART_RATE in wanted) add(RestingHeartRateRecord.BPM_AVG)
            if (wanted.any { it in setOf(DashboardMetric.CALORIES_IN, DashboardMetric.PROTEIN, DashboardMetric.CARBS,
                    DashboardMetric.FAT, DashboardMetric.CAFFEINE) }) {
                add(NutritionRecord.ENERGY_TOTAL)
                add(NutritionRecord.PROTEIN_TOTAL)
                add(NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL)
                add(NutritionRecord.TOTAL_FAT_TOTAL)
                add(NutritionRecord.CAFFEINE_TOTAL)
            }
        }
        if (metrics.isEmpty()) return base
        val zone = ZoneId.systemDefault()
        return support.withLogging("readDashboardRangeTotals", base) {
            val result = support.client().aggregate(AggregateRequest(metrics = metrics,
                timeRangeFilter = TimeRangeFilter.between(period.start.atStartOfDay(zone).toInstant(),
                    period.end.plusDays(1).atStartOfDay(zone).toInstant())))
            base.copy(
                steps = result[StepsRecord.COUNT_TOTAL] ?: 0L,
                distanceMeters = result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0,
                activeCaloriesKcal = result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories,
                floorsClimbed = result[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL]?.toInt(),
                elevationGainedMeters = result[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters,
                wheelchairPushes = result[WheelchairPushesRecord.COUNT_TOTAL],
                caloriesKcal = result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0,
                caloriesKcalSource = if (result[TotalCaloriesBurnedRecord.ENERGY_TOTAL] != null)
                    CaloriesBurnedSource.RECORDED_TOTAL else CaloriesBurnedSource.NO_DATA,
                hydrationLiters = result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0,
                caloriesInKcal = result[NutritionRecord.ENERGY_TOTAL]?.inKilocalories,
                proteinGrams = result[NutritionRecord.PROTEIN_TOTAL]?.inGrams,
                carbsGrams = result[NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL]?.inGrams,
                fatGrams = result[NutritionRecord.TOTAL_FAT_TOTAL]?.inGrams,
                caffeineGrams = result[NutritionRecord.CAFFEINE_TOTAL]?.inGrams,
                avgHeartRateBpm = result[HeartRateRecord.BPM_AVG] ?: 0L,
                restingHeartRateBpm = result[RestingHeartRateRecord.BPM_AVG] ?: 0L,
            )
        }
    }
}
