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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.ViewList
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
import com.ecoadminmovile.core.model.HistorialEventoDto
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
    onStatusChange: ((Long, String, String?) -> Unit)? = null,
    onDeleteTransfer: ((Long) -> Unit)? = null,
    onLoadHistorial: ((Long, (List<HistorialEventoDto>) -> Unit) -> Unit)? = null
) {
    // State for inline status change dialog
    var statusChangeTarget by remember { mutableStateOf<TrasladoDto?>(null) }
    // State for delete confirmation
    var deleteTarget by remember { mutableStateOf<TrasladoDto?>(null) }
    // State for quick historial bottom sheet
    var historialTarget by remember { mutableStateOf<TrasladoDto?>(null) }
    var historialItems by remember { mutableStateOf<List<HistorialEventoDto>>(emptyList()) }
    var historialLoading by remember { mutableStateOf(false) }
    
    // View state
    var isKanbanView by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    deleteTarget?.let { transfer ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar traslado") },
            text = { Text("¿Eliminar \"${transfer.codigo.orEmpty().ifBlank { "#${transfer.id}" }}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransfer?.invoke(transfer.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    // Quick historial bottom sheet
    historialTarget?.let { transfer ->
        ModalBottomSheet(onDismissRequest = {
            historialTarget = null
            historialItems = emptyList()
        }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Historial — ${transfer.codigo.orEmpty().ifBlank { "#${transfer.id}" }}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (historialLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (historialItems.isEmpty()) {
                    Text(
                        text = "Sin eventos registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EcoTextSubtle
                    )
                } else {
                    historialItems.forEach { evento ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${evento.estadoAnterior.orEmpty().replace("_", " ")} → ${evento.estadoNuevo.replace("_", " ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = EcoTextStrong
                                )
                                if (!evento.comentario.isNullOrBlank()) {
                                    Text(
                                        text = evento.comentario,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EcoTextMuted
                                    )
                                }
                            }
                            Text(
                                text = evento.fecha?.take(16).orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = EcoTextSubtle
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Status change bottom sheet (reusable from card)
    statusChangeTarget?.let { transfer ->
        InlineStatusChangeSheet(
            currentStatus = transfer.estado.orEmpty(),
            onDismiss = { statusChangeTarget = null },
            onConfirm = { newStatus, comment ->
                onStatusChange?.invoke(transfer.id, newStatus, comment)
                statusChangeTarget = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchExpanded) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                                .height(50.dp),
                            placeholder = { Text("Buscar...", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            text = "Recogidas",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) onSearchChanged("")
                    }) {
                        Icon(if (isSearchExpanded) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = "Buscar")
                    }
                    IconButton(onClick = onScanQr) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Escanear QR")
                    }
                    IconButton(onClick = { isKanbanView = !isKanbanView }) {
                        Icon(if (isKanbanView) Icons.Rounded.ViewList else Icons.Rounded.Dashboard, contentDescription = "Cambiar vista")
                    }
                }
            )
        },
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Filtros de estado en bolitas
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TRANSFER_STATES.forEach { status ->
                        val isSelected = state.selectedStatus == status
                        val color = when (status) {
                            "PENDIENTE" -> Color(0xFFF59E0B)
                            "EN_TRANSITO" -> Color(0xFF3B82F6)
                            "ENTREGADO" -> Color(0xFF8B5CF6)
                            "COMPLETADO" -> Color(0xFF10B981)
                            else -> Color.Gray
                        }
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (isSelected) color else color.copy(alpha = 0.2f),
                            onClick = { onStatusFilter(if (isSelected) null else status) },
                            modifier = Modifier.size(if (isSelected) 32.dp else 24.dp)
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = status,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (isKanbanView) {
                    TransfersKanbanView(
                        transfers = state.filteredTransfers,
                        onTransferSelected = onTransferSelected
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(state.filteredTransfers, key = { it.id }) { transfer ->
                            TransferCard(
                                transfer = transfer,
                                onClick = { onTransferSelected(transfer.id) },
                                onOpenStatusChange = { statusChangeTarget = transfer },
                                onDelete = { deleteTarget = transfer },
                                onViewHistorial = {
                                    historialTarget = transfer
                                    historialLoading = true
                                    onLoadHistorial?.invoke(transfer.id) { items ->
                                        historialItems = items
                                        historialLoading = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransferCard(
    transfer: TrasladoDto,
    onClick: () -> Unit,
    onOpenStatusChange: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onViewHistorial: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        EcoCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
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
                    text = transfer.codigo.orEmpty().ifBlank { "#${transfer.id}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = EcoTextSubtle,
                    fontWeight = FontWeight.Bold
                )
                EcoStatusPill(status = transfer.estado.orEmpty())
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

        // Long-press context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Cambiar estado") },
                leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = {
                    showMenu = false
                    onOpenStatusChange?.invoke()
                }
            )
            DropdownMenuItem(
                text = { Text("Ver historial") },
                leadingIcon = { Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = {
                    showMenu = false
                    onViewHistorial?.invoke()
                }
            )
            DropdownMenuItem(
                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDelete?.invoke()
                }
            )
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
