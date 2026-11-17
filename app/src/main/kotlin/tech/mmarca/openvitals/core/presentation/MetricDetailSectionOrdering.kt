package tech.mmarca.openvitals.core.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.ui.components.AppBarAction
import tech.mmarca.openvitals.ui.components.DeclareAppBar
import tech.mmarca.openvitals.ui.components.MetricDetailSectionListState
import tech.mmarca.openvitals.ui.components.ScreenAppBar
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState

data class MetricDetailSectionContext(
    val listState: MetricDetailSectionListState,
    val order: List<MetricDetailSectionId>,
    val isEditingSections: Boolean,
    val onMoveSectionToTarget: (MetricDetailSectionId, MetricDetailSectionId) -> Unit,
    val onMoveSection: (MetricDetailSectionId, Int) -> Unit,
    /** 131: the sections taken off; edit mode shows them at the bottom with a button to put back. */
    val hidden: Set<MetricDetailSectionId> = emptySet(),
    val onHideSection: ((MetricDetailSectionId) -> Unit)? = null,
    val onShowSection: ((MetricDetailSectionId) -> Unit)? = null,
)

/**
 * The section order of one metric screen, and the app bar toggle that arranges it.
 *
 * Every screen that orders sections gets the toggle, because the screen declares
 * it. It used to be handed up to `AppNavigation`, which showed it on three routes
 * of the five that sent it: Calories and Nutrition could not be arranged.
 */
@Composable
fun rememberMetricDetailSectionOrdering(): MetricDetailSectionContext {
    val sectionOrderViewModel = hiltViewModel<MetricDetailSectionOrderViewModel>()
    val sectionOrder by sectionOrderViewModel.sectionOrder.collectAsStateWithLifecycle()
    val isEditingSections by sectionOrderViewModel.isEditingSections.collectAsStateWithLifecycle()
    val hiddenSections by sectionOrderViewModel.hiddenSections.collectAsStateWithLifecycle()
    val sectionListState = rememberMetricDetailSectionListState()
    val editingTint = MaterialTheme.colorScheme.primary
    // 67: finishing an arrangement asks Save or Discard, as the dashboard's pencil does.
    var confirmFinish by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = isEditingSections) {
        if (sectionOrderViewModel.hasSectionEdits()) confirmFinish = true else sectionOrderViewModel.finishSectionEdit(save = false)
    }
    if (confirmFinish && isEditingSections) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.metric_sections_save_changes)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    sectionOrderViewModel.finishSectionEdit(save = true)
                    confirmFinish = false
                }) { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.action_save)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    sectionOrderViewModel.finishSectionEdit(save = false)
                    confirmFinish = false
                }) { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.action_discard)) }
            },
        )
    }

    DeclareAppBar(
        remember(isEditingSections, editingTint) {
            ScreenAppBar(
                actions = listOf(
                    AppBarAction(
                        icon = if (isEditingSections) Icons.Outlined.Check else Icons.Outlined.Tune,
                        contentDescription = if (isEditingSections) {
                            R.string.cd_finish_metric_section_editing
                        } else {
                            R.string.cd_edit_metric_sections
                        },
                        tint = if (isEditingSections) editingTint else null,
                        onClick = {
                            when {
                                !isEditingSections -> sectionOrderViewModel.toggleSectionEdit()
                                sectionOrderViewModel.hasSectionEdits() -> confirmFinish = true
                                else -> sectionOrderViewModel.finishSectionEdit(save = false)
                            }
                        },
                    ),
                ),
            )
        },
    )

    // The drag wiring reaches the list state from here, where it always runs. It was only set
    // from a zero-height item at the top of the list. A lazy list does not compose an item it
    // has scrolled past, so after a scroll restore a drag moved nothing and said nothing.
    SideEffect {
        sectionListState.isEditingSections = isEditingSections
        sectionListState.onMoveSectionToTarget = { section, target ->
            sectionOrderViewModel.moveSectionToTarget(section, target, sectionListState.visibleSectionIds.ifEmpty { null })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Leaving the screen mid-edit keeps nothing unsaved: the draft is discarded.
            if (isEditingSections) {
                sectionOrderViewModel.finishSectionEdit(save = false)
            }
        }
    }

    return MetricDetailSectionContext(
        listState = sectionListState,
        order = sectionOrder,
        isEditingSections = isEditingSections,
        onMoveSectionToTarget = { section, target ->
            sectionOrderViewModel.moveSectionToTarget(section, target, sectionListState.visibleSectionIds.ifEmpty { null })
        },
        onMoveSection = sectionOrderViewModel::moveSection,
        hidden = hiddenSections,
        onHideSection = sectionOrderViewModel::hideSection,
        onShowSection = sectionOrderViewModel::showSection,
    )
}
