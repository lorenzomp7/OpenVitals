package tech.mmarca.openvitals.data.repository.dashboard

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.model.*
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class DashboardPeriodLoaderTest {
    private val start = LocalDate.of(2025, 1, 1)
    private val end = LocalDate.of(2025, 12, 31)
    private val hc = mockk<HealthConnectManager>()

    @Test fun `year totals use one bounded range read and keep the selected anchor`() = runTest {
        val wanted = setOf(DashboardMetric.STEPS, DashboardMetric.DISTANCE)
        val granted = wanted.flatMap { MetricReadPermissions.forMetric(it, false) }.toSet()
        every { hc.managedPermissions } returns granted
        coEvery { hc.readDashboardRangeTotals(any(), any()) } answers {
            secondArg<DashboardData>().copy(steps = 4000, distanceMeters = 2800.0)
        }
        val anchor = start.plusMonths(5)
        var snapshotQuery: DashboardQuery? = null
        val result = DashboardPeriodLoader(hc).load(
            DashboardQuery(date = anchor, period = DatePeriod(start, end), visibleMetrics = wanted), granted,
        ) { query -> snapshotQuery = query; DashboardData(date = query.date) }
        assertEquals(4000L, result.steps)
        assertEquals(2800.0, result.distanceMeters, 0.0)
        assertEquals(anchor, result.date)
        assertEquals(DatePeriod(start, end), result.period)
        assertNull(snapshotQuery)
        coVerify(exactly = 1) { hc.readDashboardRangeTotals(any(), any()) }
        coVerify(exactly = 0) { hc.readDailySteps(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test fun `missing permission is reported without reading the range`() = runTest {
        val wanted = setOf(DashboardMetric.STEPS)
        val permission = MetricReadPermissions.forMetric(DashboardMetric.STEPS, false)
        every { hc.managedPermissions } returns permission
        val result = DashboardPeriodLoader(hc).load(
            DashboardQuery(period = DatePeriod(start, end), visibleMetrics = wanted), emptySet(),
        ) { DashboardData(date = it.date) }
        assertEquals(permission, result.missingPermissions)
        coVerify(exactly = 0) { hc.readDailySteps(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test fun `sleep is mean of recorded nights excluding missing nights and awake time`() = runTest {
        val wanted = setOf(DashboardMetric.SLEEP)
        val granted = MetricReadPermissions.forMetric(DashboardMetric.SLEEP, false)
        every { hc.managedPermissions } returns granted
        val zone = java.time.ZoneId.systemDefault()
        fun night(wake: LocalDate, hours: Long, awakeMinutes: Long): SleepData {
            val begin = wake.minusDays(1).atTime(23, 0).atZone(zone).toInstant()
            val finish = begin.plus(java.time.Duration.ofHours(hours))
            val awakeEnd = begin.plus(java.time.Duration.ofMinutes(awakeMinutes))
            return SleepData(
                id = "n$wake", startTime = begin, endTime = finish,
                durationMs = finish.toEpochMilli() - begin.toEpochMilli(), source = "test",
                stages = listOf(
                    SleepStage(begin, awakeEnd, SleepStage.STAGE_AWAKE),
                    SleepStage(awakeEnd, finish, SleepStage.STAGE_LIGHT),
                ),
            )
        }
        coEvery { hc.readSleepSessionsForDates(start, end, any()) } returns listOf(
            night(start.plusDays(1), hours = 6, awakeMinutes = 30),
            night(end, hours = 8, awakeMinutes = 30),
        )
        val result = DashboardPeriodLoader(hc).load(
            DashboardQuery(period = DatePeriod(start, end), visibleMetrics = wanted), granted,
        ) { DashboardData(date = it.date) }
        // (5h30 + 7h30) / 2 recorded nights: awake stages out, empty nights out, as on the Sleep screen.
        assertEquals(6 * 3600000L + 30 * 60000L, result.sleep?.durationMs)
        coVerify(exactly = 0) { hc.readDailySleepDurations(any(), any(), any()) }
    }
}
