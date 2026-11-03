package com.jfmart.backend.product

import jakarta.persistence.*
import java.math.BigDecimal

/** A line in the price list. Stock is not tracked tightly, so quantity/last_shelved are not mapped. */
@Entity
@Table(name = "products")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 256)
    var name: String = "",

    @Column(nullable = false, precision = 15, scale = 2)
    var price: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, length = 64)
    var unit: String = "",

    @Column(name = "is_active", nullable = false)
    var active: Boolean = true
)
