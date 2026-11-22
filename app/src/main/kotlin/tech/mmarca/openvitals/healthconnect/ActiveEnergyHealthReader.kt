package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.domain.insights.ActivityEnergyBucket

/**
 * The steps and the recorded active energy of [start]..[end] in slices of [bucket], for
 * active calories that count the steps ([tech.mmarca.openvitals.domain.insights.activeCaloriesWithSteps]).
 * A metric without its permission reads as zero in every slice. Null when the read failed.
 */
internal suspend fun HealthConnectReaderSupport.readActiveEnergyBuckets(
    start: Instant,
    end: Instant,
    bucket: Duration,
    includeSteps: Boolean,
    includeActive: Boolean,
): List<ActivityEnergyBucket>? {
    if (!start.isBefore(end) || (!includeSteps && !includeActive)) return emptyList()
    return withNullableLogging("readActiveEnergyBuckets") {
        val metrics = buildSet<AggregateMetric<*>> {
            if (includeSteps) add(StepsRecord.COUNT_TOTAL)
            if (includeActive) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
        }
        aggregateWindows(start, end, bucket).flatMap { (windowStart, windowEnd) ->
            client().aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(windowStart, windowEnd),
                    timeRangeSlicer = bucket,
                ),
            )
        }.map { slice ->
            ActivityEnergyBucket(
                steps = slice.result[StepsRecord.COUNT_TOTAL] ?: 0L,
                activeKcal = slice.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0,
            )
        }
    }
}

/**
 * Fifteen-minute slices up to a week, hourly ones up to a month, three-hour ones beyond: a
 * year stays within a couple of dozen requests.
 */
internal fun activeEnergyBucketFor(start: Instant, end: Instant): Duration {
    val span = Duration.between(start, end)
    return when {
        span <= Duration.ofDays(8) -> Duration.ofMinutes(15)
        span <= Duration.ofDays(35) -> Duration.ofHours(1)
        else -> Duration.ofHours(3)
    }
}

/** The active calories of [start]..[end] with the steps counted, slice by slice. */
internal suspend fun HealthConnectReaderSupport.readActiveCaloriesWithSteps(
    start: Instant,
    end: Instant,
    includeSteps: Boolean,
    includeActive: Boolean,
    kcalPerStep: Double,
): Double? = readActiveEnergyBuckets(start, end, activeEnergyBucketFor(start, end), includeSteps, includeActive)
    ?.let { tech.mmarca.openvitals.domain.insights.activeCaloriesWithSteps(it, kcalPerStep) }
