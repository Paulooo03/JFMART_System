package com.jfmart.backend.order

import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {
    // Derived queries are parameterized by Spring Data, so they are safe from SQL injection
    fun findByClientIdOrderByCreatedAtDesc(clientId: Long): List<Order>
    fun findByStatusOrderByCreatedAtDesc(status: OrderStatus): List<Order>
    fun findAllByOrderByCreatedAtDesc(): List<Order>
    fun countByStatus(status: OrderStatus): Long
}
