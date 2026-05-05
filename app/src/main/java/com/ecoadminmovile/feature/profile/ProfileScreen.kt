/**
 * Pantalla de perfil del usuario autenticado.
 *
 * ## Conceptos Kotlin demostrados:
 * - Función @Composable como pantalla completa (no necesita ViewModel para datos estáticos).
 * - `Modifier.weight(1f)`: Spacer flexible que empuja el botón al fondo.
 * - `.take(1).uppercase()`: encadenamiento de extensiones para obtener la inicial del avatar.
 * - `.isNullOrBlank()`: extensión sobre `String?` (nullable) — safe to call sin `?.`.
 * - Named parameters: `profileName`, `onLogout` — mejoran legibilidad del call-site.
 * - `Icons.AutoMirrored`: iconos que se voltean automáticamente en idiomas RTL (árabe, hebreo).
 *
 * ## Patrón de diseño — Presentational Component (Container-Presentational):
 * - Este Composable es puramente "presentacional": recibe datos + callbacks, no tiene lógica.
 * - El estado y la lógica viven en el padre (EcoAdminApp) que pasa `appState.profile`.
 * - Ventaja: fácil de testear, reutilizar, y previsualizar con @Preview.
 */
package com.ecoadminmovile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoadminmovile.ui.components.EcoCard
import com.ecoadminmovile.ui.theme.EcoTextMuted
import com.ecoadminmovile.ui.theme.EcoTextStrong
import com.ecoadminmovile.ui.theme.EcoTextSubtle

@Composable
fun ProfileScreen(
    profileName: String,       // Parámetro no-nullable: siempre tiene valor
    profileEmail: String,
    profileRole: String,
    profilePhone: String?,     // Nullable con `?`: puede no existir teléfono
    onLogout: () -> Unit       // Función de orden superior: callback sin parámetros que no retorna nada
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Profile Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = profileName.take(1).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column {
                Text(
                    text = profileName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = EcoTextStrong
                )
                Text(
                    text = profileRole,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoTextSubtle
                )
            }
        }

        EcoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileInfoRow("Email", profileEmail)
                if (!profilePhone.isNullOrBlank()) {
                    ProfileInfoRow("Teléfono", profilePhone)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "Cerrar sesión")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
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
