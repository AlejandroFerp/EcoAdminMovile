package com.ecoadminmovile.feature.residuos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.ResiduoDto
import com.ecoadminmovile.data.ResiduosRepository
import com.ecoadminmovile.ui.components.EcoCard
import com.ecoadminmovile.ui.theme.EcoTextMuted
import com.ecoadminmovile.ui.theme.EcoTextStrong
import com.ecoadminmovile.ui.theme.EcoTextSubtle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- UI State ---

data class ResiduosUiState(
    val residuos: List<ResiduoDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = ""
) {
    val filteredResiduos: List<ResiduoDto>
        get() = if (searchQuery.isBlank()) residuos
        else residuos.filter { r ->
            r.codigoLER.orEmpty().contains(searchQuery, ignoreCase = true) ||
                r.descripcion.orEmpty().contains(searchQuery, ignoreCase = true)
        }
}

// --- ViewModel ---

@HiltViewModel
class ResiduosViewModel @Inject constructor(
    private val repository: ResiduosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResiduosUiState())
    val uiState: StateFlow<ResiduosUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadAll().fold(
                onSuccess = { list -> _uiState.update { it.copy(residuos = list, isLoading = false) } },
                onFailure = { err -> _uiState.update { it.copy(errorMessage = err.message, isLoading = false) } }
            )
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.delete(id).fold(
                onSuccess = { load() },
                onFailure = { err -> _uiState.update { it.copy(errorMessage = err.message) } }
            )
        }
    }
}

// --- Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResiduosListScreen(
    state: ResiduosUiState,
    onRefresh: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onResiduoSelected: (Long) -> Unit,
    onCreateNew: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<ResiduoDto?>(null) }

    deleteTarget?.let { residuo ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar residuo") },
            text = { Text("¿Eliminar \"${residuo.codigoLER.orEmpty()} — ${residuo.descripcion.orEmpty()}\"?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { onDelete(residuo.id); deleteTarget = null },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Eliminar") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    PullToRefreshBox(isRefreshing = state.isLoading, onRefresh = onRefresh) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Residuos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = EcoTextStrong
                    )
                    FilledTonalButton(onClick = onCreateNew) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Nuevo", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por código LER o descripción...") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (!state.isLoading && state.filteredResiduos.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No hay residuos registrados", color = EcoTextMuted)
                    }
                }
            }

            items(state.filteredResiduos, key = { it.id }) { residuo ->
                ResiduoCard(
                    residuo = residuo,
                    onClick = { onResiduoSelected(residuo.id) },
                    onEdit = { onEdit(residuo.id) },
                    onDelete = { deleteTarget = residuo }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ResiduoCard(
    residuo: ResiduoDto,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    EcoCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Science,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = residuo.codigoLER.orEmpty().ifBlank { "Sin código LER" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EcoTextStrong
                    )
                    if (!residuo.descripcion.isNullOrBlank()) {
                        Text(
                            text = residuo.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = EcoTextSubtle
                        )
                    }
                    if (residuo.cantidad != null) {
                        Text(
                            text = "${residuo.cantidad} ${residuo.unidad.orEmpty()}".trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = EcoTextMuted
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// --- Previews ---

private val sampleResiduos = listOf(
    ResiduoDto(id = 1, codigoLER = "20 01 01", descripcion = "Papel y cartón", cantidad = 5.0, unidad = "kg"),
    ResiduoDto(id = 2, codigoLER = "20 01 39", descripcion = "Plástico", cantidad = 12.0, unidad = "t"),
    ResiduoDto(id = 3, codigoLER = "20 03 01", descripcion = "Mezcla de residuos")
)

@Preview(showBackground = true, name = "Lista de Residuos")
@Composable
fun ResiduosListScreenPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        ResiduosListScreen(
            state = ResiduosUiState(residuos = sampleResiduos),
            onRefresh = {},
            onSearchChanged = {},
            onResiduoSelected = {},
            onCreateNew = {},
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "Residuos - Vacía")
@Composable
fun ResiduosListEmptyPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        ResiduosListScreen(
            state = ResiduosUiState(),
            onRefresh = {},
            onSearchChanged = {},
            onResiduoSelected = {},
            onCreateNew = {},
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "Residuos - Cargando")
@Composable
fun ResiduosListLoadingPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        ResiduosListScreen(
            state = ResiduosUiState(isLoading = true),
            onRefresh = {},
            onSearchChanged = {},
            onResiduoSelected = {},
            onCreateNew = {},
            onEdit = {},
            onDelete = {}
        )
    }
}
