package tech.mmarca.openvitals.features.manualentry

import tech.mmarca.openvitals.ui.theme.Spacing
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.ui.components.AppBarAction
import tech.mmarca.openvitals.ui.components.DeclareAppBar
import tech.mmarca.openvitals.ui.components.ScreenAppBar

@Composable
fun ManualEntryScreen(
    viewModel: ManualEntryViewModel,
    onOpenHydrationEntry: () -> Unit,
    onOpenNutritionEntry: () -> Unit,
    onOpenActivityEntry: () -> Unit,
    onOpenMindfulnessEntry: () -> Unit,
    onOpenBodyCompositionEntry: () -> Unit,
    onOpenBodyMeasurementEntry: (BodyMeasurementType) -> Unit,
    onOpenVitalsMeasurementEntry: (VitalsMeasurementType) -> Unit,
    onOpenCycleEntry: () -> Unit,
    onOpenWorkoutPlans: () -> Unit = {},
    onOpenFoodEntry: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // A tile only opens its entry screen, so the one dialog Health Connect allows is not spent here.
    val specs = manualEntryWidgetSpecs(
        isEditingWidgets = state.isEditingWidgets,
        onOpenHydrationEntry = viewModel::onHydrationWidgetTapped,
        onOpenNutritionEntry = viewModel::onNutritionWidgetTapped,
        onOpenFoodEntry = viewModel::onFoodWidgetTapped,
        onOpenActivityEntry = viewModel::onActivityWidgetTapped,
        onOpenMindfulnessEntry = viewModel::onMindfulnessWidgetTapped,
        onOpenBodyCompositionEntry = viewModel::onBodyCompositionWidgetTapped,
        onOpenBodyMeasurementEntry = viewModel::onBodyMeasurementWidgetTapped,
        onOpenVitalsMeasurementEntry = viewModel::onVitalsMeasurementWidgetTapped,
        onOpenCycleEntry = viewModel::onCycleWidgetTapped,
        // The plans screen carries its own gate, so the tile navigates directly.
        onOpenWorkoutPlans = onOpenWorkoutPlans,
    )
    val specsById = specs.associateBy { it.id }
    val visibleIds = state.widgets.filter { it in specsById }
    val hiddenSpecs = specs.filter { it.id !in visibleIds }

    val editingTint = MaterialTheme.colorScheme.primary
    // Finishing or leaving an edit with changes asks Save or Discard, as on the dashboard.
    var confirmFinishEdit by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = state.isEditingWidgets) {
        if (viewModel.hasWidgetEdits()) confirmFinishEdit = true else viewModel.finishWidgetEdit(save = true)
    }
    if (confirmFinishEdit && state.isEditingWidgets) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmFinishEdit = false },
            title = { Text(stringResource(R.string.unsaved_changes_title)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.finishWidgetEdit(save = true)
                    confirmFinishEdit = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.finishWidgetEdit(save = false)
                    confirmFinishEdit = false
                }) { Text(stringResource(R.string.action_discard)) }
            },
        )
    }
    DeclareAppBar(
        remember(state.isEditingWidgets, editingTint) {
            ScreenAppBar(
                actions = listOf(
                    AppBarAction(
                        icon = Icons.Outlined.Edit,
                        contentDescription = if (state.isEditingWidgets) {
                            R.string.cd_finish_manual_entry_editing
                        } else {
                            R.string.cd_edit_manual_entry_widgets
                        },
                        tint = if (state.isEditingWidgets) editingTint else null,
                        onClick = {
                            when {
                                !state.isEditingWidgets -> viewModel.toggleWidgetEdit()
                                viewModel.hasWidgetEdits() -> confirmFinishEdit = true
                                else -> viewModel.finishWidgetEdit(save = true)
                            }
                        },
                    ),
                ),
            )
        },
    )
    LaunchedEffect(state.pendingHydrationEntryNavigation) {
        if (state.pendingHydrationEntryNavigation) {
            viewModel.onHydrationEntryNavigationHandled()
            onOpenHydrationEntry()
        }
    }
    LaunchedEffect(state.pendingNutritionEntryNavigation) {
        if (state.pendingNutritionEntryNavigation) {
            viewModel.onNutritionEntryNavigationHandled()
            onOpenNutritionEntry()
        }
    }
    LaunchedEffect(state.pendingFoodEntryNavigation) {
        if (state.pendingFoodEntryNavigation) {
            viewModel.onFoodEntryNavigationHandled()
            onOpenFoodEntry()
        }
    }
    LaunchedEffect(state.pendingActivityEntryNavigation) {
        if (state.pendingActivityEntryNavigation) {
            viewModel.onActivityEntryNavigationHandled()
            onOpenActivityEntry()
        }
    }
    LaunchedEffect(state.pendingMindfulnessEntryNavigation) {
        if (state.pendingMindfulnessEntryNavigation) {
            viewModel.onMindfulnessEntryNavigationHandled()
            onOpenMindfulnessEntry()
        }
    }
    LaunchedEffect(state.pendingCycleEntryNavigation) {
        if (state.pendingCycleEntryNavigation) {
            viewModel.onCycleEntryNavigationHandled()
            onOpenCycleEntry()
        }
    }
    LaunchedEffect(state.pendingBodyCompositionEntryNavigation) {
        if (state.pendingBodyCompositionEntryNavigation) {
            viewModel.onBodyCompositionEntryNavigationHandled()
            onOpenBodyCompositionEntry()
        }
    }
    LaunchedEffect(state.pendingBodyEntryNavigation) {
        val type = state.pendingBodyEntryNavigation
        if (type != null) {
            viewModel.onBodyEntryNavigationHandled()
            onOpenBodyMeasurementEntry(type)
        }
    }
    LaunchedEffect(state.pendingVitalsEntryNavigation) {
        val type = state.pendingVitalsEntryNavigation
        if (type != null) {
            viewModel.onVitalsEntryNavigationHandled()
            onOpenVitalsMeasurementEntry(type)
        }
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item(key = "screenshot_import") {
                // The grid's own gutter and gap: 12 dp at the sides, 4 + 4 dp to the first row.
                tech.mmarca.openvitals.features.imports.screenshot.ScreenshotImportCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs),
                )
            }
            item {
            ManualEntryWidgetGrid(
                visibleIds = visibleIds,
                specsById = specsById,
                isEditingWidgets = state.isEditingWidgets,
                onMoveWidgetToTarget = viewModel::moveWidgetToTarget,
                onRemoveWidget = viewModel::removeWidget,
            )
        }
        if (state.isEditingWidgets) {
            hiddenManualEntryWidgets(
                hiddenSpecs = hiddenSpecs,
                onAddWidget = viewModel::addWidget,
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
