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
    val error: ScreenError? = null,
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(private val repository: UserProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(UserProfileState())
    internal val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = repository.read()
            _state.value = UserProfileState(profile.name, profile.age, repository.photo(), loading = false)
        }
    }

    fun save(name: String, age: Int?, onSaved: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.save(name, age) }.fold(
                onSuccess = { _state.value = _state.value.copy(name = name.trim(), age = age); onSaved() },
                onFailure = { _state.value = _state.value.copy(error = it.toScreenError()) },
            )
        }
    }

    fun setPhoto(uri: Uri?) {
        viewModelScope.launch {
            runCatching { repository.setPhoto(uri); repository.photo() }.fold(
                onSuccess = { _state.value = _state.value.copy(photo = it, error = null) },
                onFailure = { _state.value = _state.value.copy(error = it.toScreenError()) },
            )
        }
    }
}
