package com.jfmart.JFMART.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.jfmart.JFMART.navigation.Screen

@Composable
fun CourierBottomNavigation(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar {
        // Home / Dashboard
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

        // Profile / Account
        NavigationBarItem(
            selected = currentRoute == Screen.CourierProfile.route,
            onClick = {
                navController.navigate(Screen.CourierProfile.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") }
        )
    }
}