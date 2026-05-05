/**
 * Pantalla de listado de traslados con búsqueda, filtros y pull-to-refresh.
 *
 * Conceptos Kotlin demostrados:
 * - internal val: visibilidad a nivel de módulo (solo accesible dentro del mismo módulo Gradle).
 * - Scaffold: estructura de layout con topBar, floatingActionButton y content.
 * - items(list, key = { ... }): key permite recomposición eficiente (identifica cada elemento).
 * - Modifier.weight(1f, fill = false): ocupa espacio proporcional sin llenar todo.
 * - .ifBlank { }: extensión que ejecuta bloque si el String está vacío.
 * - listOfNotNull(...): crea lista filtrando automáticamente los valores null.
 * - .joinToString(): une elementos de una colección en un String.
 * - .orEmpty(): convierte String? en String (devuelve "" si es null).
 *
 * Patrones de diseño:
 * - Container-Presentational: la pantalla solo muestra datos, la lógica está en el ViewModel.
 * - Composición de componentes: TransferCard como componente reutilizable.
 */
package com.ecoadminmovile.feature.transfers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoadminmovile.core.model.CentroResumenDto
import com.ecoadminmovile.core.model.ResiduoResumenDto
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.core.model.UsuarioResumenDto
import com.ecoadminmovile.ui.theme.EcoAdminTheme
import com.ecoadminmovile.ui.components.EcoCard
import com.ecoadminmovile.ui.components.EcoStatusPill
import com.ecoadminmovile.ui.theme.EcoTextMuted
import com.ecoadminmovile.ui.theme.EcoTextStrong
import com.ecoadminmovile.ui.theme.EcoTextSubtle

