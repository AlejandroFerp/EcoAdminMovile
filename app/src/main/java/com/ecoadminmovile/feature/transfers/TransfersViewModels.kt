package com.ecoadminmovile.feature.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.HistorialEventoDto
import com.ecoadminmovile.core.model.ResiduoDto
import com.ecoadminmovile.core.model.RutaDto
import com.ecoadminmovile.core.model.TrasladoCreateDto
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.core.model.UsuarioResumenDto
import com.ecoadminmovile.data.CatalogRepository
import com.ecoadminmovile.data.TransfersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI States ---

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
    val historial: List<HistorialEventoDto> = emptyList(),
    val isLoadingHistorial: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val showStatusSheet: Boolean = false,
    val errorMessage: String? = null
)

data class TransferFormUiState(
    val editingTransferId: Long? = null,
    val centros: List<CentroDto> = emptyList(),
    val residuos: List<ResiduoDto> = emptyList(),
    val transportistas: List<UsuarioResumenDto> = emptyList(),
    val rutas: List<RutaDto> = emptyList(),
    val selectedProductorId: Long? = null,
    val selectedGestorId: Long? = null,
    val selectedResiduoId: Long? = null,
    val selectedTransportistaId: Long? = null,
    val selectedRutaId: Long? = null,
    val observaciones: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false
) {
    val isFormValid: Boolean
        get() = selectedProductorId != null && selectedGestorId != null && selectedResiduoId != null
}

// --- ViewModels ---

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
                    _uiState.update { it.copy(isLoading = false, transfers = transfers) }
                    applyFilters()
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
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
                    transfer.residuo?.descripcion?.contains(state.searchQuery, ignoreCase = true) == true ||
                    transfer.residuo?.codigoLER?.contains(state.searchQuery, ignoreCase = true) == true ||
                    transfer.estado.contains(state.searchQuery, ignoreCase = true)

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
        if (loadedTransferId == transferId && _uiState.value.transfer != null) return

        loadedTransferId = transferId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadTransfer(transferId).fold(
                onSuccess = { transfer ->
                    _uiState.update { it.copy(isLoading = false, transfer = transfer) }
                    loadHistory(transferId)
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                }
            )
        }
    }

    private fun loadHistory(transferId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistorial = true) }
            repository.loadHistory(transferId).fold(
                onSuccess = { historial ->
                    _uiState.update { it.copy(isLoadingHistorial = false, historial = historial) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingHistorial = false) }
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

    fun changeStatus(newStatus: String, comentario: String? = null) {
        val transferId = loadedTransferId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStatus = true, showStatusSheet = false) }
            repository.updateStatus(transferId, newStatus, comentario).fold(
                onSuccess = { updated ->
                    _uiState.update { it.copy(isUpdatingStatus = false, transfer = updated) }
                    loadHistory(transferId)
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isUpdatingStatus = false, errorMessage = throwable.message) }
                }
            )
        }
    }

    fun deleteTransfer(onDeleted: () -> Unit) {
        val transferId = loadedTransferId ?: return
        viewModelScope.launch {
            repository.deleteTransfer(transferId).fold(
                onSuccess = { onDeleted() },
                onFailure = { throwable ->
                    _uiState.update { it.copy(errorMessage = throwable.message) }
                }
            )
        }
    }

    companion object {
        fun nextStates(currentStatus: String): List<String> = when (currentStatus.uppercase()) {
            "PENDIENTE" -> listOf("EN_TRANSITO")
            "EN_TRANSITO" -> listOf("ENTREGADO")
            "ENTREGADO" -> listOf("COMPLETADO")
            else -> emptyList()
        }
    }
}

@HiltViewModel
class TransferFormViewModel @Inject constructor(
    private val repository: TransfersRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransferFormUiState())
    val uiState: StateFlow<TransferFormUiState> = _uiState.asStateFlow()

    fun initForm(transferId: Long? = null) {
        viewModelScope.launch {
            // Load catalogs in parallel
            val centrosResult = catalogRepository.loadCentros()
            val residuosResult = catalogRepository.loadResiduos()
            val transportistasResult = catalogRepository.loadTransportistas()
            val rutasResult = catalogRepository.loadRutas()

            _uiState.update {
                it.copy(
                    editingTransferId = transferId,
                    centros = centrosResult.getOrDefault(emptyList()),
                    residuos = residuosResult.getOrDefault(emptyList()),
                    transportistas = transportistasResult.getOrDefault(emptyList()),
                    rutas = rutasResult.getOrDefault(emptyList())
                )
            }

            // If editing, pre-fill fields
            if (transferId != null) {
                repository.loadTransfer(transferId).onSuccess { transfer ->
                    _uiState.update {
                        it.copy(
                            selectedProductorId = transfer.centroProductor?.id,
                            selectedGestorId = transfer.centroGestor?.id,
                            selectedResiduoId = transfer.residuo?.id,
                            selectedTransportistaId = transfer.transportista?.id,
                            selectedRutaId = transfer.ruta?.id,
                            observaciones = transfer.observaciones.orEmpty()
                        )
                    }
                }
            }
        }
    }

    fun onFieldChanged(field: TransferFormField) {
        _uiState.update { state ->
            when (field) {
                is TransferFormField.Productor -> state.copy(selectedProductorId = field.id)
                is TransferFormField.Gestor -> state.copy(selectedGestorId = field.id)
                is TransferFormField.Residuo -> state.copy(selectedResiduoId = field.id)
                is TransferFormField.Transportista -> state.copy(selectedTransportistaId = field.id)
                is TransferFormField.Ruta -> state.copy(selectedRutaId = field.id)
                is TransferFormField.Observaciones -> state.copy(observaciones = field.text)
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.isFormValid) return

        val dto = TrasladoCreateDto(
            centroProductorId = state.selectedProductorId!!,
            centroGestorId = state.selectedGestorId!!,
            residuoId = state.selectedResiduoId!!,
            transportistaId = state.selectedTransportistaId,
            rutaId = state.selectedRutaId,
            observaciones = state.observaciones.ifBlank { null }
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val result = if (state.editingTransferId != null) {
                repository.updateTransfer(state.editingTransferId, dto)
            } else {
                repository.createTransfer(dto)
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = throwable.message) }
                }
            )
        }
    }
}
