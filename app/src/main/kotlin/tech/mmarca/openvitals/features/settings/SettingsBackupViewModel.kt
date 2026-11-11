package tech.mmarca.openvitals.features.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.data.repository.CyclePreferencesRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.SettingsBackupPreview
import tech.mmarca.openvitals.data.repository.SettingsBackupRepository
import tech.mmarca.openvitals.features.cycle.reminders.CycleReminderController
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.features.mindfulness.reminders.MindfulnessReminderController

internal data class SettingsBackupUiState(
    val busy: Boolean = false,
    val preview: SettingsBackupPreview? = null,
    val message: Int? = null,
    val imported: Boolean = false,
)

@HiltViewModel
internal class SettingsBackupViewModel @Inject constructor(
    private val repository: SettingsBackupRepository,
    private val preferences: PreferencesRepository,
    private val cycle: CyclePreferencesRepository,
    private val devices: BleDeviceRepository,
    private val widgets: HomeWidgetRefreshScheduler,
    private val hydrationReminders: HydrationReminderController,
    private val mindfulnessReminders: MindfulnessReminderController,
    private val cycleReminders: CycleReminderController,
) : ViewModel() {
    private val state = MutableStateFlow(SettingsBackupUiState())
    val uiState = state.asStateFlow()

    fun export(uri: Uri) = perform {
        repository.export(uri)
        state.value = state.value.copy(message = R.string.settings_backup_saved)
    }

    fun preview(uri: Uri) = perform {
        state.value = state.value.copy(preview = repository.preview(uri))
    }

    fun dismissPreview() { state.value = state.value.copy(preview = null) }
    fun consumeMessage() { state.value = state.value.copy(message = null) }

    fun restore() {
        val preview = state.value.preview ?: return
        perform {
            // A confirmed restore must finish refreshing live state even if this screen is closed.
            withContext(NonCancellable) {
                repository.restore(preview)
                cycle.reloadFromSettingsBackup()
                devices.refresh()
                preferences.reloadFromSettingsBackup()
                widgets.setInterval(preferences.homeWidgetRefreshInterval)
                widgets.refreshNow()
                hydrationReminders.applyStoredConfig()
                mindfulnessReminders.applyStoredConfig()
                cycleReminders.applyStoredConfig()
                state.value = state.value.copy(preview = null, imported = true, message = R.string.settings_backup_restored)
            }
        }
    }

    private fun perform(action: suspend () -> Unit) {
        if (state.value.busy) return
        state.value = state.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.value = state.value.copy(message = R.string.settings_backup_failed)
            } finally {
                state.value = state.value.copy(busy = false)
            }
        }
    }
}
