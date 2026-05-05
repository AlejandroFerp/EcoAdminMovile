/**
 * Dashboard: Panel de control con métricas y gráficos de progreso.
 *
 * Conceptos Kotlin demostrados:
 * - enum class con propiedades (label, daysBack) y constructor.
 * - init {} block: se ejecuta al crear la instancia del ViewModel.
 * - ?.let {}: transformación segura sobre valores nullable (si no es null, ejecuta el bloque).
 * - .coerceAtLeast(1): evita división por cero de forma idiomática.
 * - .toFloat() / .toInt(): Kotlin NO tiene conversiones implícitas (a diferencia de Java).
 * - @OptIn(ExperimentalMaterial3Api::class): acepta uso de APIs experimentales.
 * - LazyColumn con item {} / items {}: DSL (Domain-Specific Language) para listas eficientes.
 * - @Preview: permite ver la UI en el IDE sin ejecutar la app.
 *
 * Patrones de diseño:
 * - MVVM con StateFlow unidireccional.
 * - Pull-to-Refresh (PullToRefreshBox) para recarga manual.
 * - Observer pattern mediante StateFlow.
 */
package com.ecoadminmovile.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.data.DashboardRepository
import com.ecoadminmovile.data.TransfersRepository
import com.ecoadminmovile.ui.components.EcoCard
import com.ecoadminmovile.ui.components.EcoMetricCard
import com.ecoadminmovile.ui.components.EcoStatusPill
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

// enum class con propiedades: cada entrada tiene label y daysBack.
// daysBack es Int? (nullable) → ALL no tiene días asociados.
enum class DashboardPeriod(val label: String, val daysBack: Int?) {
    TODAY("Hoy", 0),
    WEEK("7 días", 7),
    MONTH("30 días", 30),
    ALL("Todo", null) // null indica "sin límite de fecha"
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val data: EstadisticasDto = EstadisticasDto(),
    val recentTransfers: List<TrasladoDto> = emptyList(),
    val selectedPeriod: DashboardPeriod = DashboardPeriod.MONTH,
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val transfersRepository: TransfersRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // init {}: bloque de inicialización, se ejecuta al crear el ViewModel.
    // Ideal para cargar datos iniciales.
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
            // Load recent transfers for the table
            transfersRepository.loadTransfers().onSuccess { transfers ->
                _uiState.update { it.copy(recentTransfers = transfers.take(10)) }
            }
        }
    }

    fun setPeriod(period: DashboardPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        load()
    }
}

