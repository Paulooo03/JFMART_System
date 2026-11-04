package com.jfmart.JFMART.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.data.remote.ApiResult
import com.jfmart.JFMART.data.remote.Order
import com.jfmart.JFMART.data.remote.apiCall
import com.jfmart.JFMART.ui.components.*
import kotlinx.coroutines.launch

class OrdersViewModel : ViewModel() {
    var state by mutableStateOf<LoadState<List<Order>>>(LoadState.Loading)
        private set

    fun load() {
        viewModelScope.launch {
            if (state !is LoadState.Loaded) state = LoadState.Loading
            state = when (val result = apiCall { AppGraph.api.myOrders() }) {
                is ApiResult.Ok -> LoadState.Loaded(result.value)
                is ApiResult.Err -> LoadState.Error(result.message)
            }
        }
    }
}

private enum class OrderFilter(val label: String, val matches: (Order) -> Boolean) {
    All("All", { true }),
    Active("Active", { it.status != "DELIVERED" && it.status != "CANCELLED" }),
    Delivered("Delivered", { it.status == "DELIVERED" }),
    Cancelled("Cancelled", { it.status == "CANCELLED" })
}

/** Order history: reference number, order and delivered dates, items and amount (open an order for the items). */
@Composable
fun OrdersScreen(onOpenOrder: (Long) -> Unit, viewModel: OrdersViewModel = viewModel()) {
    var filter by rememberSaveable { mutableStateOf(OrderFilter.All) }
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("My orders", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrderFilter.entries.forEach {
                FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it.label) })
            }
        }
        Spacer(Modifier.height(8.dp))

        LoadStateView(viewModel.state, onRetry = { viewModel.load() }) { orders ->
            val shown = orders.filter(filter.matches)
            if (shown.isEmpty()) {
                Text("No orders to show.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shown, key = { it.id }) { OrderCard(it) { onOpenOrder(it.id) } }
                }
            }
        }
    }
}
