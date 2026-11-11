package tech.mmarca.openvitals.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.resolve

@Composable
internal fun UserProfileDialog(onDismiss: () -> Unit, viewModel: UserProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) return
    var name by rememberSaveable { mutableStateOf(state.name) }
    var age by rememberSaveable { mutableStateOf(state.age?.toString().orEmpty()) }
    val validAge = age.isBlank() || age.toIntOrNull()?.let { it in 0..130 } == true
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.setPhoto(uri)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.user_profile_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val photo = state.photo
                if (photo != null) Image(photo.asImageBitmap(), null,
                    Modifier.size(80.dp).clip(CircleShape).align(Alignment.CenterHorizontally), contentScale = ContentScale.Crop)
                else Icon(Icons.Outlined.AccountCircle, null, Modifier.size(64.dp).align(Alignment.CenterHorizontally))
                TextButton(onClick = { photoPicker.launch("image/*") }) { Text(stringResource(R.string.user_profile_photo)) }
                if (photo != null) TextButton(onClick = { viewModel.setPhoto(null) }) {
                    Text(stringResource(R.string.user_profile_remove_photo))
                }
                OutlinedTextField(name, { name = it.take(100) }, label = { Text(stringResource(R.string.food_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(age, { age = it.take(3) }, label = { Text(stringResource(R.string.user_profile_age)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                    isError = !validAge, modifier = Modifier.fillMaxWidth())
                if (!validAge) Text(stringResource(R.string.body_entry_invalid_value), color = MaterialTheme.colorScheme.error)
                state.error?.resolve()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = validAge, onClick = { viewModel.save(name, age.toIntOrNull(), onDismiss) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
