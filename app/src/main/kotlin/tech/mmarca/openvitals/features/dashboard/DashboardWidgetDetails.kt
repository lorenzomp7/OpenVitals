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
 *
 * 153-180: a tile set to three values shows its reading and two lines whatever data the
 * day holds, so the related readings run on (see [dashboardRelatedWidgetIds]) past the two
 * the tile needs; the renderer takes what fits.
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
    when (model.id) {
        DashboardWidgetId.STEPS, DashboardWidgetId.DISTANCE, DashboardWidgetId.CALORIES_OUT,
        DashboardWidgetId.ACTIVE_CALORIES, DashboardWidgetId.FLOORS, DashboardWidgetId.ELEVATION,
        DashboardWidgetId.WHEELCHAIR_PUSHES, DashboardWidgetId.HYDRATION, DashboardWidgetId.MINDFULNESS,
        -> add(goal)
        DashboardWidgetId.SLEEP -> {
            // The ring shows the score when there is one; the hours then come here, never twice.
            model.sleepScore?.let { score ->
                model.value?.takeIf { model.hasValue }?.let { add(it.text) }
                add(sleepScoreRatingLabel(score.score))
            }
            add(goal)
        }
        DashboardWidgetId.BODY_ENERGY -> model.bodyEnergySubtitle?.let {
            add(stringResource(R.string.dashboard_body_energy_subtitle, it.startScore, it.charged, it.drained))
        }
        DashboardWidgetId.CALORIES_IN -> {
            add(goal)
            add(facts.macroGrams?.let { macroGramsLine(it) })
        }
        DashboardWidgetId.PROTEIN, DashboardWidgetId.CARBS, DashboardWidgetId.FAT -> {
            add(goal)
            add(macroEnergyLine(facts))
        }
        DashboardWidgetId.CAFFEINE ->
            model.caffeineConsumedTodayMg?.let { add(stringResource(R.string.dashboard_caffeine_today_subtitle, it)) }
        DashboardWidgetId.WEIGHT, DashboardWidgetId.HEIGHT -> add(model.measurementSubtitle)
        DashboardWidgetId.BMI -> add(facts.bmiCategory?.let { bmiCategoryText(it) })
        DashboardWidgetId.FFMI -> add(facts.ffmiCategory?.let { ffmiCategoryText(it) })
        DashboardWidgetId.BODY_FAT -> add(facts.fatMass?.let { stringResource(R.string.dashboard_detail_fat_mass, it.text) })
        DashboardWidgetId.LEAN_MASS, DashboardWidgetId.BONE_MASS, DashboardWidgetId.BODY_WATER_MASS ->
            addShareOfWeight(facts, ::add)
        DashboardWidgetId.RESTING_HEART_RATE, DashboardWidgetId.HRV -> addBaseline(facts, ::add)
        DashboardWidgetId.BLOOD_PRESSURE -> add(facts.bloodPressureCategory?.let { bloodPressureCategoryText(it) })
        // 156: the ring already says 438; the line under it says what it runs to, not "438 of 470".
        DashboardWidgetId.WEEKLY_CARDIO_LOAD, DashboardWidgetId.CARDIO_LOAD -> model.weeklyCardioLoad?.let { load ->
            add(stringResource(R.string.dashboard_weekly_cardio_target, load.targetScore))
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
        DashboardWidgetId.BMR, DashboardWidgetId.AVG_HEART_RATE, DashboardWidgetId.SPO2,
        DashboardWidgetId.VO2_MAX, DashboardWidgetId.RESPIRATORY_RATE, DashboardWidgetId.BODY_TEMPERATURE,
        DashboardWidgetId.SKIN_TEMPERATURE, DashboardWidgetId.BLOOD_GLUCOSE, DashboardWidgetId.MEDICAL_RECORDS,
        DashboardWidgetId.WORKOUT,
        -> Unit
    }
    dashboardRelatedWidgetIds(model.id).forEach { add(dashboardRelatedDetail(it, display)) }
    return details.distinct()
}

/**
 * The readings a tile's details go on with, closest first: the same category before the
 * others. The list runs past the two a tile shows, so a day missing some of them still
 * fills the tile. 158, 170: elevation and wheelchair pushes are not about distance.
 */
