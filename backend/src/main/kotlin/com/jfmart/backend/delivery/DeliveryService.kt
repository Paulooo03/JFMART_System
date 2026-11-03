package com.jfmart.backend.delivery

import com.jfmart.backend.auth.AuthPrincipal
import com.jfmart.backend.auth.Role
import com.jfmart.backend.auth.User
import com.jfmart.backend.auth.UserRepository
import com.jfmart.backend.auth.UserStatus
import com.jfmart.backend.billing.BillingService
import com.jfmart.backend.client.ClientRepository
import com.jfmart.backend.order.Order
import com.jfmart.backend.order.OrderRepository
import com.jfmart.backend.order.OrderStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Base64
import java.util.HexFormat
import java.util.UUID

@Service
class DeliveryService(
    private val deliveryRepository: DeliveryRepository,
    private val orderRepository: OrderRepository,
    private val clientRepository: ClientRepository,
    private val userRepository: UserRepository,
    private val billingService: BillingService,
    @Value("\${jfmart.qr.expiry-minutes}") private val qrExpiryMinutes: Long
) {
    private val random = SecureRandom()

    // ---- admin ----

    @Transactional
    fun assign(orderId: Long, request: AssignDeliveryRequest): DeliveryResponse {
        val order = orderRepository.findById(orderId).orElseThrow { notFound("Order not found") }
        if (order.status != OrderStatus.CONFIRMED) {
            throw conflict("Only confirmed orders can be scheduled for delivery")
        }
        if (deliveryRepository.existsByOrderIdAndStatusIn(orderId, ACTIVE_DELIVERY_STATUSES)) {
            throw conflict("This order already has an active delivery")
        }
        val courier = resolveCourier(request.deliveryPersonId)
        val client = clientRepository.findById(order.clientId).orElseThrow { notFound("Client not found") }

        val delivery = deliveryRepository.save(
            Delivery(
                orderId = order.id,
                deliveryPersonId = courier.id,
                scheduledDate = request.scheduledDate,
                deliveryLocation = client.deliveryAddress,
                // Placeholder until the courier asks for a QR code; real hashes are 64 hex characters
                qrTokenHash = UUID.randomUUID().toString().replace("-", "")
            )
        )
        return toResponses(listOf(delivery)).single()
    }

    @Transactional(readOnly = true)
    fun listForAdmin(status: DeliveryStatus?, date: LocalDate?): List<DeliveryResponse> {
        val deliveries = deliveryRepository.findAllByOrderByScheduledDateDesc()
            .filter { (status == null || it.status == status) && (date == null || it.scheduledDate == date) }
        return toResponses(deliveries)
    }

    @Transactional
    fun reschedule(id: Long, request: RescheduleRequest): DeliveryResponse {
        val delivery = deliveryRepository.findById(id).orElseThrow { notFound("Delivery not found") }
        if (delivery.status != DeliveryStatus.ASSIGNED) {
            throw conflict("Only deliveries that have not started can be rescheduled")
        }
        delivery.scheduledDate = request.scheduledDate
        return toResponses(listOf(delivery)).single()
    }

    // ---- client ----

    /** Delivery attempts for an order, newest first. Admins see any order; clients only their own. */
    @Transactional(readOnly = true)
    fun forOrder(principal: AuthPrincipal, orderId: Long): List<DeliveryResponse> {
        val order = orderRepository.findById(orderId).orElseThrow { notFound("Order not found") }
        if (principal.role != Role.ROLE_ADMIN && order.clientId != principal.clientId) throw notFound("Order not found")
        return toResponses(deliveryRepository.findByOrderIdOrderByIdDesc(orderId))
    }

    /** The client scans the courier's QR code; a valid scan completes the delivery and the order. */
    @Transactional
    fun confirm(principal: AuthPrincipal, token: String): DeliveryConfirmedResponse {
        // Every failure looks the same so a bad token reveals nothing about why it was rejected
        val invalid = ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired QR code")
        val delivery = deliveryRepository.findByQrTokenHash(hash(token.trim())) ?: throw invalid
        val order = orderRepository.findById(delivery.orderId).orElseThrow { invalid }
        val now = LocalDateTime.now()
        val expiresAt = delivery.qrExpiresAt

        if (order.clientId != principal.clientId ||
            delivery.status != DeliveryStatus.OUT_FOR_DELIVERY ||
            delivery.qrUsedAt != null ||
            expiresAt == null || expiresAt.isBefore(now)
        ) throw invalid

        delivery.status = DeliveryStatus.DELIVERED
        delivery.qrUsedAt = now
        delivery.deliveredAt = now
        order.status = OrderStatus.DELIVERED
        order.deliveredAt = now
        billingService.createInvoiceForDeliveredOrder(order, now.toLocalDate())
        return DeliveryConfirmedResponse(order.id, order.referenceNumber, now)
    }

    // ---- courier ----

    @Transactional(readOnly = true)
    fun listForCourier(principal: AuthPrincipal, status: DeliveryStatus?): List<CourierDeliveryResponse> =
        deliveryRepository.findByDeliveryPersonIdOrderByScheduledDateAsc(principal.id)
            .filter { status == null || it.status == status }
            .map { toCourierResponse(it) }

    @Transactional(readOnly = true)
    fun getForCourier(principal: AuthPrincipal, id: Long): CourierDeliveryResponse =
        toCourierResponse(ownDelivery(principal, id))

    @Transactional
    fun markPreparing(principal: AuthPrincipal, id: Long): CourierDeliveryResponse {
        val delivery = ownDelivery(principal, id)
        requireStatus(delivery, DeliveryStatus.ASSIGNED)
        delivery.status = DeliveryStatus.PREPARING
        orderOf(delivery).status = OrderStatus.PREPARING
        return toCourierResponse(delivery)
    }

    @Transactional
    fun markOutForDelivery(principal: AuthPrincipal, id: Long): CourierDeliveryResponse {
        val delivery = ownDelivery(principal, id)
        requireStatus(delivery, DeliveryStatus.PREPARING)
        delivery.status = DeliveryStatus.OUT_FOR_DELIVERY
        orderOf(delivery).status = OrderStatus.OUT_FOR_DELIVERY
        return toCourierResponse(delivery)
    }

    /** Issues a fresh single-use QR token, replacing any earlier one. Only the hash is stored. */
    @Transactional
    fun issueQr(principal: AuthPrincipal, id: Long): QrResponse {
        val delivery = ownDelivery(principal, id)
        requireStatus(delivery, DeliveryStatus.OUT_FOR_DELIVERY)
        val bytes = ByteArray(32).also { random.nextBytes(it) }
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val expiresAt = LocalDateTime.now().plusMinutes(qrExpiryMinutes)
        delivery.qrTokenHash = hash(token)
        delivery.qrExpiresAt = expiresAt
        delivery.qrUsedAt = null
        return QrResponse(token, expiresAt)
    }

    /** The order goes back to CONFIRMED so the admin can schedule another attempt. */
    @Transactional
    fun markUnsuccessful(principal: AuthPrincipal, id: Long, reason: String): CourierDeliveryResponse {
        val delivery = ownDelivery(principal, id)
        if (delivery.status != DeliveryStatus.PREPARING && delivery.status != DeliveryStatus.OUT_FOR_DELIVERY) {
            throw conflict("Only a delivery that is being prepared or is out for delivery can be marked unsuccessful")
        }
        delivery.status = DeliveryStatus.UNSUCCESSFUL
        delivery.unsuccessfulReason = reason.trim()
        delivery.qrExpiresAt = null
        orderOf(delivery).status = OrderStatus.CONFIRMED
        return toCourierResponse(delivery)
    }

    // ---- helpers ----

    private fun resolveCourier(requestedId: Long?): User {
        if (requestedId != null) {
            return userRepository.findById(requestedId)
                .filter { it.role == Role.ROLE_DELIVERY && it.status == UserStatus.ACTIVE }
                .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "No active courier with that id") }
        }
        return userRepository.findByRoleAndStatus(Role.ROLE_DELIVERY, UserStatus.ACTIVE).firstOrNull()
            ?: throw conflict("No courier account exists yet")
    }

    // Another courier's delivery looks like it does not exist
    private fun ownDelivery(principal: AuthPrincipal, id: Long): Delivery =
        deliveryRepository.findById(id)
            .filter { it.deliveryPersonId == principal.id }
            .orElseThrow { notFound("Delivery not found") }

    private fun orderOf(delivery: Delivery): Order =
        orderRepository.findById(delivery.orderId).orElseThrow { notFound("Order not found") }

    private fun requireStatus(delivery: Delivery, expected: DeliveryStatus) {
        if (delivery.status != expected) {
            throw conflict("Delivery is ${delivery.status}; this step needs it to be $expected")
        }
    }

    private fun hash(token: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))

    private fun notFound(message: String) = ResponseStatusException(HttpStatus.NOT_FOUND, message)

    private fun conflict(message: String) = ResponseStatusException(HttpStatus.CONFLICT, message)

    private fun toResponses(deliveries: List<Delivery>): List<DeliveryResponse> {
        val orders = orderRepository.findAllById(deliveries.map { it.orderId }.toSet()).associateBy { it.id }
        val names = clientRepository.findAllById(orders.values.map { it.clientId }.toSet())
            .associate { it.id to it.hospitalName }
        return deliveries.map { d ->
            val order = orders[d.orderId]
            DeliveryResponse(
                id = d.id,
                orderId = d.orderId,
                referenceNumber = order?.referenceNumber ?: "",
                clientName = order?.let { names[it.clientId] },
                status = d.status,
                scheduledDate = d.scheduledDate,
                deliveryLocation = d.deliveryLocation,
                deliveredAt = d.deliveredAt,
                unsuccessfulReason = d.unsuccessfulReason,
                deliveryPersonId = d.deliveryPersonId
            )
        }
    }

    private fun toCourierResponse(delivery: Delivery): CourierDeliveryResponse {
        val order = orderOf(delivery)
        val client = clientRepository.findById(order.clientId).orElse(null)
        return CourierDeliveryResponse(
            id = delivery.id,
            orderId = order.id,
            referenceNumber = order.referenceNumber,
            clientName = client?.hospitalName,
            contactPerson = client?.contactPerson,
            phoneNumber = client?.phoneNumber,
            deliveryLocation = delivery.deliveryLocation,
            scheduledDate = delivery.scheduledDate,
            status = delivery.status,
            unsuccessfulReason = delivery.unsuccessfulReason,
            deliveredAt = delivery.deliveredAt,
            specialInstructions = order.specialInstructions,
            items = order.items.map { CourierItem(it.itemName, it.quantity, it.unit, it.instructions) }
        )
    }
}
