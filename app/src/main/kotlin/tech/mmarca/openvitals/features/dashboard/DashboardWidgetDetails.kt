package tech.mmarca.openvitals.features.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.BmiCategory
import tech.mmarca.openvitals.domain.insights.FfmiCategory
import tech.mmarca.openvitals.features.cycle.cycleSecondaryLineText
import tech.mmarca.openvitals.features.heart.bloodPressureCategoryText

/**
 * What a tile shows under its reading, most telling first: the goal, what the reading
 * means, then the readings it goes with. Each line is about the metric itself; goal
 * completion percentages and the selected period are never details.
 */
@Composable
internal fun dashboardWidgetDetails(
    model: DashboardWidgetDisplayModel,
    display: DashboardDisplayState,
    @Suppress("UNUSED_PARAMETER") formatter: UnitFormatter,
): List<String> {
    if (model.isLoading) return emptyList()
    val details = mutableListOf<String>()
    fun add(value: String?) { value?.takeIf { it.isNotBlank() }?.let(details::add) }
    val facts = model.facts
    val goal = facts.goal?.let { stringResource(dashboardGoalLabelRes(model.id, display.isPeriod)) + ": " + it.text }
    val related: @Composable (DashboardWidgetId) -> Unit = { id -> add(dashboardRelatedDetail(id, display)) }
    when (model.id) {
        DashboardWidgetId.STEPS -> { add(goal); related(DashboardWidgetId.DISTANCE); related(DashboardWidgetId.ACTIVE_CALORIES) }
        DashboardWidgetId.DISTANCE -> { add(goal); related(DashboardWidgetId.STEPS); related(DashboardWidgetId.ELEVATION) }
        DashboardWidgetId.CALORIES_OUT -> { add(goal); related(DashboardWidgetId.ACTIVE_CALORIES); related(DashboardWidgetId.BMR) }
        DashboardWidgetId.ACTIVE_CALORIES -> { add(goal); related(DashboardWidgetId.CALORIES_OUT); related(DashboardWidgetId.STEPS) }
        DashboardWidgetId.FLOORS -> { add(goal); related(DashboardWidgetId.ELEVATION); related(DashboardWidgetId.STEPS) }
        DashboardWidgetId.ELEVATION -> { add(goal); related(DashboardWidgetId.FLOORS); related(DashboardWidgetId.DISTANCE) }
        DashboardWidgetId.WHEELCHAIR_PUSHES -> { add(goal); related(DashboardWidgetId.DISTANCE); related(DashboardWidgetId.ACTIVE_CALORIES) }
        DashboardWidgetId.SLEEP -> {
            model.value?.takeIf { model.hasValue }?.let { add(it.text) }
            model.sleepScore?.let { add(sleepScoreRatingLabel(it.score)) }
            add(goal)
        }
        DashboardWidgetId.BODY_ENERGY -> {
            model.bodyEnergySubtitle?.let {
                add(stringResource(R.string.dashboard_body_energy_subtitle, it.startScore, it.charged, it.drained))
            }
            related(DashboardWidgetId.SLEEP)
            related(DashboardWidgetId.HRV)
        }
        DashboardWidgetId.HYDRATION -> { add(goal); related(DashboardWidgetId.CAFFEINE) }
        DashboardWidgetId.CALORIES_IN -> {
            add(goal)
            add(facts.macroGrams?.let { macroGramsLine(it) })
            related(DashboardWidgetId.CALORIES_OUT)
        }
        DashboardWidgetId.PROTEIN, DashboardWidgetId.CARBS, DashboardWidgetId.FAT -> {
            add(goal)
            add(macroEnergyLine(facts))
            related(DashboardWidgetId.CALORIES_IN)
        }
        DashboardWidgetId.CAFFEINE -> {
            model.caffeineConsumedTodayMg?.let { add(stringResource(R.string.dashboard_caffeine_today_subtitle, it)) }
            related(DashboardWidgetId.HYDRATION)
        }
        DashboardWidgetId.WEIGHT -> { add(model.measurementSubtitle); related(DashboardWidgetId.BMI); related(DashboardWidgetId.BODY_FAT) }
        DashboardWidgetId.HEIGHT -> { add(model.measurementSubtitle); related(DashboardWidgetId.BMI) }
        DashboardWidgetId.BMI -> {
            add(facts.bmiCategory?.let { bmiCategoryText(it) })
            related(DashboardWidgetId.WEIGHT)
            related(DashboardWidgetId.HEIGHT)
        }
        DashboardWidgetId.FFMI -> {
            add(facts.ffmiCategory?.let { ffmiCategoryText(it) })
            related(DashboardWidgetId.LEAN_MASS)
            related(DashboardWidgetId.BODY_FAT)
        }
        DashboardWidgetId.BODY_FAT -> {
            add(facts.fatMass?.let { stringResource(R.string.dashboard_detail_fat_mass, it.text) })
            related(DashboardWidgetId.LEAN_MASS)
            related(DashboardWidgetId.WEIGHT)
        }
        DashboardWidgetId.LEAN_MASS -> { addShareOfWeight(facts, ::add); related(DashboardWidgetId.BODY_FAT); related(DashboardWidgetId.WEIGHT) }
        DashboardWidgetId.BONE_MASS, DashboardWidgetId.BODY_WATER_MASS -> {
            addShareOfWeight(facts, ::add)
            related(DashboardWidgetId.WEIGHT)
            related(DashboardWidgetId.LEAN_MASS)
        }
        DashboardWidgetId.BMR -> { related(DashboardWidgetId.CALORIES_OUT); related(DashboardWidgetId.LEAN_MASS) }
        DashboardWidgetId.AVG_HEART_RATE -> { related(DashboardWidgetId.RESTING_HEART_RATE); related(DashboardWidgetId.HRV) }
        DashboardWidgetId.RESTING_HEART_RATE -> {
            addBaseline(facts, ::add)
            related(DashboardWidgetId.HRV)
            related(DashboardWidgetId.AVG_HEART_RATE)
        }
        DashboardWidgetId.HRV -> { addBaseline(facts, ::add); related(DashboardWidgetId.RESTING_HEART_RATE) }
        DashboardWidgetId.BLOOD_PRESSURE -> {
            add(facts.bloodPressureCategory?.let { bloodPressureCategoryText(it) })
            related(DashboardWidgetId.RESTING_HEART_RATE)
        }
        DashboardWidgetId.SPO2 -> { related(DashboardWidgetId.RESPIRATORY_RATE); related(DashboardWidgetId.RESTING_HEART_RATE) }
        DashboardWidgetId.VO2_MAX -> { related(DashboardWidgetId.RESTING_HEART_RATE); related(DashboardWidgetId.HRV) }
        DashboardWidgetId.RESPIRATORY_RATE -> { related(DashboardWidgetId.SPO2); related(DashboardWidgetId.RESTING_HEART_RATE) }
        DashboardWidgetId.BODY_TEMPERATURE -> { related(DashboardWidgetId.SKIN_TEMPERATURE); related(DashboardWidgetId.RESTING_HEART_RATE) }
        DashboardWidgetId.SKIN_TEMPERATURE -> { related(DashboardWidgetId.BODY_TEMPERATURE); related(DashboardWidgetId.RESTING_HEART_RATE) }
        DashboardWidgetId.BLOOD_GLUCOSE -> { related(DashboardWidgetId.CARBS); related(DashboardWidgetId.CALORIES_IN) }
        DashboardWidgetId.MINDFULNESS -> { add(goal); related(DashboardWidgetId.HRV); related(DashboardWidgetId.RESTING_HEART_RATE) }
        DashboardWidgetId.WEEKLY_CARDIO_LOAD, DashboardWidgetId.CARDIO_LOAD -> model.weeklyCardioLoad?.let { load ->
            add(stringResource(R.string.dashboard_weekly_cardio_load_progress, load.currentScore, load.targetScore))
            add(stringResource(R.string.dashboard_cardio_today_points, load.todayScore))
        }
        DashboardWidgetId.CYCLE -> (model.cycle as? CycleWidgetDisplay.RecordedDay)?.let { recorded ->
            add(cycleSecondaryLineText(recorded.secondary, DateTimeFormatterProvider().mediumDate()))
        }
        DashboardWidgetId.OPENWATCH -> model.openWatch?.let { watch ->
            watch.batteryPercent?.let { add(stringResource(R.string.dashboard_watch_battery, it)) }
            watch.lastSyncedAt?.let {
                add(
                    stringResource(
                        R.string.dashboard_watch_last_sync,
                        android.text.format.DateUtils.getRelativeTimeSpanString(
                            it.toEpochMilli(),
                            System.currentTimeMillis(),
                            android.text.format.DateUtils.MINUTE_IN_MILLIS,
                        ).toString(),
                    ),
                )
            }
        }
        DashboardWidgetId.MEDICAL_RECORDS, DashboardWidgetId.WORKOUT -> Unit
    }
    return details.distinct()
}

