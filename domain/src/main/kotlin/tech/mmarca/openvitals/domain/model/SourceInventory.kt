package tech.mmarca.openvitals.domain.model

import java.time.Instant

/**
 * What each source app wrote to Health Connect over a window, one row per data type.
 * A diagnostic: it answers "which of my watch's data reaches Health Connect, and from where".
 *
 * [type] names the type the way its Health Connect permission does ("heart rate" for
 * `READ_HEART_RATE`), so it lines up with the types an app declares writing.
 * [label] is what the row shows, usually the same; "sleep stages" is a "sleep" row too.
 */
data class SourceInventoryRow(
    val type: String,
    val label: String = type,
    /** Source package to the latest record time seen for this type. Empty when nothing came. */
    val lastSeenBySource: Map<String, Instant> = emptyMap(),
    /** False when OpenVitals holds no read permission for the type: the row is unknown, not empty. */
    val readable: Boolean = true,
    /** True when the read stopped at its page limit: a source missing here may still have written. */
    val truncated: Boolean = false,
)

data class SourceInventory(
    val windowDays: Int,
    val rows: List<SourceInventoryRow>,
) {
    /** Every source seen in any row, most recent first. */
    val sources: List<String>
        get() = rows.flatMap { it.lastSeenBySource.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, times) -> times.max() }
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
}
