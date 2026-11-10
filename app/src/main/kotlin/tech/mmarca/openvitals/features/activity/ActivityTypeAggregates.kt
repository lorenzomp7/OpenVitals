package tech.mmarca.openvitals.features.activity

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.movingDurationMs

@Immutable
data class ActivityTypeAggregate(
    val exerciseType: Int,
    val count: Int,
    val totalDistanceMeters: Double,
    val totalDurationMs: Long,
    val totalMovingDurationMs: Long,
    val averageMovingSpeedMetersPerSecond: Double?,
    val bestSpeedMetersPerSecond: Double?,
    val totalCaloriesKcal: Double? = null,
    val averageHeartRateBpm: Long? = null,
    val repetitions: Int? = null,

)

internal fun List<ExerciseData>.activityTypeAggregates(): List<ActivityTypeAggregate> =
    groupBy { it.exerciseType }
        .mapNotNull { (exerciseType, workouts) ->
            val count = workouts.size
            if (count == 0) return@mapNotNull null
            val totalDistanceMeters = workouts.sumOf { it.totalDistanceMeters?.takeIf { meters -> meters > 0.0 } ?: 0.0 }
            val totalDurationMs = workouts.sumOf { it.durationMs.coerceAtLeast(0L) }
            val totalMovingDurationMs = workouts.sumOf { it.movingDurationMs().coerceAtLeast(0L) }
            val averageMovingSpeedMetersPerSecond = totalDistanceMeters
                .takeIf { it > 0.0 }
                ?.let { distance ->
                    totalMovingDurationMs
                        .takeIf { it > 0L }
                        ?.let { durationMs -> distance / (durationMs / 1_000.0) }
                }
                ?.takeIf { it.isFinite() && it > 0.0 }
            val bestSpeedMetersPerSecond = workouts
                .mapNotNull { workout ->
                    listOfNotNull(
                        workout.averageSpeedMetersPerSecond?.takeIf { it.isFinite() && it > 0.0 },
                        workout.averageMovingSpeedMetersPerSecond(),
                    ).maxOrNull()
                }
                .maxOrNull()

            ActivityTypeAggregate(
                exerciseType = exerciseType,
                count = count,
                totalCaloriesKcal = workouts.mapNotNull { it.activeCaloriesKcal ?: it.totalCaloriesKcal }
                    .takeIf { it.isNotEmpty() }?.sum(),
                averageHeartRateBpm = workouts.mapNotNull { it.averageHeartRateBpm }
                    .takeIf { it.isNotEmpty() }?.map { it.toDouble() }?.average()?.toLong(),
                repetitions = workouts.sumOf { w -> w.segments.sumOf { it.repetitions } }.takeIf { it > 0 },
                totalDistanceMeters = totalDistanceMeters,
                totalDurationMs = totalDurationMs,
                totalMovingDurationMs = totalMovingDurationMs,
                averageMovingSpeedMetersPerSecond = averageMovingSpeedMetersPerSecond,
                bestSpeedMetersPerSecond = bestSpeedMetersPerSecond,
            )
        }
        .sortedWith(
            compareByDescending<ActivityTypeAggregate> { it.totalDurationMs }
                .thenBy { it.exerciseType },
        )

private fun ExerciseData.averageMovingSpeedMetersPerSecond(): Double? {
    val distanceMeters = totalDistanceMeters?.takeIf { it > 0.0 } ?: return null
    val durationSeconds = movingDurationMs()
        .takeIf { it > 0L }
        ?.let { it / 1_000.0 }
        ?: return null
    return (distanceMeters / durationSeconds).takeIf { it.isFinite() && it > 0.0 }
}

/** Only sports where translating through space is meaningful get speed/distance headlines. */
internal fun activityUsesDistance(type: Int): Boolean = type in setOf(
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_SKATING,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_SKIING,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR,
)
