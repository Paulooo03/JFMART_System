package com.jfmart.backend.product

import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long> {
    fun findByActiveTrueAndNameContainingIgnoreCaseOrderByName(name: String): List<Product>
    fun findByNameContainingIgnoreCaseOrderByName(name: String): List<Product>
}