internal fun dashboardRelatedWidgetIds(id: DashboardWidgetId): List<DashboardWidgetId> =
    when (id) {
        W.STEPS -> listOf(W.DISTANCE, W.ACTIVE_CALORIES, W.FLOORS, W.CALORIES_OUT)
        W.DISTANCE -> listOf(W.STEPS, W.ACTIVE_CALORIES, W.ELEVATION, W.CALORIES_OUT)
        W.CALORIES_OUT -> listOf(W.ACTIVE_CALORIES, W.BMR, W.STEPS, W.CALORIES_IN)
        W.ACTIVE_CALORIES -> listOf(W.CALORIES_OUT, W.STEPS, W.BMR, W.WEEKLY_CARDIO_LOAD)
        W.FLOORS -> listOf(W.ELEVATION, W.STEPS, W.ACTIVE_CALORIES)
        W.ELEVATION -> listOf(W.FLOORS, W.STEPS, W.ACTIVE_CALORIES)
        W.WHEELCHAIR_PUSHES -> listOf(W.ACTIVE_CALORIES, W.CALORIES_OUT, W.WEEKLY_CARDIO_LOAD)
        W.SLEEP -> listOf(W.HRV, W.RESTING_HEART_RATE, W.BODY_ENERGY)
        W.BODY_ENERGY -> listOf(W.SLEEP, W.HRV, W.RESTING_HEART_RATE, W.AVG_HEART_RATE)
        W.HYDRATION -> listOf(W.CAFFEINE, W.CALORIES_IN, W.ACTIVE_CALORIES)
        W.CALORIES_IN -> listOf(W.CALORIES_OUT, W.ACTIVE_CALORIES, W.BMR)
        W.PROTEIN -> listOf(W.CALORIES_IN, W.CARBS, W.FAT, W.CALORIES_OUT)
        W.CARBS -> listOf(W.CALORIES_IN, W.PROTEIN, W.FAT, W.CALORIES_OUT)
        W.FAT -> listOf(W.CALORIES_IN, W.PROTEIN, W.CARBS, W.CALORIES_OUT)
        W.CAFFEINE -> listOf(W.HYDRATION, W.CALORIES_IN, W.SLEEP, W.RESTING_HEART_RATE)
        W.WEIGHT -> listOf(W.BMI, W.BODY_FAT, W.LEAN_MASS, W.FFMI)
        W.HEIGHT -> listOf(W.BMI, W.WEIGHT, W.FFMI)
        W.BMI -> listOf(W.WEIGHT, W.HEIGHT, W.BODY_FAT)
        W.FFMI -> listOf(W.LEAN_MASS, W.BODY_FAT, W.WEIGHT, W.BMI)
        W.BODY_FAT -> listOf(W.LEAN_MASS, W.WEIGHT, W.BMI, W.FFMI)
        W.LEAN_MASS -> listOf(W.BODY_FAT, W.WEIGHT, W.FFMI, W.BMI)
        W.BONE_MASS, W.BODY_WATER_MASS -> listOf(W.WEIGHT, W.LEAN_MASS, W.BODY_FAT, W.BMI)
        W.BMR -> listOf(W.CALORIES_OUT, W.ACTIVE_CALORIES, W.LEAN_MASS, W.WEIGHT)
        W.AVG_HEART_RATE -> listOf(W.RESTING_HEART_RATE, W.HRV, W.RESPIRATORY_RATE, W.SPO2, W.WEEKLY_CARDIO_LOAD)
        W.RESTING_HEART_RATE -> listOf(W.HRV, W.AVG_HEART_RATE, W.RESPIRATORY_RATE, W.SPO2)
        W.HRV -> listOf(W.RESTING_HEART_RATE, W.AVG_HEART_RATE, W.SLEEP, W.RESPIRATORY_RATE)
        W.BLOOD_PRESSURE -> listOf(W.RESTING_HEART_RATE, W.AVG_HEART_RATE, W.HRV, W.SPO2)
        W.SPO2 -> listOf(W.RESPIRATORY_RATE, W.RESTING_HEART_RATE, W.AVG_HEART_RATE, W.HRV)
        W.VO2_MAX -> listOf(W.RESTING_HEART_RATE, W.AVG_HEART_RATE, W.WEEKLY_CARDIO_LOAD, W.HRV)
        W.RESPIRATORY_RATE -> listOf(W.SPO2, W.RESTING_HEART_RATE, W.AVG_HEART_RATE, W.HRV)
        W.BODY_TEMPERATURE -> listOf(W.SKIN_TEMPERATURE, W.RESTING_HEART_RATE, W.AVG_HEART_RATE, W.HRV)
        W.SKIN_TEMPERATURE -> listOf(W.BODY_TEMPERATURE, W.RESTING_HEART_RATE, W.AVG_HEART_RATE, W.SLEEP)
        W.BLOOD_GLUCOSE -> listOf(W.CARBS, W.CALORIES_IN, W.ACTIVE_CALORIES, W.STEPS, W.CALORIES_OUT)
        W.MINDFULNESS -> listOf(W.HRV, W.RESTING_HEART_RATE, W.SLEEP, W.AVG_HEART_RATE)
        W.WEEKLY_CARDIO_LOAD, W.CARDIO_LOAD -> listOf(W.ACTIVE_CALORIES, W.AVG_HEART_RATE, W.RESTING_HEART_RATE)
        // The cycle is read with the temperatures and the heart at rest.
        W.CYCLE -> listOf(W.BODY_TEMPERATURE, W.SKIN_TEMPERATURE, W.RESTING_HEART_RATE, W.HRV, W.SLEEP)
        // The latest readings a clinical record would hold.
        W.MEDICAL_RECORDS -> listOf(W.BLOOD_PRESSURE, W.BLOOD_GLUCOSE, W.RESTING_HEART_RATE, W.WEIGHT, W.BMI, W.SPO2)
        W.OPENWATCH -> listOf(W.STEPS, W.RESTING_HEART_RATE, W.AVG_HEART_RATE)
        W.WORKOUT -> emptyList()
    }

/** Short for the table above. */
private typealias W = DashboardWidgetId

/** Another tile's reading, under a label short enough to fit inside a ring. */
@Composable
private fun dashboardRelatedDetail(id: DashboardWidgetId, display: DashboardDisplayState): String? {
    val widget = display.widgets[id]?.takeIf { !it.isLoading } ?: return null
    val value = widget.weeklyCardioLoad?.currentScore?.toString()
        ?: widget.value?.takeIf { widget.hasValue }?.text?.takeIf { it.isNotBlank() }
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

/** 154: "Goal" on a day, short enough for a ring; a period names whose goal it is. */
internal fun dashboardGoalLabelRes(id: DashboardWidgetId, isPeriod: Boolean): Int = when {
    !isPeriod -> R.string.dashboard_detail_goal
    id == DashboardWidgetId.SLEEP -> R.string.dashboard_nightly_goal
    else -> R.string.dashboard_period_goal
}
