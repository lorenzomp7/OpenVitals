package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.timeRangeLabel
import java.time.LocalDate

@Composable
internal fun DashboardPeriodDialog(
    state: DashboardUiState,
    onDismiss: () -> Unit,
    onRange: (TimeRange) -> Unit,
    onDate: (LocalDate) -> Unit,
    onCustom: (LocalDate, LocalDate) -> Unit,
) {
    var custom by remember { mutableStateOf(state.customPeriod != null) }
    var start by remember { mutableStateOf(state.dashboardPeriod().start) }
    var end by remember { mutableStateOf(state.dashboardPeriod().end.coerceAtMost(LocalDate.now())) }
    var picker by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cd_open_calendar)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeRange.entries.forEach { range ->
                    TextButton(onClick = { custom = false; onRange(range) }, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = !custom && state.selectedRange == range, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(timeRangeLabel(range))
                    }
                }
                TextButton(onClick = { custom = true }, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = custom, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.report_lookback_custom))
                }
                if (custom) {
                    OutlinedButton(onClick = { picker = 1 }) { Text("${stringResource(R.string.report_custom_start_date)}: $start") }
                    OutlinedButton(onClick = { picker = 2 }) { Text("${stringResource(R.string.report_custom_end_date)}: $end") }
                    if (end.isBefore(start)) Text(stringResource(R.string.report_custom_range_invalid), color = MaterialTheme.colorScheme.error)
                } else {
                    OutlinedButton(onClick = { picker = 3 }) { Text(state.selectedDate.toString()) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !custom || !end.isBefore(start), onClick = {
                if (custom) onCustom(start, end) else onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
    if (picker != 0) HealthDatePickerDialog(
        selectedDate = when (picker) { 1 -> start; 2 -> end; else -> state.selectedDate },
        onDismiss = { picker = 0 },
        onConfirm = { date ->
            when (picker) { 1 -> start = date; 2 -> end = date; else -> onDate(date) }
            picker = 0
        },
    )
}
