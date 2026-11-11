package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.ui.components.PeriodNavigator
import tech.mmarca.openvitals.ui.components.timeRangeLabel
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode

@Composable
internal fun DashboardPeriodSelector(
    state: DashboardUiState,
    onSelectRange: (TimeRange) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Box {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text(timeRangeLabel(state.selectedRange))
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                TimeRange.entries.forEach { range ->
                    DropdownMenuItem(
                        text = { Text(timeRangeLabel(range)) },
                        onClick = { expanded = false; onSelectRange(range) },
                    )
                }
            }
        }
        PeriodNavigator(
            selectedRange = state.selectedRange,
            period = state.dashboardPeriod(),
            canGoForward = state.canAdvancePeriod(),
            onPreviousPeriod = onPrevious,
            onNextPeriod = onNext,
            onOpenCalendar = onOpenCalendar,
            weekPeriodMode = state.activityWeekMode.toWeekPeriodMode(),
        )
    }
}
