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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.Job
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
    // MutableStateFlow: estado mutable INTERNO (solo el ViewModel lo modifica).
    // StateFlow: version de solo lectura EXPUESTA a la UI (principio de encapsulacion).
    // .asStateFlow(): convierte MutableStateFlow en StateFlow inmutable para el exterior.
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Job cancelable: almacena la referencia a la corrutina de carga actual.
    // Si el usuario cambia de periodo rapidamente, cancelamos la anterior para evitar
    // que dos corrutinas compitan por actualizar el estado (race condition).
    private var loadJob: Job? = null

    // init {}: bloque de inicializacion. Se ejecuta UNA vez al crear el ViewModel.
    // El ViewModel sobrevive a rotaciones de pantalla (a diferencia de la Activity).
    init {
        load()
    }

    fun load() {
        // .cancel(): cancela la corrutina anterior si aun esta ejecutandose.
        // Esto es seguro: si ya termino, cancel() no hace nada.
        loadJob?.cancel()

        // viewModelScope.launch: lanza una corrutina atada al ciclo de vida del ViewModel.
        // Cuando el ViewModel se destruye, todas sus corrutinas se cancelan automaticamente.
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // ?.let {}: si daysBack NO es null, ejecuta el bloque y retorna el resultado.
            // Si ES null (caso ALL), let no se ejecuta y `desde` queda como null.
            // El repositorio interpreta desde=null como "sin filtro de fecha".
            val desde = _uiState.value.selectedPeriod.daysBack?.let { days ->
                LocalDate.now().minusDays(days.toLong())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
            }

            // .fold(onSuccess, onFailure): manejo COMPLETO del Result (railway pattern).
            // A diferencia de .onSuccess{} que ignora errores, fold OBLIGA a tratar ambos caminos.
            repository.loadDashboard(desde).fold(
                onSuccess = { stats ->
                    _uiState.update { it.copy(data = stats) }
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(errorMessage = throwable.message) }
                }
            )

            // Carga de transfers con manejo EXPLICITO de error.
            // Antes se usaba .onSuccess{} que tragaba el error silenciosamente.
            // Ahora usamos .fold() para que si falla, el usuario lo sepa.
            transfersRepository.loadTransfers().fold(
                onSuccess = { transfers ->
                    // .take(10): toma solo los primeros 10 elementos de la lista.
                    _uiState.update { it.copy(recentTransfers = transfers.take(10)) }
                },
                onFailure = { throwable ->
                    // Si ya hay un errorMessage del dashboard, concatenamos.
                    _uiState.update { current ->
                        val msg = "Traslados: " + (throwable.message ?: "Error desconocido")
                        val combined = if (current.errorMessage != null) {
                            current.errorMessage + "\n" + msg
                        } else {
                            msg
                        }
                        current.copy(errorMessage = combined)
                    }
                }
            )

            // isLoading = false AL FINAL de ambas cargas.
            // Antes se ponia false tras loadDashboard, dejando un instante
            // donde la UI parecia "lista" pero faltaban los transfers.
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setPeriod(period: DashboardPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        load() // load() cancela la anterior internamente (Job cancelable)
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
    onTransferSelected: (Long) -> Unit = {},
    onNavigateToResiduos: () -> Unit = {},
    onNavigateToDocumentos: () -> Unit = {},
    onNavigateToRutas: () -> Unit = {}
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

        // Metrics in 2 columns
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EcoMetricCard(
                        title = "Centros",
                        value = state.data.totalCentros.toString(),
                        icon = Icons.Rounded.Business,
                        iconBgColor = EcoMetricCentrosBg,
                        iconColor = EcoMetricCentrosIcon,
                        modifier = Modifier.weight(1f)
                    )
                    EcoMetricCard(
                        title = "Residuos",
                        value = state.data.totalResiduos.toString(),
                        icon = Icons.Rounded.Autorenew,
                        iconBgColor = EcoMetricResiduosBg,
                        iconColor = EcoMetricResiduosIcon,
                        badgeColor = EcoMetricResiduosBadge,
                        badgeBgColor = EcoMetricResiduosBadgeBg,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EcoMetricCard(
                        title = "Pendientes",
                        value = state.data.trasladosPendientes.toString(),
                        icon = Icons.Rounded.Schedule,
                        iconBgColor = EcoMetricPendingBg,
                        iconColor = EcoMetricPendingIcon,
                        modifier = Modifier.weight(1f)
                    )
                    EcoMetricCard(
                        title = "En curso",
                        value = state.data.trasladosEnTransito.toString(),
                        icon = Icons.Rounded.SwapHoriz,
                        iconBgColor = EcoMetricTransitBg,
                        iconColor = EcoMetricTransitIcon,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EcoMetricCard(
                        title = "Entregados",
                        value = state.data.trasladosEntregados.toString(),
                        icon = Icons.Rounded.LocalShipping,
                        iconBgColor = EcoMetricDeliveredBg,
                        iconColor = EcoMetricDeliveredIcon,
                        modifier = Modifier.weight(1f)
                    )
                    EcoMetricCard(
                        title = "Completados",
                        value = state.data.trasladosCompletados.toString(),
                        icon = Icons.Rounded.CheckCircle,
                        iconBgColor = EcoMetricCompletedBg,
                        iconColor = EcoMetricCompletedIcon,
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
                            Text("Código", style = MaterialTheme.typography.labelSmall, color = EcoTextSubtle, modifier = Modifier.weight(0.8f))
                            Text("Residuo", style = MaterialTheme.typography.labelSmall, color = EcoTextSubtle, modifier = Modifier.weight(1.1f))
                            Text("Estado", style = MaterialTheme.typography.labelSmall, color = EcoTextSubtle, modifier = Modifier.weight(1.5f))
                            Text("Fecha", style = MaterialTheme.typography.labelSmall, color = EcoTextSubtle, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
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
                                    text = transfer.codigo.orEmpty().ifBlank { "#${transfer.id}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = EcoPrimary,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text(
                                    text = transfer.residuo?.codigoLER.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EcoTextMuted,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1.1f)
                                )
                                EcoStatusPill(
                                    status = transfer.estado.orEmpty(),
                                    modifier = Modifier.weight(1.5f)
                                )
                                Text(
                                    text = transfer.fechaCreacion?.take(10).orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EcoTextSubtle,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.1f)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }

        // Quick-access section for Residuos, Documentos, Rutas
        item {
            Text(
                text = "Gestión",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EcoTextStrong,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EcoCard(modifier = Modifier.weight(1f), onClick = onNavigateToResiduos) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Science, contentDescription = null, tint = EcoMetricResiduosIcon)
                        Text("Residuos", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                EcoCard(modifier = Modifier.weight(1f), onClick = onNavigateToDocumentos) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = null, tint = EcoMetricPendingIcon)
                        Text("Documentos", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                EcoCard(modifier = Modifier.weight(1f), onClick = onNavigateToRutas) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Route, contentDescription = null, tint = EcoMetricTransitIcon)
                        Text("Rutas", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
// device = "spec:width=411dp,height=1200dp": aumentamos la altura virtual de la preview para ver todo el contenido.
@Preview(showBackground = true, device = "spec:width=411dp,height=1400dp")
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
