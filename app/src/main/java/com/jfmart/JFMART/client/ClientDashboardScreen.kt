package com.jfmart.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfmart.ui.components.StatusCardRow

@Composable
fun ClientDashboardScreen(
    onViewOrder: () -> Unit,
    onViewPayment: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Order status", style = MaterialTheme.typography.titleMedium)
        StatusCardRow(
            label = "(Order number) (Status)",
            onViewClick = onViewOrder
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Payment status", style = MaterialTheme.typography.titleMedium)
        StatusCardRow(
            label = "(number of days) remaining",
            onViewClick = onViewPayment
        )
    }
}