package com.jfmart.JFMART.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.data.remote.ApiResult
import com.jfmart.JFMART.data.remote.Invoice
import com.jfmart.JFMART.data.remote.Statement
import com.jfmart.JFMART.data.remote.apiCall
import com.jfmart.JFMART.ui.components.*
import kotlinx.coroutines.launch

class StatementViewModel : ViewModel() {
    var state by mutableStateOf<LoadState<Statement>>(LoadState.Loading)
        private set

    fun load() {
        viewModelScope.launch {
            if (state !is LoadState.Loaded) state = LoadState.Loading
            state = when (val result = apiCall { AppGraph.api.statement() }) {
                is ApiResult.Ok -> LoadState.Loaded(result.value)
                is ApiResult.Err -> LoadState.Error(result.message)
            }
        }
    }
}

/** What the client owes and has paid. Payment is due within 30 days of delivery. */
@Composable
fun StatementScreen(onBack: () -> Unit, viewModel: StatementViewModel = viewModel()) {
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Statement", onBack = onBack)
        LoadStateView(viewModel.state, onRetry = { viewModel.load() }) { statement ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LabeledValue("Total invoiced", formatMoney(statement.totalInvoiced))
                            LabeledValue("Total paid", formatMoney(statement.totalPaid))
                            LabeledValue("Outstanding balance", formatMoney(statement.outstandingBalance), emphasized = true)
                            if (statement.overdueBalance.signum() > 0) {
                                Text(
                                    "Overdue: ${formatMoney(statement.overdueBalance)}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                item { Text("Invoices", style = MaterialTheme.typography.titleMedium) }
                if (statement.invoices.isEmpty()) {
                    item { Text("No invoices yet. An invoice is created when an order is delivered.") }
                }
                items(statement.invoices, key = { it.invoiceNumber }) { InvoiceCard(it) }
            }
        }
    }
}

@Composable
private fun InvoiceCard(invoice: Invoice) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    invoice.salesInvoiceNumber?.let { "Invoice $it" } ?: "Order ${invoice.orderReference ?: invoice.orderId}",
                    style = MaterialTheme.typography.titleMedium
                )
                StatusChip(invoice.paymentStatus)
            }
            Text("Order ${invoice.orderReference ?: invoice.orderId} - delivered ${displayDate(invoice.startDate)}", style = MaterialTheme.typography.bodySmall)
            LabeledValue("Amount", formatMoney(invoice.amount))
            LabeledValue("Paid", formatMoney(invoice.amountPaid))
            LabeledValue("Balance", formatMoney(invoice.balance))
            Text(
                "Due ${displayDate(invoice.dueDate)}" + if (invoice.overdue) " - overdue" else "",
                color = if (invoice.overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            invoice.payments.forEach {
                Text(
                    "Paid ${formatMoney(it.amountPaid)} on ${displayDate(it.paymentDate)} (${it.paymentMethod})",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
