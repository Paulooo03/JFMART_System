package com.jfmart.ui.courier

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfmart.ui.components.StatusCardRow

@Composable
fun CourierDashboardScreen(onViewPending: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Pending deliveries", style = MaterialTheme.typography.titleMedium)
        StatusCardRow(
            label = "(Order number)",
            onViewClick = onViewPending
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Delivered orders", style = MaterialTheme.typography.titleMedium)
        StatusCardRow(
            label = "(Order number)",
            onViewClick = onViewPending
        )
    }
}