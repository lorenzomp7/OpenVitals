package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneId

/** Stable IDs make repeated imports of the same metric/value/interval idempotent. */
internal fun screenshotRecords(values: List<Pair<String, String>>, start: String, end: String, exerciseType: Int? = null): List<Record> {
    val zone = ZoneId.systemDefault()
    val from = LocalDateTime.parse(start).atZone(zone)
    val to = LocalDateTime.parse(end).atZone(zone)
    require(!to.isBefore(from) && !to.toInstant().isAfter(java.time.Instant.now()))
    return values.map { (kind, raw) ->
        val value = raw.replace(',', '.').toDouble()
        require(value.isFinite() && value > 0)
        val key = "$kind|$value|${from.toInstant()}|${to.toInstant()}" +
            if (kind == "duration_min") "|${exerciseType ?: ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT}" else ""
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        val metadata = Metadata.manualEntry(clientRecordId = "screenshot_$digest", clientRecordVersion = 1)
        val time = from.toInstant()
        val offset = from.offset
        when (kind) {
            "kg" -> WeightRecord(time, offset, Mass.kilograms(value), metadata)
            "body_fat" -> { require(value <= 100); BodyFatRecord(time, offset, Percentage(value), metadata) }
            "duration_min" -> {
                val seconds = kotlin.math.round(value * 60).toLong()
                require(seconds > 0 && seconds <= 7 * 24 * 60 * 60)
                require(java.time.Duration.between(from, to).seconds == seconds)
                ExerciseSessionRecord(startTime = time, startZoneOffset = offset,
                    endTime = to.toInstant(), endZoneOffset = to.offset,
                    exerciseType = exerciseType ?: ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, metadata = metadata)
            }
            "bpm" -> {
                require(value % 1.0 == 0.0)
                HeartRateRecord(time, offset, time.plusSeconds(1), offset,
                    listOf(HeartRateRecord.Sample(time, value.toLong())), metadata)
            }
            else -> {
                require(to.isAfter(from))
                when (kind) {
                    "steps" -> {
                        require(value % 1.0 == 0.0 && value <= Long.MAX_VALUE.toDouble())
                        StepsRecord(time, offset, to.toInstant(), to.offset, value.toLong(), metadata)
                    }
                    "km" -> DistanceRecord(time, offset, to.toInstant(), to.offset, Length.kilometers(value), metadata)
                    // A screenshot cannot tell active from total calories: import only after the explicit UI label is confirmed.
                    "kcal" -> TotalCaloriesBurnedRecord(time, offset, to.toInstant(), to.offset, Energy.kilocalories(value), metadata)
                    else -> error("Unsupported screenshot metric")
                }
            }
        }
    }
}

internal fun screenshotPermissions(kinds: List<String>): Set<String> = kinds.map { kind ->
    androidx.health.connect.client.permission.HealthPermission.getWritePermission(when (kind) {
        "duration_min" -> ExerciseSessionRecord::class
        "kg" -> WeightRecord::class
        "body_fat" -> BodyFatRecord::class
        "steps" -> StepsRecord::class
        "km" -> DistanceRecord::class
        "kcal" -> TotalCaloriesBurnedRecord::class
        "bpm" -> HeartRateRecord::class
        else -> error("Unsupported screenshot metric")
    })
}.toSet()
