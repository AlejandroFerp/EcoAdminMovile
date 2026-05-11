/**
 * Feature de Centros: listado + detalle con ViewModel y Composables.
 *
 * Conceptos Kotlin demostrados:
 * - Múltiples Composables + ViewModel en UN archivo (organización por feature).
 * - listOf(...): creación de listas inmutables constantes.
 * - .equals(..., ignoreCase = true): comparación case-insensitive (parámetro named).
 * - .orEmpty(): convierte String? → String (devuelve "" si null).
 * - if (text.isBlank()) return: early return para evitar anidación innecesaria.
 * - Modifier.weight(1f): ocupa espacio proporcional en Row/Column (flexbox).
 * - Renderizado condicional: `if (state.isLoading) { ... } else { ... }` en Compose.
 *
 * Patrones de diseño:
 * - MVVM con estado reactivo (StateFlow).
 * - Feature-based organization: todo lo del feature en un archivo.
 * - Container-Presentational: ViewModel maneja lógica, Composables solo pintan.
 */
package com.ecoadminmovile.feature.centers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.data.CentersRepository
import com.ecoadminmovile.ui.components.EcoCard
import com.ecoadminmovile.ui.theme.EcoTextMuted
import com.ecoadminmovile.ui.theme.EcoTextStrong
import com.ecoadminmovile.ui.theme.EcoTextSubtle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// listOf(): crea lista inmutable. En Kotlin las listas son inmutables por defecto.
private val CENTER_TYPES = listOf("PRODUCTOR", "GESTOR")

data class CentersUiState(
    val isLoading: Boolean = true,
    val centers: List<CentroDto> = emptyList(),
    val filteredCenters: List<CentroDto> = emptyList(),
    val searchQuery: String = "",
    val selectedType: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class CentersViewModel @Inject constructor(
    private val repository: CentersRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CentersUiState())
    val uiState: StateFlow<CentersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadCenters().fold(
                onSuccess = { centers ->
                    _uiState.update { it.copy(isLoading = false, centers = centers) }
                    applyFilters()
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message)
                    }
                }
            )
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun filterByType(type: String?) {
        _uiState.update {
            it.copy(selectedType = if (it.selectedType == type) null else type)
        }
        applyFilters()
    }

    private fun applyFilters() {
        _uiState.update { state ->
            val filtered = state.centers.filter { center ->
                val matchesSearch = state.searchQuery.isBlank() ||
                    center.nombre?.contains(state.searchQuery, ignoreCase = true) == true ||
                    center.codigo?.contains(state.searchQuery, ignoreCase = true) == true ||
                    center.nima?.contains(state.searchQuery, ignoreCase = true) == true

                // .equals(..., ignoreCase = true): comparación sin distinguir mayúsculas/minúsculas.
                // Named parameter ignoreCase mejora legibilidad.
                val matchesType = state.selectedType == null ||
                    center.tipo.equals(state.selectedType, ignoreCase = true)

                matchesSearch && matchesType
            }
            state.copy(filteredCenters = filtered)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentersScreen(
    state: CentersUiState,
    onRefresh: () -> Unit,
    onSearchChanged: (String) -> Unit = {},
    onTypeFilter: (String?) -> Unit = {},
    onCenterSelected: (Long) -> Unit = {},
    onCreateNew: () -> Unit = {}
) {
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Centros",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = EcoTextStrong
                    )
                    Text(
                        text = "Gestión de productores y gestores",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EcoTextSubtle
                    )
                }
                FilledTonalButton(onClick = onCreateNew) {
                    Text("+ Nuevo")
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre, código, NIMA...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CENTER_TYPES.forEach { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { onTypeFilter(type) },
                        label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        if (state.errorMessage != null) {
            item {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        items(state.filteredCenters, key = { center -> center.id }) { center ->
            CenterCard(center = center, onClick = { onCenterSelected(center.id) })
        }
        }
    }
}

@Composable
private fun CenterCard(center: CentroDto, onClick: () -> Unit = {}) {
    EcoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = center.nombre.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EcoTextStrong
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = center.tipo.orEmpty(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Text(
                text = "NIMA: ${center.nima ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = EcoTextSubtle
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            ContactInfoRow(Icons.Rounded.LocationOn, listOfNotNull(center.direccion?.calle, center.direccion?.ciudad).joinToString(", "))
            ContactInfoRow(Icons.Rounded.Person, center.nombreContacto.orEmpty())
            ContactInfoRow(Icons.Rounded.Phone, center.telefono.orEmpty())
            ContactInfoRow(Icons.Rounded.Email, center.email.orEmpty())
        }
    }
}

@Composable
private fun ContactInfoRow(icon: ImageVector, text: String) {
    if (text.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = EcoTextSubtle
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = EcoTextMuted
        )
    }
}

// --- Center Detail ---

data class CenterDetailUiState(
    val isLoading: Boolean = true,
    val center: CentroDto? = null,
    val errorMessage: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false
)

@HiltViewModel
class CenterDetailViewModel @Inject constructor(
    private val repository: CentersRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CenterDetailUiState())
    val uiState: StateFlow<CenterDetailUiState> = _uiState.asStateFlow()

    private var loadedCenterId: Long? = null

    fun load(centerId: Long) {
        if (loadedCenterId == centerId && _uiState.value.center != null) return

        loadedCenterId = centerId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadCenter(centerId).fold(
                onSuccess = { center ->
                    _uiState.value = CenterDetailUiState(isLoading = false, center = center)
                },
                onFailure = { throwable ->
                    _uiState.value = CenterDetailUiState(
                        isLoading = false,
                        errorMessage = throwable.message
                    )
                }
            )
        }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun deleteCenter(onDeleted: () -> Unit) {
        val centerId = loadedCenterId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showDeleteConfirmation = false) }
            repository.deleteCenter(centerId).fold(
                onSuccess = { onDeleted() },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isDeleting = false, errorMessage = throwable.message) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterDetailScreen(
    state: CenterDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onShowDeleteConfirmation: () -> Unit = {},
    onDismissDeleteConfirmation: () -> Unit = {}
) {
    // Delete confirmation dialog
    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirmation,
            title = { Text("Eliminar centro") },
            text = { Text("¿Estás seguro de que deseas eliminar \"${state.center?.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteConfirmation) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.center?.nombre ?: "Centro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = onShowDeleteConfirmation) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
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
        } else if (state.errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            state.center?.let { center ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EcoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = center.nombre.orEmpty(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = center.tipo.orEmpty(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "Código: ${center.codigo}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EcoTextSubtle
                            )
                            Text(
                                text = "NIMA: ${center.nima ?: "N/A"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EcoTextSubtle
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            ContactInfoRow(Icons.Rounded.LocationOn, listOfNotNull(center.direccion?.calle, center.direccion?.ciudad).joinToString(", "))
                            ContactInfoRow(Icons.Rounded.Person, center.nombreContacto.orEmpty())
                            ContactInfoRow(Icons.Rounded.Phone, center.telefono.orEmpty())
                            ContactInfoRow(Icons.Rounded.Email, center.email.orEmpty())
                        }
                    }
                }
            }
        }
    }
}

