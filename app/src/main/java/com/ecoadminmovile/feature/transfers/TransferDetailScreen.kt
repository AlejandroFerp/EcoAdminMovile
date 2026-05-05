/**
 * Pantalla de detalle de traslado con pestañas (tabs) y HorizontalPager.
 *
 * Conceptos Kotlin demostrados:
 * - private enum class con propiedades (title, icon): enum con constructor.
 * - .entries: propiedad de Kotlin 1.9+ que reemplaza a values() (más segura y eficiente).
 * - HorizontalPager + TabRow: combinación para navegación por pestañas deslizables.
 * - when sobre enum: matching exhaustivo (el compilador obliga a cubrir todos los casos).
 * - LocalContext.current: acceso al contexto Android desde un Composable.
 * - Intent(Intent.ACTION_VIEW, uri): navegación a app externa (Google Maps).
 * - remember { mutableStateOf(...) }: estado local de Compose que sobrevive recomposiciones.
 * - var ... by: delegación de propiedad con mutableStateOf.
 *
 * Patrones de diseño:
 * - Tab + Pager: navegación por pestañas con contenido swipeable.
 * - State hoisting: el estado se eleva al padre, los hijos son stateless.
 */
package com.ecoadminmovile.feature.transfers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecoadminmovile.core.model.HistorialEventoDto
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.ui.components.EcoCard
import com.ecoadminmovile.ui.components.EcoStatusPill
import com.ecoadminmovile.ui.theme.EcoTextMuted
import com.ecoadminmovile.ui.theme.EcoTextStrong
import com.ecoadminmovile.ui.theme.EcoTextSubtle
import kotlinx.coroutines.launch

// private enum class con propiedades: cada entrada tiene title e icon como constructor.
// A diferencia de Java, en Kotlin los enum pueden tener propiedades declaradas en el constructor.
private enum class DetailTab(val title: String, val icon: ImageVector) {
    DATOS("Datos", Icons.Rounded.Info),
    DOCUMENTOS("Docs", Icons.Rounded.Description),
    HISTORIAL("Historial", Icons.Rounded.History),
    MAPA("Mapa", Icons.Rounded.Map)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDetailScreen(
    state: TransferDetailUiState,
    onBack: () -> Unit,
    onShowStatusSheet: () -> Unit = {},
    onDismissStatusSheet: () -> Unit = {},
    onChangeStatus: (String, String?) -> Unit = { _, _ -> },
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onViewPdf: (String) -> Unit = {}
) {
    // .entries: propiedad Kotlin 1.9+ que devuelve lista inmutable de valores del enum.
    // Reemplaza a values() que creaba un nuevo array cada vez.
    val tabs = DetailTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // Status change bottom sheet
    if (state.showStatusSheet) {
        StatusChangeBottomSheet(
            currentStatus = state.transfer?.estado.orEmpty(),
            onDismiss = onDismissStatusSheet,
            onConfirm = onChangeStatus
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.transfer?.codigo ?: "Detalle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    val hasTransitions = state.transfer?.let {
                        TransferDetailViewModel.nextStates(it.estado).isNotEmpty()
                    } ?: false
                    if (hasTransitions) {
                        TextButton(onClick = onShowStatusSheet) {
                            Text("Cambiar estado")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(tab.title, style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            // when sobre enum: EXHAUSTIVO — el compilador obliga a cubrir todos los casos.
            // Si se añade un nuevo tab al enum, este when dará error de compilación hasta cubrirlo.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs[page]) {
                    DetailTab.DATOS -> DatosTab(
                        transfer = state.transfer,
                        isLoading = state.isLoading,
                        error = state.errorMessage,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                    DetailTab.DOCUMENTOS -> DocumentosTab(
                        transferId = state.transfer?.id,
                        onViewPdf = onViewPdf
                    )
                    DetailTab.HISTORIAL -> HistorialTab(
                        historial = state.historial,
                        isLoadingHistorial = state.isLoadingHistorial
                    )
                    DetailTab.MAPA -> MapaTab(transfer = state.transfer)
                }
            }
        }
    }
}

@Composable
private fun DatosTab(
    transfer: TrasladoDto?,
    isLoading: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (error != null) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        return
    }

    transfer ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EcoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = transfer.codigo.ifBlank { "#${transfer.id}" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        EcoStatusPill(status = transfer.estado)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    DetailField("Residuo", buildString {
                        append(transfer.residuo?.codigoLER.orEmpty())
                        transfer.residuo?.descripcion?.let { append(" — $it") }
                    })
                    DetailField("Centro Productor", transfer.centroProductor?.nombre.orEmpty())
                    DetailField("Centro Gestor", transfer.centroGestor?.nombre.orEmpty())
                    DetailField("Transportista", transfer.transportista?.nombre ?: "Sin asignar")
                    DetailField("Ruta", transfer.ruta?.nombre ?: "Sin ruta")

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    DetailField("Fecha creación", transfer.fechaCreacion?.take(16).orEmpty())
                    DetailField("Inicio transporte", transfer.fechaInicioTransporte?.take(16).orEmpty())
                    DetailField("Entrega", transfer.fechaEntrega?.take(16).orEmpty())

                    if (!transfer.observaciones.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        DetailField("Observaciones", transfer.observaciones)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("Editar")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            }
        }
    }
}

