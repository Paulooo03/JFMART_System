package com.jfmart.backend.delivery

import com.jfmart.backend.auth.AuthPrincipal
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/** Client-facing delivery endpoints. */
@RestController
@RequestMapping("/api")
class DeliveryController(private val deliveryService: DeliveryService) {

    @PostMapping("/deliveries/confirm")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    fun confirm(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @Valid @RequestBody request: ConfirmDeliveryRequest
    ): DeliveryConfirmedResponse = deliveryService.confirm(principal, request.token)

    @GetMapping("/orders/{orderId}/deliveries")
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENT', 'ROLE_ADMIN')")
    fun forOrder(@AuthenticationPrincipal principal: AuthPrincipal, @PathVariable orderId: Long): List<DeliveryResponse> =
        deliveryService.forOrder(principal, orderId)
}

/** Scheduling for the admin. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
class AdminDeliveryController(private val deliveryService: DeliveryService) {

    @PostMapping("/orders/{orderId}/delivery")
    @ResponseStatus(HttpStatus.CREATED)
    fun assign(@PathVariable orderId: Long, @Valid @RequestBody request: AssignDeliveryRequest): DeliveryResponse =
        deliveryService.assign(orderId, request)

    @GetMapping("/deliveries")
    fun list(
        @RequestParam(required = false) status: DeliveryStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): List<DeliveryResponse> = deliveryService.listForAdmin(status, date)

    @PutMapping("/deliveries/{id}/schedule")
    fun reschedule(@PathVariable id: Long, @Valid @RequestBody request: RescheduleRequest): DeliveryResponse =
        deliveryService.reschedule(id, request)
}

/** The courier's own deliveries. Access is restricted to ROLE_DELIVERY in SecurityConfig. */
@RestController
@RequestMapping("/api/courier/deliveries")
class CourierDeliveryController(private val deliveryService: DeliveryService) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @RequestParam(required = false) status: DeliveryStatus?
    ): List<CourierDeliveryResponse> = deliveryService.listForCourier(principal, status)

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal principal: AuthPrincipal, @PathVariable id: Long): CourierDeliveryResponse =
        deliveryService.getForCourier(principal, id)

    @PostMapping("/{id}/preparing")
    fun preparing(@AuthenticationPrincipal principal: AuthPrincipal, @PathVariable id: Long): CourierDeliveryResponse =
        deliveryService.markPreparing(principal, id)

    @PostMapping("/{id}/out-for-delivery")
    fun outForDelivery(@AuthenticationPrincipal principal: AuthPrincipal, @PathVariable id: Long): CourierDeliveryResponse =
        deliveryService.markOutForDelivery(principal, id)

    // Each call issues a new single-use token that the app shows as a QR code
    @PostMapping("/{id}/qr")
    fun qr(@AuthenticationPrincipal principal: AuthPrincipal, @PathVariable id: Long): QrResponse =
        deliveryService.issueQr(principal, id)

    @PostMapping("/{id}/unsuccessful")
    fun unsuccessful(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: UnsuccessfulRequest
    ): CourierDeliveryResponse = deliveryService.markUnsuccessful(principal, id, request.reason)
}
