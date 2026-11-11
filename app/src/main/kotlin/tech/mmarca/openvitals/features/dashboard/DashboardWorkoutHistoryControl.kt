package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton

@Composable
internal fun DashboardWorkoutHistoryControl(state: DashboardWorkoutHistoryState, onLoadMore: () -> Unit) {
    Column(Modifier.padding(horizontal = DashboardScreenPadding, vertical = 8.dp)) {
        state.error?.let { ErrorMessage(it.resolve() ?: stringResource(R.string.unknown_error)) }
        if (state.canLoadMore) OpenVitalsOutlinedButton(
            onClick = onLoadMore,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (state.isLoading) R.string.loading else if (state.error == tech.mmarca.openvitals.core.presentation.ScreenError.PermissionDenied) R.string.action_grant_permission else R.string.dashboard_history_load_older))
        }
    }
}
