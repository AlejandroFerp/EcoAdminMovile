/**
 * ViewModel principal de la aplicación. Gestiona el estado de autenticación y perfil del usuario.
 *
 * Conceptos Kotlin demostrados:
 * - @HiltViewModel + @Inject constructor: Hilt inyecta las dependencias automáticamente
 *   en el constructor primario del ViewModel.
 * - data class para estado UI: `AppUiState` usa data class para obtener copy() gratis,
 *   permitiendo actualizaciones inmutables del estado.
 * - StateFlow: flujo reactivo "caliente" que siempre tiene un valor actual (a diferencia de Flow frío).
 * - MutableStateFlow + asStateFlow(): patrón de encapsulación — solo el ViewModel puede
 *   modificar el estado (_uiState), el exterior solo lo observa (uiState).
 * - init { }: bloque de inicialización que se ejecuta al crear la instancia (después del constructor).
 * - viewModelScope.launch { }: lanza una corrutina vinculada al ciclo de vida del ViewModel.
 *   Se cancela automáticamente cuando el ViewModel se destruye.
 * - .copy(): función generada por data class para crear copia con algunos campos cambiados
 *   (actualización inmutable — no modifica el objeto original).
 * - .fold(onSuccess, onFailure): descompone Result<T> en sus dos caminos posibles.
 * - .update { }: función atómica de MutableStateFlow para modificar el estado de forma thread-safe.
 *
 * Patrones de diseño:
 * - MVVM (Model-View-ViewModel): el ViewModel expone estado reactivo que la UI observa.
 * - Observer: StateFlow notifica a los colectores cuando el estado cambia.
 * - Unidirectional Data Flow: estado fluye del ViewModel → UI, eventos fluyen UI → ViewModel.
 */
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

// data class para representar el estado de la UI — inmutable, usa copy() para actualizar
data class AppUiState(
    val isLoading: Boolean = true, // Valores por defecto simplifican la creación
    val isAuthenticated: Boolean = false,
    val profile: UsuarioPerfilDto? = null, // Nullable: puede no haber perfil cargado aún
    val errorMessage: String? = null
)

// @HiltViewModel: Hilt se encarga de crear e inyectar este ViewModel
// @Inject constructor: Hilt inyecta AuthRepository automáticamente desde el grafo de DI
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    // MutableStateFlow: estado mutable PRIVADO (solo el ViewModel lo modifica)
    private val _uiState = MutableStateFlow(AppUiState())
    // asStateFlow(): expone como StateFlow de solo lectura (encapsulación)
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    // init: bloque que se ejecuta al instanciar el ViewModel (como un constructor secundario)
    init {
        refreshSession(showErrorOnFailure = false)
    }

    fun refreshSession(showErrorOnFailure: Boolean) {
        if (!authRepository.hasActiveSession()) {
            _uiState.value = AppUiState(isLoading = false)
            return
        }

        // viewModelScope.launch: lanza corrutina con ciclo de vida del ViewModel
        viewModelScope.launch {
            // .update { }: modificación atómica y thread-safe del StateFlow
            _uiState.update {
                // .copy(): crea nueva instancia con solo los campos indicados cambiados (inmutabilidad)
                it.copy(isLoading = true, errorMessage = null)
            }

            // .fold(): descompone Result en onSuccess y onFailure (programación funcional)
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