// --- Previews ---

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Centros - Lista")
@Composable
fun CentersScreenPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        CentersScreen(
            state = CentersUiState(
                isLoading = false,
                centers = listOf(
                    CentroDto(id = 1, codigo = "CP-001", nombre = "Fábrica Norte", tipo = "PRODUCTOR", nima = "ES-12345"),
                    CentroDto(id = 2, codigo = "CG-001", nombre = "Gestor Ambiental S.L.", tipo = "GESTOR", nima = "ES-67890"),
                    CentroDto(id = 3, codigo = "CP-002", nombre = "Planta Levante", tipo = "PRODUCTOR")
                ),
                filteredCenters = listOf(
                    CentroDto(id = 1, codigo = "CP-001", nombre = "Fábrica Norte", tipo = "PRODUCTOR", nima = "ES-12345"),
                    CentroDto(id = 2, codigo = "CG-001", nombre = "Gestor Ambiental S.L.", tipo = "GESTOR", nima = "ES-67890"),
                    CentroDto(id = 3, codigo = "CP-002", nombre = "Planta Levante", tipo = "PRODUCTOR")
                )
            ),
            onRefresh = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Centro - Detalle")
@Composable
fun CenterDetailScreenPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        CenterDetailScreen(
            state = CenterDetailUiState(
                isLoading = false,
                center = CentroDto(
                    id = 1,
                    codigo = "CP-001",
                    nombre = "Fábrica Norte",
                    tipo = "PRODUCTOR",
                    nima = "ES-12345",
                    telefono = "+34 965 123 456",
                    email = "fabrica@norte.es",
                    nombreContacto = "Carlos Ruiz"
                )
            ),
            onBack = {}
        )
    }
}
