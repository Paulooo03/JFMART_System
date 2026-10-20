package com.jfmart.repository

import com.jfmart.entity.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long> {

    // SAFE: Spring Data JPA automatically parameterizes derived queries
    fun findByClientId(clientId: Long): List<Order>

    // SAFE: Using named binding (:clientId) prevents SQL injection
    @Query("SELECT o FROM Order o WHERE o.client.id = :clientId AND o.status = :status")
    fun findOrdersByClientAndStatus(
        @Param("clientId") clientId: Long,
        @Param("status") status: String
    ): List<Order>
}