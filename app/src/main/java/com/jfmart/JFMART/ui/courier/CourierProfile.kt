package com.jfmart.JFMART.ui.courier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.ui.components.*

@Composable
fun CourierProfileScreen(
    onOpenDelivery: (Long) -> Unit,
    onLogout: () -> Unit,
    viewModel: CourierDeliveriesViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Profile")
        Text("Signed in as ${AppGraph.session.current?.username ?: ""}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onLogout) { Text("Sign out") }

        Spacer(Modifier.height(24.dp))
        Text("Delivery history", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LoadStateView(viewModel.state, onRetry = { viewModel.load() }) { deliveries ->
            val delivered = deliveries.filter { it.status == "DELIVERED" }.sortedByDescending { it.deliveredAt }
            if (delivered.isEmpty()) {
                Text("No completed deliveries yet.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(delivered, key = { it.id }) { delivery ->
                        OutlinedCard(onClick = { onOpenDelivery(delivery.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(delivery.clientName ?: delivery.referenceNumber, style = MaterialTheme.typography.titleMedium)
                                Text("Order ${delivery.referenceNumber}", style = MaterialTheme.typography.bodySmall)
                                Text("Delivered ${displayDateTime(delivery.deliveredAt)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
