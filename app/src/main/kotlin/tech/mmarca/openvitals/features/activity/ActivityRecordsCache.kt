package tech.mmarca.openvitals.features.activity

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.domain.model.ExerciseData

/**
 * 119: the sessions of one kind over a year, as the personal records read them. Opening one
 * activity after another would otherwise read the year again each time; held ten minutes, in
 * memory only, and dropped with the activities list when an activity is added or deleted.
 */
@Singleton
class ActivityRecordsCache @Inject constructor() {
    private data class Key(val exerciseType: Int, val start: LocalDate, val end: LocalDate)
    private data class Entry(val storedAt: Long, val revision: Long, val sessions: List<ExerciseData>)

    private val entries = HashMap<Key, Entry>()

    @Synchronized
    internal fun get(exerciseType: Int, start: LocalDate, end: LocalDate): List<ExerciseData>? =
        entries[Key(exerciseType, start, end)]
            ?.takeIf { System.nanoTime() - it.storedAt < FreshNanos && it.revision == ActivitiesReturnCache.revisionNow() }
            ?.sessions

    @Synchronized
    internal fun put(exerciseType: Int, start: LocalDate, end: LocalDate, sessions: List<ExerciseData>) {
        if (entries.size > MaxEntries) entries.clear()
        entries[Key(exerciseType, start, end)] = Entry(System.nanoTime(), ActivitiesReturnCache.revisionNow(), sessions)
    }

    private companion object {
        const val FreshNanos = 10L * 60 * 1_000_000_000
        const val MaxEntries = 16
    }
}
