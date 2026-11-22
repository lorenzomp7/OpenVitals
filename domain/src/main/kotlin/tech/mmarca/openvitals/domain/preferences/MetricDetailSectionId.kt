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
    // The charts of one activity's detail, arranged like a metric screen's sections.
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
    // The cards around the charts, arranged and hidden as the charts are.
    ACTIVITY_DETAIL_METRICS,
    ACTIVITY_DETAIL_PLAN,
    ACTIVITY_DETAIL_ROUTE,
    ACTIVITY_DETAIL_ROUTE_ANALYSIS,
    ACTIVITY_DETAIL_NAVIGATION,
    ACTIVITY_DETAIL_SEGMENTS,
    ACTIVITY_DETAIL_SESSION,
    ACTIVITY_DETAIL_EXPORT,
}

/**
 * The general order of an activity's sections, the same for every kind, read top to bottom
 * as one story: the figures and the plan; the map; the chart of the kind itself; the heart
 * (during, then after the end); how fast (speed, splits, laps); over what ground (height,
 * gradient, the route's analysis and navigation); the rhythm of the movement (cadence, stride);
 * what the effort did (calories, training effect) and the records it set; last the session's
 * own details and its export. A kind that lacks a section skips its place.
 */
val ActivityDetailGeneralOrder: List<MetricDetailSectionId> = listOf(
    MetricDetailSectionId.ACTIVITY_DETAIL_METRICS,
    MetricDetailSectionId.ACTIVITY_DETAIL_PLAN,
    MetricDetailSectionId.ACTIVITY_DETAIL_ROUTE,
    MetricDetailSectionId.ACTIVITY_DETAIL_TYPE_CHART,
    MetricDetailSectionId.ACTIVITY_DETAIL_JUMPS,
    MetricDetailSectionId.ACTIVITY_DETAIL_HEART_RATE,
    MetricDetailSectionId.ACTIVITY_DETAIL_ZONES,
    MetricDetailSectionId.ACTIVITY_DETAIL_HEART_RATE_DROP,
    MetricDetailSectionId.ACTIVITY_DETAIL_RECOVERY,
    MetricDetailSectionId.ACTIVITY_DETAIL_SPEED,
    MetricDetailSectionId.ACTIVITY_DETAIL_SPLITS,
    MetricDetailSectionId.ACTIVITY_DETAIL_LAPS,
    MetricDetailSectionId.ACTIVITY_DETAIL_ELEVATION,
    MetricDetailSectionId.ACTIVITY_DETAIL_GRADIENT,
    MetricDetailSectionId.ACTIVITY_DETAIL_ROUTE_ANALYSIS,
    MetricDetailSectionId.ACTIVITY_DETAIL_NAVIGATION,
    MetricDetailSectionId.ACTIVITY_DETAIL_CADENCE,
    MetricDetailSectionId.ACTIVITY_DETAIL_STRIDE,
    MetricDetailSectionId.ACTIVITY_DETAIL_CALORIES,
    MetricDetailSectionId.ACTIVITY_DETAIL_TRAINING_EFFECT,
    MetricDetailSectionId.ACTIVITY_DETAIL_RECORDS,
    MetricDetailSectionId.ACTIVITY_DETAIL_SEGMENTS,
    MetricDetailSectionId.ACTIVITY_DETAIL_SESSION,
    MetricDetailSectionId.ACTIVITY_DETAIL_EXPORT,
)

/**
 * The sections drawn from the session record alone. They show at once; the others wait for
 * the samples (heart rate, speed, markers) and the records.
 */
val ActivityDetailRecordSections: Set<MetricDetailSectionId> = setOf(
    MetricDetailSectionId.ACTIVITY_DETAIL_METRICS,
    MetricDetailSectionId.ACTIVITY_DETAIL_PLAN,
    MetricDetailSectionId.ACTIVITY_DETAIL_ROUTE,
    MetricDetailSectionId.ACTIVITY_DETAIL_SEGMENTS,
    MetricDetailSectionId.ACTIVITY_DETAIL_SESSION,
    MetricDetailSectionId.ACTIVITY_DETAIL_EXPORT,
)

/**
 * While the samples load, the record's own sections that come before the first section
 * that waits for them in [order]: they can show without anything above them moving later.
 */
fun activityDetailSectionsShownWhileLoading(order: List<MetricDetailSectionId>): Set<MetricDetailSectionId> =
    order.filter { it in ActivityDetailGeneralOrder }
        .takeWhile { it in ActivityDetailRecordSections }
        .toSet()

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
    // An activity's charts, in the one order every kind of activity follows.
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

/** Bumped when [ActivityDetailGeneralOrder] is to replace an arrangement saved before it. */
const val ActivityDetailGeneralOrderVersion = 2

/** [order] with the activity charts put back in the general order, the rest untouched. */
fun withActivityDetailGeneralOrder(order: List<MetricDetailSectionId>): List<MetricDetailSectionId> {
    val slots = order.indices.filter { order[it] in ActivityDetailGeneralOrder }
    if (slots.isEmpty()) return order
    val inGeneralOrder = ActivityDetailGeneralOrder.filter { it in order }
    return order.toMutableList().apply { slots.forEachIndexed { i, slot -> this[slot] = inGeneralOrder[i] } }
}
