package com.ecoadminmovile.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoadminmovile.data.AuthRepository
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

val EcoLoginGreen = Color(0xFF1A6B3C)
val EcoLoginGradient = listOf(Color(0xFF145A32), Color(0xFF1A6B3C), Color(0xFF2D9E5F))

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun submit(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        if (currentState.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Introduce tu email de acceso.") }
            return
        }

        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Introduce tu contraseña.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.login(
                email = currentState.email,
                password = currentState.password
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = false, password = "")
                    }
                    onSuccess()
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.linearGradient(colors = EcoLoginGradient)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo Circle
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Assuming logo is available, if not use a fallback icon
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = EcoLoginGreen,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bienvenido a",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EcoTextSubtle
                    )
                    Text(
                        text = "EcoAdmin",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = EcoLoginGreen,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "Inicia sesión con tu cuenta",
                        style = MaterialTheme.typography.bodySmall,
                        color = EcoTextSubtle
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (state.errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = state.errorMessage,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB91C1C),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                LoginTextField(
                    value = state.email,
                    onValueChange = onEmailChanged,
                    label = "Email",
                    icon = Icons.Rounded.Email
                )

                LoginTextField(
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    label = "Contraseña",
                    icon = Icons.Rounded.Lock,
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = EcoLoginGreen),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text(
                            text = "INGRESAR",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                HorizontalDivider(color = Color(0xFFE2E8F0))
                
                Text(
                    text = "Registrarse",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = EcoLoginGreen,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Text(
                    text = "EcoAdmin · Gestión de residuos",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = EcoTextSubtle
                )
            }
        }
    }
}

@Composable
fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EcoLoginGreen,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EcoLoginGreen,
            unfocusedBorderColor = Color(0xFFC8D6CF),
            focusedLabelColor = EcoLoginGreen,
            unfocusedLabelColor = Color(0xFF6B7D72)
        )
    )
}
