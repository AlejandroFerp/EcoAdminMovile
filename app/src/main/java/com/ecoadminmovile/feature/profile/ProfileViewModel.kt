package com.ecoadminmovile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.PasswordChangeDto
import com.ecoadminmovile.core.model.PerfilUpdateDto
import com.ecoadminmovile.core.model.UsuarioPerfilDto
import com.ecoadminmovile.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isEditing: Boolean = false,
    val editNombre: String = "",
    val editTelefono: String = "",
    val editDni: String = "",
    val editCargo: String = "",
    val isSaving: Boolean = false,
    val saveSuccessMessage: String? = null,
    val errorMessage: String? = null,
    // Password change
    val showPasswordDialog: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isChangingPassword: Boolean = false,
    val passwordError: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun startEditing(profile: UsuarioPerfilDto?) {
        if (profile == null) return
        _uiState.update {
            it.copy(
                isEditing = true,
                editNombre = profile.nombre,
                editTelefono = profile.telefono.orEmpty(),
                editDni = profile.dni.orEmpty(),
                editCargo = profile.cargo.orEmpty(),
                errorMessage = null,
                saveSuccessMessage = null
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false, errorMessage = null) }
    }

    fun updateNombre(value: String) { _uiState.update { it.copy(editNombre = value) } }
    fun updateTelefono(value: String) { _uiState.update { it.copy(editTelefono = value) } }
    fun updateDni(value: String) { _uiState.update { it.copy(editDni = value) } }
    fun updateCargo(value: String) { _uiState.update { it.copy(editCargo = value) } }

    fun saveProfile(onProfileUpdated: (UsuarioPerfilDto) -> Unit) {
        val state = _uiState.value
        if (state.editNombre.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre es obligatorio") }
            return
        }

        val dto = PerfilUpdateDto(
            nombre = state.editNombre.trim(),
            telefono = state.editTelefono.trim().ifBlank { null },
            dni = state.editDni.trim().ifBlank { null },
            cargo = state.editCargo.trim().ifBlank { null }
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.updateProfile(dto).fold(
                onSuccess = { updatedProfile ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            saveSuccessMessage = "Perfil actualizado"
                        )
                    }
                    onProfileUpdated(updatedProfile)
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = throwable.message) }
                }
            )
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(saveSuccessMessage = null) }
    }

    // --- Password change ---

    fun showPasswordDialog() {
        _uiState.update {
            it.copy(
                showPasswordDialog = true,
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
                passwordError = null
            )
        }
    }

    fun hidePasswordDialog() {
        _uiState.update { it.copy(showPasswordDialog = false, passwordError = null) }
    }

    fun updateCurrentPassword(value: String) { _uiState.update { it.copy(currentPassword = value) } }
    fun updateNewPassword(value: String) { _uiState.update { it.copy(newPassword = value) } }
    fun updateConfirmPassword(value: String) { _uiState.update { it.copy(confirmPassword = value) } }

    fun changePassword() {
        val state = _uiState.value

        when {
            state.currentPassword.isBlank() -> {
                _uiState.update { it.copy(passwordError = "Introduce tu contraseña actual") }
                return
            }
            state.newPassword.length < 6 -> {
                _uiState.update { it.copy(passwordError = "La nueva contraseña debe tener al menos 6 caracteres") }
                return
            }
            state.newPassword != state.confirmPassword -> {
                _uiState.update { it.copy(passwordError = "Las contraseñas no coinciden") }
                return
            }
        }

        val dto = PasswordChangeDto(
            currentPassword = state.currentPassword,
            newPassword = state.newPassword
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true, passwordError = null) }
            repository.changePassword(dto).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            showPasswordDialog = false,
                            saveSuccessMessage = "Contraseña actualizada"
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(isChangingPassword = false, passwordError = throwable.message)
                    }
                }
            )
        }
    }
}
