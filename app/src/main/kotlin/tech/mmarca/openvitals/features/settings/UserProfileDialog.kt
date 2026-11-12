package tech.mmarca.openvitals.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

@Composable
internal fun UserProfileSettingsCard(
    name: String,
    onName: (String) -> Unit,
    calculatedAge: Int?,
    viewModel: UserProfileViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) return
    OpenVitalsCard(Modifier.padding(horizontal = LayoutMetrics.screenGutter)) {
        Column(Modifier.fillMaxWidth().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            UserProfilePhotoRow(state, viewModel)
            ProfileTextField(name, { onName(it.take(100)) }, stringResource(R.string.food_name))
            ProfileTextField(calculatedAge?.toString().orEmpty(), {}, stringResource(R.string.user_profile_age), readOnly = true)
            state.error?.resolve()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun UserProfilePhotoRow(state: UserProfileState, viewModel: UserProfileViewModel) {
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.setPhoto(uri)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        val photo = state.photo
        if (photo != null) Image(photo.asImageBitmap(), null,
            Modifier.size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        else Icon(Icons.Outlined.AccountCircle, null, Modifier.size(80.dp))
        OpenVitalsIconButton(onClick = { photoPicker.launch("image/*") }, enabled = !state.saving) {
            Icon(Icons.Outlined.PhotoCamera, stringResource(R.string.user_profile_photo))
        }
        OpenVitalsIconButton(onClick = { viewModel.setPhoto(null) }, enabled = photo != null && !state.saving) {
            Icon(Icons.Outlined.Delete, stringResource(R.string.user_profile_remove_photo))
        }
    }
}

@Composable
private fun ProfileTextField(value: String, onValue: (String) -> Unit, label: String, readOnly: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        readOnly = readOnly,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
