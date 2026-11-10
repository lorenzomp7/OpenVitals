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
private fun ScreenshotImportDialog(onDismiss: () -> Unit, viewModel: ScreenshotImportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { confirmed = false; viewModel.scan(uri) }
    }
    val permissions = rememberHealthConnectPermissionLauncher()
    Dialog(onDismissRequest = { if (!state.busy) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.large) {
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
                            label = { Text(if (item.kind == "kcal") stringResource(R.string.screenshot_import_total_kcal) else item.kind) },
                            enabled = !state.busy && !state.saved, modifier = Modifier.weight(1f))
                        TextButton(onClick = { confirmed = false; viewModel.remove(index) }, enabled = !state.busy && !state.saved) { Text("×") }
                    }
                }
                if (state.values.isNotEmpty()) {
                    Text(stringResource(R.string.screenshot_import_time_help))
                    OutlinedTextField(start, { start = it; confirmed = false }, label = { Text(stringResource(R.string.screenshot_import_start)) }, enabled = !state.busy && !state.saved)
                    OutlinedTextField(end, { end = it; confirmed = false }, label = { Text(stringResource(R.string.screenshot_import_end)) }, enabled = !state.busy && !state.saved)
                    Row {
                        Checkbox(confirmed, { confirmed = it }, enabled = !state.busy && !state.saved)
                        Text(stringResource(R.string.screenshot_import_confirm), modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = { permissions.launch(screenshotPermissions(state.values.map { it.kind })) }, enabled = !state.busy) {
                        Text(stringResource(R.string.screenshot_import_permissions))
                    }
                    Button(onClick = { viewModel.save(start, end) }, enabled = confirmed && !state.busy && !state.saved && start.isNotBlank() && end.isNotBlank()) {
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
