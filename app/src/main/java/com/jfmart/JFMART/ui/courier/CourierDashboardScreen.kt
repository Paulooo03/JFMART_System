package com.jfmart.JFMART.ui.courier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.data.remote.ApiResult
import com.jfmart.JFMART.data.remote.CourierDelivery
import com.jfmart.JFMART.data.remote.apiCall
import com.jfmart.JFMART.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CourierDeliveriesViewModel : ViewModel() {
    var state by mutableStateOf<LoadState<List<CourierDelivery>>>(LoadState.Loading)
        private set

    fun load() {
        viewModelScope.launch {
            if (state !is LoadState.Loaded) state = LoadState.Loading
            state = when (val result = apiCall { AppGraph.api.courierDeliveries() }) {
                is ApiResult.Ok -> LoadState.Loaded(result.value)
                is ApiResult.Err -> LoadState.Error(result.message)
            }
        }
    }
}

/** A delivery is still to do until it ends as delivered or unsuccessful. */
internal val activeDeliveryStatuses = setOf("ASSIGNED", "PREPARING", "OUT_FOR_DELIVERY")

internal fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

@Composable
fun CourierDashboardScreen(onOpenDelivery: (Long) -> Unit, viewModel: CourierDeliveriesViewModel = viewModel()) {
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Deliveries") {
            IconButton(onClick = { viewModel.load() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        LoadStateView(viewModel.state, onRetry = { viewModel.load() }) { deliveries ->
            val today = todayIso()
            val active = deliveries.filter { it.status in activeDeliveryStatuses }.sortedBy { it.scheduledDate }
            val done = deliveries.filter { it.status !in activeDeliveryStatuses }.sortedByDescending { it.scheduledDate }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (active.isEmpty()) item { Text("No deliveries to make right now.") }
                deliverySection("Overdue", active.filter { it.scheduledDate < today }, onOpenDelivery)
                deliverySection("Today", active.filter { it.scheduledDate == today }, onOpenDelivery)
                deliverySection("Upcoming", active.filter { it.scheduledDate > today }, onOpenDelivery)
                deliverySection("Done", done, onOpenDelivery)
            }
        }
    }
}

private fun LazyListScope.deliverySection(title: String, deliveries: List<CourierDelivery>, onOpen: (Long) -> Unit) {
    if (deliveries.isEmpty()) return
    item(key = "header-$title") { Text(title, style = MaterialTheme.typography.titleMedium) }
    items(deliveries, key = { "$title-${it.id}" }) { DeliveryCard(it) { onOpen(it.id) } }
}

@Composable
internal fun DeliveryCard(delivery: CourierDelivery, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(delivery.clientName ?: delivery.referenceNumber, style = MaterialTheme.typography.titleMedium)
                StatusChip(delivery.status)
            }
            Text("Order ${delivery.referenceNumber}", style = MaterialTheme.typography.bodySmall)
            Text(delivery.deliveryLocation, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Scheduled ${displayDate(delivery.scheduledDate)} - ${delivery.items.size} item(s)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
