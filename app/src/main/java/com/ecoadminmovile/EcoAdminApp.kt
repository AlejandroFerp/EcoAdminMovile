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
import com.ecoadminmovile.feature.centers.CentersScreen
import com.ecoadminmovile.feature.centers.CentersViewModel
import com.ecoadminmovile.feature.dashboard.DashboardScreen
import com.ecoadminmovile.feature.dashboard.DashboardViewModel
import com.ecoadminmovile.feature.profile.ProfileScreen
import com.ecoadminmovile.feature.transfers.TransferDetailScreen
import com.ecoadminmovile.feature.transfers.TransferDetailViewModel
import com.ecoadminmovile.feature.transfers.TransfersScreen
import com.ecoadminmovile.feature.transfers.TransfersViewModel
import com.ecoadminmovile.ui.theme.EcoBlue
import com.ecoadminmovile.ui.theme.EcoSlate

private data class TopLevelDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelDestination(route = "dashboard", title = "Panel", icon = Icons.Default.Dashboard),
    TopLevelDestination(route = "traslados", title = "Traslados", icon = Icons.Default.LocalShipping),
    TopLevelDestination(route = "centros", title = "Centros", icon = Icons.Default.Business),
    TopLevelDestination(route = "perfil", title = "Perfil", icon = Icons.Default.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoAdminApp(appViewModel: AppViewModel = hiltViewModel()) {
    val appState by appViewModel.uiState.collectAsStateWithLifecycle()

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
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            val currentTitle = topLevelDestinations.firstOrNull { it.route == currentRoute }?.title
                ?: "Detalle traslado"
            val showBottomBar = currentRoute != "traslado/{trasladoId}"

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
            ) { innerPadding ->
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
                            onRefresh = dashboardViewModel::load
                        )
                    }

                    composable("traslados") {
                        val transfersViewModel: TransfersViewModel = hiltViewModel()
                        val transfersState by transfersViewModel.uiState.collectAsStateWithLifecycle()

                        TransfersScreen(
                            state = transfersState,
                            onRefresh = transfersViewModel::load,
                            onTransferSelected = { transferId ->
                                navController.navigate("traslado/$transferId")
                            }
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
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("centros") {
                        val centersViewModel: CentersViewModel = hiltViewModel()
                        val centersState by centersViewModel.uiState.collectAsStateWithLifecycle()

                        CentersScreen(
                            state = centersState,
                            onRefresh = centersViewModel::load
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
