package com.jfmart.backend.order

import com.jfmart.backend.auth.AuthPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    fun create(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @Valid @RequestBody request: CreateOrderRequest
    ): OrderResponse = orderService.create(principal, request)

    // The client id comes from the token, never from the URL, so a client can only see their own orders
    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    fun mine(@AuthenticationPrincipal principal: AuthPrincipal): List<OrderResponse> =
        orderService.listForClient(principal)

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENT', 'ROLE_ADMIN')")
    fun get(@AuthenticationPrincipal principal: AuthPrincipal, @PathVariable id: Long): OrderResponse =
        orderService.get(principal, id)

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    fun cancel(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody(required = false) request: CancelOrderRequest?
    ): OrderResponse = orderService.cancelByClient(principal, id, request?.reason)
}

/** Order review for the admin. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/orders")
class AdminOrderController(private val orderService: OrderService) {

    @GetMapping
    fun list(
        @RequestParam(required = false) status: OrderStatus?,
        @RequestParam(required = false) clientId: Long?
    ): List<OrderResponse> = orderService.listForAdmin(status, clientId)

    @PutMapping("/{orderId}/items/{itemId}")
    fun updateItem(
        @PathVariable orderId: Long,
        @PathVariable itemId: Long,
        @Valid @RequestBody request: UpdateOrderItemRequest
    ): OrderResponse = orderService.updateItem(orderId, itemId, request)

    @DeleteMapping("/{orderId}/items/{itemId}")
    fun removeItem(@PathVariable orderId: Long, @PathVariable itemId: Long): OrderResponse =
        orderService.removeItem(orderId, itemId)

    @PostMapping("/{orderId}/confirm")
    fun confirm(@PathVariable orderId: Long): OrderResponse = orderService.confirm(orderId)

    @PostMapping("/{orderId}/cancel")
    fun cancel(
        @PathVariable orderId: Long,
        @Valid @RequestBody(required = false) request: CancelOrderRequest?
    ): OrderResponse = orderService.cancelByAdmin(orderId, request?.reason)
}
