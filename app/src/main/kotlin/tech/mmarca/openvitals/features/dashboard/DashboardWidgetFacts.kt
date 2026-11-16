package tech.mmarca.openvitals.features.dashboard

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.BloodPressureCategory
import tech.mmarca.openvitals.domain.insights.BmiCategory
import tech.mmarca.openvitals.domain.insights.FfmiCategory
import tech.mmarca.openvitals.domain.insights.bloodPressureInterpretation
import tech.mmarca.openvitals.domain.insights.bmiInterpretation
import tech.mmarca.openvitals.domain.insights.ffmiInterpretation
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.preferences.BloodPressureGuideline
import tech.mmarca.openvitals.features.body.adjustedFfmiWith
import kotlin.math.roundToInt

/**
 * Figures a tile's details are written from, worked out from the day's readings.
 * The mapper has no resources: [dashboardWidgetDetails] does the wording.
 */
@Immutable
data class DashboardWidgetFacts(
    /** The goal, also when the day has no reading yet. */
    val goal: DisplayValue? = null,
    /** The personal baseline the reading compares with. */
    val baseline: DisplayValue? = null,
    /** Body fat as a mass, from the latest weight. */
    val fatMass: DisplayValue? = null,
    /** This mass as a share of the latest weight. */
    val shareOfWeight: DisplayValue? = null,
    /** The energy a macronutrient brings, and its share of the calories eaten. */
    val macroEnergy: DisplayValue? = null,
    val macroEnergyShare: DisplayValue? = null,
    /** Grams of protein, carbs and fat eaten, as counts; null where nothing was logged. */
    val macroGrams: DashboardMacroGrams? = null,
    val bmiCategory: BmiCategory? = null,
    val ffmiCategory: FfmiCategory? = null,
    val bloodPressureCategory: BloodPressureCategory? = null,
)

@Immutable
data class DashboardMacroGrams(val protein: String?, val carbs: String?, val fat: String?)

/** Energy in a gram of each macronutrient (Atwater factors). */
private const val ProteinKcalPerGram = 4.0
private const val CarbsKcalPerGram = 4.0
private const val FatKcalPerGram = 9.0

internal fun dashboardWidgetFacts(
    id: DashboardWidgetId,
    widget: DashboardWidgetDisplayModel,
    data: DashboardData,
    goals: DashboardDailyGoals,
    formatter: UnitFormatter,
    bloodPressureGuideline: BloodPressureGuideline,
): DashboardWidgetFacts {
    val goal = widget.progress?.goalLabelValue ?: goalWithoutReading(id, goals, formatter)
    val weight = data.weightKg?.takeIf { it > 0.0 }
    fun shareOfWeight(kg: Double?) = kg?.takeIf { it > 0.0 }?.let { mass ->
        weight?.let { formatter.percent(mass / it * 100.0, decimals = 0) }
    }
    fun macro(grams: Double?, kcalPerGram: Double): DashboardWidgetFacts {
        val kcal = grams?.takeIf { it > 0.0 }?.let { it * kcalPerGram }
        return DashboardWidgetFacts(
            goal = goal,
            macroEnergy = kcal?.let(formatter::energy),
            macroEnergyShare = kcal?.let { energy ->
                data.caloriesInKcal?.takeIf { it > 0.0 }?.let { formatter.percent(energy / it * 100.0, decimals = 0) }
            },
            macroGrams = macroGrams(data, formatter),
        )
    }
    return when (id) {
        DashboardWidgetId.RESTING_HEART_RATE -> DashboardWidgetFacts(
            baseline = data.restingHeartRateBaselineBpm?.takeIf { it > 0L && data.restingHeartRateBpm > 0L }
                ?.let(formatter::heartRate),
        )
        DashboardWidgetId.HRV -> DashboardWidgetFacts(
            baseline = data.hrvBaselineRmssdMs?.takeIf { it > 0.0 && data.hrvRmssdMs != null }?.let(formatter::hrv),
        )
        DashboardWidgetId.BODY_FAT -> DashboardWidgetFacts(
            fatMass = data.bodyFatPercent.takeIf { it > 0.0 }?.let { percent ->
                weight?.let { formatter.bodyMass(it * percent / 100.0) }
            },
        )
        DashboardWidgetId.LEAN_MASS -> DashboardWidgetFacts(shareOfWeight = shareOfWeight(data.leanMassKg))
        DashboardWidgetId.BONE_MASS -> DashboardWidgetFacts(shareOfWeight = shareOfWeight(data.boneMassKg))
        DashboardWidgetId.BODY_WATER_MASS -> DashboardWidgetFacts(shareOfWeight = shareOfWeight(data.bodyWaterMassKg))
        DashboardWidgetId.BMI -> DashboardWidgetFacts(bmiCategory = data.bmi?.let(::bmiInterpretation)?.category)
        DashboardWidgetId.FFMI -> DashboardWidgetFacts(
            ffmiCategory = data.ffmi.adjustedFfmiWith(data.heightCm)?.let(::ffmiInterpretation)?.category,
        )
        DashboardWidgetId.BLOOD_PRESSURE -> DashboardWidgetFacts(
            bloodPressureCategory = data.latestSystolicMmHg?.let { systolic ->
                data.latestDiastolicMmHg?.let { diastolic ->
                    bloodPressureInterpretation(systolic, diastolic, bloodPressureGuideline)?.category
                }
            },
        )
        DashboardWidgetId.CALORIES_IN -> DashboardWidgetFacts(goal = goal, macroGrams = macroGrams(data, formatter))
        DashboardWidgetId.PROTEIN -> macro(data.proteinGrams, ProteinKcalPerGram)
        DashboardWidgetId.CARBS -> macro(data.carbsGrams, CarbsKcalPerGram)
        DashboardWidgetId.FAT -> macro(data.fatGrams, FatKcalPerGram)
        else -> DashboardWidgetFacts(goal = goal)
    }
}

