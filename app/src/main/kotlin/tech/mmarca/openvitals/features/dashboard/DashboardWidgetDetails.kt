package tech.mmarca.openvitals.features.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter

/** Related recorded values and measurement context; goal completion percentages are never details. */
@Composable
internal fun dashboardWidgetDetails(
    model: DashboardWidgetDisplayModel,
    display: DashboardDisplayState,
    formatter: UnitFormatter,
): List<String> {
    if (model.isLoading) return emptyList()
    val details = mutableListOf<String>()
    fun add(value: String?) { value?.takeIf { it.isNotBlank() }?.let(details::add) }
    add(model.measurementSubtitle)
    model.weeklyCardioLoad?.let { load ->
        add(stringResource(R.string.dashboard_weekly_cardio_load_progress, load.currentScore, load.targetScore))
        add(stringResource(R.string.dashboard_cardio_today_points, load.todayScore))
    }
    if (model.id == DashboardWidgetId.SLEEP) {
        model.value?.takeIf { model.hasValue }?.let { add(it.text) }
        model.sleepScore?.let { add(sleepScoreRatingLabel(it.score)) }
    }
    model.caffeineConsumedTodayMg?.let { add(stringResource(R.string.dashboard_caffeine_today_subtitle, it)) }
    model.bodyEnergySubtitle?.let { add(stringResource(R.string.dashboard_body_energy_subtitle, it.startScore, it.charged, it.drained)) }
    model.openWatch?.let { watch ->
        watch.batteryPercent?.let { add(stringResource(R.string.dashboard_watch_battery, it)) }
        watch.lastSyncedAt?.let {
            add(stringResource(R.string.dashboard_watch_last_sync,
                android.text.format.DateUtils.getRelativeTimeSpanString(it.toEpochMilli(), System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS).toString()))
        }
    }
    val related = when (model.id) {
        DashboardWidgetId.STEPS -> listOf(DashboardWidgetId.DISTANCE, DashboardWidgetId.ACTIVE_CALORIES)
        DashboardWidgetId.DISTANCE -> listOf(DashboardWidgetId.STEPS, DashboardWidgetId.ELEVATION)
        DashboardWidgetId.CALORIES_OUT -> listOf(DashboardWidgetId.ACTIVE_CALORIES, DashboardWidgetId.BMR)
        DashboardWidgetId.ACTIVE_CALORIES -> listOf(DashboardWidgetId.CALORIES_OUT, DashboardWidgetId.WORKOUT)
        DashboardWidgetId.FLOORS, DashboardWidgetId.ELEVATION -> listOf(DashboardWidgetId.ELEVATION, DashboardWidgetId.FLOORS)
        DashboardWidgetId.WORKOUT -> listOf(DashboardWidgetId.DISTANCE, DashboardWidgetId.ACTIVE_CALORIES)
        DashboardWidgetId.WEIGHT, DashboardWidgetId.BMI, DashboardWidgetId.FFMI -> listOf(DashboardWidgetId.BMI, DashboardWidgetId.WEIGHT, DashboardWidgetId.LEAN_MASS)
        DashboardWidgetId.BODY_FAT, DashboardWidgetId.LEAN_MASS, DashboardWidgetId.BONE_MASS, DashboardWidgetId.BODY_WATER_MASS -> listOf(DashboardWidgetId.WEIGHT, DashboardWidgetId.LEAN_MASS)
        DashboardWidgetId.CALORIES_IN -> listOf(DashboardWidgetId.PROTEIN, DashboardWidgetId.CARBS)
        DashboardWidgetId.PROTEIN, DashboardWidgetId.CARBS, DashboardWidgetId.FAT -> listOf(DashboardWidgetId.CALORIES_IN)
        DashboardWidgetId.AVG_HEART_RATE, DashboardWidgetId.RESTING_HEART_RATE, DashboardWidgetId.HRV -> listOf(DashboardWidgetId.RESTING_HEART_RATE, DashboardWidgetId.HRV, DashboardWidgetId.AVG_HEART_RATE)
        else -> emptyList()
    }
    model.progress?.let { add(stringResource(R.string.daily_goal) + ": " + it.goalLabelValue.text) }
    related.filter { it != model.id }.forEach { id ->
        display.widgets[id]?.takeIf { it.hasValue && !it.isLoading }?.value?.let { value ->
            add(dashboardWidgetTitle(id) + ": " + value.text)
        }
    }
    model.periodLabel?.let { add(stringResource(R.string.period_selected) + ": " + it) }
    return details.distinct()
}
