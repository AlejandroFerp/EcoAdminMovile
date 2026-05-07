/**
 * Pantalla de perfil del usuario autenticado con soporte para edición y cambio de contraseña.
 */
package com.ecoadminmovile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoadminmovile.ui.components.EcoCard
import com.ecoadminmovile.ui.theme.EcoAdminTheme
import com.ecoadminmovile.ui.theme.EcoTextMuted
import com.ecoadminmovile.ui.theme.EcoTextStrong
import com.ecoadminmovile.ui.theme.EcoTextSubtle

@Composable
fun ProfileScreen(
    profileName: String,
    profileEmail: String,
    profileRole: String,
    profilePhone: String?,
    profileDni: String? = null,
    profileCargo: String? = null,
    profileState: ProfileUiState = ProfileUiState(),
    onLogout: () -> Unit,
    onStartEditing: () -> Unit = {},
    onCancelEditing: () -> Unit = {},
    onSaveProfile: () -> Unit = {},
    onNombreChanged: (String) -> Unit = {},
    onTelefonoChanged: (String) -> Unit = {},
    onDniChanged: (String) -> Unit = {},
    onCargoChanged: (String) -> Unit = {},
    onShowPasswordDialog: () -> Unit = {},
    onDismissPasswordDialog: () -> Unit = {},
    onCurrentPasswordChanged: (String) -> Unit = {},
    onNewPasswordChanged: (String) -> Unit = {},
    onConfirmPasswordChanged: (String) -> Unit = {},
    onChangePassword: () -> Unit = {},
    onClearSuccessMessage: () -> Unit = {}
) {
    // Show success snackbar
    LaunchedEffect(profileState.saveSuccessMessage) {
        if (profileState.saveSuccessMessage != null) {
            kotlinx.coroutines.delay(2000)
            onClearSuccessMessage()
        }
    }

    // Password change dialog
    if (profileState.showPasswordDialog) {
        AlertDialog(
            onDismissRequest = onDismissPasswordDialog,
            title = { Text("Cambiar contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = profileState.currentPassword,
                        onValueChange = onCurrentPasswordChanged,
                        label = { Text("Contraseña actual") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = profileState.newPassword,
                        onValueChange = onNewPasswordChanged,
                        label = { Text("Nueva contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = profileState.confirmPassword,
                        onValueChange = onConfirmPasswordChanged,
                        label = { Text("Confirmar contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (profileState.passwordError != null) {
                        Text(
                            text = profileState.passwordError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onChangePassword,
                    enabled = !profileState.isChangingPassword
                ) {
                    if (profileState.isChangingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Cambiar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPasswordDialog) { Text("Cancelar") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Success message
        if (profileState.saveSuccessMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = profileState.saveSuccessMessage,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

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

            Column(modifier = Modifier.weight(1f)) {
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

            if (!profileState.isEditing) {
                IconButton(onClick = onStartEditing) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar perfil")
                }
            }
        }

        if (profileState.isEditing) {
            // Edit mode
            EcoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Editar datos personales",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = profileState.editNombre,
                        onValueChange = onNombreChanged,
                        label = { Text("Nombre *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = profileState.editTelefono,
                        onValueChange = onTelefonoChanged,
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = profileState.editDni,
                        onValueChange = onDniChanged,
                        label = { Text("DNI") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = profileState.editCargo,
                        onValueChange = onCargoChanged,
                        label = { Text("Cargo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (profileState.errorMessage != null) {
                        Text(
                            text = profileState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onCancelEditing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("Cancelar") }

                        Button(
                            onClick = onSaveProfile,
                            enabled = !profileState.isSaving,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (profileState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Guardar")
                            }
                        }
                    }
                }
            }
        } else {
            // View mode
            EcoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileInfoRow("Email", profileEmail)
                    if (!profilePhone.isNullOrBlank()) {
                        ProfileInfoRow("Teléfono", profilePhone)
                    }
                    if (!profileDni.isNullOrBlank()) {
                        ProfileInfoRow("DNI", profileDni)
                    }
                    if (!profileCargo.isNullOrBlank()) {
                        ProfileInfoRow("Cargo", profileCargo)
                    }
                }
            }
        }

        // Change password button
        OutlinedButton(
            onClick = onShowPasswordDialog,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cambiar contraseña")
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

// --- Previews ---

@Preview(showBackground = true, name = "Perfil - Vista")
@Composable
fun ProfileScreenPreview() {
    EcoAdminTheme {
        ProfileScreen(
            profileName = "Juan García",
            profileEmail = "juan.garcia@ecoadmin.com",
            profileRole = "Administrador",
            profilePhone = "666 123 456",
            profileDni = "12345678A",
            profileCargo = "Supervisor de Residuos",
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, name = "Perfil - Editando")
@Composable
fun ProfileScreenEditingPreview() {
    EcoAdminTheme {
        ProfileScreen(
            profileName = "Juan García",
            profileEmail = "juan.garcia@ecoadmin.com",
            profileRole = "Administrador",
            profilePhone = "666 123 456",
            profileState = ProfileUiState(
                isEditing = true,
                editNombre = "Juan García López",
                editTelefono = "666 123 456",
                editDni = "12345678A",
                editCargo = "Supervisor"
            ),
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, name = "Perfil - Contraseña")
@Composable
fun ProfileScreenPasswordPreview() {
    EcoAdminTheme {
        ProfileScreen(
            profileName = "Juan García",
            profileEmail = "juan.garcia@ecoadmin.com",
            profileRole = "Administrador",
            profilePhone = null,
            profileState = ProfileUiState(
                showPasswordDialog = true,
                newPassword = "123456",
                confirmPassword = "12345",
                passwordError = "Las contraseñas no coinciden"
            ),
            onLogout = {}
        )
    }
}
