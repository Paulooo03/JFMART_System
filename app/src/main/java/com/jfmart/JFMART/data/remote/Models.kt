package com.jfmart.JFMART.data.remote

import java.math.BigDecimal

// JSON shapes of the backend API. Dates and timestamps stay ISO strings (yyyy-MM-dd, yyyy-MM-ddTHH:mm:ss)
// because java.time needs API 26 and the app supports API 24.

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(val token: String, val role: String, val userId: Long, val clientId: Long?)

data class SignUpRequest(
    val username: String,
    val password: String,
    val hospitalName: String,
    val contactPerson: String,
    val phoneNumber: String,
    val email: String,
    val deliveryAddress: String,
    val department: String?
)

data class ErrorBody(val message: String?)

data class Product(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val unit: String,
    val active: Boolean
)

/** Set productId for a listed product, or customItemName + unit for an item that is not listed. */
data class OrderItemRequest(
    val productId: Long? = null,
    val customItemName: String? = null,
    val quantity: Int,
    val unit: String? = null,
    val instructions: String? = null
)

data class CreateOrderRequest(val items: List<OrderItemRequest>, val specialInstructions: String?)

data class CancelRequest(val reason: String?)

data class OrderItem(
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

data class Order(
    val id: Long,
    val referenceNumber: String,
    val status: String,
    val clientId: Long,
    val clientName: String?,
    val totalAmount: BigDecimal?,
    val hasUnpricedItems: Boolean,
    val specialInstructions: String?,
    val createdAt: String,
    val confirmedAt: String?,
    val deliveredAt: String?,
    val cancelledReason: String?,
    val items: List<OrderItem>
)

data class DeliveryInfo(
    val id: Long,
    val orderId: Long,
    val referenceNumber: String,
    val clientName: String?,
    val status: String,
    val scheduledDate: String,
    val deliveryLocation: String,
    val deliveredAt: String?,
    val unsuccessfulReason: String?,
    val deliveryPersonId: Long
)

data class Payment(
    val id: Long,
    val invoiceNumber: Long,
    val amountPaid: BigDecimal,
    val paymentMethod: String,
    val paymentDate: String,
    val referenceNumber: String?
)

data class Invoice(
    val invoiceNumber: Long,
    val salesInvoiceNumber: String?,
    val orderId: Long,
    val orderReference: String?,
    val clientId: Long,
    val clientName: String?,
    val startDate: String,
    val dueDate: String,
    val amount: BigDecimal,
    val amountPaid: BigDecimal,
    val balance: BigDecimal,
    val paymentStatus: String,
    val overdue: Boolean,
    val payments: List<Payment>
)

data class Statement(
    val clientId: Long,
    val clientName: String,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val outstandingBalance: BigDecimal,
    val overdueBalance: BigDecimal,
    val nextDueDate: String?,
    val invoices: List<Invoice>
)

data class ClientProfile(
    val clientId: Long,
    val username: String?,
    val hospitalName: String,
    val contactPerson: String,
    val phoneNumber: String,
    val email: String,
    val deliveryAddress: String,
    val department: String?
)

data class CourierItem(
    val name: String,
    val quantity: Int,
    val unit: String,
    val instructions: String?
)

/** A delivery as the courier sees it: where to go and what to bring, without prices. */
data class CourierDelivery(
    val id: Long,
    val orderId: Long,
    val referenceNumber: String,
    val clientName: String?,
    val contactPerson: String?,
    val phoneNumber: String?,
    val deliveryLocation: String,
    val scheduledDate: String,
    val status: String,
    val unsuccessfulReason: String?,
    val deliveredAt: String?,
    val specialInstructions: String?,
    val items: List<CourierItem>
)

data class QrToken(val token: String, val expiresAt: String)

data class UnsuccessfulRequest(val reason: String)

data class ConfirmDeliveryRequest(val token: String)

data class DeliveryConfirmed(val orderId: Long, val referenceNumber: String, val deliveredAt: String)

data class UpdateProfileRequest(
    val contactPerson: String,
    val phoneNumber: String,
    val email: String,
    val deliveryAddress: String,
    val department: String?
)
