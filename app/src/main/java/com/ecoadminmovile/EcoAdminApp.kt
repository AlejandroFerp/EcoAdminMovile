/**
 * Composable principal de navegación de EcoAdmin.
 *
 * Conceptos Kotlin y Compose demostrados:
 * - **Navigation pattern**: `NavHost` + `composable("route")` define un grafo de navegación DSL.
 * - `private data class`: clase de datos con visibilidad de archivo; genera equals/hashCode/copy.
 * - `private val listOf(...)`: constante top-level privada con una lista inmutable.
 * - `hiltViewModel()`: obtiene un ViewModel inyectado por Hilt (Dependency Injection en Compose).
 * - `collectAsStateWithLifecycle()`: convierte un Flow en State de Compose respetando el ciclo
 *   de vida (se cancela en onStop, se reanuda en onStart).
 * - `by` (property delegation): delega el getter de una propiedad a otro objeto (aquí State).
 * - `when { }` sin argumento: actúa como cadena if-elseif; evalúa condiciones booleanas.
 * - `LaunchedEffect(key)`: ejecuta una coroutine como efecto lateral en Compose.
 *   Se relanza si `key` cambia. Útil para navegación o llamadas únicas.
 * - `rememberNavController()`: preserva el NavController entre recomposiciones.
 * - `navArgument("id") { type = NavType.LongType }`: argumento tipado en rutas.
 * - `popUpTo(...) { saveState = true }`: gestión del back stack al navegar.
 * - `?.` (safe call operator): accede a propiedades solo si el receptor no es null.
 * - `Brush.verticalGradient()`: crea un degradado vertical para fondos.
 * - `Modifier.padding(innerPadding)`: consume el padding que Scaffold reserva para barras.
 */
package com.ecoadminmovile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ecoadminmovile.app.AppViewModel
import com.ecoadminmovile.feature.auth.LoginScreen
import com.ecoadminmovile.feature.auth.LoginViewModel
import com.ecoadminmovile.feature.centers.CenterDetailScreen
import com.ecoadminmovile.feature.centers.CenterDetailViewModel
import com.ecoadminmovile.feature.centers.CentersScreen
import com.ecoadminmovile.feature.centers.CentersViewModel
import com.ecoadminmovile.feature.dashboard.DashboardScreen
import com.ecoadminmovile.feature.dashboard.DashboardViewModel
import com.ecoadminmovile.feature.profile.ProfileScreen
import com.ecoadminmovile.feature.transfers.TransferDetailScreen
import com.ecoadminmovile.feature.transfers.TransferDetailViewModel
import com.ecoadminmovile.feature.transfers.TransferFormScreen
import com.ecoadminmovile.feature.transfers.TransferFormViewModel
import com.ecoadminmovile.feature.transfers.TransfersListScreen
import com.ecoadminmovile.feature.transfers.TransfersViewModel
import com.ecoadminmovile.feature.transfers.QrScannerScreen
import com.ecoadminmovile.ui.theme.EcoBlue
import com.ecoadminmovile.ui.theme.EcoSlate

// private data class: solo visible en este archivo. Agrupa ruta, título e ícono.
private data class TopLevelDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
)

