package com.ecoadminmovile.feature.residuos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.ResiduoCreateDto
import com.ecoadminmovile.data.CentersRepository
import com.ecoadminmovile.data.ResiduosRepository
import com.ecoadminmovile.ui.theme.EcoAdminTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResiduoFormUiState(
    val codigoLER: String = "",
    val descripcion: String = "",
    val cantidad: String = "",
    val unidad: String = "",
    val selectedCentroId: Long? = null,
    val centros: List<CentroDto> = emptyList(),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
) {
    val isFormValid: Boolean get() = codigoLER.isNotBlank() && selectedCentroId != null
}

@HiltViewModel
class ResiduoFormViewModel @Inject constructor(
    private val repository: ResiduosRepository,
    private val centersRepository: CentersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResiduoFormUiState())
    val uiState: StateFlow<ResiduoFormUiState> = _uiState.asStateFlow()

    private var editingId: Long? = null

    fun initForm(residuoId: Long?) {
        viewModelScope.launch {
            val centersResult = centersRepository.loadCenters()
            val centersList = centersResult.getOrDefault(emptyList())

            _uiState.update { it.copy(centros = centersList) }

            if (residuoId != null) {
                editingId = residuoId
                repository.load(residuoId).fold(
                    onSuccess = { r ->
                        _uiState.update {
                            it.copy(
                                codigoLER = r.codigoLER.orEmpty(),
                                descripcion = r.descripcion.orEmpty(),
                                cantidad = r.cantidad?.toString().orEmpty(),
                                unidad = r.unidad.orEmpty(),
                                selectedCentroId = r.centro?.id ?: centersList.firstOrNull()?.id,
                                isEditing = true
                            )
                        }
                    },
                    onFailure = { err -> _uiState.update { it.copy(errorMessage = err.message) } }
                )
            } else {
                _uiState.update {
                    it.copy(
                        selectedCentroId = centersList.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun onCodigoLERChanged(value: String) { _uiState.update { it.copy(codigoLER = value) } }
    fun onDescripcionChanged(value: String) { _uiState.update { it.copy(descripcion = value) } }
    fun onCantidadChanged(value: String) { _uiState.update { it.copy(cantidad = value) } }
    fun onUnidadChanged(value: String) { _uiState.update { it.copy(unidad = value) } }
    fun onCentroIdChanged(value: Long?) { _uiState.update { it.copy(selectedCentroId = value) } }

    fun save() {
        val state = _uiState.value
        if (!state.isFormValid) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        val dto = ResiduoCreateDto(
            centroId = state.selectedCentroId!!,
            codigoLER = state.codigoLER.trim(),
            descripcion = state.descripcion.trim().ifBlank { null },
            cantidad = state.cantidad.toDoubleOrNull(),
            unidad = state.unidad.trim().ifBlank { null },
            estado = "ALMACENADO"
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
fun ResiduoFormScreen(
    state: ResiduoFormUiState,
    onBack: () -> Unit,
    onCodigoLERChanged: (String) -> Unit,
    onDescripcionChanged: (String) -> Unit,
    onCantidadChanged: (String) -> Unit,
    onUnidadChanged: (String) -> Unit,
    onCentroIdChanged: (Long?) -> Unit,
    onSave: () -> Unit
) {
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar residuo" else "Nuevo residuo", fontWeight = FontWeight.Bold) },
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

            var dropdownExpanded by remember { mutableStateOf(false) }
            val selectedCentro = state.centros.firstOrNull { it.id == state.selectedCentroId }

            Column {
                Text(
                    text = "Centro Asociado *",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCentro?.nombre.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        state.centros.forEach { centro ->
                            DropdownMenuItem(
                                text = { Text(centro.nombre.orEmpty()) },
                                onClick = {
                                    onCentroIdChanged(centro.id)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.codigoLER,
                onValueChange = onCodigoLERChanged,
                label = { Text("Código LER *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.descripcion,
                onValueChange = onDescripcionChanged,
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.cantidad,
                    onValueChange = onCantidadChanged,
                    label = { Text("Cantidad") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.unidad,
                    onValueChange = onUnidadChanged,
                    label = { Text("Unidad") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("kg, L, t...") }
                )
            }

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

@Preview(showBackground = true, name = "Nuevo Residuo")
@Composable
fun ResiduoFormScreenNewPreview() {
    EcoAdminTheme {
        ResiduoFormScreen(
            state = ResiduoFormUiState(),
            onBack = {},
            onCodigoLERChanged = {},
            onDescripcionChanged = {},
            onCantidadChanged = {},
            onUnidadChanged = {},
            onCentroIdChanged = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, name = "Editar Residuo")
@Composable
fun ResiduoFormScreenEditPreview() {
    EcoAdminTheme {
        ResiduoFormScreen(
            state = ResiduoFormUiState(
                codigoLER = "20 01 01",
                descripcion = "Papel y cartón",
                cantidad = "12.5",
                unidad = "kg",
                isEditing = true
            ),
            onBack = {},
            onCodigoLERChanged = {},
            onDescripcionChanged = {},
            onCantidadChanged = {},
            onUnidadChanged = {},
            onCentroIdChanged = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, name = "Residuo - Error")
@Composable
fun ResiduoFormScreenErrorPreview() {
    EcoAdminTheme {
        ResiduoFormScreen(
            state = ResiduoFormUiState(
                codigoLER = "20 01",
                errorMessage = "Error al guardar el residuo"
            ),
            onBack = {},
            onCodigoLERChanged = {},
            onDescripcionChanged = {},
            onCantidadChanged = {},
            onUnidadChanged = {},
            onCentroIdChanged = {},
            onSave = {}
        )
    }
}
