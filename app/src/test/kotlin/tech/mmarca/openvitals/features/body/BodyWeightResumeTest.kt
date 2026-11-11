package tech.mmarca.openvitals.features.body

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.query.BodyPeriodData
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class BodyWeightResumeTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `resume reloads a weigh-in written while the scale app was open`() = runTest {
        val repository = mockk<BodyRepository>()
        var result = BodyPeriodData()
        coEvery { repository.loadBodyPeriod(any(), any()) } answers { result }
        val vm = BodyViewModel(
            repository = repository,
            periodPreferences = FakePreferences(),
            dispatchers = mainDispatcherRule.dispatcherProvider,
            savedStateHandle = SavedStateHandle(),
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value.weightEntries.isEmpty())
        val latest = WeightEntry(time = Instant.now(), weightKg = 69.2, source = "scale")
        result = BodyPeriodData(weightEntries = listOf(latest))
        vm.resumeCurrentPeriod(refreshCurrent = true)
        advanceUntilIdle()
        assertEquals(listOf(latest), vm.uiState.value.weightEntries)
    }
}
