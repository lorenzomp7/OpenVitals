package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.sync.BmrEstimateService
import tech.mmarca.openvitals.openwatch.relativeTime
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher
import tech.mmarca.openvitals.ui.theme.LayoutMetrics

/** Body profile: the declared body, the BMR estimate and the caffeine clearance factors. Shares [BodySettingsViewModel] with Recovery. */
@Composable
fun BodyProfileSettingsScreen(viewModel: BodySettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profileViewModel: UserProfileViewModel = hiltViewModel()
    val identity by profileViewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable(identity.name) { mutableStateOf(identity.name) }
    var draft by remember(state.bodyProfile) { mutableStateOf(state.bodyProfile) }
    var metabolism by remember(state.caffeinePreferences) { mutableStateOf(state.caffeinePreferences) }
    var bmrEnabled by remember(state.bmrEstimateEnabled) { mutableStateOf(state.bmrEstimateEnabled) }
    val openWatchSync by viewModel.openWatchProfileSync.collectAsStateWithLifecycle()

    // The write permission is granted in Health Connect, so it is re-read on the way back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val permissionLauncher = rememberHealthConnectPermissionLauncher(onResult = { viewModel.refresh() })

    SettingsSectionList {
        item {
            UserProfileSettingsCard(
                name = name,
                onName = { name = it },
                calculatedAge = draft.birthDate?.let { draft.ageYears() },
                viewModel = profileViewModel,
            )
        }
        item { SettingsCardSpacer() }
        item { SectionHeader(stringResource(SettingsSection.BODY_PROFILE.titleRes)) }
        openWatchSync?.let { updatedAt ->
            item {
                Text(
                    text = if (updatedAt > 1_000_000_000_000L) {
                        stringResource(R.string.body_profile_openwatch_synced_updated, relativeTime(updatedAt))
                    } else {
                        stringResource(R.string.body_profile_openwatch_synced)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = 4.dp),
                )
            }
        }
        item {
            BodyProfileCard(
                profile = draft,
                onDraftChange = { draft = it },
                showSaveButton = false,
                // The card's only unit-sensitive input is body weight.
                unitSystem = state.weightUnitSystem,
                onSave = viewModel::updateBodyProfile,
                weightMeasured = state.bodyProfileWeightMeasured,
                heightMeasured = state.bodyProfileHeightMeasured,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
        }
        item { SettingsCardSpacer() }
        item {
            BmrEstimateCard(
                enabled = bmrEnabled,
                todayKcal = state.bmrEstimateTodayKcal,
                missingInputs = state.bmrEstimateMissingInputs,
                writePermissionMissing = state.bmrEstimateWritePermissionMissing,
                onEnabledChange = { bmrEnabled = it },
                onGrantPermission = { permissionLauncher.launch(BmrEstimateService.RequiredPermissions) },
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
        }
        item { SettingsCardSpacer() }
        item {
            MetabolismCard(
                preferences = metabolism,
                onDraftChange = { metabolism = it },
                showSaveButton = false,
                onSave = viewModel::updateCaffeinePreferences,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
        }
        item {
            OpenVitalsTonalButton(
                enabled = !identity.loading && !identity.saving,
                onClick = {
                    profileViewModel.save(name, draft.birthDate?.let { draft.ageYears() }) {
                        viewModel.updateBodyProfile(draft)
                        viewModel.updateCaffeinePreferences(metabolism.copy(profileCompleted = true))
                        viewModel.setBmrEstimateEnabled(bmrEnabled)
                    }
                },
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter, vertical = 16.dp),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}