@Composable
private fun DocumentosTab(
    transferId: Long?,
    onViewPdf: (String) -> Unit
) {
    if (transferId == null) return

    data class DocItem(val tipo: String, val title: String, val subtitle: String, val color: Color)

    val docs = listOf(
        DocItem("carta-porte", "Carta de Porte", "Documento de transporte oficial", Color(0xFF3B82F6)),
        DocItem("notificacion", "Notificación Previa", "Aviso previo al traslado", Color(0xFFF59E0B)),
        DocItem("certificado", "Certificado de Recepción", "Confirmación de entrega", Color(0xFF10B981))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        docs.forEach { doc ->
            EcoCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onViewPdf(doc.tipo) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = doc.color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Description,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp).size(24.dp),
                            tint = doc.color
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EcoTextStrong
                        )
                        Text(
                            text = doc.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = EcoTextSubtle
                        )
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = EcoTextSubtle)
                }
            }
        }
    }
}

@Composable
private fun HistorialTab(
    historial: List<HistorialEventoDto>,
    isLoadingHistorial: Boolean
) {
    if (isLoadingHistorial) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (historial.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Sin eventos registrados", color = EcoTextSubtle)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(historial) { evento ->
            TimelineEvent(evento)
        }
    }
}

@Composable
private fun TimelineEvent(evento: HistorialEventoDto) {
    val color = statusColor(evento.estadoNuevo)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline dot + line
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(50),
                color = color,
                modifier = Modifier.size(12.dp)
            ) {}
            Spacer(
                Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .padding(top = 4.dp)
            )
        }

        // Event content
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = buildString {
                        evento.estadoAnterior?.let { append("$it → ") }
                        append(evento.estadoNuevo)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = EcoTextStrong
                )
                Text(
                    text = evento.fecha?.take(16).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = EcoTextSubtle
                )
            }
            evento.usuario?.let { user ->
                Text(
                    text = user.nombre,
                    style = MaterialTheme.typography.bodySmall,
                    color = EcoTextMuted
                )
            }
            evento.comentario?.let { comment ->
                if (comment.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = EcoTextMuted,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapaTab(transfer: TrasladoDto?) {
    // LocalContext.current: accede al Context de Android desde un Composable
    val context = LocalContext.current
    val ruta = transfer?.ruta

    if (ruta == null || ruta.origenLatitud == null || ruta.destinoLatitud == null) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sin ruta con coordenadas asignada", color = EcoTextSubtle)
                Text("Asigna una ruta para ver el mapa", style = MaterialTheme.typography.bodySmall, color = EcoTextMuted)
            }
        }
        return
    }

    // Open in external maps app
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EcoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Ruta: ${ruta.nombre.orEmpty()}", fontWeight = FontWeight.Bold, color = EcoTextStrong)
                ruta.distanciaKm?.let {
                    Text("Distancia: $it km", color = EcoTextMuted)
                }
                DetailField("Origen", "${ruta.origenLatitud}, ${ruta.origenLongitud}")
                DetailField("Destino", "${ruta.destinoLatitud}, ${ruta.destinoLongitud}")
            }
        }

        Button(
            onClick = {
                // Intent(ACTION_VIEW, uri): abre una app externa capaz de manejar la URI.
                // Uri.parse construye la URI para Google Maps con origen y destino.
                val uri = Uri.parse(
                    "https://www.google.com/maps/dir/${ruta.origenLatitud},${ruta.origenLongitud}/${ruta.destinoLatitud},${ruta.destinoLongitud}"
                )
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Abrir ruta en Google Maps")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChangeBottomSheet(
    currentStatus: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    val availableStates = TransferDetailViewModel.nextStates(currentStatus)
    var comentario by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Cambiar Estado",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Estado actual: ${currentStatus.replace("_", " ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = EcoTextSubtle
            )

            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Comentario (opcional)") },
                placeholder = { Text("Motivo del cambio...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(8.dp))

            availableStates.forEach { nextStatus ->
                Button(
                    onClick = { onConfirm(nextStatus, comentario.ifBlank { null }) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pasar a ${nextStatus.replace("_", " ")}")
                }
            }

            if (availableStates.isEmpty()) {
                Text(
                    text = "No hay transiciones disponibles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoTextMuted
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = EcoTextSubtle
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = EcoTextStrong
        )
    }
}

private fun statusColor(estado: String): Color = when (estado.uppercase()) {
    "PENDIENTE" -> Color(0xFFF59E0B)
    "EN_TRANSITO" -> Color(0xFF3B82F6)
    "ENTREGADO" -> Color(0xFF8B5CF6)
    "COMPLETADO" -> Color(0xFF10B981)
    else -> Color(0xFF64748B)
}
