package tech.mmarca.openvitals.features.dashboard

import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.domain.model.ExerciseData

internal data class DashboardWorkoutHistoryState(
    val date: LocalDate? = null,
    val workouts: List<ExerciseData> = emptyList(),
    val isLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: ScreenError? = null,
)

/** One bounded date-window read per request; more records are revealed ten at a time. */
internal class DashboardWorkoutHistory(
    private val read: suspend (LocalDate, LocalDate) -> List<ExerciseData>,
) {
    private val mutableState = MutableStateFlow(DashboardWorkoutHistoryState())
    val state = mutableState.asStateFlow()
    private var all = emptyList<ExerciseData>()
    private var nextEnd: LocalDate? = null
    private var visibleCount = 0
    private var generation = 0

    fun reset(date: LocalDate) {
        generation++
        all = emptyList()
        visibleCount = 0
        nextEnd = date.minusDays(1)
        mutableState.value = DashboardWorkoutHistoryState(date = date)
    }

    suspend fun loadMore() {
        val current = mutableState.value
        if (current.isLoading || current.date == null || !current.canLoadMore) return
        if (visibleCount < all.size) {
            visibleCount = minOf(visibleCount + 10, all.size)
            mutableState.value = current.copy(workouts = all.take(visibleCount))
            return
        }
        val end = nextEnd ?: return
        val start = end.minusDays(29)
        val revision = generation
        mutableState.value = current.copy(isLoading = true, error = null)
        try {
            val records = read(start, end)
            if (revision != generation) return
            all = (all + records).distinctBy { it.id.ifBlank { "${it.startTime}/${it.endTime}/${it.source}" } }
                .sortedByDescending { it.startTime }
            visibleCount = minOf(visibleCount + 10, all.size)
            nextEnd = start.minusDays(1)
            mutableState.value = current.copy(workouts = all.take(visibleCount))
        } catch (cancelled: CancellationException) {
            if (revision == generation) mutableState.value = current.copy(isLoading = false)
            throw cancelled
        } catch (error: Exception) {
            if (revision == generation) mutableState.value = current.copy(
                error = error.toScreenError(R.string.screen_error_load_activity),
            )
        }
    }
}
