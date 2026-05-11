package com.ecoadminmovile.feature.centers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.CentroCreateDto
import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.DireccionCreateDto
import com.ecoadminmovile.data.CentersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val CENTER_TYPE_OPTIONS = listOf("PRODUCTOR", "GESTOR")

data class CenterFormUiState(
    val editingCenterId: Long? = null,
    val nombre: String = "",
    val tipo: String = CENTER_TYPE_OPTIONS.first(),
    val nima: String = "",
    val telefono: String = "",
    val email: String = "",
    val nombreContacto: String = "",
    val calle: String = "",
    val ciudad: String = "",
    val provincia: String = "",
    val codigoPostal: String = "",
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false
) {
    val isEditing: Boolean get() = editingCenterId != null
    val isFormValid: Boolean get() = nombre.isNotBlank()
}

sealed interface CenterFormField {
    data class Nombre(val value: String) : CenterFormField
    data class Tipo(val value: String) : CenterFormField
    data class Nima(val value: String) : CenterFormField
    data class Telefono(val value: String) : CenterFormField
    data class Email(val value: String) : CenterFormField
    data class NombreContacto(val value: String) : CenterFormField
    data class Calle(val value: String) : CenterFormField
    data class Ciudad(val value: String) : CenterFormField
    data class Provincia(val value: String) : CenterFormField
    data class CodigoPostal(val value: String) : CenterFormField
}

@HiltViewModel
class CenterFormViewModel @Inject constructor(
    private val repository: CentersRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CenterFormUiState())
    val uiState: StateFlow<CenterFormUiState> = _uiState.asStateFlow()

    fun initForm(centerId: Long? = null) {
        if (centerId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.loadCenter(centerId).fold(
                onSuccess = { center ->
                    _uiState.value = CenterFormUiState(
                        editingCenterId = center.id,
                        nombre = center.nombre.orEmpty(),
                        tipo = center.tipo ?: CENTER_TYPE_OPTIONS.first(),
                        nima = center.nima.orEmpty(),
                        telefono = center.telefono.orEmpty(),
                        email = center.email.orEmpty(),
                        nombreContacto = center.nombreContacto.orEmpty(),
                        calle = center.direccion?.calle.orEmpty(),
                        ciudad = center.direccion?.ciudad.orEmpty(),
                        provincia = center.direccion?.provincia.orEmpty(),
                        codigoPostal = center.direccion?.codigoPostal.orEmpty()
                    )
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                }
            )
        }
    }

    fun onFieldChanged(field: CenterFormField) {
        _uiState.update { state ->
            when (field) {
                is CenterFormField.Nombre -> state.copy(nombre = field.value)
                is CenterFormField.Tipo -> state.copy(tipo = field.value)
                is CenterFormField.Nima -> state.copy(nima = field.value)
                is CenterFormField.Telefono -> state.copy(telefono = field.value)
                is CenterFormField.Email -> state.copy(email = field.value)
                is CenterFormField.NombreContacto -> state.copy(nombreContacto = field.value)
                is CenterFormField.Calle -> state.copy(calle = field.value)
                is CenterFormField.Ciudad -> state.copy(ciudad = field.value)
                is CenterFormField.Provincia -> state.copy(provincia = field.value)
                is CenterFormField.CodigoPostal -> state.copy(codigoPostal = field.value)
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.isFormValid) return

        val dto = CentroCreateDto(
            nombre = state.nombre.trim(),
            tipo = state.tipo,
            nima = state.nima.trim().ifBlank { null },
            telefono = state.telefono.trim().ifBlank { null },
            email = state.email.trim().ifBlank { null },
            nombreContacto = state.nombreContacto.trim().ifBlank { null },
            direccion = DireccionCreateDto(
                calle = state.calle.trim().ifBlank { null },
                ciudad = state.ciudad.trim().ifBlank { null },
                provincia = state.provincia.trim().ifBlank { null },
                codigoPostal = state.codigoPostal.trim().ifBlank { null }
            ).takeIf { it.calle != null || it.ciudad != null || it.provincia != null || it.codigoPostal != null }
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val result = if (state.isEditing) {
                repository.updateCenter(state.editingCenterId!!, dto)
            } else {
                repository.createCenter(dto)
            }

            result.fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) } },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = throwable.message) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterFormScreen(
    state: CenterFormUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onFieldChanged: (CenterFormField) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar centro" else "Nuevo centro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Nombre (required)
                OutlinedTextField(
                    value = state.nombre,
                    onValueChange = { onFieldChanged(CenterFormField.Nombre(it)) },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Tipo selector
                Text(
                    text = "Tipo de centro",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CENTER_TYPE_OPTIONS.forEach { type ->
                        FilterChip(
                            selected = state.tipo == type,
                            onClick = { onFieldChanged(CenterFormField.Tipo(type)) },
                            label = { Text(type) }
                        )
                    }
                }

                // NIMA
                OutlinedTextField(
                    value = state.nima,
                    onValueChange = { onFieldChanged(CenterFormField.Nima(it)) },
                    label = { Text("NIMA") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                HorizontalDivider()

                Text(
                    text = "Contacto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = state.nombreContacto,
                    onValueChange = { onFieldChanged(CenterFormField.NombreContacto(it)) },
                    label = { Text("Persona de contacto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = state.telefono,
                    onValueChange = { onFieldChanged(CenterFormField.Telefono(it)) },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onFieldChanged(CenterFormField.Email(it)) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                HorizontalDivider()

                Text(
                    text = "Dirección",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = state.calle,
                    onValueChange = { onFieldChanged(CenterFormField.Calle(it)) },
                    label = { Text("Calle") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = state.ciudad,
                        onValueChange = { onFieldChanged(CenterFormField.Ciudad(it)) },
                        label = { Text("Ciudad") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = state.codigoPostal,
                        onValueChange = { onFieldChanged(CenterFormField.CodigoPostal(it)) },
                        label = { Text("C.P.") },
                        modifier = Modifier.weight(0.5f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = state.provincia,
                    onValueChange = { onFieldChanged(CenterFormField.Provincia(it)) },
                    label = { Text("Provincia") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Error message
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Save button
                Button(
                    onClick = onSave,
                    enabled = state.isFormValid && !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (state.isEditing) "Guardar cambios" else "Crear centro")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
