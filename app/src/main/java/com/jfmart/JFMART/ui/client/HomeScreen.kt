package com.jfmart.JFMART.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.data.remote.ApiResult
import com.jfmart.JFMART.data.remote.Order
import com.jfmart.JFMART.data.remote.Statement
import com.jfmart.JFMART.data.remote.apiCall
import com.jfmart.JFMART.ui.components.*
import kotlinx.coroutines.launch

data class HomeData(val orders: List<Order>, val statement: Statement)

class HomeViewModel : ViewModel() {
    var state by mutableStateOf<LoadState<HomeData>>(LoadState.Loading)
        private set

    fun load() {
        viewModelScope.launch {
            if (state !is LoadState.Loaded) state = LoadState.Loading
            val orders = apiCall { AppGraph.api.myOrders() }
            val statement = apiCall { AppGraph.api.statement() }
            state = when {
                orders is ApiResult.Ok && statement is ApiResult.Ok ->
                    LoadState.Loaded(HomeData(orders.value, statement.value))
                orders is ApiResult.Err -> LoadState.Error(orders.message)
                statement is ApiResult.Err -> LoadState.Error(statement.message)
                else -> LoadState.Error("Could not load your account.")
            }
        }
    }
}

private val finishedStatuses = setOf("DELIVERED", "CANCELLED")

@Composable
fun HomeScreen(
    onOpenOrder: (Long) -> Unit,
    onPlaceOrder: () -> Unit,
    onOpenStatement: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Home") {
            IconButton(onClick = { viewModel.load() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        LoadStateView(viewModel.state, onRetry = { viewModel.load() }) { data ->
            val active = data.orders.filter { it.status !in finishedStatuses }
            val recentDelivered = data.orders.filter { it.status == "DELIVERED" }.take(3)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(data.statement.clientName, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    BalanceCard(data.statement, onOpenStatement)
                }
                item {
                    Button(onClick = onPlaceOrder, modifier = Modifier.fillMaxWidth()) { Text("Place an order") }
                }
                item { Text("Active orders", style = MaterialTheme.typography.titleMedium) }
                if (active.isEmpty()) item { Text("No active orders.") }
                items(active, key = { it.id }) { OrderCard(it) { onOpenOrder(it.id) } }

                if (recentDelivered.isNotEmpty()) {
                    item { Text("Recently delivered", style = MaterialTheme.typography.titleMedium) }
                    items(recentDelivered, key = { "done-${it.id}" }) { OrderCard(it) { onOpenOrder(it.id) } }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(statement: Statement, onOpenStatement: () -> Unit) {
    val overdue = statement.overdueBalance.signum() > 0
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Outstanding balance", style = MaterialTheme.typography.labelLarge)
            Text(formatMoney(statement.outstandingBalance), style = MaterialTheme.typography.headlineMedium)
            if (overdue) {
                Text(
                    "Overdue: ${formatMoney(statement.overdueBalance)}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            statement.nextDueDate?.let {
                Text("Next payment due ${displayDate(it)}", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onOpenStatement) { Text("View statement") }
        }
    }
}
