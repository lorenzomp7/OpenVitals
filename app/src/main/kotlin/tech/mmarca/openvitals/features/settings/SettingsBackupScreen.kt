package tech.mmarca.openvitals.features.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.LayoutMetrics

@Composable
internal fun SettingsBackupScreen(viewModel: SettingsBackupViewModel, onImported: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::export)
    }
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::preview)
    }
    LaunchedEffect(state.message) {
        state.message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.consumeMessage() }
    }
    LaunchedEffect(state.imported) { if (state.imported) onImported() }
    SettingsSectionList {
        item { SectionHeader(stringResource(R.string.settings_backup_title)) }
        item {
            SettingsBackupCard(
                busy = state.busy,
                onExport = { saver.launch("OpenVitals-settings.json") },
                onImport = { opener.launch(arrayOf("application/json", "text/json", "text/plain", "application/octet-stream")) },
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
        }
    }
    state.preview?.let { preview ->
        AlertDialog(
            onDismissRequest = { if (!state.busy) viewModel.dismissPreview() },
            title = { Text(stringResource(R.string.settings_backup_import)) },
            text = { Text(stringResource(R.string.settings_backup_confirm, preview.count)) },
            confirmButton = {
                OpenVitalsTextButton(onClick = viewModel::restore, enabled = !state.busy) {
                    Text(stringResource(R.string.settings_backup_import))
                }
            },
            dismissButton = {
                OpenVitalsTextButton(onClick = viewModel::dismissPreview, enabled = !state.busy) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
internal fun SettingsBackupCard(busy: Boolean, onExport: () -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_backup_body), style = MaterialTheme.typography.bodyMedium)
            OpenVitalsTonalButton(onClick = onExport, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_backup_export))
            }
            OpenVitalsOutlinedButton(onClick = onImport, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_backup_import))
            }
            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
