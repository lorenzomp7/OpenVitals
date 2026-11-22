package tech.mmarca.openvitals.domain.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class MetricDetailSectionIdTest {

    @Test
    fun metricDetailSectionOrderFromStored_returnsDefaultWhenNull() {
        assertEquals(DefaultMetricDetailSectionOrder, metricDetailSectionOrderFromStored(null))
    }

    @Test
    fun metricDetailSectionOrderFromStored_mergesMissingSections() {
        val stored = listOf(
            MetricDetailSectionId.ENTRIES.name,
            MetricDetailSectionId.STATISTICS.name,
        )

        val order = metricDetailSectionOrderFromStored(stored)

        assertEquals(MetricDetailSectionId.ENTRIES, order.first())
        assertEquals(MetricDetailSectionId.STATISTICS, order[1])
        assertEquals(DefaultMetricDetailSectionOrder.size, order.size)
        assertEquals(DefaultMetricDetailSectionOrder.toSet(), order.toSet())
    }

    @Test
    fun metricDetailSectionOrderFromStored_ignoresUnknownValues() {
        val order = metricDetailSectionOrderFromStored(listOf("UNKNOWN", MetricDetailSectionId.DAILY_GOAL.name))

        assertEquals(MetricDetailSectionId.DAILY_GOAL, order.first())
        assertEquals(DefaultMetricDetailSectionOrder.size, order.size)
    }
}

/** 132: an activity's charts follow one general order, whatever was saved before. */
class ActivityDetailGeneralOrderTest {

    @Test
    fun everyActivityChartHasAPlace() {
        val activityCharts = MetricDetailSectionId.entries.filter { it.name.startsWith("ACTIVITY_DETAIL_") }
        assertEquals(activityCharts.toSet(), ActivityDetailGeneralOrder.toSet())
    }

    @Test
    fun theActivityChartsAreSortedInTheirOwnSlots() {
        val saved = listOf(
            MetricDetailSectionId.ACTIVITY_DETAIL_RECORDS,
            MetricDetailSectionId.ENTRIES,
            MetricDetailSectionId.ACTIVITY_DETAIL_ELEVATION,
            MetricDetailSectionId.ACTIVITY_DETAIL_HEART_RATE,
        )

        assertEquals(
            listOf(
                MetricDetailSectionId.ACTIVITY_DETAIL_HEART_RATE,
                MetricDetailSectionId.ENTRIES,
                MetricDetailSectionId.ACTIVITY_DETAIL_ELEVATION,
                MetricDetailSectionId.ACTIVITY_DETAIL_RECORDS,
            ),
            withActivityDetailGeneralOrder(saved),
        )
    }

    @Test
    fun theDefaultOrderIsAlreadyInIt() {
        assertEquals(DefaultMetricDetailSectionOrder, withActivityDetailGeneralOrder(DefaultMetricDetailSectionOrder))
    }

    @Test
    fun whileTheSamplesLoad_theRecordsOwnSectionsBeforeTheFirstChartShow() {
        val order = listOf(
            MetricDetailSectionId.ACTIVITY_DETAIL_METRICS,
            MetricDetailSectionId.ENTRIES,
            MetricDetailSectionId.ACTIVITY_DETAIL_ROUTE,
            MetricDetailSectionId.ACTIVITY_DETAIL_HEART_RATE,
            MetricDetailSectionId.ACTIVITY_DETAIL_SESSION,
        )

        assertEquals(
            setOf(MetricDetailSectionId.ACTIVITY_DETAIL_METRICS, MetricDetailSectionId.ACTIVITY_DETAIL_ROUTE),
            activityDetailSectionsShownWhileLoading(order),
        )
    }
}