// internal val: visible solo dentro de este módulo Gradle (no desde otros módulos).
// Constante a nivel de archivo (top-level declaration) — no necesita clase contenedora.
internal val TRANSFER_STATES = listOf("PENDIENTE", "EN_TRANSITO", "ENTREGADO", "COMPLETADO")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersListScreen(
    state: TransfersUiState,
    onRefresh: () -> Unit,
    onTransferSelected: (Long) -> Unit,
    onSearchChanged: (String) -> Unit,
    onStatusFilter: (String?) -> Unit,
    onCreateNew: () -> Unit,
    onScanQr: () -> Unit,
    onStatusChange: ((Long, String, String?) -> Unit)? = null
) {
    // State for inline status change dialog
    var statusChangeTarget by remember { mutableStateOf<TrasladoDto?>(null) }

    // Status change bottom sheet (reusable from card)
    statusChangeTarget?.let { transfer ->
        InlineStatusChangeSheet(
            currentStatus = transfer.estado,
            onDismiss = { statusChangeTarget = null },
            onConfirm = { newStatus, comment ->
                onStatusChange?.invoke(transfer.id, newStatus, comment)
                statusChangeTarget = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNew) {
                Icon(Icons.Rounded.Add, contentDescription = "Nuevo traslado")
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
                            text = "Recogidas",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = EcoTextStrong
                        )
                        Text(
                            text = "Seguimiento y gestión de recogidas de residuos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EcoTextSubtle
                        )
                    }
                }

                // Prominent QR Scanner access
                item {
                    Surface(
                        onClick = onScanQr,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF3B82F6).copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Escanear QR de traslado",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B82F6)
                                )
                                Text(
                                    text = "Completar recogida escaneando el código",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EcoTextMuted
                                )
                            }
                            Text("›", style = MaterialTheme.typography.titleLarge, color = Color(0xFF3B82F6))
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar por código, productor, gestor, residuo...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
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
                                label = {
                                    Text(
                                        status.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
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

                // items con key: key identifica cada elemento para recomposición eficiente.
                // Sin key, Compose recompone TODO al cambiar la lista.
                items(state.filteredTransfers, key = { it.id }) { transfer ->
                    TransferCard(
                        transfer = transfer,
                        onClick = { onTransferSelected(transfer.id) },
                        onOpenStatusChange = { statusChangeTarget = transfer }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferCard(
    transfer: TrasladoDto,
    onClick: () -> Unit,
    onOpenStatusChange: (() -> Unit)? = null
) {
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
                    text = transfer.codigo.ifBlank { "#${transfer.id}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = EcoTextSubtle,
                    fontWeight = FontWeight.Bold
                )
                EcoStatusPill(status = transfer.estado)
            }

            val residuoText = listOfNotNull(
                transfer.residuo?.codigoLER,
                transfer.residuo?.descripcion
            ).joinToString(" — ").ifBlank { "Sin residuo" }
            Text(
                text = residuoText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = EcoTextStrong,
                maxLines = 1
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transfer.centroProductor?.nombre.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoTextMuted,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Text("→", color = EcoTextSubtle)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = transfer.centroGestor?.nombre.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoTextMuted,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Footer: transportista, fecha, cambio de estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transfer.transportista?.nombre ?: "Sin transportista",
                        style = MaterialTheme.typography.bodySmall,
                        color = EcoTextSubtle
                    )
                    Text(
                        text = transfer.fechaCreacion?.take(10).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = EcoTextSubtle
                    )
                }

                // Inline status change chip (like web)
                if (onOpenStatusChange != null) {
                    Surface(
                        onClick = onOpenStatusChange,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF3B82F6).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Cambiar estado",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineStatusChangeSheet(
    currentStatus: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    val availableStates = TransferDetailViewModel.nextStates(currentStatus)
    var selectedState by remember { mutableStateOf<String?>(null) }
    var comentario by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Cambiar Estado",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Actual: ${currentStatus.replace("_", " ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = EcoTextSubtle
            )

            availableStates.forEach { state ->
                val isSelected = selectedState == state
                Surface(
                    onClick = { selectedState = state },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = isSelected, onClick = { selectedState = state })
                        Text(
                            text = state.replace("_", " "),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = EcoTextStrong
                        )
                    }
                }
            }

            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Comentario") },
                placeholder = { Text("Motivo del cambio...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { selectedState?.let { onConfirm(it, comentario.ifBlank { null }) } },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = selectedState != null
            ) {
                Text(
                    text = if (selectedState != null) "Confirmar: ${selectedState!!.replace("_", " ")}"
                    else "Selecciona un estado",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// --- Previews ---

private val sampleTransfers = listOf(
    TrasladoDto(
        id = 1, codigo = "ECO-2025-001", estado = "PENDIENTE",
        centroProductor = CentroResumenDto(1, "CP-01", "Fábrica Norte"),
        centroGestor = CentroResumenDto(2, "CG-01", "Gestor Ambiental S.L."),
        residuo = ResiduoResumenDto(1, "R-01", codigoLER = "17 01 01", descripcion = "Hormigón"),
        transportista = UsuarioResumenDto(1, "TransEco S.A."),
        fechaCreacion = "2025-05-01T10:00:00"
    ),
    TrasladoDto(
        id = 2, codigo = "ECO-2025-002", estado = "EN_TRANSITO",
        centroProductor = CentroResumenDto(3, "CP-02", "Planta Sur"),
        centroGestor = CentroResumenDto(4, "CG-02", "ReciclaMás"),
        residuo = ResiduoResumenDto(2, "R-02", codigoLER = "20 01 39", descripcion = "Plásticos"),
        transportista = UsuarioResumenDto(2, "Logística Verde"),
        fechaCreacion = "2025-05-02T14:30:00"
    ),
    TrasladoDto(
        id = 3, codigo = "ECO-2025-003", estado = "COMPLETADO",
        centroProductor = CentroResumenDto(5, "CP-03", "Oficina Central"),
        centroGestor = CentroResumenDto(6, "CG-03", "EcoGest"),
        residuo = ResiduoResumenDto(3, "R-03", codigoLER = "15 01 06", descripcion = "Envases mixtos"),
        fechaCreacion = "2025-04-28T09:15:00"
    )
)

@Preview(showBackground = true, name = "Lista de Traslados")
@Composable
fun TransfersListScreenPreview() {
    EcoAdminTheme {
        TransfersListScreen(
            state = TransfersUiState(
                isLoading = false,
                transfers = sampleTransfers,
                filteredTransfers = sampleTransfers
            ),
            onRefresh = {},
            onTransferSelected = {},
            onSearchChanged = {},
            onStatusFilter = {},
            onCreateNew = {},
            onScanQr = {},
            onStatusChange = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Lista vacía")
@Composable
fun TransfersListEmptyPreview() {
    EcoAdminTheme {
        TransfersListScreen(
            state = TransfersUiState(isLoading = false),
            onRefresh = {},
            onTransferSelected = {},
            onSearchChanged = {},
            onStatusFilter = {},
            onCreateNew = {},
            onScanQr = {}
        )
    }
}

@Preview(showBackground = true, name = "Lista cargando")
@Composable
fun TransfersListLoadingPreview() {
    EcoAdminTheme {
        TransfersListScreen(
            state = TransfersUiState(isLoading = true),
            onRefresh = {},
            onTransferSelected = {},
            onSearchChanged = {},
            onStatusFilter = {},
            onCreateNew = {},
            onScanQr = {}
        )
    }
}
