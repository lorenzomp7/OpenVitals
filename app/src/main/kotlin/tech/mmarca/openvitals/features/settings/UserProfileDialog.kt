package tech.mmarca.openvitals.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var menuOpen by remember { mutableStateOf(false) }
    var viewerOpen by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.setPhoto(uri)
    }
    val photo = state.photo
    val description = stringResource(R.string.user_profile_photo)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box {
            Box(
                Modifier.size(80.dp).clip(CircleShape)
                    .clickable(enabled = !state.saving, onClickLabel = description) { menuOpen = true },
            ) {
                if (photo != null) Image(photo.asImageBitmap(), description,
                    Modifier.size(80.dp), contentScale = ContentScale.Crop)
                else Icon(Icons.Outlined.AccountCircle, description, Modifier.size(80.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.user_profile_view_photo)) },
                    enabled = photo != null,
                    onClick = { menuOpen = false; viewerOpen = true },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.user_profile_change_photo)) },
                    onClick = { menuOpen = false; photoPicker.launch("image/*") },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    enabled = photo != null,
                    onClick = { menuOpen = false; viewModel.setPhoto(null) },
                )
            }
        }
    }
    if (viewerOpen && photo != null) AlertDialog(
        onDismissRequest = { viewerOpen = false },
        text = {
            Image(photo.asImageBitmap(), description,
                Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Fit)
        },
        confirmButton = {
            TextButton(onClick = { viewerOpen = false }) { Text(stringResource(R.string.action_close)) }
        },
    )
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