// listOf(): crea una lista inmutable de destinos de nivel superior
private val topLevelDestinations = listOf(
    TopLevelDestination(route = "dashboard", title = "Panel", icon = Icons.Default.Dashboard),
    TopLevelDestination(route = "traslados", title = "Traslados", icon = Icons.Default.LocalShipping),
    TopLevelDestination(route = "centros", title = "Centros", icon = Icons.Default.Business),
    TopLevelDestination(route = "perfil", title = "Perfil", icon = Icons.Default.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoAdminApp(appViewModel: AppViewModel = hiltViewModel()) { // hiltViewModel(): DI provee el ViewModel
    // `by` delega el getter; collectAsStateWithLifecycle() convierte Flow→State lifecycle-aware
    val appState by appViewModel.uiState.collectAsStateWithLifecycle()

    // when sin argumento: actúa como if-elseif evaluando condiciones booleanas
    // Decide qué pantalla mostrar según el estado de autenticación
    when {
        appState.isLoading -> LoadingScreen()
        !appState.isAuthenticated -> {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val loginState by loginViewModel.uiState.collectAsStateWithLifecycle()

            LoginScreen(
                state = loginState,
                onEmailChanged = loginViewModel::updateEmail,
                onPasswordChanged = loginViewModel::updatePassword,
                onLoginClick = {
                    loginViewModel.submit {
                        appViewModel.refreshSession(showErrorOnFailure = true)
                    }
                }
            )
        }

        else -> {
            // rememberNavController: preserva el controlador entre recomposiciones
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            // ?. (safe call): accede a route solo si destination no es null
            val currentRoute = backStackEntry?.destination?.route
            val currentTitle = topLevelDestinations.firstOrNull { it.route == currentRoute }?.title
                ?: "Detalle traslado"
            // Visibilidad del bottom bar: se oculta en pantallas de detalle/formulario
            val showBottomBar = currentRoute != "traslado/{trasladoId}" &&
                currentRoute != "traslado/form" && currentRoute != "traslado/form/{trasladoId}" &&
                currentRoute != "qr-scanner"

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = currentTitle) }
                    )
                },
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            topLevelDestinations.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentRoute == destination.route,
                                    onClick = {
                                        navController.navigate(destination.route) {
                                            // popUpTo + saveState: limpia el back stack pero guarda estado
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = destination.title
                                        )
                                    },
                                    label = { Text(text = destination.title) }
                                )
                            }
                        }
                    }
                }
            // Modifier.padding(innerPadding): consume el espacio que Scaffold reserva para barras
            ) { innerPadding ->
                // NavHost define el grafo de navegación: cada composable("ruta") es un destino
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("dashboard") {
                        val dashboardViewModel: DashboardViewModel = hiltViewModel()
                        val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

                        DashboardScreen(
                            state = dashboardState,
                            onRefresh = dashboardViewModel::load,
                            onPeriodSelected = dashboardViewModel::setPeriod
                        )
                    }

                    composable("traslados") {
                        val transfersViewModel: TransfersViewModel = hiltViewModel()
                        val transfersState by transfersViewModel.uiState.collectAsStateWithLifecycle()

                        TransfersListScreen(
                            state = transfersState,
                            onRefresh = transfersViewModel::load,
                            onTransferSelected = { transferId ->
                                navController.navigate("traslado/$transferId")
                            },
                            onSearchChanged = transfersViewModel::updateSearch,
                            onStatusFilter = transfersViewModel::filterByStatus,
                            onCreateNew = { navController.navigate("traslado/form") },
                            onScanQr = { navController.navigate("qr-scanner") }
                        )
                    }

                    composable("traslado/form") {
                        val formViewModel: TransferFormViewModel = hiltViewModel()
                        val formState by formViewModel.uiState.collectAsStateWithLifecycle()

                        // LaunchedEffect(Unit): se ejecuta UNA sola vez al entrar en la composición
                        LaunchedEffect(Unit) { formViewModel.initForm() }

                        // LaunchedEffect como trigger de navegación: efecto lateral en Compose
                        if (formState.savedSuccessfully) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }

                        TransferFormScreen(
                            state = formState,
                            onBack = { navController.popBackStack() },
                            onSave = formViewModel::save,
                            onFieldChanged = formViewModel::onFieldChanged
                        )
                    }

                    composable(
                        route = "traslado/form/{trasladoId}",
                        // navArgument con tipo: define parámetros tipados en la ruta
                        arguments = listOf(navArgument("trasladoId") { type = NavType.LongType })
                    ) { entry ->
                        val transferId = entry.arguments?.getLong("trasladoId") ?: return@composable
                        val formViewModel: TransferFormViewModel = hiltViewModel()
                        val formState by formViewModel.uiState.collectAsStateWithLifecycle()

                        LaunchedEffect(transferId) { formViewModel.initForm(transferId) }

                        if (formState.savedSuccessfully) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }

                        TransferFormScreen(
                            state = formState,
                            onBack = { navController.popBackStack() },
                            onSave = formViewModel::save,
                            onFieldChanged = formViewModel::onFieldChanged
                        )
                    }

                    composable(
                        route = "traslado/{trasladoId}",
                        arguments = listOf(
                            navArgument("trasladoId") {
                                type = NavType.LongType
                            }
                        )
                    ) { navBackStackEntry ->
                        val transferId = navBackStackEntry.arguments?.getLong("trasladoId") ?: return@composable
                        val transferDetailViewModel: TransferDetailViewModel = hiltViewModel()
                        val detailState by transferDetailViewModel.uiState.collectAsStateWithLifecycle()

                        LaunchedEffect(transferId) {
                            transferDetailViewModel.load(transferId)
                        }

                        TransferDetailScreen(
                            state = detailState,
                            onBack = { navController.popBackStack() },
                            onShowStatusSheet = transferDetailViewModel::showStatusSheet,
                            onDismissStatusSheet = transferDetailViewModel::hideStatusSheet,
                            onChangeStatus = transferDetailViewModel::changeStatus,
                            onEdit = { navController.navigate("traslado/form/$transferId") },
                            onDelete = {
                                transferDetailViewModel.deleteTransfer {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    composable("qr-scanner") {
                        QrScannerScreen(
                            onBack = { navController.popBackStack() },
                            onQrScanned = { code ->
                                // Try to extract traslado ID from the QR code
                                val id = code.filter { it.isDigit() }.toLongOrNull()
                                navController.popBackStack()
                                if (id != null) {
                                    navController.navigate("traslado/$id")
                                }
                            }
                        )
                    }

                    composable("centros") {
                        val centersViewModel: CentersViewModel = hiltViewModel()
                        val centersState by centersViewModel.uiState.collectAsStateWithLifecycle()

                        CentersScreen(
                            state = centersState,
                            onRefresh = centersViewModel::load,
                            onSearchChanged = centersViewModel::updateSearch,
                            onTypeFilter = centersViewModel::filterByType,
                            onCenterSelected = { centerId ->
                                navController.navigate("centro/$centerId")
                            }
                        )
                    }

                    composable(
                        "centro/{centroId}",
                        arguments = listOf(
                            navArgument("centroId") {
                                type = NavType.LongType
                            }
                        )
                    ) { navBackStackEntry ->
                        val centerId = navBackStackEntry.arguments?.getLong("centroId") ?: return@composable
                        val centerDetailViewModel: CenterDetailViewModel = hiltViewModel()
                        val detailState by centerDetailViewModel.uiState.collectAsStateWithLifecycle()

                        LaunchedEffect(centerId) {
                            centerDetailViewModel.load(centerId)
                        }

                        CenterDetailScreen(
                            state = detailState,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("perfil") {
                        ProfileScreen(
                            profileName = appState.profile?.nombre.orEmpty(),
                            profileEmail = appState.profile?.email.orEmpty(),
                            profileRole = appState.profile?.rol.orEmpty(),
                            profilePhone = appState.profile?.telefono,
                            onLogout = appViewModel::logout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    // Brush.verticalGradient: crea un degradado de arriba a abajo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(EcoSlate, EcoBlue)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Conectando EcoAdmin Movile",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Validando la sesion con el backend.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
