/**
 * Formulario de creación/edición de traslados con dropdowns genéricos.
 *
 * Conceptos Kotlin demostrados:
 * - sealed interface en `when`: matching exhaustivo (el compilador obliga a cubrir todos los casos).
 * - Función genérica <T> en Composable: DropdownSelector<T> reutilizable con cualquier tipo.
 * - `where T : Any`: restricción genérica (T debe ser no-nullable).
 * - `var expanded by remember { mutableStateOf(false) }`: delegated property con by.
 * - @Suppress("UNCHECKED_CAST"): suprime warning de cast no seguro.
 * - buildString { append(...) }: constructor de String eficiente (evita concatenaciones).
 * - ExposedDropdownMenuBox: patrón Material 3 para dropdowns.
 *
 * Patrones de diseño:
 * - Generic/Template: DropdownSelector<T> funciona con cualquier tipo de dato.
 * - Sealed types para modelar los campos del formulario de forma type-safe.
 */
package com.ecoadminmovile.feature.transfers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.ResiduoDto
import com.ecoadminmovile.core.model.RutaDto
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.core.model.UsuarioResumenDto
import com.ecoadminmovile.ui.theme.EcoTextSubtle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFormScreen(
    state: TransferFormUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onFieldChanged: (TransferFormField) -> Unit
) {
    val isEditing = state.editingTransferId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Traslado" else "Nuevo Traslado") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = state.isFormValid && !state.isSaving
                    ) {
                        Text("Guardar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.errorMessage?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            // Centro Productor
            DropdownSelector(
                label = "Centro Productor *",
                items = state.centros,
                selectedId = state.selectedProductorId,
                displayText = { it.nombre },
                onSelected = { onFieldChanged(TransferFormField.Productor(it?.id)) }
            )

            // Centro Gestor
            DropdownSelector(
                label = "Centro Gestor *",
                items = state.centros,
                selectedId = state.selectedGestorId,
                displayText = { it.nombre },
                onSelected = { onFieldChanged(TransferFormField.Gestor(it?.id)) }
            )

            // Residuo
            DropdownSelector(
                label = "Residuo *",
                items = state.residuos,
                selectedId = state.selectedResiduoId,
                displayText = { "${it.codigoLER.orEmpty()} — ${it.descripcion.orEmpty()}" },
                onSelected = { onFieldChanged(TransferFormField.Residuo(it?.id)) }
            )

            // Transportista
            DropdownSelector(
                label = "Transportista",
                items = state.transportistas,
                selectedId = state.selectedTransportistaId,
                displayText = { it.nombre },
                onSelected = { onFieldChanged(TransferFormField.Transportista(it?.id)) },
                optional = true
            )

            // Ruta
            DropdownSelector(
                label = "Ruta asignada",
                items = state.rutas,
                selectedId = state.selectedRutaId,
                // buildString { append(...) }: construye String de forma eficiente
                // Mejor que concatenar con + (crea menos objetos intermedios)
                displayText = { buildString {
                    append(it.nombre)
                    it.distanciaKm?.let { km -> append(" · $km km") }
                }},
                onSelected = { onFieldChanged(TransferFormField.Ruta(it?.id)) },
                optional = true
            )

            // Observaciones
            OutlinedTextField(
                value = state.observaciones,
                onValueChange = { onFieldChanged(TransferFormField.Observaciones(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Observaciones") },
                placeholder = { Text("Notas adicionales...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// Función genérica <T>: funciona con CUALQUIER tipo (CentroDto, ResiduoDto, etc.)
// `where T : Any` restringe T a tipos no-nullable (T no puede ser null).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSelector(
    label: String,
    items: List<T>,
    selectedId: Long?,
    displayText: (T) -> String,
    onSelected: (T?) -> Unit,
    optional: Boolean = false
) where T : Any {
    // var ... by remember { mutableStateOf(false) }: delegated property.
    // `by` delega get/set al objeto MutableState. `remember` persiste entre recomposiciones.
    var expanded by remember { mutableStateOf(false) }
    val selectedItem = items.firstOrNull { itemId(it) == selectedId }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = EcoTextSubtle
        )
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedItem?.let { displayText(it) } ?: if (optional) "Sin asignar" else "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (optional) {
                    DropdownMenuItem(
                        text = { Text("Sin asignar") },
                        onClick = {
                            onSelected(null)
                            expanded = false
                        }
                    )
                }
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(displayText(item)) },
                        onClick = {
                            onSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// @Suppress("UNCHECKED_CAST"): suprime el warning del compilador por cast no seguro.
// Necesario porque no hay forma de verificar el tipo genérico en runtime (type erasure).
@Suppress("UNCHECKED_CAST")
private fun <T : Any> itemId(item: T): Long? = when (item) {
    is CentroDto -> item.id
    is ResiduoDto -> item.id
    is UsuarioResumenDto -> item.id
    is RutaDto -> item.id
    else -> null
}

// sealed interface: jerarquía CERRADA de tipos. El compilador conoce TODOS los subtipos.
// Esto permite `when` exhaustivo: si se añade un nuevo subtipo, el compilador obliga a manejarlo.
sealed interface TransferFormField {
    data class Productor(val id: Long?) : TransferFormField
    data class Gestor(val id: Long?) : TransferFormField
    data class Residuo(val id: Long?) : TransferFormField
    data class Transportista(val id: Long?) : TransferFormField
    data class Ruta(val id: Long?) : TransferFormField
    data class Observaciones(val text: String) : TransferFormField
}
