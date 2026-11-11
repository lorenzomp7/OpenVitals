package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R

/** The main reading counts as the first detail. Small tiles never display extra lines. */
internal fun dashboardDetailCount(size: DashboardTileSize, requested: Int): Int =
    if (size == DashboardTileSize.SMALL) 1 else requested.coerceIn(2, 3)

@Composable
internal fun DashboardTileOptionsDialog(
    title: String,
    size: DashboardTileSize,
    count: Int,
    onSize: (DashboardTileSize) -> Unit,
    onCount: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.dashboard_widget_size_action))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardTileSize.entries.forEach { option ->
                        FilterChip(
                            selected = size == option,
                            onClick = { onSize(option) },
                            label = { Text("${option.columns}×${option.rows}") },
                        )
                    }
                }
                Text(stringResource(R.string.dashboard_details_title))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val choices = if (size == DashboardTileSize.SMALL) listOf(1) else listOf(2, 3)
                    choices.forEach { option ->
                        FilterChip(
                            selected = dashboardDetailCount(size, count) == option,
                            onClick = { onCount(option) },
                            label = { Text(option.toString()) },
                        )
                    }
                }
                Text(stringResource(R.string.dashboard_details_help))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_save)) } },
    )
}
