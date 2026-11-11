package com.jfmart.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.jfmart.ui.navigation.Screen

@Composable
fun ClientBottomNavigation(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute in listOf(
                Screen.ClientDashboard.route,
                Screen.ClientOrderDetails.route,
                Screen.ClientPayments.route
            ),
            onClick = {
                navController.navigate(Screen.ClientDashboard.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.ClientAvailableItems.route,
            onClick = {
                navController.navigate(Screen.ClientAvailableItems.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Edit, contentDescription = "Order") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.ClientCart.route,
            onClick = {
                navController.navigate(Screen.ClientCart.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Cart") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.ClientManageAccount.route,
            onClick = {
                navController.navigate(Screen.ClientManageAccount.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Account") }
        )
    }
}

@Composable
fun CourierBottomNavigation(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute in listOf(
                Screen.CourierDashboard.route,
                Screen.CourierDeliveryDetails.route
            ),
            onClick = {
                navController.navigate(Screen.CourierDashboard.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") }
        )
    }
}