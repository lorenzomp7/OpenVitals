package tech.mmarca.openvitals.features.settings

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.data.repository.UserProfileRepository

internal data class UserProfileState(
    val name: String = "",
    val age: Int? = null,
    val photo: Bitmap? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: ScreenError? = null,
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(private val repository: UserProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(UserProfileState())
    internal val state = _state.asStateFlow()

    private var pendingPhoto: Uri? = null
    private var photoChanged = false

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val profile = repository.read()
            _state.value = UserProfileState(profile.name, profile.age, if (photoChanged) _state.value.photo else repository.photo(), loading = false)
        }
    }

    fun save(name: String, age: Int?, onSaved: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            runCatching {
                if (photoChanged) repository.setPhoto(pendingPhoto)
                repository.save(name, age)
            }.fold(
                onSuccess = {
                    photoChanged = false
                    pendingPhoto = null
                    _state.value = _state.value.copy(name = name.trim(), age = age, saving = false)
                    onSaved()
                },
                onFailure = { _state.value = _state.value.copy(error = it.toScreenError(), saving = false) },
            )
        }
    }

    fun setPhoto(uri: Uri?) {
        viewModelScope.launch {
            runCatching { repository.previewPhoto(uri) }.fold(
                onSuccess = { pendingPhoto = uri; photoChanged = true; _state.value = _state.value.copy(photo = it, error = null) },
                onFailure = { _state.value = _state.value.copy(error = it.toScreenError(), saving = false) },
            )
        }
    }
}
