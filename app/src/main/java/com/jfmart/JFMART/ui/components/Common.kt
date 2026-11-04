package com.jfmart.JFMART.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.util.Locale

// ---- formatting ----

fun formatMoney(value: BigDecimal?): String =
    if (value == null) "—" else "₱" + String.format(Locale.US, "%,.2f", value)

/** "2026-11-04" from an ISO date or timestamp. */
fun displayDate(value: String?): String = value?.take(10) ?: "—"

/** "2026-11-04 14:30" from an ISO timestamp. */
fun displayDateTime(value: String?): String = value?.replace('T', ' ')?.take(16) ?: "—"

/** "OUT_FOR_DELIVERY" -> "Out for delivery". */
fun prettyStatus(status: String): String =
    status.lowercase(Locale.ROOT).replace('_', ' ').replaceFirstChar { it.uppercase() }

// ---- loading states ----

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Error(val message: String) : LoadState<Nothing>
    data class Loaded<T>(val data: T) : LoadState<T>
}

@Composable
fun <T> LoadStateView(
    state: LoadState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    when (state) {
        is LoadState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is LoadState.Error -> Column(
            modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onRetry) { Text("Try again") }
        }
        is LoadState.Loaded -> content(state.data)
    }
}

// ---- small building blocks ----

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun StatusChip(status: String) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (status) {
        "PENDING", "UNPAID", "ASSIGNED" -> colors.tertiaryContainer to colors.onTertiaryContainer
        "CONFIRMED", "PREPARING", "PARTIALLY_PAID" -> colors.secondaryContainer to colors.onSecondaryContainer
        "OUT_FOR_DELIVERY" -> colors.primaryContainer to colors.onPrimaryContainer
        "DELIVERED", "PAID" -> colors.primary to colors.onPrimary
        else -> colors.errorContainer to colors.onErrorContainer // CANCELLED, UNSUCCESSFUL
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            prettyStatus(status),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(quantity - 1) }) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
        }
        Text(
            quantity.toString(),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 28.dp)
        )
        IconButton(onClick = { onChange(quantity + 1) }) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun LabeledValue(label: String, value: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        )
    }
}
