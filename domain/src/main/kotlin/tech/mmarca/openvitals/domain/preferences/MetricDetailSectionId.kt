package tech.mmarca.openvitals.domain.preferences

enum class MetricDetailSectionId {
    PERIOD_CHART,
    INTRADAY_CHART,
    SELECTED_DAY_ENTRIES,
    DAILY_GOAL,
    STATISTICS,
    METRIC_CONTEXT,
    CROSS_METRIC_INSIGHTS,
    DATA_CONFIDENCE,
    ENTRIES,
    ACTIVITY_SUMMARY,
    ACTIVITY_WEEK_OVERVIEW,
    ACTIVITY_KEY_METRICS,
    VITALS_HEART_SECTION,
    VITALS_CARDIOVASCULAR_SECTION,
    VITALS_RESPIRATORY_SECTION,
    // The cycle screen's own sections. All new ids, so a saved order still shows them in this default order.
    CYCLE_TODAY,
    CYCLE_CALENDAR,
    CYCLE_OBSERVATIONS,
    CYCLE_PILL,
    CYCLE_ENTRIES,
    CYCLE_ESTIMATE,
    CYCLE_TIP,
    CYCLE_STATISTICS,
    CYCLE_SETUP,
    CYCLE_HISTORY,
    CYCLE_THERMAL,
    CYCLE_PATTERNS,
    CYCLE_NIGHTLY,
    // 113: the charts of one activity's detail, arranged like a metric screen's sections.
    ACTIVITY_DETAIL_SPLITS,
    ACTIVITY_DETAIL_HEART_RATE,
    ACTIVITY_DETAIL_CALORIES,
    ACTIVITY_DETAIL_HEART_RATE_DROP,
    ACTIVITY_DETAIL_TYPE_CHART,
    ACTIVITY_DETAIL_ZONES,
    ACTIVITY_DETAIL_RECOVERY,
    ACTIVITY_DETAIL_SPEED,
    ACTIVITY_DETAIL_ELEVATION,
    ACTIVITY_DETAIL_CADENCE,
    ACTIVITY_DETAIL_GRADIENT,
    ACTIVITY_DETAIL_TRAINING_EFFECT,
    ACTIVITY_DETAIL_STRIDE,
    ACTIVITY_DETAIL_LAPS,
    ACTIVITY_DETAIL_JUMPS,
    ACTIVITY_DETAIL_RECORDS,
}

/**
 * 132: the general order of an activity's charts, the same for every kind: the heart first,
 * then how fast and over what ground, then the rhythm of the movement, then what the effort
 * did; the records last, read after the rest. A kind that lacks a chart skips its place.
 */
val ActivityDetailGeneralOrder: List<MetricDetailSectionId> = listOf(
    MetricDetailSectionId.ACTIVITY_DETAIL_HEART_RATE,
    MetricDetailSectionId.ACTIVITY_DETAIL_ZONES,
    MetricDetailSectionId.ACTIVITY_DETAIL_TYPE_CHART,
    MetricDetailSectionId.ACTIVITY_DETAIL_JUMPS,
    MetricDetailSectionId.ACTIVITY_DETAIL_SPEED,
    MetricDetailSectionId.ACTIVITY_DETAIL_ELEVATION,
    MetricDetailSectionId.ACTIVITY_DETAIL_GRADIENT,
    MetricDetailSectionId.ACTIVITY_DETAIL_SPLITS,
    MetricDetailSectionId.ACTIVITY_DETAIL_LAPS,
    MetricDetailSectionId.ACTIVITY_DETAIL_CADENCE,
    MetricDetailSectionId.ACTIVITY_DETAIL_STRIDE,
    MetricDetailSectionId.ACTIVITY_DETAIL_CALORIES,
    MetricDetailSectionId.ACTIVITY_DETAIL_TRAINING_EFFECT,
    MetricDetailSectionId.ACTIVITY_DETAIL_HEART_RATE_DROP,
    MetricDetailSectionId.ACTIVITY_DETAIL_RECOVERY,
    MetricDetailSectionId.ACTIVITY_DETAIL_RECORDS,
)

val DefaultMetricDetailSectionOrder: List<MetricDetailSectionId> = listOf(
    MetricDetailSectionId.ACTIVITY_SUMMARY,
    MetricDetailSectionId.ACTIVITY_WEEK_OVERVIEW,
    MetricDetailSectionId.ACTIVITY_KEY_METRICS,
    MetricDetailSectionId.PERIOD_CHART,
    MetricDetailSectionId.INTRADAY_CHART,
    MetricDetailSectionId.SELECTED_DAY_ENTRIES,
    MetricDetailSectionId.DAILY_GOAL,
    MetricDetailSectionId.STATISTICS,
    MetricDetailSectionId.METRIC_CONTEXT,
    MetricDetailSectionId.CROSS_METRIC_INSIGHTS,
    MetricDetailSectionId.DATA_CONFIDENCE,
    MetricDetailSectionId.ENTRIES,
    MetricDetailSectionId.VITALS_HEART_SECTION,
    MetricDetailSectionId.VITALS_CARDIOVASCULAR_SECTION,
    MetricDetailSectionId.VITALS_RESPIRATORY_SECTION,
    MetricDetailSectionId.CYCLE_TODAY,
    MetricDetailSectionId.CYCLE_CALENDAR,
    MetricDetailSectionId.CYCLE_OBSERVATIONS,
    MetricDetailSectionId.CYCLE_PILL,
    MetricDetailSectionId.CYCLE_ENTRIES,
    MetricDetailSectionId.CYCLE_ESTIMATE,
    MetricDetailSectionId.CYCLE_TIP,
    MetricDetailSectionId.CYCLE_STATISTICS,
    MetricDetailSectionId.CYCLE_SETUP,
    MetricDetailSectionId.CYCLE_HISTORY,
    MetricDetailSectionId.CYCLE_THERMAL,
    MetricDetailSectionId.CYCLE_PATTERNS,
    MetricDetailSectionId.CYCLE_NIGHTLY,
    // 132: an activity's charts, in the one order every kind of activity follows.
    *ActivityDetailGeneralOrder.toTypedArray(),
)

fun metricDetailSectionOrderFromStored(storedIds: List<String>?): List<MetricDetailSectionId> {
    if (storedIds == null) return DefaultMetricDetailSectionOrder

    val parsedIds = storedIds.mapNotNull { storedId ->
        runCatching { MetricDetailSectionId.valueOf(storedId) }.getOrNull()
    }.distinct()

    if (parsedIds.isEmpty()) return DefaultMetricDetailSectionOrder

    val merged = parsedIds.toMutableList()
    DefaultMetricDetailSectionOrder.forEach { sectionId ->
        if (sectionId !in merged) {
            merged += sectionId
        }
    }
    return merged
}

/** 132: bumped when [ActivityDetailGeneralOrder] is to replace an arrangement saved before it. */
const val ActivityDetailGeneralOrderVersion = 1

/** 132: [order] with the activity charts put back in the general order, the rest untouched. */
fun withActivityDetailGeneralOrder(order: List<MetricDetailSectionId>): List<MetricDetailSectionId> {
    val slots = order.indices.filter { order[it] in ActivityDetailGeneralOrder }
    if (slots.isEmpty()) return order
    val inGeneralOrder = ActivityDetailGeneralOrder.filter { it in order }
    return order.toMutableList().apply { slots.forEachIndexed { i, slot -> this[slot] = inGeneralOrder[i] } }
}
