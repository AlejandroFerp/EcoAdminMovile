package com.ecoadminmovile.feature.transfers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.data.TransfersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransfersUiState(
    val isLoading: Boolean = true,
    val transfers: List<TrasladoDto> = emptyList(),
    val errorMessage: String? = null
)

data class TransferDetailUiState(
    val isLoading: Boolean = true,
    val transfer: TrasladoDto? = null,
    val errorMessage: String? = null
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
                    _uiState.value = TransfersUiState(
                        isLoading = false,
                        transfers = transfers
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = TransfersUiState(
                        isLoading = false,
                        errorMessage = throwable.message
                    )
                }
            )
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
}

@Composable
fun TransfersScreen(
    state: TransfersUiState,
    onRefresh: () -> Unit,
    onTransferSelected: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Traslados",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Listado conectado a /api/traslados con detalle basico por envio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onRefresh) {
                    Text(text = if (state.isLoading) "Cargando" else "Recargar")
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

        items(state.transfers, key = { transfer -> transfer.id }) { transfer ->
            TransferCard(
                transfer = transfer,
                onClick = { onTransferSelected(transfer.id) }
            )
        }
    }
}

@Composable
fun TransferDetailScreen(
    state: TransferDetailUiState,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedButton(onClick = onBack) {
                Text(text = "Volver")
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

        state.transfer?.let { transfer ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = transfer.codigo,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Estado: ${transfer.estado}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Centro productor: ${transfer.centroProductor?.nombre.orEmpty()}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Centro gestor: ${transfer.centroGestor?.nombre.orEmpty()}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Residuo: ${transfer.residuo?.codigo.orEmpty()} ${transfer.residuo?.descripcion.orEmpty()}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Transportista: ${transfer.transportista?.nombre.orEmpty()}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Creado: ${transfer.fechaCreacion.orEmpty()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Inicio transporte: ${transfer.fechaInicioTransporte.orEmpty()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Entrega: ${transfer.fechaEntrega.orEmpty()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!transfer.observaciones.isNullOrBlank()) {
                            Text(
                                text = "Observaciones: ${transfer.observaciones}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferCard(
    transfer: TrasladoDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = transfer.codigo,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = transfer.estado,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${transfer.centroProductor?.nombre.orEmpty()} -> ${transfer.centroGestor?.nombre.orEmpty()}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Residuo ${transfer.residuo?.codigo.orEmpty()} ${transfer.residuo?.cantidad ?: 0.0} ${transfer.residuo?.unidad.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
