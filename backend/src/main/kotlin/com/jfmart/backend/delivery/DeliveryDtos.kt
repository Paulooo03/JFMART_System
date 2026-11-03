package com.jfmart.backend.delivery

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime

/** `deliveryPersonId` is optional: there is only one courier account, which is used by default. */
data class AssignDeliveryRequest(
    @field:NotNull @field:FutureOrPresent val scheduledDate: LocalDate,
    val deliveryPersonId: Long? = null
)

data class RescheduleRequest(
    @field:NotNull @field:FutureOrPresent val scheduledDate: LocalDate
)

data class UnsuccessfulRequest(
    @field:NotBlank @field:Size(max = 256) val reason: String
)

data class ConfirmDeliveryRequest(
    @field:NotBlank val token: String
)

/** What the admin and the owning client see. Never includes the scan token. */
data class DeliveryResponse(
    val id: Long,
    val orderId: Long,
    val referenceNumber: String,
    val clientName: String?,
    val status: DeliveryStatus,
    val scheduledDate: LocalDate,
    val deliveryLocation: String,
    val deliveredAt: LocalDateTime?,
    val unsuccessfulReason: String?,
    val deliveryPersonId: Long
)

data class CourierItem(
    val name: String,
    val quantity: Int,
    val unit: String,
    val instructions: String?
)

/** What the courier sees: where to go and what to bring, without prices. */
data class CourierDeliveryResponse(
    val id: Long,
    val orderId: Long,
    val referenceNumber: String,
    val clientName: String?,
    val contactPerson: String?,
    val phoneNumber: String?,
    val deliveryLocation: String,
    val scheduledDate: LocalDate,
    val status: DeliveryStatus,
    val unsuccessfulReason: String?,
    val deliveredAt: LocalDateTime?,
    val specialInstructions: String?,
    val items: List<CourierItem>
)

data class QrResponse(
    val token: String,
    val expiresAt: LocalDateTime
)

data class DeliveryConfirmedResponse(
    val orderId: Long,
    val referenceNumber: String,
    val deliveredAt: LocalDateTime
)
