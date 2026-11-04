package com.jfmart.JFMART.ui.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.data.remote.ApiResult
import com.jfmart.JFMART.data.remote.CreateOrderRequest
import com.jfmart.JFMART.data.remote.Order
import com.jfmart.JFMART.data.remote.OrderItemRequest
import com.jfmart.JFMART.data.remote.Product
import com.jfmart.JFMART.data.remote.apiCall
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** A listed product has a productId and a price; a requested (unlisted) item has neither until the admin prices it. */
data class CartLine(
    val id: String,
    val productId: Long?,
    val name: String,
    val unit: String,
    val unitPrice: BigDecimal?,
    val quantity: Int,
    val instructions: String?
)

/** The cart is shared by the catalog and cart screens, so it lives at the activity level. */
class CartViewModel : ViewModel() {
    val lines = mutableStateListOf<CartLine>()

    var submitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val itemCount: Int get() = lines.size

    /** Priced lines only; requested items are left out until the admin prices them. */
    val estimatedTotal: BigDecimal
        get() = lines.fold(BigDecimal.ZERO) { sum, line ->
            val price = line.unitPrice
            if (price == null) sum else sum + price.multiply(BigDecimal(line.quantity))
        }

    val hasRequestedItems: Boolean get() = lines.any { it.productId == null }

    fun quantityOf(productId: Long): Int = lines.find { it.productId == productId }?.quantity ?: 0

    fun setQuantity(product: Product, quantity: Int) {
        val index = lines.indexOfFirst { it.productId == product.id }
        when {
            quantity <= 0 -> if (index >= 0) lines.removeAt(index)
            index >= 0 -> lines[index] = lines[index].copy(quantity = quantity)
            else -> lines.add(
                CartLine("product-${product.id}", product.id, product.name, product.unit, product.price, quantity, null)
            )
        }
    }

    fun addRequestedItem(name: String, unit: String, quantity: Int, instructions: String?) {
        lines.add(
            CartLine(
                id = "request-${System.nanoTime()}",
                productId = null,
                name = name.trim(),
                unit = unit.trim(),
                unitPrice = null,
                quantity = quantity,
                instructions = instructions?.trim()?.ifEmpty { null }
            )
        )
    }

    fun setLineQuantity(id: String, quantity: Int) {
        val index = lines.indexOfFirst { it.id == id }
        if (index < 0) return
        if (quantity <= 0) lines.removeAt(index) else lines[index] = lines[index].copy(quantity = quantity)
    }

    fun remove(id: String) {
        lines.removeAll { it.id == id }
    }

    fun clear() {
        lines.clear()
        error = null
    }

    fun submit(specialInstructions: String, onSuccess: (Order) -> Unit) {
        if (lines.isEmpty() || submitting) return
        val request = CreateOrderRequest(
            items = lines.map {
                if (it.productId != null) {
                    OrderItemRequest(productId = it.productId, quantity = it.quantity, instructions = it.instructions)
                } else {
                    OrderItemRequest(
                        customItemName = it.name,
                        quantity = it.quantity,
                        unit = it.unit,
                        instructions = it.instructions
                    )
                }
            },
            specialInstructions = specialInstructions.trim().ifEmpty { null }
        )
        viewModelScope.launch {
            submitting = true
            error = null
            when (val result = apiCall { AppGraph.api.createOrder(request) }) {
                is ApiResult.Ok -> {
                    lines.clear()
                    onSuccess(result.value)
                }
                is ApiResult.Err -> error = result.message
            }
            submitting = false
        }
    }
}
