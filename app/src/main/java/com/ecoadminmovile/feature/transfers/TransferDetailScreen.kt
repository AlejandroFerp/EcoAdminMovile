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
import androidx.compose.ui.tooling.preview.Preview
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
                        TransferDetailViewModel.nextStates(it.estado.orEmpty()).isNotEmpty()
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
                            text = transfer.codigo.orEmpty().ifBlank { "#${transfer.id}" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        EcoStatusPill(status = transfer.estado.orEmpty())
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    DetailField("Residuo", buildString {
                        append(transfer.residuo?.codigoLER.orEmpty())
                        transfer.residuo?.descripcion?.let { append(" — $it") }
                    })
                    if (transfer.residuo?.cantidad != null) {
                        DetailField("Cantidad", buildString {
                            append(transfer.residuo.cantidad.toString())
                            transfer.residuo.unidad?.let { append(" $it") }
                        })
                    }
                    DetailField("Centro Productor", transfer.centroProductor?.nombre.orEmpty())
                    DetailField("Centro Gestor", transfer.centroGestor?.nombre.orEmpty())
                    DetailField("Transportista", buildString {
                        append(transfer.transportista?.nombre ?: "Sin asignar")
                        transfer.transportista?.email?.let { append(" ($it)") }
                    })
                    DetailField("Ruta", buildString {
                        append(transfer.ruta?.nombre ?: "Sin ruta")
                        transfer.ruta?.distanciaKm?.let { append(" · $it km") }
                    })

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    DetailField("Fecha creación", transfer.fechaCreacion?.take(16).orEmpty())
                    if (!transfer.fechaProgramadaInicio.isNullOrBlank()) {
                        DetailField("Programada inicio", transfer.fechaProgramadaInicio.take(16))
                    }
                    if (!transfer.fechaProgramadaFin.isNullOrBlank()) {
                        DetailField("Programada fin", transfer.fechaProgramadaFin.take(16))
                    }
                    DetailField("Inicio transporte", transfer.fechaInicioTransporte?.take(16).orEmpty())
                    DetailField("Entrega", transfer.fechaEntrega?.take(16).orEmpty())
                    if (!transfer.fechaUltimoCambioEstado.isNullOrBlank()) {
                        DetailField("Último cambio estado", transfer.fechaUltimoCambioEstado.take(16))
                    }

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
    val context = LocalContext.current
    val ruta = transfer?.ruta
    val productor = transfer?.centroProductor
    val gestor = transfer?.centroGestor

    if (ruta == null) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sin ruta asignada", color = EcoTextSubtle)
                Text("Asigna una ruta para ver el mapa", style = MaterialTheme.typography.bodySmall, color = EcoTextMuted)
            }
        }
        return
    }

    val hasCoordinates = ruta.origenLatitud != null && ruta.origenLongitud != null &&
                         ruta.destinoLatitud != null && ruta.destinoLongitud != null

    val hasCenters = productor != null && gestor != null

    if (!hasCoordinates && !hasCenters) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Informacion de ruta incompleta", color = EcoTextSubtle)
                Text("Se requiere origen y destino para calcular la ruta", style = MaterialTheme.typography.bodySmall, color = EcoTextMuted)
            }
        }
        return
    }

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
                if (hasCoordinates) {
                    DetailField("Origen (Coordenadas)", "${ruta.origenLatitud}, ${ruta.origenLongitud}")
                    DetailField("Destino (Coordenadas)", "${ruta.destinoLatitud}, ${ruta.destinoLongitud}")
                } else {
                    DetailField("Origen (Centro Productor)", productor?.nombre.orEmpty())
                    DetailField("Destino (Centro Gestor)", gestor?.nombre.orEmpty())
                }
            }
        }

        Button(
            onClick = {
                val uri = if (hasCoordinates) {
                    Uri.parse("https://www.google.com/maps/dir/${ruta.origenLatitud},${ruta.origenLongitud}/${ruta.destinoLatitud},${ruta.destinoLongitud}")
                } else {
                    // Fallback using center names encoded safely for the Google Maps query
                    Uri.parse("https://www.google.com/maps/dir/${Uri.encode(productor?.nombre)}/${Uri.encode(gestor?.nombre)}")
                }
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
    var selectedState by remember { mutableStateOf<String?>(null) }
    var comentario by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Cambiar Estado",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Current status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Actual:", style = MaterialTheme.typography.bodyMedium, color = EcoTextSubtle)
                EcoStatusPill(status = currentStatus)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // State selection — radio-style
            Text(
                text = "Nuevo estado",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EcoTextStrong
            )

            availableStates.forEach { state ->
                val isSelected = selectedState == state
                Surface(
                    onClick = { selectedState = state },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected)
                        statusColor(state).copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (isSelected)
                        ButtonDefaults.outlinedButtonBorder(enabled = true)
                    else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedState = state }
                        )
                        Column {
                            Text(
                                text = state.replace("_", " "),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = EcoTextStrong
                            )
                            Text(
                                text = statusDescription(state),
                                style = MaterialTheme.typography.bodySmall,
                                color = EcoTextMuted
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Comment field
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Comentario") },
                placeholder = { Text("Motivo del cambio de estado...") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            // Confirm button
            Button(
                onClick = {
                    selectedState?.let { onConfirm(it, comentario.ifBlank { null }) }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = selectedState != null
            ) {
                Text(
                    text = if (selectedState != null)
                        "Confirmar: ${selectedState!!.replace("_", " ")}"
                    else
                        "Selecciona un estado",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun statusDescription(estado: String): String = when (estado.uppercase()) {
    "PENDIENTE" -> "Traslado registrado, pendiente de inicio"
    "EN_TRANSITO" -> "En proceso de transporte"
    "ENTREGADO" -> "Material entregado en destino"
    "COMPLETADO" -> "Proceso finalizado y documentado"
    else -> ""
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

// --- Previews ---

@Preview(showBackground = true, name = "Detalle - Datos")
@Composable
private fun TransferDetailPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        TransferDetailScreen(
            state = TransferDetailUiState(
                isLoading = false,
                transfer = com.ecoadminmovile.core.model.TrasladoDto(
                    id = 1,
                    codigo = "ECO-2025-001",
                    estado = "EN_TRANSITO",
                    centroProductor = com.ecoadminmovile.core.model.CentroResumenDto(1, "CP-01", "Fábrica Norte"),
                    centroGestor = com.ecoadminmovile.core.model.CentroResumenDto(2, "CG-01", "Gestor Ambiental S.L."),
                    residuo = com.ecoadminmovile.core.model.ResiduoResumenDto(1, "R-01", cantidad = 1500.0, unidad = "kg", codigoLER = "17 01 01", descripcion = "Hormigón"),
                    transportista = com.ecoadminmovile.core.model.UsuarioResumenDto(1, "TransEco S.A.", email = "info@transeco.es"),
                    ruta = com.ecoadminmovile.core.model.RutaResumenDto(1, "Ruta Norte", distanciaKm = 45.2),
                    fechaCreacion = "2025-05-01T10:00:00",
                    fechaInicioTransporte = "2025-05-02T08:00:00",
                    observaciones = "Carga pesada, requiere grúa"
                ),
                historial = listOf(
                    HistorialEventoDto(estadoAnterior = "PENDIENTE", estadoNuevo = "EN_TRANSITO", fecha = "2025-05-02T08:00:00", comentario = "Inicio de transporte")
                )
            ),
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Detalle - Status Sheet")
@Composable
private fun TransferDetailStatusSheetPreview() {
    com.ecoadminmovile.ui.theme.EcoAdminTheme {
        TransferDetailScreen(
            state = TransferDetailUiState(
                isLoading = false,
                showStatusSheet = true,
                transfer = com.ecoadminmovile.core.model.TrasladoDto(
                    id = 1, codigo = "ECO-2025-001", estado = "PENDIENTE"
                )
            ),
            onBack = {}
        )
    }
}
