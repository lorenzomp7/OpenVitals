package tech.mmarca.openvitals.domain.insights

/** The steps taken and the active energy recorded in one slice of time. */
data class ActivityEnergyBucket(val steps: Long, val activeKcal: Double)

/** Net cost of walking: about half a kilocalorie per kilogram per kilometre above rest. */
private const val WalkingNetKcalPerKgPerKm = 0.5

/** Without a profile: an adult of 70 kg whose step is 0.415 of a 175 cm height. */
private const val DefaultWeightKg = 70.0
private const val DefaultHeightCm = 175.0
private const val StrideShareOfHeight = 0.415

/**
 * 142: the active energy of one step for a body of [weightKg] and [heightCm]: the step's length
 * (0.415 of the height) times the net cost of walking (0.5 kcal per kg per km).
 */
fun activeKcalPerStep(weightKg: Double?, heightCm: Double?): Double {
    val weight = weightKg?.takeIf { it in 20.0..400.0 } ?: DefaultWeightKg
    val height = heightCm?.takeIf { it in 90.0..250.0 } ?: DefaultHeightCm
    val strideKm = height * StrideShareOfHeight / 100_000.0
    return WalkingNetKcalPerKgPerKm * weight * strideKm
}

/**
 * 142: active calories that count the steps as well. Many watches write active energy only for
 * their workouts, so a day of walking read as almost nothing. Slice by slice, the larger of the
 * recorded energy and the steps' own: walking adds its steps, a workout keeps its recorded
 * energy and its steps are not counted twice. Null when neither was recorded.
 */
fun activeCaloriesWithSteps(buckets: List<ActivityEnergyBucket>, kcalPerStep: Double): Double? {
    if (buckets.none { it.steps > 0 || it.activeKcal > 0.0 }) return null
    return buckets.sumOf { maxOf(it.activeKcal, it.steps * kcalPerStep) }
}
