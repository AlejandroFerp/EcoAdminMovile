package com.ecoadminmovile.feature.centers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.data.CentersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CentersUiState(
    val isLoading: Boolean = true,
    val centers: List<CentroDto> = emptyList(),
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
                    _uiState.value = CentersUiState(isLoading = false, centers = centers)
                },
                onFailure = { throwable ->
                    _uiState.value = CentersUiState(
                        isLoading = false,
                        errorMessage = throwable.message
                    )
                }
            )
        }
    }
}

@Composable
fun CentersScreen(
    state: CentersUiState,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Centros",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Vista movil inicial para productores y gestores conectada a /api/centros.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onRefresh) {
                    Text(text = if (state.isLoading) "Actualizando" else "Actualizar centros")
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

        items(state.centers, key = { center -> center.id }) { center ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = center.nombre,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Codigo: ${center.codigo}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Tipo: ${center.tipo.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Direccion: ${listOfNotNull(center.direccion?.calle, center.direccion?.ciudad).joinToString()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Contacto: ${center.nombreContacto.orEmpty()} ${center.telefono.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
