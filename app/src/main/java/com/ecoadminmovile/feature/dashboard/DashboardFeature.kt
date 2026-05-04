package com.ecoadminmovile.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.EstadisticasDto
import com.ecoadminmovile.data.DashboardRepository
import com.ecoadminmovile.ui.components.EcoCard
import com.ecoadminmovile.ui.components.EcoMetricCard
import com.ecoadminmovile.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class DashboardPeriod(val label: String, val daysBack: Int?) {
    TODAY("Hoy", 0),
    WEEK("7 días", 7),
    MONTH("30 días", 30),
    ALL("Todo", null)
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val data: EstadisticasDto = EstadisticasDto(),
    val selectedPeriod: DashboardPeriod = DashboardPeriod.MONTH,
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
            val desde = _uiState.value.selectedPeriod.daysBack?.let { days ->
                LocalDate.now().minusDays(days.toLong())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
            }
            repository.loadDashboard(desde).fold(
                onSuccess = { stats ->
                    _uiState.update { it.copy(isLoading = false, data = stats) }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message)
                    }
                }
            )
        }
    }

    fun setPeriod(period: DashboardPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        load()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onPeriodSelected: (DashboardPeriod) -> Unit = {}
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Panel de Control",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = EcoTextStrong
                )
                Text(
                    text = "Resumen del sistema EcoAdmin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoTextSubtle
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DashboardPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = state.selectedPeriod == period,
                        onClick = { onPeriodSelected(period) },
                        label = { Text(period.label) }
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

        item {
            EcoMetricCard(
                title = "Centros registrados",
                value = state.data.totalCentros.toString(),
                icon = Icons.Rounded.Business,
                iconBgColor = Color(0xFFEBF2FF),
                iconColor = EcoPrimary,
                badgeText = "activo"
            )
        }

        item {
            EcoMetricCard(
                title = "Residuos catalogados",
                value = state.data.totalResiduos.toString(),
                icon = Icons.Rounded.Autorenew,
                iconBgColor = Color(0xFFFFF7ED),
                iconColor = Color(0xFFF97316),
                badgeText = "peligroso",
                badgeColor = Color(0xFFC2410C),
                badgeBgColor = Color(0xFFFFEDD5)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EcoMetricCard(
                    title = "En curso",
                    value = state.data.trasladosEnTransito.toString(),
                    icon = Icons.Rounded.SwapHoriz,
                    iconBgColor = Color(0xFFFEFCE8),
                    iconColor = Color(0xFFEAB308),
                    modifier = Modifier.weight(1f)
                )
                EcoMetricCard(
                    title = "Completados",
                    value = state.data.trasladosCompletados.toString(),
                    icon = Icons.Rounded.CheckCircle,
                    iconBgColor = Color(0xFFECFDF5),
                    iconColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Traslados por estado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EcoTextStrong,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            EcoCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val total = (state.data.trasladosPendientes + state.data.trasladosEnTransito + 
                                state.data.trasladosEntregados + state.data.trasladosCompletados).coerceAtLeast(1)
                    
                    StatusProgressRow("Pendiente", state.data.trasladosPendientes, total, EcoPendingDot)
                    StatusProgressRow("En tránsito", state.data.trasladosEnTransito, total, EcoTransitDot)
                    StatusProgressRow("Entregado", state.data.trasladosEntregados, total, EcoDeliveredDot)
                    StatusProgressRow("Completado", state.data.trasladosCompletados, total, EcoCompletedDot)
                }
            }
        }

        }
    }
}

@Composable
fun StatusProgressRow(
    label: String,
    value: Int,
    total: Int,
    color: Color
) {
    val progress = value.toFloat() / total.toFloat()
    val percentage = (progress * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = EcoTextMuted,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$value ($percentage%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = EcoTextStrong
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = EcoBg,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    EcoAdminTheme {
        DashboardScreen(
            state = DashboardUiState(
                isLoading = false,
                data = EstadisticasDto(
                    totalCentros = 12,
                    totalResiduos = 45,
                    trasladosPendientes = 5,
                    trasladosEnTransito = 3,
                    trasladosEntregados = 2,
                    trasladosCompletados = 35
                )
            ),
            onRefresh = {}
        )
    }
}