/** Another tile's reading, under a label short enough to fit inside a ring. */
@Composable
private fun dashboardRelatedDetail(id: DashboardWidgetId, display: DashboardDisplayState): String? {
    val value = display.widgets[id]?.takeIf { it.hasValue && !it.isLoading }?.value?.text?.takeIf { it.isNotBlank() }
        ?: return null
    return dashboardDetailLabel(id) + ": " + value
}

@Composable
internal fun dashboardDetailLabel(id: DashboardWidgetId): String = when (id) {
    DashboardWidgetId.CALORIES_OUT -> stringResource(R.string.dashboard_detail_label_total_calories)
    DashboardWidgetId.ACTIVE_CALORIES -> stringResource(R.string.dashboard_detail_label_active_calories)
    DashboardWidgetId.FLOORS -> stringResource(R.string.dashboard_detail_label_floors)
    DashboardWidgetId.WEIGHT -> stringResource(R.string.metric_weight)
    DashboardWidgetId.AVG_HEART_RATE -> stringResource(R.string.dashboard_detail_label_avg_heart_rate)
    DashboardWidgetId.RESTING_HEART_RATE -> stringResource(R.string.cardio_load_resting_hr)
    DashboardWidgetId.HRV -> stringResource(R.string.home_widget_hrv_short)
    DashboardWidgetId.RESPIRATORY_RATE -> stringResource(R.string.dashboard_detail_label_respiratory_rate)
    DashboardWidgetId.SKIN_TEMPERATURE -> stringResource(R.string.dashboard_detail_label_skin_temperature)
    else -> dashboardWidgetTitle(id)
}

