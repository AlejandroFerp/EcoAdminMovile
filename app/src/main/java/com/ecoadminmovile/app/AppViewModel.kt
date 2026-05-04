package com.ecoadminmovile.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.UsuarioPerfilDto
import com.ecoadminmovile.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUiState(
    val isLoading: Boolean = true,
    val isAuthenticated: Boolean = false,
    val profile: UsuarioPerfilDto? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        refreshSession(showErrorOnFailure = false)
    }

    fun refreshSession(showErrorOnFailure: Boolean) {
        if (!authRepository.hasActiveSession()) {
            _uiState.value = AppUiState(isLoading = false)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            authRepository.validateSession().fold(
                onSuccess = { profile ->
                    _uiState.value = AppUiState(
                        isLoading = false,
                        isAuthenticated = true,
                        profile = profile
                    )
                },
                onFailure = { throwable ->
                    authRepository.logout()
                    _uiState.value = AppUiState(
                        isLoading = false,
                        errorMessage = if (showErrorOnFailure) throwable.message else null
                    )
                }
            )
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AppUiState(isLoading = false)
    }
}
