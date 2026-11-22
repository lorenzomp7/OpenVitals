package tech.mmarca.openvitals.features.activity

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.domain.model.ExerciseData

/**
 * The sessions of one kind over a year, as the personal records read them. Opening one
 * activity after another would otherwise read the year again each time; held thirty minutes, in
 * memory only, and dropped with the activities list when an activity is added or deleted.
 */
@Singleton
class ActivityRecordsCache @Inject constructor() {
    private data class Entry(
        val storedAt: Long,
        val revision: Long,
        val start: LocalDate,
        val end: LocalDate,
        val sessions: List<ExerciseData>,
    )

    /** One span per kind; any window inside it is answered from it, not read again. */
    private val entries = HashMap<Int, Entry>()

    @Synchronized
    internal fun get(exerciseType: Int, start: LocalDate, end: LocalDate): List<ExerciseData>? =
        entries[exerciseType]
            ?.takeIf { System.nanoTime() - it.storedAt < FreshNanos && it.revision == ActivitiesReturnCache.revisionNow() }
            ?.takeIf { !it.start.isAfter(start) && !it.end.isBefore(end) }
            ?.sessions
            ?.filter { session ->
                val day = session.startTime.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                !day.isBefore(start) && !day.isAfter(end)
            }

    @Synchronized
    internal fun put(exerciseType: Int, start: LocalDate, end: LocalDate, sessions: List<ExerciseData>) {
        if (entries.size > MaxEntries) entries.clear()
        entries[exerciseType] = Entry(System.nanoTime(), ActivitiesReturnCache.revisionNow(), start, end, sessions)
    }

    private companion object {
        const val FreshNanos = 30L * 60 * 1_000_000_000
        const val MaxEntries = 16
    }
}
