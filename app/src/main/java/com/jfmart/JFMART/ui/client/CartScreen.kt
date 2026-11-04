package com.jfmart.JFMART.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfmart.JFMART.ui.components.*
import java.math.BigDecimal

@Composable
fun CartScreen(cart: CartViewModel, onBrowse: () -> Unit, onOrderPlaced: (Long) -> Unit) {
    var notes by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Cart", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))

        if (cart.lines.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Your cart is empty.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBrowse) { Text("Browse products") }
            }
            return@Column
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cart.lines, key = { it.id }) { line -> CartLineRow(line, cart) }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Special instructions (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        LabeledValue("Estimated total", formatMoney(cart.estimatedTotal), emphasized = true)
        if (cart.hasRequestedItems) {
            Text(
                "Requested items are not included until JF MiniMart prices them.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        cart.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { cart.submit(notes) { order -> notes = ""; onOrderPlaced(order.id) } },
            enabled = !cart.submitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (cart.submitting) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Submit order")
            }
        }
    }
}

@Composable
private fun CartLineRow(line: CartLine, cart: CartViewModel) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(line.name, style = MaterialTheme.typography.titleMedium)
                val price = line.unitPrice
                if (price == null) {
                    Text("Requested item (${line.unit}) - price to be confirmed", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        "${formatMoney(price)} / ${line.unit} = ${formatMoney(price.multiply(BigDecimal(line.quantity)))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                line.instructions?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            QuantityStepper(line.quantity) { cart.setLineQuantity(line.id, it) }
            IconButton(onClick = { cart.remove(line.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}
