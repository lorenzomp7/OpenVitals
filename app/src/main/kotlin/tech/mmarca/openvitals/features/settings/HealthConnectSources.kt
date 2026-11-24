package tech.mmarca.openvitals.features.settings

import java.time.Instant

/**
 * One app contributing to Health Connect. A diagnostic: a WearOS watch
 * shows up as its vendor app's package.
 *
 * [declaredWrites] are the data types the app's manifest asks to write ("heart rate", "sleep"),
 * null when the app is not installed on this phone (a source synced from another phone).
 * [lastSeen] is null for an app that declares a checked type but sent none of it this week.
 */
data class HealthConnectSource(
    val packageName: String,
    val recordCount: Int,
    val lastSeen: Instant?,
    val metrics: Set<String>,
    val declaredWrites: Set<String>? = null,
) {
    /** A friendly name for the well-known contributors, else the raw package. */
    val displayName: String
        get() = FRIENDLY_NAMES[packageName] ?: packageName

    /** Checked types the app declares it writes, none of which arrived: "writes it, nothing came". */
    fun declaredNotSeen(checked: Set<String>): Set<String> =
        declaredWrites.orEmpty().intersect(checked) - metrics

    companion object {
        const val UNKNOWN_PACKAGE = "unknown"

        private val FRIENDLY_NAMES = mapOf(
            "tech.mmarca.openvitals" to "OpenVitals (this app)",
            "com.sec.android.app.shealth" to "Samsung Health",
            "com.google.android.apps.healthdata" to "Health Connect",
            "com.google.android.apps.fitness" to "Google Fit",
            "com.fitbit.FitbitMobile" to "Fitbit",
            "com.garmin.android.apps.connectmobile" to "Garmin Connect",
            UNKNOWN_PACKAGE to "Unknown source",
        )
    }
}

private const val HEALTH_WRITE_PREFIX = "android.permission.health.WRITE_"

/**
 * The data types a set of requested permissions declares writing, named as the diagnostics
 * names them: `android.permission.health.WRITE_HEART_RATE` is "heart rate".
 */
fun declaredHealthWrites(requestedPermissions: Collection<String>): Set<String> =
    requestedPermissions
        .filter { it.startsWith(HEALTH_WRITE_PREFIX) }
        .map { it.removePrefix(HEALTH_WRITE_PREFIX).lowercase().replace('_', ' ') }
        .toSet()

/**
 * Folds (source, time) observations per metric into a list, most recent first.
 *
 * [declarations] maps a package to the health permissions its manifest requests (null when it
 * is not installed here). An app there that declares writing one of the checked metrics but
 * sent none of it joins the list with no records, after the contributors.
 */
fun aggregateHealthConnectSources(
    byMetric: Map<String, List<Pair<String, Instant>>>,
    declarations: Map<String, Set<String>?> = emptyMap(),
): List<HealthConnectSource> {
    data class Acc(var count: Int, var lastSeen: Instant, val metrics: MutableSet<String>)

    val acc = mutableMapOf<String, Acc>()
    for ((metric, observations) in byMetric) {
        for ((source, time) in observations) {
            val key = source.trim().ifEmpty { HealthConnectSource.UNKNOWN_PACKAGE }
            val existing = acc[key]
            if (existing == null) {
                acc[key] = Acc(count = 1, lastSeen = time, metrics = mutableSetOf(metric))
            } else {
                existing.count += 1
                if (time.isAfter(existing.lastSeen)) existing.lastSeen = time
                existing.metrics += metric
            }
        }
    }
    val declaredWrites = declarations.mapValues { (_, requested) -> requested?.let(::declaredHealthWrites) }
    val contributors = acc.map { (packageName, value) ->
        HealthConnectSource(
            packageName = packageName,
            recordCount = value.count,
            lastSeen = value.lastSeen,
            metrics = value.metrics,
            declaredWrites = declaredWrites[packageName],
        )
    }.sortedByDescending { it.lastSeen }
    val checked = byMetric.keys
    val silent = declaredWrites
        .filter { (packageName, writes) ->
            packageName !in acc && writes.orEmpty().any { it in checked }
        }
        .map { (packageName, writes) ->
            HealthConnectSource(
                packageName = packageName,
                recordCount = 0,
                lastSeen = null,
                metrics = emptySet(),
                declaredWrites = writes,
            )
        }
        .sortedBy { it.packageName }
    return contributors + silent
}
