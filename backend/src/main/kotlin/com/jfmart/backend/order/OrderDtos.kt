package com.jfmart.backend.order

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * One requested line. Set `productId` for a listed product (name, unit and price come from the price list),
 * or `customItemName` + `unit` for an item that is not listed (priced later by the admin).
 */
data class OrderItemRequest(
    val productId: Long? = null,
    @field:Size(max = 256) val customItemName: String? = null,
    @field:Min(1) val quantity: Int,
    @field:Size(max = 64) val unit: String? = null,
    @field:Size(max = 512) val instructions: String? = null
)

data class CreateOrderRequest(
    @field:Valid @field:Size(min = 1, max = 200) val items: List<OrderItemRequest>,
    @field:Size(max = 512) val specialInstructions: String? = null
)

data class CancelOrderRequest(
    @field:Size(max = 256) val reason: String? = null
)

/** Admin edit of a line while the order is still PENDING. Send only the fields to change. */
data class UpdateOrderItemRequest(
    @field:Min(1) val quantity: Int? = null,
    @field:DecimalMin("0.00") @field:Digits(integer = 13, fraction = 2) val unitPrice: BigDecimal? = null
)

data class OrderItemResponse(
    val id: Long,
    val productId: Long?,
    val name: String,
    val customRequest: Boolean,
    val quantity: Int,
    val unit: String,
    val unitPrice: BigDecimal?,
    val lineTotal: BigDecimal?,
    val instructions: String?
)

data class OrderResponse(
    val id: Long,
    val referenceNumber: String,
    val status: OrderStatus,
    val clientId: Long,
    val clientName: String?,
    val totalAmount: BigDecimal?,
    val hasUnpricedItems: Boolean,
    val specialInstructions: String?,
    val createdAt: LocalDateTime,
    val confirmedAt: LocalDateTime?,
    val deliveredAt: LocalDateTime?,
    val cancelledReason: String?,
    val items: List<OrderItemResponse>
)
