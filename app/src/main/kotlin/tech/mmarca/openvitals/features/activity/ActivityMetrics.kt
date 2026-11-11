package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.movingDurationMs

internal fun ExerciseData.averageSpeed(unitFormatter: UnitFormatter): DisplayValue? {
    val distanceMeters = totalDistanceMeters?.takeIf { it.isFinite() && it > 0.0 }
        ?: return averageSpeedMetersPerSecond?.takeIf { it.isFinite() && it > 0.0 }?.let(unitFormatter::speed)
    val movingDurationMs = movingDurationMs().takeIf { it > 0L } ?: return null
    return unitFormatter.averageSpeed(distanceMeters, movingDurationMs)
}

internal fun ExerciseData.averagePace(unitFormatter: UnitFormatter): DisplayValue? {
    val distanceMeters = totalDistanceMeters?.takeIf { it.isFinite() && it > 0.0 }
        ?: return averageSpeedMetersPerSecond?.takeIf { it.isFinite() && it > 0.0 }
            ?.let { unitFormatter.averagePace(it, 1_000L) }
    val movingDurationMs = movingDurationMs().takeIf { it > 0L } ?: return null
    return unitFormatter.averagePace(distanceMeters, movingDurationMs)
}
