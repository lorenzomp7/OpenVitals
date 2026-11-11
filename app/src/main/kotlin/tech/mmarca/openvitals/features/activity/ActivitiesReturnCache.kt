package tech.mmarca.openvitals.features.activity

import javax.inject.Inject
import javax.inject.Singleton

/** Short lived, in-memory presentation results so returning from the dashboard does not reread a year.
 * No Health Connect records are held. The key includes the query, profile, timezone and current grants.
 * Pull-to-refresh bypasses this cache; deletion clears it. Only the last successful result is retained.
 */
@Singleton
class ActivitiesReturnCache @Inject constructor() {
    private var key: String? = null
    private var storedAt: Long = 0
    private var result: ActivitiesLoadResult? = null

    internal fun get(fingerprint: String): ActivitiesLoadResult? =
        result.takeIf { key == fingerprint && System.nanoTime() - storedAt < 300_000_000_000L }

    internal fun put(fingerprint: String, value: ActivitiesLoadResult) {
        key = fingerprint
        result = value
        storedAt = System.nanoTime()
    }

    fun clear() { key = null; result = null }
}
