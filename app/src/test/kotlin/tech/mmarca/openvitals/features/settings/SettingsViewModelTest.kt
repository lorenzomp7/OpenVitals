package tech.mmarca.openvitals.features.settings

import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.HealthConnectSourceDeclarations
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.preferences.BloodPressureGuideline
import tech.mmarca.openvitals.features.cycle.reminders.CycleReminderController
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test fun `refresh includes cycle permissions with visible permissions`() = runTest {
        val vm = viewModel(repository = repo())

        assertEquals(setOf("steps", "write", "route", "cycle"), vm.uiState.value.visiblePermissions)
    }

    @Test fun `missingVisiblePermissions excludes already granted visible permissions`() = runTest {
        val vm = viewModel(repository = repo(grantedPermissions = setOf("steps")))

        assertEquals(setOf("write", "route", "cycle"), vm.uiState.value.missingVisiblePermissions)
        assertEquals(setOf("route"), vm.uiState.value.missingManualVisiblePermissions)
    }

    @Test fun `missingVisiblePermissions is empty when all visible permissions are granted`() = runTest {
        val vm = viewModel(repository = repo(grantedPermissions = setOf("steps", "write", "route", "cycle")))

        assertTrue(vm.uiState.value.missingVisiblePermissions.isEmpty())
        assertTrue(vm.uiState.value.missingManualVisiblePermissions.isEmpty())
    }

    @Test fun `setBloodPressureGuideline persists preference and updates ui state`() = runTest {
        val prefs = FakePreferences()
        val vm = viewModel(preferences = prefs)

        vm.setBloodPressureGuideline(BloodPressureGuideline.ESH_2023)

        assertEquals(BloodPressureGuideline.ESH_2023, prefs.bloodPressureGuideline)
        assertEquals(BloodPressureGuideline.ESH_2023, vm.uiState.value.bloodPressureGuideline)
    }

    @Test fun `mindfulness toggle persists and triggers a refresh`() = runTest {
        val prefs = FakePreferences()
        val repository = repo()
        val vm = viewModel(repository = repository, preferences = prefs)

        vm.setHealthConnectMindfulnessEnabled(true)
        advanceUntilIdle()

        assertTrue(prefs.healthConnectMindfulnessEnabled)
        assertTrue(vm.uiState.value.healthConnectMindfulnessEnabled)
        // The initial load plus the toggle-triggered reload.
        verify(atLeast = 2) { repository.availability() }
    }

    @Test fun `refresh skips granted permissions when Health Connect is unsupported`() = runTest {
        val repository = repo(availability = HealthConnectAvailability.NOT_SUPPORTED)

        val vm = viewModel(repository = repository)

        assertEquals(HealthConnectAvailability.NOT_SUPPORTED, vm.uiState.value.availability)
        assertTrue(vm.uiState.value.grantedPermissions.isEmpty())
        coVerify(exactly = 0) { repository.grantedPermissions() }
    }

    @Test fun `the test reminder is posted through the hydration reminder controller`() = runTest {
        val reminders = mockk<HydrationReminderController>(relaxed = true)
        val vm = viewModel(hydrationReminderController = reminders)

        vm.showTestHydrationReminder()

        // The settings action posts the same reminder the schedule posts.
        verify(exactly = 1) { reminders.showTestReminder(any()) }
    }

    @Test fun `diagnostics sources carry what each app declares and what never arrived`() = runTest {
        val heart = mockk<HeartRepository>().also { repo ->
            coEvery {
                repo.loadHeartRateSamples(any<java.time.LocalDate>(), any<java.time.LocalDate>())
            } returns listOf(
                HeartRateSample(java.time.Instant.parse("2026-11-20T08:00:00Z"), 60, "com.huami.watch.hmwatchmanager"),
            )
        }
        val declarations = mockk<HealthConnectSourceDeclarations>()
        coEvery { declarations.snapshot(setOf("com.huami.watch.hmwatchmanager")) } returns mapOf(
            "com.huami.watch.hmwatchmanager" to setOf(
                "android.permission.health.WRITE_HEART_RATE",
                "android.permission.health.WRITE_SLEEP",
            ),
            "nodomain.freeyourgadget.gadgetbridge" to setOf("android.permission.health.WRITE_SLEEP"),
        )

        val vm = viewModel(heartRepository = heart, sourceDeclarations = declarations)
        advanceUntilIdle()

        val sources = vm.uiState.value.healthConnectSources
        assertEquals(
            listOf("com.huami.watch.hmwatchmanager", "nodomain.freeyourgadget.gadgetbridge"),
            sources.map { it.packageName },
        )
        assertEquals(setOf("sleep"), sources.first().declaredNotSeen(setOf("heart rate", "sleep")))
        assertEquals(0, sources.last().recordCount)
    }

    @Test fun `diagnostics sources survive a failing declarations read`() = runTest {
        val heart = mockk<HeartRepository>().also { repo ->
            coEvery {
                repo.loadHeartRateSamples(any<java.time.LocalDate>(), any<java.time.LocalDate>())
            } returns listOf(
                HeartRateSample(java.time.Instant.parse("2026-11-20T08:00:00Z"), 60, "com.huami.watch.hmwatchmanager"),
            )
        }
        val declarations = mockk<HealthConnectSourceDeclarations>()
        coEvery { declarations.snapshot(any()) } throws SecurityException("denied")

        val vm = viewModel(heartRepository = heart, sourceDeclarations = declarations)
        advanceUntilIdle()

        val source = vm.uiState.value.healthConnectSources.single()
        assertEquals(1, source.recordCount)
        assertNull(source.declaredWrites)
    }

    private fun viewModel(
        repository: HealthRepository = repo(),
        heartRepository: HeartRepository = heartRepo(),
        sleepRepository: SleepRepository = sleepRepo(),
        hydrationReminderController: HydrationReminderController = mockk(relaxed = true),
        cycleReminderController: CycleReminderController = mockk(relaxed = true),
        preferences: FakePreferences = FakePreferences(),
        permissionUxState: HealthConnectPermissionUxState = mockk(relaxed = true),
        sourceDeclarations: HealthConnectSourceDeclarations? = null,
    ): SettingsViewModel =
        SettingsViewModel(
            repository = repository,
            heartRepository = heartRepository,
            sleepRepository = sleepRepository,
            hydrationReminderController = hydrationReminderController,
            cycleReminderController = cycleReminderController,
            healthConnectPreferences = preferences,
            heartThresholdPreferences = preferences,
            permissionUxState = permissionUxState,
            sourceDeclarations = sourceDeclarations,
        )

    private fun heartRepo(): HeartRepository =
        mockk<HeartRepository>().also { repo ->
            coEvery {
                repo.loadHeartRateSamples(any<java.time.LocalDate>(), any<java.time.LocalDate>())
            } returns emptyList()
        }

    private fun sleepRepo(): SleepRepository =
        mockk<SleepRepository>().also { repo ->
            coEvery { repo.loadSleepSessions(any(), any()) } returns emptyList()
        }

    private fun repo(
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        grantedPermissions: Set<String> = emptySet(),
    ): HealthRepository =
        mockk<HealthRepository>().also { repo ->
            every { repo.availability() } returns availability
            every { repo.minimumOnboardingPermissions } returns setOf("steps")
            every { repo.corePermissions } returns setOf("steps")
            every { repo.routePermissions } returns setOf("route")
            every { repo.heartPermissions } returns emptySet()
            every { repo.bodyPermissions } returns emptySet()
            every { repo.activityExtrasPermissions } returns emptySet()
            every { repo.nutritionHydrationPermissions } returns emptySet()
            every { repo.requestableWritePermissions } returns setOf("write")
            every { repo.mindfulnessPermissions } returns emptySet()
            every { repo.additionalDataAccessPermissions } returns emptySet()
            every { repo.vitalsPermissions } returns emptySet()
            every { repo.dataImportWritePermissions } returns emptySet()
            every { repo.isMindfulnessAvailable() } returns false
            every { repo.allPermissions } returns setOf("steps", "write", "route", "cycle")
            every { repo.cyclePermissions } returns setOf("cycle")
            every { repo.manualOnlyPermissions } returns setOf("route")
            coEvery { repo.grantedPermissions() } returns grantedPermissions
        }

}
