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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecoadminmovile.core.model.TrasladoDto
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
    onScanQr: () -> Unit
) {
    // Scaffold: estructura estándar de Material 3 con zonas predefinidas
    // (topBar, bottomBar, floatingActionButton, content)
    Scaffold(
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                SmallFloatingActionButton(onClick = onScanQr) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Escanear QR")
                }
                FloatingActionButton(onClick = onCreateNew) {
                    Icon(Icons.Rounded.Add, contentDescription = "Nuevo traslado")
                }
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
                        onClick = { onTransferSelected(transfer.id) }
                    )
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
                // .ifBlank { }: si el código está vacío, usa el id como fallback
                Text(
                    text = transfer.codigo.ifBlank { "#${transfer.id}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = EcoTextSubtle,
                    fontWeight = FontWeight.Bold
                )
                EcoStatusPill(status = transfer.estado)
            }

            // Residuo
            // listOfNotNull: crea lista eliminando valores null automáticamente
            val residuoText = listOfNotNull(
                transfer.residuo?.codigoLER,
                transfer.residuo?.descripcion
            ).joinToString(" — ").ifBlank { "Sin residuo" } // .joinToString(): une con separador
            Text(
                text = residuoText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = EcoTextStrong,
                maxLines = 1
            )

            // Productor → Gestor
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    // .orEmpty(): convierte String? → String (devuelve "" si null)
                    text = transfer.centroProductor?.nombre.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoTextMuted,
                    maxLines = 1,
                    // weight(1f, fill = false): ocupa espacio proporcional pero NO se estira
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

            // Footer: transportista + fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
        }
    }
}
