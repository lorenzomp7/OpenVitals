package tech.mmarca.openvitals.features.dashboard

import androidx.compose.ui.graphics.Color
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.MedicalRecordsColor
import tech.mmarca.openvitals.ui.theme.MindfulnessColor
import tech.mmarca.openvitals.ui.theme.NutritionColor
import tech.mmarca.openvitals.ui.theme.SleepColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor

/**
 * The group a dashboard tile belongs to. Tiles of one group share one colour, so the
 * colour says what kind of reading a tile holds before its title is read.
 */
enum class DashboardWidgetCategory(val accentColor: Color) {
    /** How far and how high the user moved. */
    MOVEMENT(StepsColor),

    /** The energy spent: total, active and at rest. */
    ENERGY(CaloriesColor),

    /** Training load and sessions. */
    TRAINING(WorkoutColor),
    SLEEP(SleepColor),

    /** What was eaten and drunk. */
    NUTRITION(NutritionColor),

    /** Body measurements and composition. */
    BODY(WeightColor),

    /** Heart, breathing, blood and temperature readings. */
    VITALS(VitalsColor),
    MINDFULNESS(MindfulnessColor),
    CYCLE(CycleColor),
    MEDICAL(MedicalRecordsColor),
}

fun dashboardWidgetCategory(id: DashboardWidgetId): DashboardWidgetCategory = when (id) {
    DashboardWidgetId.STEPS,
    DashboardWidgetId.DISTANCE,
    DashboardWidgetId.FLOORS,
    DashboardWidgetId.ELEVATION,
    DashboardWidgetId.WHEELCHAIR_PUSHES,
    -> DashboardWidgetCategory.MOVEMENT
    DashboardWidgetId.CALORIES_OUT,
    DashboardWidgetId.ACTIVE_CALORIES,
    DashboardWidgetId.BMR,
    -> DashboardWidgetCategory.ENERGY
    DashboardWidgetId.WEEKLY_CARDIO_LOAD,
    DashboardWidgetId.CARDIO_LOAD,
    DashboardWidgetId.WORKOUT,
    -> DashboardWidgetCategory.TRAINING
    DashboardWidgetId.SLEEP -> DashboardWidgetCategory.SLEEP
    DashboardWidgetId.HYDRATION,
    DashboardWidgetId.CALORIES_IN,
    DashboardWidgetId.PROTEIN,
    DashboardWidgetId.CARBS,
    DashboardWidgetId.FAT,
    DashboardWidgetId.CAFFEINE,
    -> DashboardWidgetCategory.NUTRITION
    DashboardWidgetId.WEIGHT,
    DashboardWidgetId.HEIGHT,
    DashboardWidgetId.BMI,
    DashboardWidgetId.FFMI,
    DashboardWidgetId.BODY_FAT,
    DashboardWidgetId.LEAN_MASS,
    DashboardWidgetId.BONE_MASS,
    DashboardWidgetId.BODY_WATER_MASS,
    -> DashboardWidgetCategory.BODY
    DashboardWidgetId.BODY_ENERGY,
    DashboardWidgetId.AVG_HEART_RATE,
    DashboardWidgetId.RESTING_HEART_RATE,
    DashboardWidgetId.HRV,
    DashboardWidgetId.BLOOD_PRESSURE,
    DashboardWidgetId.SPO2,
    DashboardWidgetId.VO2_MAX,
    DashboardWidgetId.RESPIRATORY_RATE,
    DashboardWidgetId.BODY_TEMPERATURE,
    DashboardWidgetId.SKIN_TEMPERATURE,
    DashboardWidgetId.BLOOD_GLUCOSE,
    DashboardWidgetId.OPENWATCH,
    -> DashboardWidgetCategory.VITALS
    DashboardWidgetId.MINDFULNESS -> DashboardWidgetCategory.MINDFULNESS
    DashboardWidgetId.CYCLE -> DashboardWidgetCategory.CYCLE
    DashboardWidgetId.MEDICAL_RECORDS -> DashboardWidgetCategory.MEDICAL
}
