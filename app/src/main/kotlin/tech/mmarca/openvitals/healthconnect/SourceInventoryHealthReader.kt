package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import kotlin.reflect.KClass
import tech.mmarca.openvitals.domain.model.SourceInventory
import tech.mmarca.openvitals.domain.model.SourceInventoryRow

/**
 * The "what does each source write" table of the diagnostics: for each data type OpenVitals
 * reads, the latest record time per source app over [windowDays].
 *
 * One page of records per type, newest first, through the shared support (pacer, backoff,
 * tally). A page that does not cover the window cannot rule a source out, so for the
 * [candidates] of a type (apps that declare writing it but were not on the page) one more
 * page is read, filtered to those apps. Only origins and times are kept: no value.
 * Runs on demand from the diagnostics card, never on its own.
 */
internal class SourceInventoryHealthReader(
    private val support: HealthConnectReaderSupport,
    private val nowProvider: () -> Instant = Instant::now,
) {
    suspend fun read(
        grantedPermissions: Set<String>,
        candidates: Map<String, Set<String>>,
        windowDays: Int = DefaultWindowDays,
    ): SourceInventory {
        val start = nowProvider().minusSeconds(windowDays * SecondsPerDay)
        val rows = InventoryTypes.flatMap { spec ->
            val readable = HealthPermission.getReadPermission(spec.recordType) in grantedPermissions
            if (!readable) {
                spec.rowsFor(emptyList(), truncated = false, readable = false)
            } else {
                val page = readPage(spec.recordType, start, origins = emptySet())
                val seen = page.records.map { SyncedSourceOverlay.displaySource(it.metadata) }.toSet()
                val missing = candidates[spec.type].orEmpty() - seen
                val extra = if (page.truncated && missing.isNotEmpty()) {
                    readPage(spec.recordType, start, origins = missing).records
                } else {
                    emptyList()
                }
                spec.rowsFor(page.records + extra, truncated = page.truncated, readable = true)
            }
        }
        return SourceInventory(windowDays = windowDays, rows = rows)
    }

    private class Page(val records: List<Record>, val truncated: Boolean)

    private suspend fun readPage(recordType: KClass<out Record>, start: Instant, origins: Set<String>): Page =
        support.withLogging("readSourceInventory[${recordType.simpleName}]", Page(emptyList(), truncated = false)) {
            @Suppress("UNCHECKED_CAST")
            val anyRecord = recordType as KClass<Record>
            val response = support.client().readRecords(
                ReadRecordsRequest(
                    recordType = anyRecord,
                    timeRangeFilter = TimeRangeFilter.after(start),
                    dataOriginFilter = origins.map(::DataOrigin).toSet(),
                    ascendingOrder = false,
                    pageSize = PageSize,
                ),
            )
            Page(response.records, truncated = response.pageToken != null)
        }

    /**
     * A row read from another type's records, such as sleep stages. [type] is the permission
     * it is declared under: "sleep" for stages, "exercise route" for routes.
     */
    private class DerivedRow(val label: String, val type: String, val keep: (Record) -> Boolean)

    /** One data type to inventory, and the rows it produces. */
    private class InventoryType(
        val recordType: KClass<out Record>,
        /** When a record of this type happened: the end of an interval, or the instant. */
        val time: (Record) -> Instant,
        val derived: List<DerivedRow> = emptyList(),
    ) {
        /** The permission name of the type: READ_HEART_RATE is "heart rate". */
        val type: String = permissionType(HealthPermission.getReadPermission(recordType))
        val label: String = recordLabel(recordType)

        fun rowsFor(records: List<Record>, truncated: Boolean, readable: Boolean): List<SourceInventoryRow> =
            listOf(row(type, label, records, truncated, readable)) +
                derived.map { row(it.type, it.label, records.filter(it.keep), truncated, readable) }

        private fun row(type: String, label: String, records: List<Record>, truncated: Boolean, readable: Boolean) =
            SourceInventoryRow(
                type = type,
                label = label,
                lastSeenBySource = latestBySource(records, time),
                readable = readable,
                truncated = truncated,
            )
    }

    companion object {
        const val DefaultWindowDays = 30
        private const val PageSize = 1000
        private const val SecondsPerDay = 86_400L
        private const val ReadPrefix = "android.permission.health.READ_"

        internal fun permissionType(permission: String): String =
            permission.removePrefix(ReadPrefix).lowercase().replace('_', ' ')

        /** StepsCadenceRecord is "steps cadence": the class name, for types sharing a permission. */
        internal fun recordLabel(recordType: KClass<out Record>): String =
            recordType.simpleName.orEmpty()
                .removeSuffix("Record")
                .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
                .lowercase()

        internal fun latestBySource(records: List<Record>, timeOf: (Record) -> Instant): Map<String, Instant> {
            val latest = mutableMapOf<String, Instant>()
            for (record in records) {
                val time = timeOf(record)
                val source = SyncedSourceOverlay.displaySource(record.metadata)
                val previous = latest[source]
                if (previous == null || time.isAfter(previous)) latest[source] = time
            }
            return latest
        }

        /** The record interfaces that carry a time are internal to the client: each type says. */
        private fun <T : Record> type(
            recordType: KClass<T>,
            time: (T) -> Instant,
            derived: List<DerivedRow> = emptyList(),
        ): InventoryType {
            @Suppress("UNCHECKED_CAST")
            val timeOfRecord: (Record) -> Instant = { record -> time(record as T) }
            return InventoryType(recordType, timeOfRecord, derived)
        }

        private val InventoryTypes: List<InventoryType> = listOf(
            type(StepsRecord::class, StepsRecord::endTime),
            type(DistanceRecord::class, DistanceRecord::endTime),
            type(
                ExerciseSessionRecord::class,
                ExerciseSessionRecord::endTime,
                derived = listOf(
                    DerivedRow("exercise route", type = "exercise route") { record ->
                        (record as ExerciseSessionRecord).exerciseRouteResult !is ExerciseRouteResult.NoData
                    },
                ),
            ),
            type(
                SleepSessionRecord::class,
                SleepSessionRecord::endTime,
                derived = listOf(
                    DerivedRow("sleep stages", type = "sleep") { record ->
                        (record as SleepSessionRecord).stages.isNotEmpty()
                    },
                ),
            ),
            type(HeartRateRecord::class, HeartRateRecord::endTime),
            type(RestingHeartRateRecord::class, RestingHeartRateRecord::time),
            type(HeartRateVariabilityRmssdRecord::class, HeartRateVariabilityRmssdRecord::time),
            type(OxygenSaturationRecord::class, OxygenSaturationRecord::time),
            type(RespiratoryRateRecord::class, RespiratoryRateRecord::time),
            type(BodyTemperatureRecord::class, BodyTemperatureRecord::time),
            type(SkinTemperatureRecord::class, SkinTemperatureRecord::endTime),
            type(BloodPressureRecord::class, BloodPressureRecord::time),
            type(BloodGlucoseRecord::class, BloodGlucoseRecord::time),
            type(Vo2MaxRecord::class, Vo2MaxRecord::time),
            type(ActiveCaloriesBurnedRecord::class, ActiveCaloriesBurnedRecord::endTime),
            type(TotalCaloriesBurnedRecord::class, TotalCaloriesBurnedRecord::endTime),
            type(BasalMetabolicRateRecord::class, BasalMetabolicRateRecord::time),
            type(FloorsClimbedRecord::class, FloorsClimbedRecord::endTime),
            type(ElevationGainedRecord::class, ElevationGainedRecord::endTime),
            type(SpeedRecord::class, SpeedRecord::endTime),
            type(PowerRecord::class, PowerRecord::endTime),
            type(StepsCadenceRecord::class, StepsCadenceRecord::endTime),
            type(CyclingPedalingCadenceRecord::class, CyclingPedalingCadenceRecord::endTime),
            type(WheelchairPushesRecord::class, WheelchairPushesRecord::endTime),
            type(WeightRecord::class, WeightRecord::time),
            type(HeightRecord::class, HeightRecord::time),
            type(BodyFatRecord::class, BodyFatRecord::time),
            type(LeanBodyMassRecord::class, LeanBodyMassRecord::time),
            type(BoneMassRecord::class, BoneMassRecord::time),
            type(BodyWaterMassRecord::class, BodyWaterMassRecord::time),
            type(HydrationRecord::class, HydrationRecord::endTime),
            type(NutritionRecord::class, NutritionRecord::endTime),
            type(MindfulnessSessionRecord::class, MindfulnessSessionRecord::endTime),
        )
    }
}
