package tech.mmarca.openvitals.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.settings.OpenWatchMoveNoticeViewModel

@Composable
fun OpenWatchMoveNoticeDialog(
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.openwatch_move_title)) },
        text = { Text(stringResource(R.string.openwatch_move_body)) },
        confirmButton = {
            OpenVitalsTextButton(onClick = onClose) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

/** Shows the notice while the ViewModel says it is due; every way out dismisses it. */
@Composable
fun OpenWatchMoveNoticePrompt(viewModel: OpenWatchMoveNoticeViewModel) {
    val visible by viewModel.visible.collectAsStateWithLifecycle()
    if (visible) OpenWatchMoveNoticeDialog(onClose = viewModel::onDismissed)
}
