package com.jfmart.JFMART.navigation
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.jfmart.JFMART.client.*
import com.jfmart.JFMART.courier.*
import com.jfmart.JFMART.components.ClientBottomNavigation
import com.jfmart.JFMART.components.CourierBottomNavigation

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val clientRoutes = listOf(
        Screen.ClientDashboard.route,
        Screen.ClientAvailableItems.route,
        Screen.ClientCart.route,
        Screen.ClientManageAccount.route,
        Screen.ClientOrderDetails.route,
        Screen.ClientPayments.route
    )

    val courierRoutes = listOf(
        Screen.CourierDashboard.route,
        Screen.CourierDeliveryDetails.route,
        Screen.CourierProfile.route
    )

    Scaffold(
        bottomBar = {
            when {
                currentRoute in clientRoutes -> {
                    ClientBottomNavigation(
                        navController = navController,
                        currentRoute = currentRoute
                    )
                }
                currentRoute in courierRoutes -> {
                    CourierBottomNavigation(
                        navController = navController,
                        currentRoute = currentRoute
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.ClientLogin.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Client Login
            composable(Screen.ClientLogin.route) {
                ClientLoginScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Screen.ClientDashboard.route) {
                            popUpTo(Screen.ClientLogin.route) { inclusive = true }
                        }
                    },
                    onSwitchToCourier = {
                        navController.navigate(Screen.CourierLogin.route)
                    }
                )
            }

            // Courier Login
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

            // Client Screens
            composable(Screen.ClientDashboard.route) {
                ClientDashboardScreen(
                    onViewPendingDelivery = { navController.navigate(Screen.ClientOrderDetails.route) },
                    onViewDeliveredOrder = { navController.navigate(Screen.ClientOrderDetails.route) }
                )
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

            composable(Screen.ClientOrderDetails.route) {
                ClientOrderDetailsScreen()
            }

            composable(Screen.ClientPayments.route) {
                ClientPaymentsScreen()
            }

            // Courier Screens
            composable(Screen.CourierDashboard.route) {
                CourierDashboardScreen(
                    onViewPending = { navController.navigate(Screen.CourierDeliveryDetails.route) },
                    onViewDelivered = { navController.navigate(Screen.CourierDeliveryDetails.route) }
                )
            }

            composable(Screen.CourierDeliveryDetails.route) {
                CourierDeliveryDetailsScreen()
            }

            composable(Screen.CourierProfile.route) {
                CourierProfileScreen()
            }
        }
    }
}