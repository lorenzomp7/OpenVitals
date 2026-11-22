package tech.mmarca.openvitals.core.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import tech.mmarca.openvitals.data.repository.contract.WidgetOrderPreferences
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.domain.preferences.ActivityDetailGeneralOrderVersion
import tech.mmarca.openvitals.domain.preferences.metricDetailSectionOrderFromStored
import tech.mmarca.openvitals.domain.preferences.withActivityDetailGeneralOrder
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class MetricDetailSectionOrderViewModel @Inject constructor(
    private val widgetOrder: WidgetOrderPreferences,
) : ViewModel() {

    private val _sectionOrder = MutableStateFlow(storedOrderInGeneralOrder())
    val sectionOrder: StateFlow<List<MetricDetailSectionId>> = _sectionOrder.asStateFlow()

    /** The sections taken off their screens; edit mode lists them at the bottom to put back. */
    private val _hiddenSections = MutableStateFlow(
        widgetOrder.hiddenMetricDetailSections()
            .mapNotNull { name -> MetricDetailSectionId.entries.firstOrNull { it.name == name } }
            .toSet(),
    )
    val hiddenSections: StateFlow<Set<MetricDetailSectionId>> = _hiddenSections.asStateFlow()
    private var hiddenBeforeEdit: Set<MetricDetailSectionId>? = null

    /**
     * An arrangement saved before the activity charts had one general order is put in it
     * once; after that, a chart moved in one activity moves in all of them.
     */
    private fun storedOrderInGeneralOrder(): List<MetricDetailSectionId> {
        val stored = metricDetailSectionOrderFromStored(widgetOrder.metricDetailSectionOrder())
        if (widgetOrder.activityDetailGeneralOrderVersion() >= ActivityDetailGeneralOrderVersion) return stored
        val ordered = withActivityDetailGeneralOrder(stored)
        if (widgetOrder.metricDetailSectionOrder() != null) widgetOrder.setMetricDetailSectionOrder(ordered.map { it.name })
        widgetOrder.setActivityDetailGeneralOrderVersion(ActivityDetailGeneralOrderVersion)
        return ordered
    }

    /** Takes [sectionId] off every screen it appears on; a draft until Save while editing. */
    fun hideSection(sectionId: MetricDetailSectionId) = updateHidden(_hiddenSections.value + sectionId)

    /** Puts [sectionId] back where the order places it. */
    fun showSection(sectionId: MetricDetailSectionId) = updateHidden(_hiddenSections.value - sectionId)

    private fun updateHidden(hidden: Set<MetricDetailSectionId>) {
        if (hidden == _hiddenSections.value) return
        if (!_isEditingSections.value) widgetOrder.setHiddenMetricDetailSections(hidden.map { it.name }.toSet())
        _hiddenSections.value = hidden
    }

    private val _isEditingSections = MutableStateFlow(false)
    val isEditingSections: StateFlow<Boolean> = _isEditingSections.asStateFlow()

    /** The order before editing began; moves made while editing are a draft until Save. */
    private var orderBeforeEdit: List<MetricDetailSectionId>? = null

    /** Starts editing; while editing, the toggle asks Save or Discard (see [finishSectionEdit]). */
    fun toggleSectionEdit() {
        if (_isEditingSections.value) {
            finishSectionEdit(save = true)
        } else {
            orderBeforeEdit = _sectionOrder.value
            hiddenBeforeEdit = _hiddenSections.value
            _isEditingSections.value = true
        }
    }

    /** Whether the order differs from where editing began. */
    fun hasSectionEdits(): Boolean = orderBeforeEdit?.let { it != _sectionOrder.value } == true ||
        hiddenBeforeEdit?.let { it != _hiddenSections.value } == true

    /** Ends editing like the dashboard does: Save keeps the new order, Discard puts the old one back. */
    fun finishSectionEdit(save: Boolean) {
        val before = orderBeforeEdit
        val hiddenBefore = hiddenBeforeEdit
        orderBeforeEdit = null
        hiddenBeforeEdit = null
        _isEditingSections.value = false
        if (save) {
            if (before != _sectionOrder.value) {
                widgetOrder.setMetricDetailSectionOrder(_sectionOrder.value.map { it.name })
            }
            if (hiddenBefore != _hiddenSections.value) {
                widgetOrder.setHiddenMetricDetailSections(_hiddenSections.value.map { it.name }.toSet())
            }
        } else {
            if (before != null) _sectionOrder.value = before
            if (hiddenBefore != null) _hiddenSections.value = hiddenBefore
        }
    }

    /** [visible] is the sections the screen shows; the others keep their slots (see [moveWithinVisible]). */
    fun moveSectionToTarget(
        sectionId: MetricDetailSectionId,
        targetSectionId: MetricDetailSectionId,
        visible: Set<MetricDetailSectionId>? = null,
    ) {
        if (visible != null) {
            val moved = moveWithinVisible(_sectionOrder.value, visible, sectionId, targetSectionId)
            if (moved != _sectionOrder.value) updateSectionOrder(moved)
            return
        }
        val current = _sectionOrder.value
        val fromIndex = current.indexOf(sectionId)
        val targetIndex = current.indexOf(targetSectionId)
        if (fromIndex == -1 || targetIndex == -1 || fromIndex == targetIndex) return

        updateSectionOrder(
            current.toMutableList().apply {
                removeAt(fromIndex)
                add(targetIndex, sectionId)
            },
        )
    }

    fun moveSection(sectionId: MetricDetailSectionId, offset: Int) {
        val current = _sectionOrder.value
        val fromIndex = current.indexOf(sectionId)
        if (fromIndex == -1) return

        val toIndex = (fromIndex + offset).coerceIn(current.indices)
        if (fromIndex == toIndex) return

        updateSectionOrder(
            current.toMutableList().apply {
                removeAt(fromIndex)
                add(toIndex, sectionId)
            },
        )
    }

    private fun updateSectionOrder(order: List<MetricDetailSectionId>) {
        // While editing, the order is a draft that only Save writes.
        if (!_isEditingSections.value) widgetOrder.setMetricDetailSectionOrder(order.map { it.name })
        _sectionOrder.value = order
    }
}
