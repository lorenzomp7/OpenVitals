package tech.mmarca.openvitals.features.dashboard

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.model.ExerciseData

class DashboardWorkoutHistoryTest {
    private fun workout(index: Int) = ExerciseData(
        id = "workout-$index", title = null, exerciseType = 1,
        startTime = Instant.EPOCH.minusSeconds(index * 3600L),
        endTime = Instant.EPOCH.minusSeconds(index * 3600L - 1800), durationMs = 1_800_000, source = "test",
    )

    @Test fun revealsMoreThanTwentyRecordsAndMovesBackThirtyDays() = runTest {
        val ranges = mutableListOf<Pair<LocalDate, LocalDate>>()
        val history = DashboardWorkoutHistory { start, end ->
            ranges += start to end
            if (ranges.size == 1) (1..25).map(::workout) else (26..50).map(::workout)
        }
        val day = LocalDate.of(2026, 11, 10)
        history.reset(day)
        history.loadMore(); assertEquals(10, history.state.value.workouts.size)
        history.loadMore(); assertEquals(20, history.state.value.workouts.size)
        history.loadMore(); assertEquals(25, history.state.value.workouts.size)
        assertEquals(1, ranges.size)
        history.loadMore(); assertEquals(35, history.state.value.workouts.size)
        assertEquals(day.minusDays(30), ranges[0].first)
        assertEquals(day.minusDays(31), ranges[1].second)
    }

    @Test fun anOldResponseCannotReplaceANewDay() = runTest {
        val response = CompletableDeferred<List<ExerciseData>>()
        val history = DashboardWorkoutHistory { _, _ -> response.await() }
        history.reset(LocalDate.of(2026, 10, 10))
        val load = async { history.loadMore() }
        runCurrent()
        val nextDay = LocalDate.of(2026, 11, 10)
        history.reset(nextDay)
        response.complete(listOf(workout(1)))
        load.await()
        assertEquals(nextDay, history.state.value.date)
        assertTrue(history.state.value.workouts.isEmpty())
        assertFalse(history.state.value.isLoading)
    }

    @Test fun deniedPermissionIsAnErrorAndDoesNotAdvanceTheCursor() = runTest {
        val ranges = mutableListOf<Pair<LocalDate, LocalDate>>()
        val history = DashboardWorkoutHistory { start, end ->
            ranges += start to end
            if (ranges.size == 1) throw SecurityException("denied") else listOf(workout(1))
        }
        history.reset(LocalDate.of(2026, 11, 10))
        history.loadMore()
        assertEquals(ScreenError.PermissionDenied, history.state.value.error)
        history.loadMore()
        assertEquals(ranges[0], ranges[1])
        assertNull(history.state.value.error)
        assertEquals(1, history.state.value.workouts.size)
    }
}