// @OptIn: acepta explícitamente el uso de APIs marcadas como experimentales.
// Sin esto, el compilador genera un warning/error.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onPeriodSelected: (DashboardPeriod) -> Unit = {},
    onTransferSelected: (Long) -> Unit = {}
) {
    // PullToRefreshBox: componente que permite "tirar hacia abajo" para refrescar
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        // LazyColumn: lista virtualizada (solo renderiza elementos visibles, como RecyclerView)
        // item {} y items {} son parte del DSL (Domain-Specific Language) de LazyColumn
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

        // Metrics Grid (2 columns)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EcoMetricCard(
                        title = "Centros registrados",
                        value = state.data.totalCentros.toString(),
                        icon = Icons.Rounded.Business,
                        iconBgColor = Color(0xFFEBF2FF),
                        iconColor = EcoPrimary,
                        badgeText = "activo",
                        modifier = Modifier.weight(1f)
                    )

                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()){
                    EcoMetricCard(
                        title = "Residuos",
                        value = state.data.totalResiduos.toString(),
                        icon = Icons.Rounded.Autorenew,
                        iconBgColor = Color(0xFFFFF7ED),
                        iconColor = Color(0xFFF97316),
                        badgeText = "peligroso",
                        badgeColor = Color(0xFFC2410C),
                        badgeBgColor = Color(0xFFFFEDD5),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EcoMetricCard(
                        title = "Pendientes",
                        value = state.data.trasladosPendientes.toString(),
                        icon = Icons.Rounded.Schedule,
                        iconBgColor = Color(0xFFFEFCE8),
                        iconColor = Color(0xFFEAB308),
                        modifier = Modifier.weight(1f)
                    )
                    EcoMetricCard(
                        title = "En curso",
                        value = state.data.trasladosEnTransito.toString(),
                        icon = Icons.Rounded.SwapHoriz,
                        iconBgColor = Color(0xFFEBF2FF),
                        iconColor = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EcoMetricCard(
                        title = "Entregados",
                        value = state.data.trasladosEntregados.toString(),
                        icon = Icons.Rounded.LocalShipping,
                        iconBgColor = Color(0xFFEDE9FE),
                        iconColor = Color(0xFF8B5CF6),
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
                    // .coerceAtLeast(1): asegura que total sea mínimo 1, evitando división por cero
                    val total = (state.data.trasladosPendientes + state.data.trasladosEnTransito + 
                                state.data.trasladosEntregados + state.data.trasladosCompletados).coerceAtLeast(1)
                    
                    StatusProgressRow("Pendiente", state.data.trasladosPendientes, total, EcoPendingDot)
                    StatusProgressRow("En tránsito", state.data.trasladosEnTransito, total, EcoTransitDot)
                    StatusProgressRow("Entregado", state.data.trasladosEntregados, total, EcoDeliveredDot)
                    StatusProgressRow("Completado", state.data.trasladosCompletados, total, EcoCompletedDot)
                }
            }
        }

        // Residuos por centro (data from DTO)
        if (state.data.residuosPorCentro.isNotEmpty()) {
            item {
                Text(
                    text = "Residuos por centro",
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val maxValue = state.data.residuosPorCentro.values.maxOrNull() ?: 1
                        state.data.residuosPorCentro.entries.forEach { (centro, cantidad) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = centro,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EcoTextMuted,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Text(
                                        text = cantidad.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EcoTextStrong
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { cantidad.toFloat() / maxValue.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = EcoPrimary,
                                    trackColor = EcoBg,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent transfers table (like web dashboard)
        if (state.recentTransfers.isNotEmpty()) {
            item {
                Text(
                    text = "Últimos traslados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EcoTextStrong,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                EcoCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Table header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Código", style = MaterialTheme.typography.labelSmall, color = EcoTextSubtle, modifier = Modifier.weight(1f))
                            Text("Residuo", style = MaterialTheme.typography.labelSmall, color = EcoTextSubtle, modifier = Modifier.weight(1.5f))
                            Text("Estado", style = MaterialTheme.typography.labelSmall, color = EcoTextSubtle, modifier = Modifier.weight(1f))
                            Text("Fecha", style = MaterialTheme.typography.labelSmall, color = EcoTextSubtle, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        // Table rows
                        state.recentTransfers.forEach { transfer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTransferSelected(transfer.id) }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = transfer.codigo.ifBlank { "#${transfer.id}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = EcoPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = transfer.residuo?.codigoLER.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EcoTextMuted,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1.5f)
                                )
                                EcoStatusPill(
                                    status = transfer.estado,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = transfer.fechaCreacion?.take(10).orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EcoTextSubtle,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                        }
                    }
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
    // .toFloat(): conversión EXPLÍCITA. Kotlin NO convierte Int→Float automáticamente.
    val progress = value.toFloat() / total.toFloat()
    val percentage = (progress * 100).toInt() // .toInt(): trunca el decimal

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

// @Preview: permite visualizar el Composable directamente en Android Studio sin ejecutar la app.
// Útil para iterar rápido sobre la UI.
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
                    trasladosCompletados = 35,
                    residuosPorCentro = mapOf(
                        "Fábrica Norte" to 15,
                        "Planta Sur" to 25,
                        "Almacén Central" to 5
                    )
                ),
                recentTransfers = listOf(
                    TrasladoDto(
                        id = 1,
                        codigo = "ECO-001",
                        estado = "EN_TRANSITO",
                        fechaCreacion = "2025-05-01",
                        residuo = com.ecoadminmovile.core.model.ResiduoResumenDto(codigoLER = "17 01 01")
                    ),
                    TrasladoDto(
                        id = 2,
                        codigo = "ECO-002",
                        estado = "PENDIENTE",
                        fechaCreacion = "2025-05-02",
                        residuo = com.ecoadminmovile.core.model.ResiduoResumenDto(codigoLER = "20 01 39")
                    )
                )
            ),
            onRefresh = {}
        )
    }
}