@Composable
private fun macroGramsLine(macros: DashboardMacroGrams): String = listOfNotNull(
    macros.protein?.let { stringResource(R.string.macro_protein_short, it) },
    macros.carbs?.let { stringResource(R.string.macro_carbs_short, it) },
    macros.fat?.let { stringResource(R.string.macro_fat_short, it) },
).joinToString(" · ")

@Composable
private fun macroEnergyLine(facts: DashboardWidgetFacts): String? {
    val energy = facts.macroEnergy ?: return null
    val share = facts.macroEnergyShare ?: return "≈ " + energy.text
    return stringResource(R.string.dashboard_detail_macro_energy, energy.text, share.text)
}

@Composable
private fun addShareOfWeight(facts: DashboardWidgetFacts, add: (String?) -> Unit) {
    add(facts.shareOfWeight?.let { stringResource(R.string.dashboard_detail_share_of_weight, it.text) })
}

@Composable
private fun addBaseline(facts: DashboardWidgetFacts, add: (String?) -> Unit) {
    add(facts.baseline?.let { stringResource(R.string.stat_baseline) + ": " + it.text })
}

@Composable
private fun bmiCategoryText(category: BmiCategory): String = stringResource(
    when (category) {
        BmiCategory.UNDERWEIGHT -> R.string.interpretation_bmi_underweight
        BmiCategory.HEALTHY -> R.string.interpretation_bmi_healthy
        BmiCategory.OVERWEIGHT -> R.string.interpretation_bmi_overweight
        BmiCategory.OBESITY_CLASS_1 -> R.string.interpretation_bmi_obesity_1
        BmiCategory.OBESITY_CLASS_2 -> R.string.interpretation_bmi_obesity_2
        BmiCategory.OBESITY_CLASS_3 -> R.string.interpretation_bmi_obesity_3
    },
)

@Composable
private fun ffmiCategoryText(category: FfmiCategory): String = stringResource(
    when (category) {
        FfmiCategory.BELOW_AVERAGE -> R.string.interpretation_ffmi_below_average
        FfmiCategory.AVERAGE -> R.string.interpretation_ffmi_average
        FfmiCategory.ABOVE_AVERAGE -> R.string.interpretation_ffmi_above_average
        FfmiCategory.EXCELLENT -> R.string.interpretation_ffmi_excellent
        FfmiCategory.SUPERIOR -> R.string.interpretation_ffmi_superior
        FfmiCategory.EXCEPTIONAL -> R.string.interpretation_ffmi_exceptional
        FfmiCategory.ELITE -> R.string.interpretation_ffmi_elite
    },
)

internal fun dashboardGoalLabelRes(id: DashboardWidgetId, isPeriod: Boolean): Int = when {
    !isPeriod -> R.string.daily_goal
    id == DashboardWidgetId.SLEEP -> R.string.dashboard_nightly_goal
    else -> R.string.dashboard_period_goal
}
