package com.ecoadminmovile.feature.rutas

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
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.RutaDto
import com.ecoadminmovile.data.RutasRepository
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

data class RutasUiState(
    val rutas: List<RutaDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = ""
) {
    val filteredRutas: List<RutaDto>
        get() = if (searchQuery.isBlank()) rutas
        else {
            val normalizedQuery = searchQuery.replace(" ", "").lowercase()
            rutas.filter { it.nombre.replace(" ", "").lowercase().contains(normalizedQuery) }
        }
}

// --- ViewModel ---

@HiltViewModel
class RutasViewModel @Inject constructor(
    private val repository: RutasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RutasUiState())
    val uiState: StateFlow<RutasUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadAll().fold(
                onSuccess = { list -> _uiState.update { it.copy(rutas = list, isLoading = false) } },
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
fun RutasListScreen(
    state: RutasUiState,
    onRefresh: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onRutaSelected: (Long) -> Unit,
    onCreateNew: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit = {}
) {
    var deleteTarget by remember { mutableStateOf<RutaDto?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    deleteTarget?.let { ruta ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar ruta") },
            text = { Text("¿Eliminar la ruta \"${ruta.nombre}\"?") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(ruta.id); deleteTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchExpanded) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChanged,
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            placeholder = { Text("Buscar...", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Text("Rutas", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) onSearchChanged("")
                    }) {
                        Icon(if (isSearchExpanded) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = "Buscar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNew) {
                Icon(Icons.Default.Add, contentDescription = "Nueva")
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.errorMessage != null) {
                    item {
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (!state.isLoading && state.filteredRutas.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                            Text("No hay rutas registradas", color = EcoTextMuted)
                        }
                    }
                }

                items(state.filteredRutas, key = { it.id }) { ruta ->
                    RutaCard(
                        ruta = ruta,
                        onClick = { onRutaSelected(ruta.id) },
                        onEdit = { onEdit(ruta.id) },
                        onDelete = { deleteTarget = ruta }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun RutaCard(
    ruta: RutaDto,
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
                    Icons.Default.Route,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = ruta.nombre.ifBlank { "Ruta #${ruta.id}" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EcoTextStrong
                    )
                    if (ruta.distanciaKm != null) {
                        Text(
                            text = "${ruta.distanciaKm} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = EcoTextSubtle
                        )
                    }
                    if (ruta.transportista != null) {
                        Text(
                            text = ruta.transportista.nombre,
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

private val sampleRutas = listOf(
    RutaDto(id = 1, nombre = "Madrid-Valencia", distanciaKm = 350.0),
    RutaDto(id = 2, nombre = "Madrid-Barcelona", distanciaKm = 620.0),
    RutaDto(id = 3, nombre = "Sevilla-Granada", distanciaKm = 250.0)
)

@Preview(showBackground = true, name = "Lista de Rutas")
@Composable
fun RutasListScreenPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        RutasListScreen(
            state = RutasUiState(rutas = sampleRutas),
            onRefresh = {},
            onSearchChanged = {},
            onRutaSelected = {},
            onCreateNew = {},
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "Rutas - Vacía")
@Composable
fun RutasListEmptyPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        RutasListScreen(
            state = RutasUiState(),
            onRefresh = {},
            onSearchChanged = {},
            onRutaSelected = {},
            onCreateNew = {},
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "Rutas - Cargando")
@Composable
fun RutasListLoadingPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        RutasListScreen(
            state = RutasUiState(isLoading = true),
            onRefresh = {},
            onSearchChanged = {},
            onRutaSelected = {},
            onCreateNew = {},
            onEdit = {},
            onDelete = {}
        )
    }
}
