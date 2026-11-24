package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.FakeHealthConnectClient
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SourceInventoryHealthReaderTest {

    @Before
    fun setUp() {
        HealthConnectRateLimitBackoff.resetForTest()
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        HealthConnectRateLimitBackoff.resetForTest()
    }

    @Test
    fun `types are named like their permission and labelled like their record`() {
        assertThat(SourceInventoryHealthReader.permissionType(HealthPermission.getReadPermission(HeartRateRecord::class)))
            .isEqualTo("heart rate")
        assertThat(
            SourceInventoryHealthReader.permissionType(
                HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
            ),
        ).isEqualTo("heart rate variability")
        // Steps cadence is declared under the steps permission, so its label tells it apart.
        assertThat(SourceInventoryHealthReader.permissionType(HealthPermission.getReadPermission(StepsCadenceRecord::class)))
            .isEqualTo("steps")
        assertThat(SourceInventoryHealthReader.recordLabel(StepsCadenceRecord::class)).isEqualTo("steps cadence")
        assertThat(SourceInventoryHealthReader.recordLabel(HeartRateRecord::class)).isEqualTo("heart rate")
    }

    @Test
    fun `each source keeps its latest record, sleep stages get their own row, unread types are marked`() = runTest {
        val client = FakeHealthConnectClient()
        client.setPackageName(ZEPP)
        client.insertRecords(
            listOf(
                heartRate(NOW.minusSeconds(3 * DAY)),
                heartRate(NOW.minusSeconds(DAY)),
                sleep(NOW.minusSeconds(2 * DAY), withStages = true),
            ),
        )
        client.setPackageName(FIT)
        client.insertRecords(listOf(steps(NOW.minusSeconds(4 * DAY)), sleep(NOW.minusSeconds(5 * DAY), withStages = false)))

        val inventory = reader(client).read(
            grantedPermissions = setOf(
                HealthPermission.getReadPermission(HeartRateRecord::class),
                HealthPermission.getReadPermission(SleepSessionRecord::class),
                HealthPermission.getReadPermission(StepsRecord::class),
            ),
            candidates = emptyMap(),
        )
        val rows = inventory.rows.associateBy { it.label }

        assertThat(rows.getValue("heart rate").lastSeenBySource)
            .containsExactly(ZEPP, NOW.minusSeconds(DAY).plusSeconds(60))
        assertThat(rows.getValue("sleep session").lastSeenBySource.keys).containsExactly(ZEPP, FIT)
        assertThat(rows.getValue("sleep stages").lastSeenBySource.keys).containsExactly(ZEPP)
        assertThat(rows.getValue("sleep stages").type).isEqualTo("sleep")
        assertThat(rows.getValue("steps").lastSeenBySource.keys).containsExactly(FIT)
        assertThat(rows.getValue("weight").readable).isFalse()
        assertThat(inventory.sources).containsExactly(ZEPP, FIT).inOrder()
        assertThat(inventory.windowDays).isEqualTo(SourceInventoryHealthReader.DefaultWindowDays)
    }

    @Test
    fun `records older than the window are left out`() = runTest {
        val client = FakeHealthConnectClient()
        client.setPackageName(ZEPP)
        client.insertRecords(listOf(heartRate(NOW.minusSeconds(40 * DAY))))

        val inventory = reader(client).read(
            grantedPermissions = setOf(HealthPermission.getReadPermission(HeartRateRecord::class)),
            candidates = emptyMap(),
        )

        assertThat(inventory.rows.first { it.label == "heart rate" }.lastSeenBySource).isEmpty()
    }

    private fun reader(client: FakeHealthConnectClient): SourceInventoryHealthReader {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "test"
        val support = HealthConnectReaderSupport(
            clientProvider = { client },
            diagnostics = diagnostics,
            rateLimitMessage = { "rate limited" },
        )
        return SourceInventoryHealthReader(support, nowProvider = { NOW })
    }

    private fun heartRate(time: Instant) = HeartRateRecord(
        startTime = time,
        startZoneOffset = null,
        endTime = time.plusSeconds(60),
        endZoneOffset = null,
        samples = listOf(HeartRateRecord.Sample(time = time, beatsPerMinute = 60)),
        metadata = Metadata.autoRecorded(WATCH),
    )

    private fun steps(start: Instant) = StepsRecord(
        startTime = start,
        startZoneOffset = null,
        endTime = start.plusSeconds(600),
        endZoneOffset = null,
        count = 100,
        metadata = Metadata.autoRecorded(WATCH),
    )

    private fun sleep(start: Instant, withStages: Boolean) = SleepSessionRecord(
        startTime = start,
        startZoneOffset = null,
        endTime = start.plusSeconds(8 * 3_600),
        endZoneOffset = null,
        stages = if (withStages) {
            listOf(SleepSessionRecord.Stage(start, start.plusSeconds(3_600), SleepSessionRecord.STAGE_TYPE_DEEP))
        } else {
            emptyList()
        },
        metadata = Metadata.autoRecorded(WATCH),
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-11-24T08:00:00Z")
        const val DAY = 86_400L
        const val ZEPP = "com.huami.watch.hmwatchmanager"
        const val FIT = "com.google.android.apps.fitness"
        val WATCH = Device(type = Device.TYPE_WATCH)
    }
}
