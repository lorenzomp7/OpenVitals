package tech.mmarca.openvitals.features.dashboard

import java.time.temporal.ChronoUnit
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.PeriodSelection
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode

internal fun DashboardUiState.dashboardPeriod(): DatePeriod =
    customPeriod ?: periodFor(selectedRange, selectedDate, weekPeriodMode = activityWeekMode.toWeekPeriodMode())

internal fun DashboardUiState.canAdvancePeriod(): Boolean {
    customPeriod?.let { return it.end.isBefore(java.time.LocalDate.now()) }
    val selection = PeriodSelection(selectedRange, selectedDate)
    return selection.nextPeriod(weekPeriodMode = activityWeekMode.toWeekPeriodMode()) != selection
}

/** Cumulative metrics use period goals; sleep is an average per recorded night. */
internal fun DashboardDailyGoals.forPeriod(period: DatePeriod?): DashboardDailyGoals {
    if (period == null) return this
    val days = (ChronoUnit.DAYS.between(period.start, period.end) + 1).coerceAtLeast(1).toDouble()
    return copy(
        steps = steps * days,
        distanceMeters = distanceMeters * days,
        caloriesOutKcal = caloriesOutKcal * days,
        activeCaloriesKcal = activeCaloriesKcal * days,
        floors = floors * days,
        elevationMeters = elevationMeters * days,
        wheelchairPushes = wheelchairPushes * days,
        hydrationLiters = hydrationLiters * days,
        caloriesInKcal = caloriesInKcal * days,
        proteinGrams = proteinGrams * days,
        carbsGrams = carbsGrams * days,
        fatGrams = fatGrams * days,
        mindfulnessMinutes = mindfulnessMinutes * days,
    )
}
