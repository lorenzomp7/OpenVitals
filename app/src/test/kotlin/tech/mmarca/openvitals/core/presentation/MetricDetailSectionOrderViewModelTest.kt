package tech.mmarca.openvitals.core.presentation

import tech.mmarca.openvitals.data.repository.contract.WidgetOrderPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId

class MetricDetailSectionOrderViewModelTest {

    @Test
    fun moveSectionToTarget_reordersAndPersists() {
        val prefs = mockk<WidgetOrderPreferences>(relaxed = true) {
            every { metricDetailSectionOrder() } returns listOf(
                MetricDetailSectionId.DAILY_GOAL.name,
                MetricDetailSectionId.STATISTICS.name,
                MetricDetailSectionId.ENTRIES.name,
            )
        }
        val viewModel = MetricDetailSectionOrderViewModel(prefs)

        viewModel.moveSectionToTarget(
            MetricDetailSectionId.ENTRIES,
            MetricDetailSectionId.DAILY_GOAL,
        )

        assertEquals(
            listOf(
                MetricDetailSectionId.ENTRIES,
                MetricDetailSectionId.DAILY_GOAL,
                MetricDetailSectionId.STATISTICS,
            ),
            viewModel.sectionOrder.value.filter {
                it in setOf(
                    MetricDetailSectionId.DAILY_GOAL,
                    MetricDetailSectionId.STATISTICS,
                    MetricDetailSectionId.ENTRIES,
                )
            },
        )
        verify {
            prefs.setMetricDetailSectionOrder(
                viewModel.sectionOrder.value.map { it.name },
            )
        }
    }

    @Test
    fun toggleSectionEdit_switchesEditingState() {
        val prefs = mockk<WidgetOrderPreferences>(relaxed = true) {
            every { metricDetailSectionOrder() } returns null
        }
        val viewModel = MetricDetailSectionOrderViewModel(prefs)

        assertFalse(viewModel.isEditingSections.value)
        viewModel.toggleSectionEdit()
        assertTrue(viewModel.isEditingSections.value)
    }

    @Test
    fun moveSection_nudgesOnePlaceInTheFullOrder() {
        val prefs = mockk<WidgetOrderPreferences>(relaxed = true) {
            every { metricDetailSectionOrder() } returns null
        }
        val viewModel = MetricDetailSectionOrderViewModel(prefs)

        fun order() = viewModel.sectionOrder.value
        val before = order().indexOf(MetricDetailSectionId.ENTRIES)

        viewModel.moveSection(MetricDetailSectionId.ENTRIES, -1)
        assertEquals(before - 1, order().indexOf(MetricDetailSectionId.ENTRIES))

        viewModel.moveSection(MetricDetailSectionId.ENTRIES, 1)
        assertEquals(before, order().indexOf(MetricDetailSectionId.ENTRIES))
    }

    @Test
    fun moveSection_cannotPushASectionPastEitherEnd() {
        val prefs = mockk<WidgetOrderPreferences>(relaxed = true) {
            every { metricDetailSectionOrder() } returns null
        }
        val viewModel = MetricDetailSectionOrderViewModel(prefs)

        val first = viewModel.sectionOrder.value.first()
        val last = viewModel.sectionOrder.value.last()

        viewModel.moveSection(first, -1)
        viewModel.moveSection(last, 1)

        assertEquals(first, viewModel.sectionOrder.value.first())
        assertEquals(last, viewModel.sectionOrder.value.last())
    }

    @Test
    fun initialOrder_usesDefaultWhenPreferencesMissing() {
        val prefs = mockk<WidgetOrderPreferences>(relaxed = true) {
            every { metricDetailSectionOrder() } returns null
        }
        val viewModel = MetricDetailSectionOrderViewModel(prefs)

        assertEquals(DefaultMetricDetailSectionOrder, viewModel.sectionOrder.value)
    }

    @Test
    fun editing_isADraftThatDiscardRestoresAndSaveWrites() {
        val prefs = mockk<WidgetOrderPreferences>(relaxed = true) {
            every { metricDetailSectionOrder() } returns null
        }
        val viewModel = MetricDetailSectionOrderViewModel(prefs)
        val original = viewModel.sectionOrder.value

        viewModel.toggleSectionEdit()
        viewModel.moveSection(original.last(), -1)
        verify(exactly = 0) { prefs.setMetricDetailSectionOrder(any()) }
        viewModel.finishSectionEdit(save = false)
        assertEquals(original, viewModel.sectionOrder.value)
        assertFalse(viewModel.isEditingSections.value)

        viewModel.toggleSectionEdit()
        viewModel.moveSection(original.last(), -1)
        viewModel.finishSectionEdit(save = true)
        verify(exactly = 1) { prefs.setMetricDetailSectionOrder(viewModel.sectionOrder.value.map { it.name }) }
    }

    @Test
    fun hiddenSection_isADraftUntilSaveAndDiscardBringsItBack() {
        val prefs = mockk<WidgetOrderPreferences>(relaxed = true) {
            every { metricDetailSectionOrder() } returns null
            every { hiddenMetricDetailSections() } returns emptySet()
            every { activityDetailGeneralOrderVersion() } returns Int.MAX_VALUE
        }
        val viewModel = MetricDetailSectionOrderViewModel(prefs)

        viewModel.toggleSectionEdit()
        viewModel.hideSection(MetricDetailSectionId.ACTIVITY_DETAIL_SPEED)
        assertTrue(viewModel.hasSectionEdits())
        verify(exactly = 0) { prefs.setHiddenMetricDetailSections(any()) }
        viewModel.finishSectionEdit(save = false)
        assertEquals(emptySet<MetricDetailSectionId>(), viewModel.hiddenSections.value)

        viewModel.toggleSectionEdit()
        viewModel.hideSection(MetricDetailSectionId.ACTIVITY_DETAIL_SPEED)
        viewModel.finishSectionEdit(save = true)
        verify { prefs.setHiddenMetricDetailSections(setOf(MetricDetailSectionId.ACTIVITY_DETAIL_SPEED.name)) }
    }

    @Test
    fun anOrderSavedBeforeTheGeneralOrder_isPutInItOnce() {
        val prefs = mockk<WidgetOrderPreferences>(relaxed = true) {
            every { metricDetailSectionOrder() } returns listOf(
                MetricDetailSectionId.ACTIVITY_DETAIL_RECORDS.name,
                MetricDetailSectionId.ACTIVITY_DETAIL_HEART_RATE.name,
            )
            every { activityDetailGeneralOrderVersion() } returns 0
        }
        val viewModel = MetricDetailSectionOrderViewModel(prefs)

        val activity = viewModel.sectionOrder.value.filter { it.name.startsWith("ACTIVITY_DETAIL_") }
        assertEquals(tech.mmarca.openvitals.domain.preferences.ActivityDetailGeneralOrder, activity)
        verify { prefs.setActivityDetailGeneralOrderVersion(tech.mmarca.openvitals.domain.preferences.ActivityDetailGeneralOrderVersion) }
    }
}
