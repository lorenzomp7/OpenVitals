package tech.mmarca.openvitals.web

import android.content.pm.PackageManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Volume
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.preferences.AppLanguage

/**
 * The web copy of the app (scripts/web-capture.sh) photographs the real APK on an emulator.
 * This puts the demo it shows in place: thirty days of plausible records in Health Connect,
 * and an app past onboarding, in Italian. Run only by that script, never in CI's test suite.
 * The numbers follow fixed curves, so every build shows the same demo.
 */
@RunWith(AndroidJUnit4::class)
class WebDemoSeedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val zone = ZoneId.systemDefault()
    private val watch = Device(type = Device.TYPE_WATCH, manufacturer = "Demo", model = "Watch")

    /** Health Connect refuses a record from the future: today stops at the present hour. */
    private val now = Instant.now()
    private fun past(end: Instant) = !end.isAfter(now)

    @Test
    fun seedTheWebDemo() {
        PreferencesRepository(context).apply {
            onboardingDone = true
            openWatchMoveNoticeSeen = true
            acceptedPrivacyPolicyVersion = PreferencesRepository.CURRENT_PRIVACY_POLICY_VERSION
            appLanguage = AppLanguage.ITALIAN
        }
        grantEveryRequestedPermission()
        val client = HealthConnectClient.getOrCreate(context)
        val today = LocalDate.now(zone)
        val records = (0L until DemoDays).flatMap { back -> day(today.minusDays(back), back) } + profile(today)
        runBlocking { records.chunked(InsertChunk).forEach { client.insertRecords(it) } }
    }

    /** Every runtime permission the app asks for, Health Connect's included: no grant dialog on the tour. */
    private fun grantEveryRequestedPermission() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        @Suppress("DEPRECATION")
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty()
        val granted = requested.count { permission ->
            runCatching { automation.grantRuntimePermission(context.packageName, permission) }.isSuccess
        }
        println("WebDemoSeed: granted $granted of ${requested.size} requested permissions")
    }

    private var serial = 0
    private fun meta(): Metadata = Metadata.autoRecorded(watch, clientRecordId = "web-demo-${serial++}")

    private fun at(date: LocalDate, hour: Double): Instant =
        date.atStartOfDay(zone).toInstant().plusSeconds((hour * 3_600).roundToLong())

    private fun offset(time: Instant) = zone.rules.getOffset(time)

    /** A gentle wave across the month, so the charts have shape: 0..1. */
    private fun wave(back: Long, period: Double, phase: Double = 0.0) =
        (sin(2 * PI * back / period + phase) + 1) / 2

    private fun day(date: LocalDate, back: Long): List<Record> = buildList {
        val activity = 0.55 + 0.45 * wave(back, 7.0)
        // Steps hour by hour, busiest at lunch and in the evening.
        for (hour in 7 until 22) {
            val shape = when (hour) { in 12..13 -> 1.6; in 18..19 -> 1.9; else -> 0.7 }
            val count = (500 * shape * activity).roundToLong().coerceAtLeast(30)
            val start = at(date, hour.toDouble())
            if (!past(start.plusSeconds(3_000))) break
            add(StepsRecord(start, offset(start), start.plusSeconds(3_000), offset(start), count, meta()))
            add(
                DistanceRecord(
                    start, offset(start), start.plusSeconds(3_000), offset(start),
                    Length.meters(count * 0.74), meta(),
                ),
            )
            add(
                ActiveCaloriesBurnedRecord(
                    start, offset(start), start.plusSeconds(3_000), offset(start),
                    Energy.kilocalories(count * 0.045), meta(),
                ),
            )
            // Heart rate every ten minutes in the hour.
            val samples = (0 until 6).map { tick ->
                val time = start.plusSeconds(tick * 600L)
                HeartRateRecord.Sample(time, (64 + 18 * shape * activity + tick % 3).roundToLong())
            }
            add(HeartRateRecord(start, offset(start), start.plusSeconds(3_000), offset(start), samples, meta()))
        }
        // Last night's sleep, from about 23:15 to 7:00, in stages.
        val bed = at(date.minusDays(1), 23.25 + 0.5 * wave(back, 5.0))
        val wake = at(date, 6.75 + 0.5 * wave(back, 6.0, 1.0))
        if (past(wake)) add(SleepSessionRecord(bed, offset(bed), wake, offset(wake), meta(), stages = stages(bed, wake)))
        val morning = at(date, 7.5)
        if (past(morning)) {
            add(RestingHeartRateRecord(morning, offset(morning), (56 + 5 * wave(back, 9.0)).roundToLong(), meta()))
            add(OxygenSaturationRecord(morning, offset(morning), Percentage(96.0 + 2 * wave(back, 4.0)), meta()))
        }
        for (drink in listOf(8.0, 11.0, 14.0, 17.0, 20.0)) {
            val time = at(date, drink)
            if (!past(time.plusSeconds(60))) break
            add(HydrationRecord(time, offset(time), time.plusSeconds(60), offset(time), Volume.liters(0.35), meta()))
        }
        val weighed = at(date, 7.25)
        if (back % 3 == 0L && past(weighed)) {
            add(WeightRecord(weighed, offset(weighed), Mass.kilograms(74.0 - back * 0.04 + 0.4 * wave(back, 6.0)), meta()))
        }
        // A run every three days, a ride every five.
        if (back % 3 == 1L) addAll(workout(date, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, "Corsa", 18.5, 42, 7_800.0))
        if (back % 5 == 2L) addAll(workout(date, ExerciseSessionRecord.EXERCISE_TYPE_BIKING, "Bici", 9.0, 65, 21_000.0))
    }

    private fun stages(bed: Instant, wake: Instant): List<SleepSessionRecord.Stage> {
        val cycle = listOf(
            SleepSessionRecord.STAGE_TYPE_LIGHT to 25L,
            SleepSessionRecord.STAGE_TYPE_DEEP to 30L,
            SleepSessionRecord.STAGE_TYPE_LIGHT to 20L,
            SleepSessionRecord.STAGE_TYPE_REM to 20L,
            SleepSessionRecord.STAGE_TYPE_AWAKE to 5L,
        )
        return buildList {
            var time = bed
            var index = 0
            while (time < wake) {
                val (stage, minutes) = cycle[index++ % cycle.size]
                val end = minOf(wake, time.plusSeconds(minutes * 60))
                add(SleepSessionRecord.Stage(time, end, stage))
                time = end
            }
        }
    }

    private fun workout(date: LocalDate, type: Int, title: String, hour: Double, minutes: Long, meters: Double): List<Record> {
        val start = at(date, hour)
        val end = start.plusSeconds(minutes * 60)
        if (!past(end)) return emptyList()
        val beats = (0 until minutes).map { minute ->
            HeartRateRecord.Sample(start.plusSeconds(minute * 60), (125 + 30 * sin(minute / 9.0).let { it * it }).roundToLong())
        }
        return listOf(
            ExerciseSessionRecord(start, offset(start), end, offset(end), meta(), type, title),
            HeartRateRecord(start, offset(start), end, offset(end), beats, meta()),
            DistanceRecord(start, offset(start), end, offset(end), Length.meters(meters), meta()),
            ActiveCaloriesBurnedRecord(start, offset(start), end, offset(end), Energy.kilocalories(minutes * 9.5), meta()),
        )
    }

    private fun profile(today: LocalDate): List<Record> {
        val time = at(today.minusDays(DemoDays), 8.0)
        return listOf(HeightRecord(time, offset(time), Length.meters(1.78), meta()))
    }

    private companion object {
        const val DemoDays = 30L
        const val InsertChunk = 400
    }
}
