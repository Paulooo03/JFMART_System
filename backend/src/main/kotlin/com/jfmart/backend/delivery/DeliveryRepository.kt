package com.jfmart.backend.delivery

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.time.LocalDate

interface DeliveryRepository : JpaRepository<Delivery, Long> {
    fun findAllByOrderByScheduledDateDesc(): List<Delivery>
    fun findByDeliveryPersonIdOrderByScheduledDateAsc(deliveryPersonId: Long): List<Delivery>
    fun findByOrderIdOrderByIdDesc(orderId: Long): List<Delivery>
    fun countByScheduledDateAndStatusIn(scheduledDate: LocalDate, statuses: Collection<DeliveryStatus>): Long
    fun existsByOrderIdAndStatusIn(orderId: Long, statuses: Collection<DeliveryStatus>): Boolean
    fun findByOrderIdAndStatusIn(orderId: Long, statuses: Collection<DeliveryStatus>): List<Delivery>

    // Row lock so one QR token can only be redeemed once, even if scanned twice at the same moment
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByQrTokenHash(qrTokenHash: String): Delivery?
}
