package com.ecoadminmovile.feature.transfers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.data.TransfersRepository
import com.ecoadminmovile.ui.components.EcoCard
import com.ecoadminmovile.ui.components.EcoStatusPill
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

private val TRANSFER_STATES = listOf("PENDIENTE", "EN_TRANSITO", "ENTREGADO", "COMPLETADO")

data class TransfersUiState(
    val isLoading: Boolean = true,
    val transfers: List<TrasladoDto> = emptyList(),
    val filteredTransfers: List<TrasladoDto> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: String? = null,
    val errorMessage: String? = null
)

data class TransferDetailUiState(
    val isLoading: Boolean = true,
    val transfer: TrasladoDto? = null,
    val errorMessage: String? = null,
    val isUpdatingStatus: Boolean = false,
    val showStatusSheet: Boolean = false
)

@HiltViewModel
class TransfersViewModel @Inject constructor(
    private val repository: TransfersRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransfersUiState())
    val uiState: StateFlow<TransfersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadTransfers().fold(
                onSuccess = { transfers ->
                    _uiState.update {
                        it.copy(isLoading = false, transfers = transfers)
                    }
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

    fun filterByStatus(status: String?) {
        _uiState.update {
            it.copy(selectedStatus = if (it.selectedStatus == status) null else status)
        }
        applyFilters()
    }

    private fun applyFilters() {
        _uiState.update { state ->
            val filtered = state.transfers.filter { transfer ->
                val matchesSearch = state.searchQuery.isBlank() ||
                    transfer.codigo.contains(state.searchQuery, ignoreCase = true) ||
                    transfer.centroProductor?.nombre?.contains(state.searchQuery, ignoreCase = true) == true ||
                    transfer.centroGestor?.nombre?.contains(state.searchQuery, ignoreCase = true) == true ||
                    transfer.residuo?.descripcion?.contains(state.searchQuery, ignoreCase = true) == true

                val matchesStatus = state.selectedStatus == null ||
                    transfer.estado.equals(state.selectedStatus, ignoreCase = true)

                matchesSearch && matchesStatus
            }
            state.copy(filteredTransfers = filtered)
        }
    }
}

@HiltViewModel
class TransferDetailViewModel @Inject constructor(
    private val repository: TransfersRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransferDetailUiState())
    val uiState: StateFlow<TransferDetailUiState> = _uiState.asStateFlow()

    private var loadedTransferId: Long? = null

    fun load(transferId: Long) {
        if (loadedTransferId == transferId && _uiState.value.transfer != null) {
            return
        }

        loadedTransferId = transferId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadTransfer(transferId).fold(
                onSuccess = { transfer ->
                    _uiState.value = TransferDetailUiState(
                        isLoading = false,
                        transfer = transfer
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = TransferDetailUiState(
                        isLoading = false,
                        errorMessage = throwable.message
                    )
                }
            )
        }
    }

    fun showStatusSheet() {
        _uiState.update { it.copy(showStatusSheet = true) }
    }

    fun hideStatusSheet() {
        _uiState.update { it.copy(showStatusSheet = false) }
    }

    fun changeStatus(newStatus: String) {
        val transferId = loadedTransferId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStatus = true, showStatusSheet = false) }
            repository.updateStatus(transferId, newStatus).fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(isUpdatingStatus = false, transfer = updated)
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(isUpdatingStatus = false, errorMessage = throwable.message)
                    }
                }
            )
        }
    }

    companion object {
        fun nextStates(currentStatus: String): List<String> {
            return when (currentStatus.uppercase()) {
                "PENDIENTE" -> listOf("EN_TRANSITO")
                "EN_TRANSITO" -> listOf("ENTREGADO")
                "ENTREGADO" -> listOf("COMPLETADO")
                else -> emptyList()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(
    state: TransfersUiState,
    onRefresh: () -> Unit,
    onTransferSelected: (Long) -> Unit,
    onSearchChanged: (String) -> Unit = {},
    onStatusFilter: (String?) -> Unit = {}
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Traslados",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = EcoTextStrong
                    )
                    Text(
                        text = "Listado de envios registrados en el sistema",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EcoTextSubtle
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por código, productor, gestor...") },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TRANSFER_STATES.forEach { status ->
                        FilterChip(
                            selected = state.selectedStatus == status,
                            onClick = { onStatusFilter(status) },
                            label = { Text(status.replace("_", " "), style = MaterialTheme.typography.labelSmall) }
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

            items(state.filteredTransfers, key = { transfer -> transfer.id }) { transfer ->
                TransferCard(
                    transfer = transfer,
                    onClick = { onTransferSelected(transfer.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDetailScreen(
    state: TransferDetailUiState,
    onBack: () -> Unit,
    onShowStatusSheet: () -> Unit = {},
    onDismissStatusSheet: () -> Unit = {},
    onChangeStatus: (String) -> Unit = {}
) {
    if (state.showStatusSheet) {
        val availableStates = state.transfer?.let {
            TransferDetailViewModel.nextStates(it.estado)
        } ?: emptyList()

        ModalBottomSheet(onDismissRequest = onDismissStatusSheet) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cambiar estado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Estado actual: ${state.transfer?.estado.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoTextSubtle
                )
                Spacer(Modifier.height(8.dp))
                availableStates.forEach { nextStatus ->
                    Button(
                        onClick = { onChangeStatus(nextStatus) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pasar a ${nextStatus.replace("_", " ")}")
                    }
                }
                if (availableStates.isEmpty()) {
                    Text(
                        text = "No hay transiciones disponibles desde este estado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EcoTextMuted
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.transfer?.codigo ?: "Detalle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            val hasTransitions = state.transfer?.let {
                TransferDetailViewModel.nextStates(it.estado).isNotEmpty()
            } ?: false
            if (hasTransitions) {
                FloatingActionButton(onClick = onShowStatusSheet) {
                    Text("Cambiar estado", modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            state.transfer?.let { transfer ->
                item {
                    EcoCard {
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
                                    text = transfer.codigo,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                EcoStatusPill(status = transfer.estado)
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            DetailItem("Productor", transfer.centroProductor?.nombre.orEmpty())
                            DetailItem("Gestor", transfer.centroGestor?.nombre.orEmpty())
                            DetailItem("Residuo", "${transfer.residuo?.codigo.orEmpty()} - ${transfer.residuo?.descripcion.orEmpty()}")
                            DetailItem("Transportista", transfer.transportista?.nombre.orEmpty())
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            DetailItem("Creado", transfer.fechaCreacion.orEmpty())
                            DetailItem("Inicio transporte", transfer.fechaInicioTransporte.orEmpty())
                            DetailItem("Entrega", transfer.fechaEntrega.orEmpty())
                            
                            if (!transfer.observaciones.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Observaciones",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = transfer.observaciones,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EcoTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = EcoTextSubtle
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = EcoTextStrong
        )
    }
}

@Composable
private fun TransferCard(
    transfer: TrasladoDto,
    onClick: () -> Unit
) {
    EcoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    text = "#${transfer.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EcoTextSubtle,
                    fontWeight = FontWeight.Bold
                )
                EcoStatusPill(status = transfer.estado)
            }
            
            Text(
                text = transfer.centroProductor?.nombre.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = EcoTextStrong
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(4.dp).background(EcoTextSubtle, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = transfer.centroGestor?.nombre.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoTextMuted
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LER ${transfer.residuo?.codigo.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EcoTextSubtle,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = transfer.fechaCreacion?.take(10).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = EcoTextSubtle
                )
            }
        }
    }
}
