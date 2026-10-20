package com.jfmart.controller

import com.jfmart.entity.Order
import com.jfmart.service.OrderService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    // Only Admins can view all orders across all clients
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun getAllOrders(): List<Order> {
        return orderService.findAllOrders()
    }

    // Delivery drivers can only view orders assigned to them
    @GetMapping("/delivery/{driverId}")
    @PreAuthorize("hasAuthority('ROLE_DELIVERY')")
    fun getDeliveryOrders(@PathVariable driverId: Long): List<Order> {
        return orderService.findAssignedDeliveries(driverId)
    }

    // Clients can only access their own order records
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAuthority('ROLE_CLIENT') and #clientId == authentication.principal.id")
    fun getClientOrders(@PathVariable clientId: Long): List<Order> {
        return orderService.findOrdersByClientId(clientId)
    }
}