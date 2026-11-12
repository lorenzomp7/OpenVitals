package tech.mmarca.openvitals.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.ui.theme.Spacing
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve

@Composable
internal fun UserProfileSettingsCard(
    name: String,
    onName: (String) -> Unit,
    calculatedAge: Int?,
    viewModel: UserProfileViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) return
    tech.mmarca.openvitals.ui.components.OpenVitalsCard(Modifier.padding(horizontal = tech.mmarca.openvitals.ui.theme.LayoutMetrics.screenGutter)) {
        Column(Modifier.fillMaxWidth().padding(Spacing.lg)) {
            UserProfileFields(state, name, onName, calculatedAge?.toString().orEmpty(), viewModel)
        }
    }
}

@Composable
private fun UserProfileFields(
    state: UserProfileState, name: String, onName: (String) -> Unit,
    age: String, viewModel: UserProfileViewModel,
) {
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.setPhoto(uri)
    }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                val photo = state.photo
                if (photo != null) Image(photo.asImageBitmap(), null,
                    Modifier.size(80.dp).clip(CircleShape).align(Alignment.CenterHorizontally), contentScale = ContentScale.Crop)
                else Icon(Icons.Outlined.AccountCircle, null, Modifier.size(Spacing.xxxl * 2).align(Alignment.CenterHorizontally))
                TextButton(onClick = { photoPicker.launch("image/*") }) { Text(stringResource(R.string.user_profile_photo)) }
                if (photo != null) TextButton(onClick = { viewModel.setPhoto(null) }) {
                    Text(stringResource(R.string.user_profile_remove_photo))
                }
                OutlinedTextField(name, { onName(it.take(100)) }, label = { Text(stringResource(R.string.food_name)) },
                    shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(age, {}, readOnly = true, label = { Text(stringResource(R.string.user_profile_age)) },
                    shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                state.error?.resolve()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
}
