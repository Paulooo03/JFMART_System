package com.jfmart.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jfmart.ui.client.*
import com.jfmart.ui.components.ClientBottomNavigation
import com.jfmart.ui.components.CourierBottomNavigation
import com.jfmart.ui.courier.*

@Composable
fun JFMartApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val clientRoutes = listOf(
        Screen.ClientDashboard.route,
        Screen.ClientOrderDetails.route,
        Screen.ClientPayments.route,
        Screen.ClientAvailableItems.route,
        Screen.ClientCart.route,
        Screen.ClientManageAccount.route
    )

    val courierRoutes = listOf(
        Screen.CourierDashboard.route,
        Screen.CourierDeliveryDetails.route
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.ClientLogin.route
                ) {
                    // Client Flow
                    composable(Screen.ClientLogin.route) {
                        ClientLoginScreen(
                            onNavigateToDashboard = {
                                navController.navigate(Screen.ClientDashboard.route) {
                                    popUpTo(Screen.ClientLogin.route) { inclusive = true }
                                }
                            },
                            onSwitchToCourier = {
                                navController.navigate(Screen.CourierLogin.route) {
                                    popUpTo(Screen.ClientLogin.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.ClientDashboard.route) {
                        ClientDashboardScreen(
                            onViewOrder = { navController.navigate(Screen.ClientOrderDetails.route) },
                            onViewPayment = { navController.navigate(Screen.ClientPayments.route) }
                        )
                    }
                    composable(Screen.ClientOrderDetails.route) {
                        ClientOrderDetailsScreen()
                    }
                    composable(Screen.ClientPayments.route) {
                        ClientPaymentsScreen()
                    }
                    composable(Screen.ClientAvailableItems.route) {
                        ClientAvailableItemsScreen(
                            onViewCart = { navController.navigate(Screen.ClientCart.route) }
                        )
                    }
                    composable(Screen.ClientCart.route) {
                        ClientCartScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.ClientManageAccount.route) {
                        ClientManageAccountScreen()
                    }

                    // Courier Flow
                    composable(Screen.CourierLogin.route) {
                        CourierLoginScreen(
                            onNavigateToDashboard = {
                                navController.navigate(Screen.CourierDashboard.route) {
                                    popUpTo(Screen.CourierLogin.route) { inclusive = true }
                                }
                            },
                            onSwitchToClient = {
                                navController.navigate(Screen.ClientLogin.route) {
                                    popUpTo(Screen.CourierLogin.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.CourierDashboard.route) {
                        CourierDashboardScreen(
                            onViewPending = { navController.navigate(Screen.CourierDeliveryDetails.route) }
                        )
                    }
                    composable(Screen.CourierDeliveryDetails.route) {
                        CourierDeliveryDetailsScreen()
                    }
                }
            }

            // Render navigation bars according to current active graph
            when {
                currentRoute in clientRoutes -> {
                    ClientBottomNavigation(navController = navController, currentRoute = currentRoute)
                }
                currentRoute in courierRoutes -> {
                    CourierBottomNavigation(navController = navController, currentRoute = currentRoute)
                }
            }
        }
    }
}