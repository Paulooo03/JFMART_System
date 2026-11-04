package com.jfmart.JFMART.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.data.session.ROLE_ADMIN
import com.jfmart.JFMART.data.session.ROLE_CLIENT
import com.jfmart.JFMART.data.session.ROLE_DELIVERY
import com.jfmart.JFMART.data.session.Session
import com.jfmart.JFMART.ui.admin.AdminNoticeScreen
import com.jfmart.JFMART.ui.auth.AuthViewModel
import com.jfmart.JFMART.ui.auth.LoginScreen
import com.jfmart.JFMART.ui.auth.SignUpScreen
import com.jfmart.JFMART.ui.client.*
import com.jfmart.JFMART.ui.components.ClientBottomNavigation
import com.jfmart.JFMART.ui.components.CourierBottomNavigation
import com.jfmart.JFMART.ui.components.navigateToTab
import com.jfmart.JFMART.ui.courier.CourierDashboardScreen
import com.jfmart.JFMART.ui.courier.CourierDeliveryDetailsScreen
import com.jfmart.JFMART.ui.courier.CourierProfileScreen
import com.jfmart.JFMART.ui.courier.CourierQrScreen

/** Where a signed-in user starts; null means signed out. */
private fun homeRouteFor(session: Session?): String? = when (session?.role) {
    ROLE_CLIENT -> Screen.ClientHome.route
    ROLE_DELIVERY -> Screen.CourierDashboard.route
    ROLE_ADMIN -> Screen.AdminNotice.route
    else -> null
}

private val clientTabRoutes = listOf(
    Screen.ClientHome.route,
    Screen.ClientCatalog.route,
    Screen.ClientCart.route,
    Screen.ClientOrders.route,
    Screen.ClientOrderDetails.route,
    Screen.ClientOrderConfirmation.route,
    Screen.ClientAccount.route,
    Screen.ClientStatement.route
)

private val courierTabRoutes = listOf(
    Screen.CourierDashboard.route,
    Screen.CourierDeliveryDetails.route,
    Screen.CourierQr.route,
    Screen.CourierProfile.route
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val session by AppGraph.session.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val authViewModel: AuthViewModel = viewModel()
    val cartViewModel: CartViewModel = viewModel()
    val startDestination = remember { homeRouteFor(AppGraph.session.current) ?: Screen.Login.route }

    // Signing in moves to the role's home; signing out (or an expired token) returns to the login screen
    LaunchedEffect(session) {
        val target = homeRouteFor(session)
        val route = navController.currentDestination?.route
        if (target == null) {
            cartViewModel.clear()
            if (route != null && route != Screen.Login.route && route != Screen.SignUp.route) {
                navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
            }
        } else if (route == Screen.Login.route || route == Screen.SignUp.route) {
            navController.navigate(target) { popUpTo(0) { inclusive = true } }
        }
    }

    Scaffold(
        bottomBar = {
            when (currentRoute) {
                in clientTabRoutes -> ClientBottomNavigation(navController, currentRoute, cartViewModel.itemCount)
                in courierTabRoutes -> CourierBottomNavigation(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(authViewModel, onSignUp = { navController.navigate(Screen.SignUp.route) })
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(authViewModel, onBack = { navController.popBackStack() })
            }

            // Client
            composable(Screen.ClientHome.route) {
                HomeScreen(
                    onOpenOrder = { navController.navigate(Screen.ClientOrderDetails.create(it)) },
                    onPlaceOrder = { navController.navigateToTab(Screen.ClientCatalog.route, Screen.ClientHome.route) },
                    onOpenStatement = { navController.navigate(Screen.ClientStatement.route) }
                )
            }
            composable(Screen.ClientCatalog.route) {
                CatalogScreen(
                    cart = cartViewModel,
                    onViewCart = { navController.navigateToTab(Screen.ClientCart.route, Screen.ClientHome.route) }
                )
            }
            composable(Screen.ClientCart.route) {
                CartScreen(
                    cart = cartViewModel,
                    onBrowse = { navController.navigateToTab(Screen.ClientCatalog.route, Screen.ClientHome.route) },
                    onOrderPlaced = { orderId ->
                        navController.navigate(Screen.ClientOrderConfirmation.create(orderId)) {
                            popUpTo(Screen.ClientHome.route)
                        }
                    }
                )
            }
            composable(Screen.ClientOrders.route) {
                OrdersScreen(onOpenOrder = { navController.navigate(Screen.ClientOrderDetails.create(it)) })
            }
            composable(
                Screen.ClientOrderDetails.route,
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) { entry ->
                OrderDetailsScreen(
                    orderId = entry.arguments?.getLong("orderId") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.ClientOrderConfirmation.route,
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) { entry ->
                val orderId = entry.arguments?.getLong("orderId") ?: 0L
                OrderConfirmationScreen(
                    orderId = orderId,
                    onViewOrder = {
                        navController.navigate(Screen.ClientOrderDetails.create(orderId)) {
                            popUpTo(Screen.ClientHome.route)
                        }
                    },
                    onHome = { navController.navigateToTab(Screen.ClientHome.route, Screen.ClientHome.route) }
                )
            }
            composable(Screen.ClientAccount.route) {
                AccountScreen(
                    onOpenStatement = { navController.navigate(Screen.ClientStatement.route) },
                    onLogout = { authViewModel.logout() }
                )
            }
            composable(Screen.ClientStatement.route) {
                StatementScreen(onBack = { navController.popBackStack() })
            }

            // Courier
            composable(Screen.CourierDashboard.route) {
                CourierDashboardScreen(
                    onOpenDelivery = { navController.navigate(Screen.CourierDeliveryDetails.create(it)) }
                )
            }
            composable(
                Screen.CourierDeliveryDetails.route,
                arguments = listOf(navArgument("deliveryId") { type = NavType.LongType })
            ) { entry ->
                val deliveryId = entry.arguments?.getLong("deliveryId") ?: 0L
                CourierDeliveryDetailsScreen(
                    deliveryId = deliveryId,
                    onBack = { navController.popBackStack() },
                    onShowQr = { navController.navigate(Screen.CourierQr.create(deliveryId)) }
                )
            }
            composable(
                Screen.CourierQr.route,
                arguments = listOf(navArgument("deliveryId") { type = NavType.LongType })
            ) { entry ->
                CourierQrScreen(
                    deliveryId = entry.arguments?.getLong("deliveryId") ?: 0L,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Screen.CourierProfile.route) {
                CourierProfileScreen(
                    onOpenDelivery = { navController.navigate(Screen.CourierDeliveryDetails.create(it)) },
                    onLogout = { authViewModel.logout() }
                )
            }

            // Admin
            composable(Screen.AdminNotice.route) {
                AdminNoticeScreen(onLogout = { authViewModel.logout() })
            }
        }
    }
}
