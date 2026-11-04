package com.jfmart.JFMART.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfmart.JFMART.data.remote.Order
import com.jfmart.JFMART.ui.components.StatusChip
import com.jfmart.JFMART.ui.components.displayDate
import com.jfmart.JFMART.ui.components.formatMoney

@Composable
fun OrderCard(order: Order, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.referenceNumber, style = MaterialTheme.typography.titleMedium)
                StatusChip(order.status)
            }
            Text("Ordered ${displayDate(order.createdAt)}", style = MaterialTheme.typography.bodySmall)
            order.deliveredAt?.let { Text("Delivered ${displayDate(it)}", style = MaterialTheme.typography.bodySmall) }
            Text(
                "${order.items.size} item(s) - ${formatMoney(order.totalAmount)}" +
                    if (order.hasUnpricedItems) " (some items not priced yet)" else "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