private fun macroGrams(data: DashboardData, formatter: UnitFormatter): DashboardMacroGrams? {
    fun grams(value: Double?) = value?.takeIf { it > 0.0 }?.let { formatter.count(it.roundToInt()) }
    val macros = DashboardMacroGrams(grams(data.proteinGrams), grams(data.carbsGrams), grams(data.fatGrams))
    return macros.takeIf { it.protein != null || it.carbs != null || it.fat != null }
}

/** The goal of a tile whose progress only exists once there is a reading. */
private fun goalWithoutReading(id: DashboardWidgetId, goals: DashboardDailyGoals, formatter: UnitFormatter): DisplayValue? =
    when (id) {
        DashboardWidgetId.ACTIVE_CALORIES -> formatter.energy(goals.activeCaloriesKcal)
        DashboardWidgetId.FLOORS -> DisplayValue(formatter.count(goals.floors.roundToInt()), "")
        DashboardWidgetId.ELEVATION -> formatter.elevation(goals.elevationMeters)
        DashboardWidgetId.WHEELCHAIR_PUSHES -> DisplayValue(formatter.count(goals.wheelchairPushes.roundToInt()), "")
        DashboardWidgetId.SLEEP -> DisplayValue(formatter.duration((goals.sleepHours * 3_600_000.0).toLong()), "")
        DashboardWidgetId.CALORIES_IN -> formatter.energy(goals.caloriesInKcal)
        DashboardWidgetId.PROTEIN -> DisplayValue(formatter.count(goals.proteinGrams.roundToInt()), "g")
        DashboardWidgetId.CARBS -> DisplayValue(formatter.count(goals.carbsGrams.roundToInt()), "g")
        DashboardWidgetId.FAT -> DisplayValue(formatter.count(goals.fatGrams.roundToInt()), "g")
        else -> null
    }.takeIf { goalTargetIsSet(id, goals) }

/** A zero goal is no goal. */
private fun goalTargetIsSet(id: DashboardWidgetId, goals: DashboardDailyGoals): Boolean = when (id) {
    DashboardWidgetId.ACTIVE_CALORIES -> goals.activeCaloriesKcal > 0.0
    DashboardWidgetId.FLOORS -> goals.floors > 0.0
    DashboardWidgetId.ELEVATION -> goals.elevationMeters > 0.0
    DashboardWidgetId.WHEELCHAIR_PUSHES -> goals.wheelchairPushes > 0.0
    DashboardWidgetId.SLEEP -> goals.sleepHours > 0.0
    DashboardWidgetId.CALORIES_IN -> goals.caloriesInKcal > 0.0
    DashboardWidgetId.PROTEIN -> goals.proteinGrams > 0.0
    DashboardWidgetId.CARBS -> goals.carbsGrams > 0.0
    DashboardWidgetId.FAT -> goals.fatGrams > 0.0
    else -> true
}
