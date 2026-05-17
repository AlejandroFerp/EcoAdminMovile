package com.ecoadminmovile.feature.documentos

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.DocumentoDto
import com.ecoadminmovile.data.DocumentosRepository
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

data class DocumentosUiState(
    val documentos: List<DocumentoDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val tipoFilter: String? = null,
    val searchQuery: String = ""
) {
    val filteredDocumentos: List<DocumentoDto>
        get() = documentos.filter { doc ->
            val matchesFilter = tipoFilter == null || doc.tipo.equals(tipoFilter, ignoreCase = true)
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                val normalizedQuery = searchQuery.replace(" ", "").lowercase()
                val normalizedNombre = doc.nombre.replace(" ", "").lowercase()
                val normalizedTipo = tipoLabel(doc.tipo).replace(" ", "").lowercase()
                normalizedNombre.contains(normalizedQuery) || normalizedTipo.contains(normalizedQuery)
            }
            matchesFilter && matchesQuery
        }

    val tiposDisponibles: List<String>
        get() = documentos.map { it.tipo }.distinct().sorted()
}

// --- ViewModel ---

@HiltViewModel
class DocumentosViewModel @Inject constructor(
    private val repository: DocumentosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentosUiState())
    val uiState: StateFlow<DocumentosUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadAll().fold(
                onSuccess = { list -> _uiState.update { it.copy(documentos = list, isLoading = false) } },
                onFailure = { err -> _uiState.update { it.copy(errorMessage = err.message, isLoading = false) } }
            )
        }
    }

    fun filterByTipo(tipo: String?) {
        _uiState.update { it.copy(tipoFilter = tipo) }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentosListScreen(
    state: DocumentosUiState,
    onRefresh: () -> Unit,
    onFilterChanged: (String?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenDocument: (DocumentoDto) -> Unit,
    onBack: () -> Unit = {}
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
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
                            Text("Documentos", fontWeight = FontWeight.Bold)
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
                // Filter chips ALWAYS pinned at topbar!
                if (state.tiposDisponibles.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = state.tipoFilter == null,
                            onClick = { onFilterChanged(null) },
                            label = { Text("Todos") }
                        )
                        state.tiposDisponibles.forEach { tipo ->
                            FilterChip(
                                selected = state.tipoFilter == tipo,
                                onClick = { onFilterChanged(tipo) },
                                label = { Text(tipoLabel(tipo)) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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

                    if (!state.isLoading && state.filteredDocumentos.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                                Text("No hay documentos disponibles", color = EcoTextMuted)
                            }
                        }
                    }

                    items(state.filteredDocumentos, key = { it.id }) { documento ->
                        DocumentoCard(documento = documento, onOpen = { onOpenDocument(documento) })
                    }

                }
            }
        }
    }
}

@Composable
private fun DocumentoCard(
    documento: DocumentoDto,
    onOpen: () -> Unit
) {
    EcoCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
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
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = documento.nombre.ifBlank { tipoLabel(documento.tipo) },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EcoTextStrong
                    )
                    Text(
                        text = tipoLabel(documento.tipo),
                        style = MaterialTheme.typography.bodySmall,
                        color = EcoTextSubtle
                    )
                    if (!documento.fechaCreacion.isNullOrBlank()) {
                        Text(
                            text = documento.fechaCreacion.take(10),
                            style = MaterialTheme.typography.bodySmall,
                            color = EcoTextMuted
                        )
                    }
                }
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.Default.OpenInNew, contentDescription = "Abrir", modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun tipoLabel(tipo: String): String = when (tipo.lowercase()) {
    "carta-porte", "carta_porte" -> "Carta de Porte"
    "notificacion", "notificación" -> "Notificación Previa"
    "certificado" -> "Certificado de Recepción"
    "di" -> "Documento de Identificación"
    "contrato" -> "Contrato"
    else -> tipo.replaceFirstChar { it.uppercase() }
}

// --- Previews ---

private val sampleDocumentos = listOf(
    DocumentoDto(id = 1, tipo = "carta-porte", nombre = "CP-2024-001", fechaCreacion = "2024-03-15"),
    DocumentoDto(id = 2, tipo = "notificacion", nombre = "NP-2024-012", fechaCreacion = "2024-03-10"),
    DocumentoDto(id = 3, tipo = "certificado", nombre = "CERT-2024-003", fechaCreacion = "2024-02-28"),
    DocumentoDto(id = 4, tipo = "di", nombre = "DI-2024-007")
)

@Preview(showBackground = true, name = "Lista de Documentos")
@Composable
fun DocumentosListScreenPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        DocumentosListScreen(
            state = DocumentosUiState(documentos = sampleDocumentos),
            onRefresh = {},
            onFilterChanged = {},
            onSearchChanged = {},
            onOpenDocument = {}
        )
    }
}

@Preview(showBackground = true, name = "Documentos - Vacía")
@Composable
fun DocumentosListEmptyPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        DocumentosListScreen(
            state = DocumentosUiState(),
            onRefresh = {},
            onFilterChanged = {},
            onSearchChanged = {},
            onOpenDocument = {}
        )
    }
}

@Preview(showBackground = true, name = "Documentos - Filtrado")
@Composable
fun DocumentosListFilteredPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        DocumentosListScreen(
            state = DocumentosUiState(
                documentos = sampleDocumentos,
                tipoFilter = "carta-porte"
            ),
            onRefresh = {},
            onFilterChanged = {},
            onSearchChanged = {},
            onOpenDocument = {}
        )
    }
}
