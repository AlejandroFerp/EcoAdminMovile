package com.ecoadminmovile.feature.centers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.data.CentersRepository
import com.ecoadminmovile.ui.components.EcoCard
import com.ecoadminmovile.ui.theme.EcoTextMuted
import com.ecoadminmovile.ui.theme.EcoTextStrong
import com.ecoadminmovile.ui.theme.EcoTextSubtle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CentersUiState(
    val isLoading: Boolean = true,
    val centers: List<CentroDto> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class CentersViewModel @Inject constructor(
    private val repository: CentersRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CentersUiState())
    val uiState: StateFlow<CentersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadCenters().fold(
                onSuccess = { centers ->
                    _uiState.value = CentersUiState(isLoading = false, centers = centers)
                },
                onFailure = { throwable ->
                    _uiState.value = CentersUiState(
                        isLoading = false,
                        errorMessage = throwable.message
                    )
                }
            )
        }
    }
}

@Composable
fun CentersScreen(
    state: CentersUiState,
    onRefresh: () -> Unit
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
                    text = "Centros",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = EcoTextStrong
                )
                Text(
                    text = "Gestión de productores y gestores",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoTextSubtle
                )
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

        items(state.centers, key = { center -> center.id }) { center ->
            CenterCard(center = center)
        }

        item {
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = "Actualizar centros")
            }
        }
    }
}

@Composable
private fun CenterCard(center: CentroDto) {
    EcoCard(modifier = Modifier.fillMaxWidth()) {
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
                    text = center.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EcoTextStrong
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = center.tipo.orEmpty(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Text(
                text = "NIMA: ${center.nima ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = EcoTextSubtle
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            ContactInfoRow(Icons.Rounded.LocationOn, listOfNotNull(center.direccion?.calle, center.direccion?.ciudad).joinToString(", "))
            ContactInfoRow(Icons.Rounded.Person, center.nombreContacto.orEmpty())
            ContactInfoRow(Icons.Rounded.Phone, center.telefono.orEmpty())
            ContactInfoRow(Icons.Rounded.Email, center.email.orEmpty())
        }
    }
}

@Composable
private fun ContactInfoRow(icon: ImageVector, text: String) {
    if (text.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = EcoTextSubtle
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = EcoTextMuted
        )
    }
}
