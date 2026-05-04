package com.ecoadminmovile.feature.dashboard

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.EstadisticasDto
import com.ecoadminmovile.data.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val data: EstadisticasDto = EstadisticasDto(),
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadDashboard().fold(
                onSuccess = { stats ->
                    _uiState.value = DashboardUiState(isLoading = false, data = stats)
                },
                onFailure = { throwable ->
                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        errorMessage = throwable.message
                    )
                }
            )
        }
    }
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onRefresh: () -> Unit
) {
    val cards = listOf(
        "Centros" to state.data.totalCentros.toString(),
        "Residuos" to state.data.totalResiduos.toString(),
        "Pendientes" to state.data.trasladosPendientes.toString(),
        "En transito" to state.data.trasladosEnTransito.toString(),
        "Entregados" to state.data.trasladosEntregados.toString(),
        "Completados" to state.data.trasladosCompletados.toString()
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Resumen operativo",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Este panel consume /api/estadisticas para ofrecer una vista rapida del estado del sistema.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Button(onClick = onRefresh) {
                Text(text = if (state.isLoading) "Actualizando..." else "Actualizar metricas")
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

        items(cards.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { (title, value) ->
                    MetricCard(
                        title = title,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (pair.size == 1) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Distribucion de residuos por centro",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (state.data.residuosPorCentro.isEmpty()) {
                        Text(
                            text = "No hay datos agregados todavia.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        state.data.residuosPorCentro.forEach { (name, total) ->
                            Text(
                                text = "$name: $total",
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
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
