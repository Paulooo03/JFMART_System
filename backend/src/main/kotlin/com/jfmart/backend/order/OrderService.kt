package com.jfmart.backend.order

import com.jfmart.backend.auth.AuthPrincipal
import com.jfmart.backend.auth.Role
import com.jfmart.backend.client.ClientRepository
import com.jfmart.backend.delivery.ACTIVE_DELIVERY_STATUSES
import com.jfmart.backend.delivery.DeliveryRepository
import com.jfmart.backend.delivery.DeliveryStatus
import com.jfmart.backend.product.ProductRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val clientRepository: ClientRepository,
    private val deliveryRepository: DeliveryRepository
) {

    // ---- client ----

    @Transactional
    fun create(principal: AuthPrincipal, request: CreateOrderRequest): OrderResponse {
        val clientId = principal.clientId ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not a client account")
        val order = Order(
            // Placeholder: the real reference number needs the generated id, set below in the same transaction
            referenceNumber = UUID.randomUUID().toString().replace("-", ""),
            clientId = clientId,
            specialInstructions = request.specialInstructions?.takeIf { it.isNotBlank() }
        )
        request.items.forEach { order.addItem(buildItem(it)) }
        order.recalculateTotal()

        val saved = orderRepository.save(order)
        saved.referenceNumber = "JF-%d-%06d".format(saved.createdAt.year, saved.id)
        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun listForClient(principal: AuthPrincipal): List<OrderResponse> {
        val clientId = principal.clientId ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not a client account")
        return toResponses(orderRepository.findByClientIdOrderByCreatedAtDesc(clientId))
    }

    /** Admins can read any order; clients only their own (others look like 404). */
    @Transactional(readOnly = true)
    fun get(principal: AuthPrincipal, id: Long): OrderResponse {
        val order = findOrder(id)
        if (principal.role != Role.ROLE_ADMIN && order.clientId != principal.clientId) {
            throw notFound()
        }
        return toResponse(order)
    }

    @Transactional
    fun cancelByClient(principal: AuthPrincipal, id: Long, reason: String?): OrderResponse {
        val order = findOrder(id)
        if (order.clientId != principal.clientId) throw notFound()
        if (order.status != OrderStatus.PENDING) {
            throw conflict("Only pending orders can be cancelled by the client; contact JF MiniMart")
        }
        cancel(order, reason)
        return toResponse(order)
    }

    // ---- admin ----

    @Transactional(readOnly = true)
    fun listForAdmin(status: OrderStatus?): List<OrderResponse> {
        val orders = if (status == null) orderRepository.findAllByOrderByCreatedAtDesc()
        else orderRepository.findByStatusOrderByCreatedAtDesc(status)
        return toResponses(orders)
    }

    @Transactional
    fun updateItem(orderId: Long, itemId: Long, request: UpdateOrderItemRequest): OrderResponse {
        val order = findPendingOrder(orderId)
        val item = order.items.find { it.id == itemId } ?: throw notFound("Order item not found")
        request.quantity?.let { item.quantity = it }
        request.unitPrice?.let { item.unitPrice = it }
        order.recalculateTotal()
        return toResponse(order)
    }

    @Transactional
    fun removeItem(orderId: Long, itemId: Long): OrderResponse {
        val order = findPendingOrder(orderId)
        if (!order.items.removeIf { it.id == itemId }) throw notFound("Order item not found")
        order.recalculateTotal()
        return toResponse(order)
    }

    @Transactional
    fun confirm(orderId: Long): OrderResponse {
        val order = findPendingOrder(orderId)
        if (order.items.isEmpty()) throw conflict("Order has no items")
        if (order.hasUnpricedItems()) throw conflict("Price every requested item before confirming")
        order.recalculateTotal()
        order.status = OrderStatus.CONFIRMED
        order.confirmedAt = LocalDateTime.now()
        return toResponse(order)
    }

    @Transactional
    fun cancelByAdmin(orderId: Long, reason: String?): OrderResponse {
        val order = findOrder(orderId)
        if (order.status != OrderStatus.PENDING && order.status != OrderStatus.CONFIRMED) {
            throw conflict("Only pending or confirmed orders can be cancelled")
        }
        // A scheduled delivery must not stay on the courier's list for a cancelled order
        deliveryRepository.findByOrderIdAndStatusIn(order.id, ACTIVE_DELIVERY_STATUSES).forEach {
            it.status = DeliveryStatus.UNSUCCESSFUL
            it.unsuccessfulReason = "Order cancelled"
            it.qrExpiresAt = null
        }
        cancel(order, reason)
        return toResponse(order)
    }

    // ---- helpers ----

    private fun buildItem(request: OrderItemRequest): OrderItem {
        val instructions = request.instructions?.takeIf { it.isNotBlank() }
        if (request.productId != null) {
            if (!request.customItemName.isNullOrBlank()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Give either productId or customItemName, not both")
            }
            val product = productRepository.findById(request.productId)
                .filter { it.active }
                .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Product ${request.productId} is not available") }
            return OrderItem(
                productId = product.id,
                itemName = product.name,
                quantity = request.quantity,
                unit = product.unit,
                unitPrice = product.price,
                instructions = instructions
            )
        }
        if (request.customItemName.isNullOrBlank() || request.unit.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A requested item needs customItemName and unit")
        }
        return OrderItem(
            itemName = request.customItemName.trim(),
            customRequest = true,
            quantity = request.quantity,
            unit = request.unit.trim(),
            unitPrice = null,
            instructions = instructions
        )
    }

    private fun cancel(order: Order, reason: String?) {
        order.status = OrderStatus.CANCELLED
        order.cancelledReason = reason?.takeIf { it.isNotBlank() }
    }

    private fun findOrder(id: Long): Order =
        orderRepository.findById(id).orElseThrow { notFound() }

    private fun findPendingOrder(id: Long): Order {
        val order = findOrder(id)
        if (order.status != OrderStatus.PENDING) throw conflict("Only pending orders can be edited")
        return order
    }

    private fun notFound(message: String = "Order not found") = ResponseStatusException(HttpStatus.NOT_FOUND, message)

    private fun conflict(message: String) = ResponseStatusException(HttpStatus.CONFLICT, message)

    private fun toResponses(orders: List<Order>): List<OrderResponse> {
        val names = clientRepository.findAllById(orders.map { it.clientId }.toSet()).associate { it.id to it.hospitalName }
        return orders.map { toResponse(it, names[it.clientId]) }
    }

    private fun toResponse(order: Order): OrderResponse =
        toResponse(order, clientRepository.findById(order.clientId).map { it.hospitalName }.orElse(null))

    private fun toResponse(order: Order, clientName: String?) = OrderResponse(
        id = order.id,
        referenceNumber = order.referenceNumber,
        status = order.status,
        clientId = order.clientId,
        clientName = clientName,
        totalAmount = order.totalAmount,
        hasUnpricedItems = order.hasUnpricedItems(),
        specialInstructions = order.specialInstructions,
        createdAt = order.createdAt,
        confirmedAt = order.confirmedAt,
        deliveredAt = order.deliveredAt,
        cancelledReason = order.cancelledReason,
        items = order.items.map {
            OrderItemResponse(
                id = it.id,
                productId = it.productId,
                name = it.itemName,
                customRequest = it.customRequest,
                quantity = it.quantity,
                unit = it.unit,
                unitPrice = it.unitPrice,
                lineTotal = it.lineTotal(),
                instructions = it.instructions
            )
        }
    )
}
