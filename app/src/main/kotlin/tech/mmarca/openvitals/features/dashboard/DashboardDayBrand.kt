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
import tech.mmarca.openvitals.domain.preferences.BrandPaletteSource
import tech.mmarca.openvitals.ui.components.BrandColors
import tech.mmarca.openvitals.ui.components.BrandDay
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.Spacing

/** One of the selected period's four goals, as the toggle fills it and the summary lists it. */
internal data class DashboardDayGoal(
    val id: DashboardWidgetId,
    val title: String,
    val value: String,
    val goal: String?,
    val fraction: Float,
    val color: Color,
    val icon: ImageVector,
)

/** 94: the toggle's four lobes, in order: steps, active calories, workout, sleep. */
private val DayGoalWidgets = listOf(
    DashboardWidgetId.STEPS,
    DashboardWidgetId.ACTIVE_CALORIES,
    DashboardWidgetId.WORKOUT,
    DashboardWidgetId.SLEEP,
)

/** 94: the order the summary lists the goals in. */
private val SummaryOrder = listOf(
    DashboardWidgetId.SLEEP,
    DashboardWidgetId.STEPS,
    DashboardWidgetId.WORKOUT,
    DashboardWidgetId.ACTIVE_CALORIES,
)

/** 97: the home screen's palette, from the source chosen in Display settings. */
@Composable
internal fun dashboardBrandColors(source: BrandPaletteSource, placedInOrder: List<DashboardWidgetId>): List<Color> {
    val scheme = MaterialTheme.colorScheme
    val hero = placedInOrder.take(3).map(::dashboardWidgetAccent)
    val tiles = placedInOrder.drop(3).take(6).map(::dashboardWidgetAccent)
    val colors = when (source) {
        BrandPaletteSource.GOOGLE -> BrandColors
        BrandPaletteSource.PHONE -> listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.inversePrimary)
        BrandPaletteSource.HERO -> hero
        BrandPaletteSource.TILES -> tiles
        BrandPaletteSource.HERO_AND_TILES -> hero + tiles
    }
    // One colour cannot run round a ring: it pairs with itself.
    return when (colors.size) {
        0 -> BrandColors
        1 -> colors + colors
        else -> colors
    }
}

/**
 * 94: the four goals of the selected period, read from the same widgets the dashboard shows for
 * it. The workout is a yes or a no: whether the period holds any session ([hadWorkout]); null
 * while the period is loading.
 */
@Composable
internal fun dashboardDayGoals(
    display: DashboardDisplayState,
    colors: List<Color> = BrandColors,
    hadWorkout: Boolean? = null,
): List<DashboardDayGoal> =
    DayGoalWidgets.mapIndexed { index, id ->
        val color = colors[index % colors.size]
        if (id == DashboardWidgetId.WORKOUT) {
            return@mapIndexed DashboardDayGoal(
                id = id,
                title = dashboardWidgetTitle(id),
                value = when (hadWorkout) {
                    true -> stringResource(R.string.brand_summary_workout_yes)
                    false -> stringResource(R.string.brand_summary_workout_no)
                    null -> "–"
                },
                goal = null,
                fraction = if (hadWorkout == true) 1f else 0f,
                color = color,
                icon = dashboardWidgetIcon(id),
            )
        }
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

internal fun brandDay(name: String?, goals: List<DashboardDayGoal>): BrandDay =
    BrandDay(name = name, fills = goals.map { it.fraction })

/**
 * 94: what tapping the toggle opens: the selected period's sleep, steps, workout and active
 * calories, under the period's own title. Health Connect's state is not its business.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardDaySummarySheet(
    title: String,
    goals: List<DashboardDayGoal>,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xxl).padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            // In the order asked for: sleep, steps, workout, active calories.
            goals.sortedBy { SummaryOrder.indexOf(it.id) }.forEach { goal ->
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
        }
    }
}
