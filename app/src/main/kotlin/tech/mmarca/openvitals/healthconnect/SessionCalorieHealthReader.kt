package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

/**
 * Cumulative energy over a session, bucketed so a session spans about a
 * hundred points. Total calories when any app recorded them, else active.
 */
internal suspend fun HealthConnectReaderSupport.readSessionCalorieSamples(
    start: Instant,
    end: Instant,
    includeTotal: Boolean,
    includeActive: Boolean,
): List<tech.mmarca.openvitals.domain.model.ActivityCalorieSample> {
    if (!start.isBefore(end) || (!includeTotal && !includeActive)) return emptyList()
    return withLogging("readCalorieSamples", emptyList()) {
        val client = client()
        val spanSeconds = Duration.between(start, end).seconds
        val bucket = Duration.ofSeconds((spanSeconds / 100L).coerceIn(30L, 900L))
        val metrics = buildSet {
            if (includeTotal) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            if (includeActive) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
        }
        val buckets = aggregateWindows(start, end, bucket).flatMap { (windowStart, windowEnd) ->
            client.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(windowStart, windowEnd),
                    timeRangeSlicer = bucket,
                ),
            )
        }.sortedBy { it.startTime }
        val useTotal = includeTotal &&
            buckets.any { (it.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0) > 0.0 }
        val useActive = !useTotal && includeActive &&
            buckets.any { (it.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0) > 0.0 }
        if (!useTotal && !useActive) return@withLogging emptyList()
        var cumulative = 0.0
        buildList {
            add(tech.mmarca.openvitals.domain.model.ActivityCalorieSample(start, 0.0, active = useActive))
            buckets.forEach { b ->
                cumulative += if (useTotal) {
                    b.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
                } else {
                    b.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
                }
                val time = if (b.endTime.isAfter(end)) end else b.endTime
                add(tech.mmarca.openvitals.domain.model.ActivityCalorieSample(time, cumulative, active = useActive))
            }
        }
    }
}
