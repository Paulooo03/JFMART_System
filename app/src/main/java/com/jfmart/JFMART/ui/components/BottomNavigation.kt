package com.jfmart.JFMART.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.jfmart.JFMART.navigation.Screen

/**
 * Switches tab and always lands on that tab's first screen, even when one of its detail screens is open.
 * [root] is the tab the user landed on after signing in.
 */
fun NavController.navigateToTab(route: String, root: String) {
    navigate(route) {
        popUpTo(root)
        launchSingleTop = true
    }
}

@Composable
fun ClientBottomNavigation(
    navController: NavController,
    currentRoute: String?,
    cartCount: Int
) {
    val root = Screen.ClientHome.route

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.ClientHome.route,
            onClick = { navController.navigateToTab(Screen.ClientHome.route, root) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.ClientCatalog.route,
            onClick = { navController.navigateToTab(Screen.ClientCatalog.route, root) },
            icon = { Icon(Icons.Default.Edit, contentDescription = "Order") },
            label = { Text("Order") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.ClientCart.route,
            onClick = { navController.navigateToTab(Screen.ClientCart.route, root) },
            icon = {
                BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                }
            },
            label = { Text("Cart") }
        )
        NavigationBarItem(
            selected = currentRoute in listOf(
                Screen.ClientOrders.route,
                Screen.ClientOrderDetails.route,
                Screen.ClientOrderConfirmation.route
            ),
            onClick = { navController.navigateToTab(Screen.ClientOrders.route, root) },
            icon = { Icon(Icons.Default.Receipt, contentDescription = "Orders") },
            label = { Text("Orders") }
        )
        NavigationBarItem(
            selected = currentRoute in listOf(Screen.ClientAccount.route, Screen.ClientStatement.route),
            onClick = { navController.navigateToTab(Screen.ClientAccount.route, root) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
            label = { Text("Account") }
        )
    }
}
