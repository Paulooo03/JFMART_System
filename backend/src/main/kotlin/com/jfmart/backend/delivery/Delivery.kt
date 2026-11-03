package com.jfmart.backend.delivery

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

enum class DeliveryStatus { ASSIGNED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, UNSUCCESSFUL }

/** A delivery is "active" until it ends as DELIVERED or UNSUCCESSFUL; an order has at most one active delivery. */
val ACTIVE_DELIVERY_STATUSES = listOf(DeliveryStatus.ASSIGNED, DeliveryStatus.PREPARING, DeliveryStatus.OUT_FOR_DELIVERY)

@Entity
@Table(name = "deliveries")
class Delivery(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "order_id", nullable = false)
    val orderId: Long = 0,

    @Column(name = "delivery_person_id", nullable = false)
    val deliveryPersonId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 32)
    var status: DeliveryStatus = DeliveryStatus.ASSIGNED,

    @Column(name = "scheduled_date", nullable = false)
    var scheduledDate: LocalDate = LocalDate.now(),

    /** Snapshot of the client's delivery address when the delivery was scheduled. */
    @Column(name = "delivery_location", nullable = false, length = 512)
    val deliveryLocation: String = "",

    /** SHA-256 of the current scan token. The token itself is never stored. */
    @Column(name = "qr_code_token", nullable = false, unique = true, length = 128)
    var qrTokenHash: String = "",

    @Column(name = "qr_expires_at")
    var qrExpiresAt: LocalDateTime? = null,

    @Column(name = "qr_used_at")
    var qrUsedAt: LocalDateTime? = null,

    @Column(name = "delivered_at")
    var deliveredAt: LocalDateTime? = null,

    @Column(name = "unsuccessful_reason", length = 256)
    var unsuccessfulReason: String? = null
)
