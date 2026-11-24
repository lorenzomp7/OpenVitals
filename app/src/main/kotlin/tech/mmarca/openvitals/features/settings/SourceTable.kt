package tech.mmarca.openvitals.features.settings

import java.time.Instant
import tech.mmarca.openvitals.domain.model.SourceInventory

/** One cell of the "what does each source write" table. */
sealed interface SourceTableCell {
    /** The source wrote this type; [lastSeen] is its latest record in the window. */
    data class Seen(val lastSeen: Instant) : SourceTableCell

    /** The source declares writing this type, and none of it arrived in the window. */
    data object DeclaredNotSeen : SourceTableCell

    /** Nothing seen, nothing declared (or the declarations are unknown). */
    data object None : SourceTableCell
}

data class SourceTableRow(
    val label: String,
    /** The read stopped at its page limit: an empty cell here is not proof of silence. */
    val truncated: Boolean,
    /** One cell per [SourceTable.sources] entry, in the same order. */
    val cells: List<SourceTableCell>,
)

/**
 * Rows are the data types something was seen or declared for; columns are the source apps.
 * Types OpenVitals may not read are listed apart: their cells would be guesses.
 */
data class SourceTable(
    val windowDays: Int,
    val sources: List<String>,
    val rows: List<SourceTableRow>,
    val unreadableTypes: List<String>,
) {
    /** A Zepp OS watch reaches Health Connect through Zepp: worth the note on what it leaves out. */
    val showsZeppNote: Boolean
        get() = sources.any { it in ZeppPackages }

    val declaredNotSeenCount: Int
        get() = rows.sumOf { row -> row.cells.count { it == SourceTableCell.DeclaredNotSeen } }

    companion object {
        val ZeppPackages = setOf("com.huami.watch.hmwatchmanager", "com.xiaomi.hm.health")
    }
}

/** For each type, the apps that declare writing it: where a silent source is looked for. */
fun declaredWritersByType(declarations: Map<String, Set<String>?>): Map<String, Set<String>> =
    declarations.entries
        .flatMap { (packageName, requested) ->
            requested?.let(::declaredHealthWrites).orEmpty().map { type -> type to packageName }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, packages) -> packages.toSet() }

/**
 * Folds the inventory and each app's declared writes ([declarations], as requested
 * permissions; null for an app not installed here) into the table.
 */
fun buildSourceTable(
    inventory: SourceInventory,
    declarations: Map<String, Set<String>?>,
): SourceTable {
    val declaredWrites = declarations.mapValues { (_, requested) -> requested?.let(::declaredHealthWrites) }
    val readableRows = inventory.rows.filter { it.readable }
    val readableTypes = readableRows.map { it.type }.toSet()
    val silentDeclarers = declaredWrites
        .filter { (packageName, writes) ->
            packageName !in inventory.sources && writes.orEmpty().any { it in readableTypes }
        }
        .keys
        .sorted()
    val sources = inventory.sources + silentDeclarers
    val rows = readableRows.map { row ->
        SourceTableRow(
            label = row.label,
            truncated = row.truncated,
            cells = sources.map { source ->
                val lastSeen = row.lastSeenBySource[source]
                when {
                    lastSeen != null -> SourceTableCell.Seen(lastSeen)
                    declaredWrites[source].orEmpty().contains(row.type) -> SourceTableCell.DeclaredNotSeen
                    else -> SourceTableCell.None
                }
            },
        )
    }.filter { row -> row.cells.any { it != SourceTableCell.None } }
    return SourceTable(
        windowDays = inventory.windowDays,
        sources = sources,
        rows = rows,
        unreadableTypes = inventory.rows.filterNot { it.readable }.map { it.label }.distinct(),
    )
}
