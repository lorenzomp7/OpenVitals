package tech.mmarca.openvitals.features.imports.screenshot

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.manualentry.rememberManualEntryWritePermissionRequester
import tech.mmarca.openvitals.ui.theme.Spacing

@Composable
internal fun ScreenshotImportCard(modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(modifier) {
        TextButton(onClick = { open = true }) { Text(stringResource(R.string.screenshot_import_title)) }
    }
    if (open) ScreenshotImportDialog(onDismiss = { open = false })
}

@Composable
internal fun ScreenshotImportDialog(onDismiss: () -> Unit, initialUri: Uri? = null, viewModel: ScreenshotImportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val requester = rememberManualEntryWritePermissionRequester {
        scope.launch { viewModel.refreshWritePermissions(showDenied = true) }
    }
    LaunchedEffect(state.values.map { it.kind }) { viewModel.refreshWritePermissions() }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        scope.launch { viewModel.refreshWritePermissions() }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { confirmed = false; viewModel.scan(uri) }
    }
    LaunchedEffect(initialUri) { initialUri?.let(viewModel::scanShared) }
    LaunchedEffect(state.suggestedStart, state.suggestedEnd) {
        start = state.suggestedStart; end = state.suggestedEnd; confirmed = false
    }
    val actions = ScreenshotImportActions(
        onPick = { picker.launch(arrayOf("image/*")) },
        onEdit = { index, value -> confirmed = false; viewModel.edit(index, value) },
        onRemove = { confirmed = false; viewModel.remove(it) },
        onStart = { start = it; if (!screenshotNeedsInterval(state.values)) end = it; confirmed = false },
        onEnd = { end = it; confirmed = false },
        onConfirmed = { confirmed = it },
        onType = { viewModel.selectExerciseType(it); confirmed = false },
        onPermissions = {
            scope.launch {
                try {
                    val missing = viewModel.missingWritePermissions()
                    if (missing.isNotEmpty()) requester.launch(missing)
                    else viewModel.refreshWritePermissions()
                } catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    viewModel.permissionScreenFailed()
                }
            }
        },
        onSave = { viewModel.save(start, end) }, onClose = onDismiss,
    )
    Dialog(onDismissRequest = { if (!state.busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(Spacing.lg).fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f),
            shape = MaterialTheme.shapes.large,
        ) { ScreenshotImportContent(state, start, end, confirmed, actions) }
    }
}
