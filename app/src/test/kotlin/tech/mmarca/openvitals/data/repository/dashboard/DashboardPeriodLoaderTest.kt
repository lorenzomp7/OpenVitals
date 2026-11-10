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
        coEvery { hc.readDailySteps(start, end, true, true, false, false, false, false) } returns listOf(
            DailySteps(start, 1000, 700.0), DailySteps(end, 3000, 2100.0),
        )
        val anchor = start.plusMonths(5)
        var snapshotQuery: DashboardQuery? = null
        val result = DashboardPeriodLoader(hc).load(
            DashboardQuery(date = anchor, period = DatePeriod(start, end), visibleMetrics = wanted), granted,
        ) { query -> snapshotQuery = query; DashboardData(date = query.date) }
        assertEquals(4000L, result.steps)
        assertEquals(2800.0, result.distanceMeters, 0.0)
        assertEquals(anchor, result.date)
        assertEquals(DatePeriod(start, end), result.period)
        assertEquals(end, snapshotQuery?.date)
        assertTrue(snapshotQuery!!.visibleMetrics.isEmpty())
        coVerify(exactly = 1) { hc.readDailySteps(any(), any(), any(), any(), any(), any(), any(), any()) }
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

    @Test fun `sleep is mean of recorded nights excluding missing nights`() = runTest {
        val wanted = setOf(DashboardMetric.SLEEP)
        val granted = MetricReadPermissions.forMetric(DashboardMetric.SLEEP, false)
        every { hc.managedPermissions } returns granted
        coEvery { hc.readDailySleepDurations(start, end, any()) } returns listOf(
            DailySleepDuration(start, 6 * 3600000L), DailySleepDuration(end, 8 * 3600000L),
        )
        val result = DashboardPeriodLoader(hc).load(
            DashboardQuery(period = DatePeriod(start, end), visibleMetrics = wanted), granted,
        ) { DashboardData(date = it.date) }
        assertEquals(7 * 3600000L, result.sleep?.durationMs)
    }
}
