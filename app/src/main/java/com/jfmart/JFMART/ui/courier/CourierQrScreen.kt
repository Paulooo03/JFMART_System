package com.jfmart.JFMART.ui.courier

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.data.remote.ApiResult
import com.jfmart.JFMART.data.remote.QrToken
import com.jfmart.JFMART.data.remote.apiCall
import com.jfmart.JFMART.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CourierQrViewModel : ViewModel() {
    var token by mutableStateOf<QrToken?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var delivered by mutableStateOf(false)
        private set

    /** Each call gives a new single-use code and retires the previous one. */
    fun issue(deliveryId: Long) {
        viewModelScope.launch {
            loading = true
            error = null
            when (val result = apiCall { AppGraph.api.courierQr(deliveryId) }) {
                is ApiResult.Ok -> token = result.value
                is ApiResult.Err -> error = result.message
            }
            loading = false
        }
    }

    /** Detects that the client scanned the code. */
    fun checkDelivered(deliveryId: Long) {
        viewModelScope.launch {
            val result = apiCall { AppGraph.api.courierDelivery(deliveryId) }
            if (result is ApiResult.Ok && result.value.status == "DELIVERED") delivered = true
        }
    }
}

@Composable
fun CourierQrScreen(deliveryId: Long, onDone: () -> Unit, viewModel: CourierQrViewModel = viewModel()) {
    LaunchedEffect(deliveryId) { viewModel.issue(deliveryId) }
    // Asks the server every few seconds whether the client has scanned; stops when the screen closes
    LaunchedEffect(deliveryId) {
        while (!viewModel.delivered) {
            delay(3000)
            viewModel.checkDelivered(deliveryId)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("Delivery QR code", onBack = onDone)

        if (viewModel.delivered) {
            Spacer(Modifier.height(48.dp))
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Delivered", style = MaterialTheme.typography.headlineMedium)
            Text("The client confirmed receipt.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDone) { Text("Back to deliveries") }
            return@Column
        }

        Spacer(Modifier.height(8.dp))
        Text("Ask the client to scan this code with the JF MiniMart app.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))

        val token = viewModel.token
        when {
            token != null -> {
                val bitmap = remember(token.token) { qrBitmap(token.token) }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Delivery QR code",
                    modifier = Modifier.size(280.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("If scanning does not work, the client can type this code:", style = MaterialTheme.typography.bodySmall)
                SelectionContainer {
                    Text(token.token, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text("Waiting for the client to scan...", style = MaterialTheme.typography.bodySmall)
            }
            viewModel.loading -> CircularProgressIndicator()
        }

        viewModel.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { viewModel.issue(deliveryId) }, enabled = !viewModel.loading) {
            Text("Get a new code")
        }
        Text(
            "A new code cancels the old one. Each code works once and expires after 30 minutes.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
