package tech.mmarca.openvitals.features.imports.screenshot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

internal data class ScreenshotImportActions(
    val onPick: () -> Unit = {}, val onEdit: (Int, String) -> Unit = { _, _ -> }, val onRemove: (Int) -> Unit = {},
    val onStart: (String) -> Unit = {}, val onEnd: (String) -> Unit = {}, val onConfirmed: (Boolean) -> Unit = {},
    val onType: (Int) -> Unit = {}, val onPermissions: () -> Unit = {}, val onSave: () -> Unit = {}, val onClose: () -> Unit = {},
)

internal fun screenshotNeedsInterval(values: List<ScreenshotValue>) = values.any { it.kind in setOf("duration_min", "steps", "km", "kcal") }

@Composable
internal fun ScreenshotImportContent(state: ScreenshotImportState, start: String, end: String, confirmed: Boolean, actions: ScreenshotImportActions) {
    val editable = !state.busy && !state.saved
    val canSave = state.values.any { it.kind != "bmi" } &&
        (state.values.none { it.kind == "body_water_percent" } || state.values.any { it.kind == "kg" }) &&
        (state.values.none { it.kind == "duration_min" } || state.exerciseType != null) &&
        confirmed && state.writesGranted && editable && start.isNotBlank() && end.isNotBlank()
    Column(Modifier.fillMaxWidth().padding(Spacing.lg).testTag("screenshot_import_form")) {
        Text(stringResource(R.string.screenshot_import_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.sm))
        Text(stringResource(R.string.screenshot_import_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedButton(onClick = actions.onPick, enabled = !state.busy, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().heightIn(min = LayoutMetrics.minTouchTarget)) {
                Text(stringResource(R.string.screenshot_import_pick), style = MaterialTheme.typography.labelLarge)
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.values.forEachIndexed { index, item ->
                ScreenshotValueField(item, editable, { actions.onEdit(index, it) }, { actions.onRemove(index) })
            }
            if (state.values.any { it.kind == "bmi" }) ScreenshotHint(stringResource(R.string.screenshot_bmi_preview))
            if (state.values.any { it.kind == "body_water_percent" }) ScreenshotHint(stringResource(R.string.screenshot_water_percent))
            if (state.values.isNotEmpty()) {
                if (state.values.any { it.kind == "duration_min" }) ScreenshotWorkoutPicker(state, actions.onType)
                ScreenshotHint(stringResource(R.string.screenshot_import_time_help))
                ScreenshotTimePicker(start, stringResource(R.string.screenshot_import_start), editable, actions.onStart)
                if (screenshotNeedsInterval(state.values)) ScreenshotTimePicker(end, stringResource(R.string.screenshot_import_end), editable, actions.onEnd)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(confirmed, actions.onConfirmed, enabled = editable)
                    Text(stringResource(R.string.screenshot_import_confirm), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                if (state.writesGranted) ScreenshotHint(stringResource(R.string.onboarding_status_granted))
                else OutlinedButton(onClick = actions.onPermissions, enabled = !state.busy, shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().heightIn(min = LayoutMetrics.minTouchTarget).testTag("screenshot_write_permissions")) {
                    Text(stringResource(R.string.screenshot_import_permissions), style = MaterialTheme.typography.labelLarge)
                }
            } else if (!state.busy && !state.saved) ScreenshotHint(stringResource(R.string.screenshot_import_empty))
            state.error.resolve()?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            if (state.saved) Text(stringResource(R.string.screenshot_import_saved), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = actions.onClose, enabled = !state.busy) {
                Text(stringResource(R.string.screenshot_import_close), style = MaterialTheme.typography.labelLarge)
            }
            Button(onClick = actions.onSave, enabled = canSave, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f).heightIn(min = LayoutMetrics.minTouchTarget)) {
                Text(stringResource(R.string.screenshot_import_save), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ScreenshotValueField(item: ScreenshotValue, enabled: Boolean, onEdit: (String) -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        OutlinedTextField(value = item.value, onValueChange = onEdit, enabled = enabled, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            label = { Text(screenshotValueLabel(item.kind), style = MaterialTheme.typography.labelMedium) },
            suffix = { Text(screenshotValueUnit(item.kind), style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.weight(1f).testTag("screenshot_value_${item.kind}"))
        IconButton(onClick = onRemove, enabled = enabled) {
            Icon(Icons.Outlined.Close, stringResource(R.string.action_remove), modifier = Modifier.size(Spacing.xl))
        }
    }
}

@Composable
private fun ScreenshotHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ScreenshotWorkoutPicker(state: ScreenshotImportState, onType: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, enabled = !state.busy && !state.saved, shape = MaterialTheme.shapes.medium) {
            Text(state.exerciseType?.let { stringResource(tech.mmarca.openvitals.features.activity.exerciseTypeLabelRes(it)) }
                ?: stringResource(R.string.activity_entry_type_label), style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(open, { open = false }) {
            tech.mmarca.openvitals.domain.model.ExerciseTypeNames.byType.keys.forEach { type ->
                DropdownMenuItem(text = { Text(stringResource(tech.mmarca.openvitals.features.activity.exerciseTypeLabelRes(type)), style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onType(type); open = false })
            }
        }
    }
}

@Composable
internal fun screenshotValueLabel(kind: String): String = stringResource(when (kind) {
    "kg" -> R.string.metric_weight; "body_fat" -> R.string.metric_body_fat; "bmi" -> R.string.metric_bmi
    "lean_mass" -> R.string.metric_lean_mass; "body_water_percent" -> R.string.metric_body_water
    "body_water" -> R.string.metric_body_water_mass; "bone_mass" -> R.string.metric_bone_mass; "bmr" -> R.string.metric_bmr
    "kcal" -> R.string.screenshot_import_total_kcal; "duration_min" -> R.string.screenshot_import_duration
    "steps" -> R.string.metric_steps; "km" -> R.string.metric_distance
    else -> R.string.settings_csv_import_metric_heart_rate
})

internal fun screenshotValueUnit(kind: String): String = when (kind) {
    "kg", "lean_mass", "body_water", "bone_mass" -> "kg"
    "body_fat", "body_water_percent" -> "%"
    "km" -> "km"; "kcal" -> "kcal"; "bmr" -> "kcal/d"; "bpm" -> "bpm"; "duration_min" -> "min"
    else -> ""
}
