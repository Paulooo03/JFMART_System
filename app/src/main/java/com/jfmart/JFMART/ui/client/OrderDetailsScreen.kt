package com.jfmart.JFMART.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.data.remote.ApiResult
import com.jfmart.JFMART.data.remote.CancelRequest
import com.jfmart.JFMART.data.remote.DeliveryInfo
import com.jfmart.JFMART.data.remote.Order
import com.jfmart.JFMART.data.remote.apiCall
import com.jfmart.JFMART.ui.components.*
import kotlinx.coroutines.launch

data class OrderDetail(val order: Order, val deliveries: List<DeliveryInfo>)

class OrderDetailViewModel : ViewModel() {
    var state by mutableStateOf<LoadState<OrderDetail>>(LoadState.Loading)
        private set
    var cancelling by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    fun load(orderId: Long) {
        viewModelScope.launch {
            if (state !is LoadState.Loaded) state = LoadState.Loading
            val order = apiCall { AppGraph.api.order(orderId) }
            val deliveries = apiCall { AppGraph.api.orderDeliveries(orderId) }
            state = when {
                order is ApiResult.Ok -> LoadState.Loaded(
                    OrderDetail(order.value, (deliveries as? ApiResult.Ok)?.value.orEmpty())
                )
                order is ApiResult.Err -> LoadState.Error(order.message)
                else -> LoadState.Error("Could not load this order.")
            }
        }
    }

    fun cancel(orderId: Long) {
        viewModelScope.launch {
            cancelling = true
            actionError = null
            when (val result = apiCall { AppGraph.api.cancelOrder(orderId, CancelRequest("Cancelled by client")) }) {
                is ApiResult.Ok -> load(orderId)
                is ApiResult.Err -> actionError = result.message
            }
            cancelling = false
        }
    }
}

private val timeline = listOf("PENDING", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED")

@Composable
fun OrderDetailsScreen(orderId: Long, onBack: () -> Unit, viewModel: OrderDetailViewModel = viewModel()) {
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(orderId) { viewModel.load(orderId) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Order details", onBack = onBack)
        LoadStateView(viewModel.state, onRetry = { viewModel.load(orderId) }) { detail ->
            val order = detail.order
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(order.referenceNumber, style = MaterialTheme.typography.titleLarge)
                    StatusChip(order.status)
                }

                StatusTimeline(order)

                Text("Items", style = MaterialTheme.typography.titleMedium)
                order.items.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${item.name}  x ${item.quantity} ${item.unit}")
                            item.instructions?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                        Text(if (item.lineTotal == null) "Price TBD" else formatMoney(item.lineTotal))
                    }
                }
                HorizontalDivider()
                LabeledValue("Total", formatMoney(order.totalAmount), emphasized = true)
                if (order.hasUnpricedItems) {
                    Text(
                        "Some items have not been priced yet. The total will be final once JF MiniMart confirms your order.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                order.specialInstructions?.let { Text("Your instructions: $it", style = MaterialTheme.typography.bodyMedium) }
                Text("Ordered ${displayDateTime(order.createdAt)}", style = MaterialTheme.typography.bodySmall)
                order.deliveredAt?.let { Text("Delivered ${displayDateTime(it)}", style = MaterialTheme.typography.bodySmall) }

                DeliverySection(order, detail.deliveries)

                viewModel.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (order.status == "PENDING") {
                    OutlinedButton(
                        onClick = { confirmCancel = true },
                        enabled = !viewModel.cancelling,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cancel order") }
                }
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel this order?") },
            text = { Text("This cannot be undone. You can place a new order any time.") },
            confirmButton = {
                TextButton(onClick = { confirmCancel = false; viewModel.cancel(orderId) }) { Text("Cancel order") }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep order") } }
        )
    }
}

@Composable
private fun StatusTimeline(order: Order) {
    if (order.status == "CANCELLED") {
        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
            Text(
                "Cancelled" + (order.cancelledReason?.let { ": $it" } ?: ""),
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
        }
        return
    }
    val reached = timeline.indexOf(order.status)
    Column {
        timeline.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Icon(
                    if (index <= reached) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (index <= reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    prettyStatus(step),
                    style = if (index == reached) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DeliverySection(order: Order, deliveries: List<DeliveryInfo>) {
    val latest = deliveries.firstOrNull() ?: return
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Delivery", style = MaterialTheme.typography.titleMedium)
                StatusChip(latest.status)
            }
            Text("Scheduled ${displayDate(latest.scheduledDate)}")
            Text("To ${latest.deliveryLocation}", style = MaterialTheme.typography.bodyMedium)
            latest.unsuccessfulReason?.let {
                Text("Last attempt failed: $it", color = MaterialTheme.colorScheme.error)
            }
            if (order.status == "OUT_FOR_DELIVERY") {
                Text(
                    "Your order is on its way. When it arrives, scan the courier's QR code to confirm you received it.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/** Shown right after an order is submitted. */
@Composable
fun OrderConfirmationScreen(
    orderId: Long,
    onViewOrder: () -> Unit,
    onHome: () -> Unit,
    viewModel: OrderDetailViewModel = viewModel()
) {
    LaunchedEffect(orderId) { viewModel.load(orderId) }

    LoadStateView(viewModel.state, onRetry = { viewModel.load(orderId) }) { detail ->
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Order submitted", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Your order number", style = MaterialTheme.typography.labelLarge)
            Text(detail.order.referenceNumber, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "JF MiniMart will review it and confirm. Any requested items will be priced first.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onViewOrder, modifier = Modifier.fillMaxWidth()) { Text("View order") }
            TextButton(onClick = onHome) { Text("Back to home") }
        }
    }
}
