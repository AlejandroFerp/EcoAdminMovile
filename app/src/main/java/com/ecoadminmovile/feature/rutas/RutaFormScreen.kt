package com.ecoadminmovile.feature.rutas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoadminmovile.ui.theme.EcoAdminTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.RutaCreateDto
import com.ecoadminmovile.data.RutasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RutaFormUiState(
    val nombre: String = "",
    val distanciaKm: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
) {
    val isFormValid: Boolean get() = nombre.isNotBlank()
}

@HiltViewModel
class RutaFormViewModel @Inject constructor(
    private val repository: RutasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RutaFormUiState())
    val uiState: StateFlow<RutaFormUiState> = _uiState.asStateFlow()

    private var editingId: Long? = null

    fun initForm(rutaId: Long?) {
        if (rutaId == null) return
        editingId = rutaId
        viewModelScope.launch {
            repository.load(rutaId).fold(
                onSuccess = { r ->
                    _uiState.update {
                        it.copy(
                            nombre = r.nombre,
                            distanciaKm = r.distanciaKm?.toString().orEmpty(),
                            isEditing = true
                        )
                    }
                },
                onFailure = { err -> _uiState.update { it.copy(errorMessage = err.message) } }
            )
        }
    }

    fun onNombreChanged(value: String) { _uiState.update { it.copy(nombre = value) } }
    fun onDistanciaChanged(value: String) { _uiState.update { it.copy(distanciaKm = value) } }

    fun save() {
        val state = _uiState.value
        if (!state.isFormValid) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        val dto = RutaCreateDto(
            nombre = state.nombre.trim(),
            distanciaKm = state.distanciaKm.toDoubleOrNull()
        )

        viewModelScope.launch {
            val result = if (editingId != null) {
                repository.update(editingId!!, dto)
            } else {
                repository.create(dto)
            }
            result.fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) } },
                onFailure = { err -> _uiState.update { it.copy(isSaving = false, errorMessage = err.message) } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutaFormScreen(
    state: RutaFormUiState,
    onBack: () -> Unit,
    onNombreChanged: (String) -> Unit,
    onDistanciaChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar ruta" else "Nueva ruta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.nombre,
                onValueChange = onNombreChanged,
                label = { Text("Nombre de la ruta *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.distanciaKm,
                onValueChange = onDistanciaChanged,
                label = { Text("Distancia (km)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isFormValid && !state.isSaving
            ) {
                Text(if (state.isSaving) "Guardando..." else "Guardar")
            }
        }
    }
}

@Preview(showBackground = true, name = "Nueva Ruta")
@Composable
fun RutaFormScreenNewPreview() {
    EcoAdminTheme {
        RutaFormScreen(
            state = RutaFormUiState(),
            onBack = {},
            onNombreChanged = {},
            onDistanciaChanged = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, name = "Editar Ruta")
@Composable
fun RutaFormScreenEditPreview() {
    EcoAdminTheme {
        RutaFormScreen(
            state = RutaFormUiState(
                nombre = "Madrid-Valencia",
                distanciaKm = "350.0",
                isEditing = true
            ),
            onBack = {},
            onNombreChanged = {},
            onDistanciaChanged = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, name = "Ruta - Error")
@Composable
fun RutaFormScreenErrorPreview() {
    EcoAdminTheme {
        RutaFormScreen(
            state = RutaFormUiState(
                nombre = "Ruta Norte",
                errorMessage = "Ya existe una ruta con ese nombre"
            ),
            onBack = {},
            onNombreChanged = {},
            onDistanciaChanged = {},
            onSave = {}
        )
    }
}
