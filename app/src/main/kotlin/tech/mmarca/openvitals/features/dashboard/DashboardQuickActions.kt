package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.BrandPillButton

/** 107: "Start workout" on the left and "+ Log" on the right, each half the row. The pencil is in the header (106). */
@Composable
internal fun DashboardQuickActions(
    onOpenLog: () -> Unit,
    onStartActivity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DashboardActionsSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 96: the actions dressed like the animated toggle.
        BrandPillButton(
            onClick = onStartActivity,
            modifier = Modifier
                .weight(1f)
                .testTag("start_workout_action")
                .height(DashboardQuickActionHeight),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(DashboardQuickActionIconSize),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.dashboard_action_start_workout),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BrandPillButton(
            onClick = onOpenLog,
            modifier = Modifier
                .weight(1f)
                .height(DashboardQuickActionHeight),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(DashboardQuickActionIconSize),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.dashboard_action_log),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
