package tech.mmarca.openvitals.features.imports.screenshot

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.healthconnect.screenshotPermissions
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher

@Composable
internal fun ScreenshotImportCard(modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(modifier) {
        TextButton(onClick = { open = true }) { Text(stringResource(R.string.screenshot_import_title)) }
    }
    if (open) ScreenshotImportDialog(onDismiss = { open = false })
}

@Composable
internal fun ScreenshotImportDialog(onDismiss: () -> Unit, initialUri: android.net.Uri? = null, viewModel: ScreenshotImportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { confirmed = false; viewModel.scan(uri) }
    }
    LaunchedEffect(initialUri) { initialUri?.let(viewModel::scanShared) }
    LaunchedEffect(state.suggestedStart, state.suggestedEnd) {
        start = state.suggestedStart
        end = state.suggestedEnd
        confirmed = false
    }
    var typeMenu by remember { mutableStateOf(false) }
    val permissions = rememberHealthConnectPermissionLauncher()
    val dialogHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    Dialog(onDismissRequest = { if (!state.busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.padding(16.dp).fillMaxWidth().heightIn(max = dialogHeight), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.screenshot_import_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.screenshot_import_body))
                Button(onClick = { picker.launch(arrayOf("image/*")) }, enabled = !state.busy) {
                    Text(stringResource(R.string.screenshot_import_pick))
                }
                if (state.busy) CircularProgressIndicator()
                state.values.forEachIndexed { index, item ->
                    Row {
                        OutlinedTextField(value = item.value, onValueChange = { confirmed = false; viewModel.edit(index, it) },
                            label = { Text(when (item.kind) { "kg" -> stringResource(R.string.metric_weight); "body_fat" -> stringResource(R.string.metric_body_fat); "kcal" -> stringResource(R.string.screenshot_import_total_kcal); "duration_min" -> stringResource(R.string.screenshot_import_duration); else -> item.kind }) },
                            enabled = !state.busy && !state.saved, modifier = Modifier.weight(1f))
                        TextButton(onClick = { confirmed = false; viewModel.remove(index) }, enabled = !state.busy && !state.saved) { Text("×") }
                    }
                }
                if (state.values.isNotEmpty()) {
                    if (state.values.any { it.kind == "duration_min" }) {
                        Box {
                            TextButton(onClick = { typeMenu = true }, enabled = !state.busy && !state.saved) {
                                Text(state.exerciseType?.let { stringResource(tech.mmarca.openvitals.features.activity.exerciseTypeLabelRes(it)) }
                                    ?: stringResource(R.string.activity_entry_type_label))
                            }
                            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                                tech.mmarca.openvitals.domain.model.ExerciseTypeNames.byType.keys.forEach { type ->
                                    DropdownMenuItem(text = { Text(stringResource(tech.mmarca.openvitals.features.activity.exerciseTypeLabelRes(type))) },
                                        onClick = { viewModel.selectExerciseType(type); confirmed = false; typeMenu = false })
                                }
                            }
                        }
                    }
                    Text(stringResource(R.string.screenshot_import_time_help))
                    ScreenshotTimePicker(start, stringResource(R.string.screenshot_import_start), !state.busy && !state.saved) { start = it; confirmed = false }
                    ScreenshotTimePicker(end, stringResource(R.string.screenshot_import_end), !state.busy && !state.saved) { end = it; confirmed = false }
                    Row {
                        Checkbox(confirmed, { confirmed = it }, enabled = !state.busy && !state.saved)
                        Text(stringResource(R.string.screenshot_import_confirm), modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = { permissions.launch(screenshotPermissions(state.values.map { it.kind })) }, enabled = !state.busy) {
                        Text(stringResource(R.string.screenshot_import_permissions))
                    }
                    Button(onClick = { viewModel.save(start, end) }, enabled = (state.values.none { it.kind == "duration_min" } || state.exerciseType != null) && confirmed && !state.busy && !state.saved && start.isNotBlank() && end.isNotBlank()) {
                        Text(stringResource(R.string.screenshot_import_save))
                    }
                } else if (!state.busy && !state.saved) Text(stringResource(R.string.screenshot_import_empty))
                state.error.resolve()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.saved) Text(stringResource(R.string.screenshot_import_saved))
                TextButton(onClick = onDismiss, enabled = !state.busy) { Text(stringResource(R.string.screenshot_import_close)) }
            }
        }
    }
}

@Composable
private fun ScreenshotTimePicker(value: String, label: String, enabled: Boolean, onValue: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val date = runCatching { java.time.LocalDateTime.parse(value) }.getOrNull()
    OutlinedButton(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = {
        val seed = date ?: java.time.LocalDateTime.now()
        android.app.DatePickerDialog(context, { _, year, month, day ->
            android.app.TimePickerDialog(context, { _, hour, minute ->
                onValue(java.time.LocalDateTime.of(year, month + 1, day, hour, minute).toString())
            }, seed.hour, seed.minute, android.text.format.DateFormat.is24HourFormat(context)).show()
        }, seed.year, seed.monthValue - 1, seed.dayOfMonth).show()
    }) {
        Column(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(date?.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss")) ?: "—")
        }
    }
}
