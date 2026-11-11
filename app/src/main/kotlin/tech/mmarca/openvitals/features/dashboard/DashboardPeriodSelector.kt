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
    PeriodNavigator(
        selectedRange = state.selectedRange,
        period = state.dashboardPeriod(),
        title = state.customPeriod?.let { "${it.start} – ${it.end}" },
        subtitle = state.customPeriod?.let { stringResource(R.string.report_lookback_custom) },
        canGoForward = state.canAdvancePeriod(),
        onPreviousPeriod = onPrevious,
        onNextPeriod = onNext,
        onOpenCalendar = onOpenCalendar,
        weekPeriodMode = state.activityWeekMode.toWeekPeriodMode(),
        modifier = modifier,
    )
}
