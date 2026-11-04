package com.jfmart.JFMART.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.data.remote.ApiResult
import com.jfmart.JFMART.data.remote.Product
import com.jfmart.JFMART.data.remote.apiCall
import com.jfmart.JFMART.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CatalogViewModel : ViewModel() {
    var state by mutableStateOf<LoadState<List<Product>>>(LoadState.Loading)
        private set

    fun load(search: String) {
        viewModelScope.launch {
            if (state !is LoadState.Loaded) state = LoadState.Loading
            state = when (val result = apiCall { AppGraph.api.products(search.trim()) }) {
                is ApiResult.Ok -> LoadState.Loaded(result.value)
                is ApiResult.Err -> LoadState.Error(result.message)
            }
        }
    }
}

/** Current prices with a live estimate. Items JF MiniMart does not list can be requested by name. */
@Composable
fun CatalogScreen(cart: CartViewModel, onViewCart: () -> Unit, viewModel: CatalogViewModel = viewModel()) {
    var query by rememberSaveable { mutableStateOf("") }
    var showRequestDialog by rememberSaveable { mutableStateOf(false) }

    // Reloads when the screen opens and, after a short pause, whenever the search text changes
    LaunchedEffect(query) {
        delay(300)
        viewModel.load(query)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Order", style = MaterialTheme.typography.headlineLarge)
        Text("Current prices", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search products") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        LoadStateView(
            state = viewModel.state,
            onRetry = { viewModel.load(query) },
            modifier = Modifier.weight(1f)
        ) { products ->
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (products.isEmpty()) {
                    item { Text("No products match. You can request an item below.") }
                }
                items(products, key = { it.id }) { product ->
                    ProductRow(product, cart.quantityOf(product.id)) { cart.setQuantity(product, it) }
                }
                item {
                    OutlinedButton(onClick = { showRequestDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Can't find it? Request an item")
                    }
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onViewCart) { Text("View cart (${cart.itemCount})") }
            Column(horizontalAlignment = Alignment.End) {
                Text("Estimated total", style = MaterialTheme.typography.labelMedium)
                Text(formatMoney(cart.estimatedTotal), style = MaterialTheme.typography.titleLarge)
            }
        }
    }

    if (showRequestDialog) {
        RequestItemDialog(
            onDismiss = { showRequestDialog = false },
            onAdd = { name, unit, quantity, instructions ->
                cart.addRequestedItem(name, unit, quantity, instructions)
                showRequestDialog = false
            }
        )
    }
}

@Composable
private fun ProductRow(product: Product, quantity: Int, onQuantityChange: (Int) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text("${formatMoney(product.price)} / ${product.unit}", style = MaterialTheme.typography.bodyMedium)
            }
            if (quantity == 0) {
                Button(onClick = { onQuantityChange(1) }) { Text("Add") }
            } else {
                QuantityStepper(quantity, onQuantityChange)
            }
        }
    }
}

@Composable
private fun RequestItemDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, unit: String, quantity: Int, instructions: String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var instructions by rememberSaveable { mutableStateOf("") }
    val parsedQuantity = quantity.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request an item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("JF MiniMart will price this item when it reviews your order.")
                FormField("Product name", name, { name = it })
                FormField("Quantity", quantity, { quantity = it.filter(Char::isDigit) }, keyboardType = KeyboardType.Number)
                FormField("Preferred unit (kg, box, pcs...)", unit, { unit = it })
                FormField("Other instructions", instructions, { instructions = it }, singleLine = false)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, unit, parsedQuantity, instructions) },
                enabled = name.isNotBlank() && unit.isNotBlank() && parsedQuantity > 0
            ) { Text("Add to cart") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
