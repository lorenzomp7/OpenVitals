package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.BrandBlue
import tech.mmarca.openvitals.ui.components.BrandDay
import tech.mmarca.openvitals.ui.components.BrandGreen
import tech.mmarca.openvitals.ui.components.BrandRed
import tech.mmarca.openvitals.ui.components.BrandSync
import tech.mmarca.openvitals.ui.components.BrandYellow
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.Spacing

/** One of today's four goals, as the toggle fills it and the summary lists it. */
internal data class DashboardDayGoal(
    val id: DashboardWidgetId,
    val title: String,
    val value: String,
    val goal: String?,
    val fraction: Float,
    val color: Color,
    val icon: ImageVector,
)

/** 94: the toggle's four lobes, in their colours: steps, calories, activity, sleep. */
private val DayGoalWidgets = listOf(
    DashboardWidgetId.STEPS to BrandBlue,
    DashboardWidgetId.ACTIVE_CALORIES to BrandRed,
    DashboardWidgetId.WORKOUT to BrandYellow,
    DashboardWidgetId.SLEEP to BrandGreen,
)

@Composable
internal fun dashboardDayGoals(display: DashboardDisplayState): List<DashboardDayGoal> =
    DayGoalWidgets.map { (id, color) ->
        val model = display.widgets[id]
        val fraction = model?.sleepScore?.let { it.score / 100f } ?: model?.progress?.fraction ?: 0f
        DashboardDayGoal(
            id = id,
            title = dashboardWidgetTitle(id),
            value = model?.sleepScore?.score?.toString()
                ?: model?.value?.takeIf { model.hasValue && !model.isLoading }?.text?.takeIf { it.isNotBlank() }
                ?: "–",
            goal = model?.progress?.goalLabelValue?.text,
            fraction = fraction.coerceIn(0f, 1f),
            color = color,
            icon = dashboardWidgetIcon(id),
        )
    }

internal fun brandDay(name: String?, goals: List<DashboardDayGoal>, syncPaused: Boolean, refreshing: Boolean): BrandDay =
    BrandDay(
        name = name,
        fills = goals.map { it.fraction },
        sync = when {
            syncPaused -> BrandSync.PAUSED
            refreshing -> BrandSync.SYNCING
            else -> BrandSync.UP_TO_DATE
        },
    )

/** 94: what tapping the toggle opens: today's four goals and where the sync stands. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardDaySummarySheet(
    goals: List<DashboardDayGoal>,
    sync: BrandSync,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xxl).padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                stringResource(R.string.brand_summary_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            goals.forEach { goal ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss(); onOpenMetric(goal.id) },
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(goal.icon, contentDescription = null, tint = goal.color, modifier = Modifier.size(Spacing.xl))
                        Spacer(Modifier.width(Spacing.sm))
                        Text(goal.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(
                            goal.goal?.let { stringResource(R.string.brand_summary_goal, goal.value, it) } ?: goal.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { goal.fraction },
                        color = goal.color,
                        trackColor = goal.color.copy(alpha = Emphasis.wash),
                        modifier = Modifier.fillMaxWidth().height(Spacing.sm),
                    )
                }
            }
            Text(
                stringResource(
                    when (sync) {
                        BrandSync.UP_TO_DATE -> R.string.brand_sync_up_to_date
                        BrandSync.SYNCING -> R.string.brand_sync_syncing
                        BrandSync.PAUSED -> R.string.brand_sync_paused
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
