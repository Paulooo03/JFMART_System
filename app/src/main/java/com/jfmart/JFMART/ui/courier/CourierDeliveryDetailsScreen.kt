package com.jfmart.JFMART.ui.courier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.jfmart.JFMART.data.remote.CourierDelivery
import com.jfmart.JFMART.data.remote.UnsuccessfulRequest
import com.jfmart.JFMART.data.remote.apiCall
import com.jfmart.JFMART.ui.components.*
import kotlinx.coroutines.launch

class CourierDeliveryViewModel : ViewModel() {
    var state by mutableStateOf<LoadState<CourierDelivery>>(LoadState.Loading)
        private set
    var busy by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    fun load(id: Long) {
        viewModelScope.launch {
            if (state !is LoadState.Loaded) state = LoadState.Loading
            state = when (val result = apiCall { AppGraph.api.courierDelivery(id) }) {
                is ApiResult.Ok -> LoadState.Loaded(result.value)
                is ApiResult.Err -> LoadState.Error(result.message)
            }
        }
    }

    fun markPreparing(id: Long) = perform { AppGraph.api.courierPreparing(id) }

    fun markOutForDelivery(id: Long) = perform { AppGraph.api.courierOutForDelivery(id) }

    fun markUnsuccessful(id: Long, reason: String) =
        perform { AppGraph.api.courierUnsuccessful(id, UnsuccessfulRequest(reason.trim())) }

    private fun perform(call: suspend () -> CourierDelivery) {
        viewModelScope.launch {
            busy = true
            actionError = null
            when (val result = apiCall { call() }) {
                is ApiResult.Ok -> state = LoadState.Loaded(result.value)
                is ApiResult.Err -> actionError = result.message
            }
            busy = false
        }
    }
}

@Composable
fun CourierDeliveryDetailsScreen(
    deliveryId: Long,
    onBack: () -> Unit,
    onShowQr: () -> Unit,
    viewModel: CourierDeliveryViewModel = viewModel()
) {
    var askFailureReason by rememberSaveable { mutableStateOf(false) }
    // Reloads on every return, e.g. after the QR screen where the client may have confirmed the delivery
    LaunchedEffect(deliveryId) { viewModel.load(deliveryId) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Delivery", onBack = onBack)
        LoadStateView(viewModel.state, onRetry = { viewModel.load(deliveryId) }) { delivery ->
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(delivery.clientName ?: "Client", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    StatusChip(delivery.status)
                }
                Text("Order ${delivery.referenceNumber}", style = MaterialTheme.typography.bodyMedium)
                DetailRow("Deliver to", delivery.deliveryLocation)
                DetailRow("Scheduled", displayDate(delivery.scheduledDate))
                delivery.contactPerson?.let { DetailRow("Contact", it + (delivery.phoneNumber?.let { p -> " - $p" } ?: "")) }
                delivery.specialInstructions?.let { DetailRow("Instructions", it) }

                HorizontalDivider()
                Text("Items to deliver", style = MaterialTheme.typography.titleMedium)
                delivery.items.forEach { item ->
                    Column {
                        Text("${item.name}  x ${item.quantity} ${item.unit}")
                        item.instructions?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                HorizontalDivider()

                delivery.unsuccessfulReason?.let {
                    Text("Not delivered: $it", color = MaterialTheme.colorScheme.error)
                }
                delivery.deliveredAt?.let { Text("Delivered ${displayDateTime(it)}") }
                viewModel.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                DeliveryActions(
                    delivery = delivery,
                    busy = viewModel.busy,
                    onPreparing = { viewModel.markPreparing(deliveryId) },
                    onOutForDelivery = { viewModel.markOutForDelivery(deliveryId) },
                    onShowQr = onShowQr,
                    onFailed = { askFailureReason = true }
                )
            }
        }
    }

    if (askFailureReason) {
        FailureReasonDialog(
            onDismiss = { askFailureReason = false },
            onConfirm = { reason ->
                askFailureReason = false
                viewModel.markUnsuccessful(deliveryId, reason)
            }
        )
    }
}

@Composable
private fun DeliveryActions(
    delivery: CourierDelivery,
    busy: Boolean,
    onPreparing: () -> Unit,
    onOutForDelivery: () -> Unit,
    onShowQr: () -> Unit,
    onFailed: () -> Unit
) {
    when (delivery.status) {
        "ASSIGNED" -> Button(onClick = onPreparing, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Start preparing")
        }
        "PREPARING" -> {
            Button(onClick = onOutForDelivery, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Out for delivery")
            }
            OutlinedButton(onClick = onFailed, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Cannot deliver")
            }
        }
        "OUT_FOR_DELIVERY" -> {
            Button(onClick = onShowQr, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Show QR code to client")
            }
            OutlinedButton(onClick = onFailed, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Delivery unsuccessful")
            }
        }
    }
}

@Composable
private fun FailureReasonDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Why could it not be delivered?") },
        text = { FormField("Reason", reason, { reason = it }, singleLine = false) },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }, enabled = reason.isNotBlank()) { Text("Mark unsuccessful") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
