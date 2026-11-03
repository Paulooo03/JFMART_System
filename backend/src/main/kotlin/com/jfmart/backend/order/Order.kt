package com.jfmart.backend.order

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

enum class OrderStatus { PENDING, CONFIRMED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED }

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "reference_number", nullable = false, unique = true, length = 32)
    var referenceNumber: String = "",

    @Column(name = "client_id", nullable = false)
    val clientId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 32)
    var status: OrderStatus = OrderStatus.PENDING,

    /** Sum of priced lines; unpriced (custom) lines are excluded until the admin prices them. */
    @Column(name = "total_amount", precision = 15, scale = 2)
    var totalAmount: BigDecimal? = null,

    @Column(name = "special_instructions", length = 512)
    var specialInstructions: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "confirmed_at")
    var confirmedAt: LocalDateTime? = null,

    @Column(name = "delivered_at")
    var deliveredAt: LocalDateTime? = null,

    @Column(name = "cancelled_reason", length = 256)
    var cancelledReason: String? = null
) {
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<OrderItem> = mutableListOf()

    fun addItem(item: OrderItem) {
        item.order = this
        items.add(item)
    }

    fun hasUnpricedItems() = items.any { it.unitPrice == null }

    fun recalculateTotal() {
        totalAmount = items.mapNotNull { it.lineTotal() }.fold(BigDecimal.ZERO, BigDecimal::add)
    }
}

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "product_id")
    val productId: Long? = null,

    /** Name as shown to the client: a snapshot of the product name, or the name typed for a custom request. */
    @Column(name = "custom_item_name", length = 256)
    val itemName: String = "",

    @Column(name = "is_custom_request", nullable = false)
    val customRequest: Boolean = false,

    @Column(nullable = false)
    var quantity: Int = 1,

    @Column(nullable = false, length = 64)
    val unit: String = "",

    /** Null for a custom request until the admin prices it. */
    @Column(name = "unit_price", precision = 15, scale = 2)
    var unitPrice: BigDecimal? = null,

    @Column(length = 512)
    val instructions: String? = null
) {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    lateinit var order: Order

    fun lineTotal(): BigDecimal? = unitPrice?.multiply(BigDecimal(quantity))
}
