package com.jfmart.backend.dashboard

import com.jfmart.backend.auth.Role
import com.jfmart.backend.auth.UserRepository
import com.jfmart.backend.auth.UserStatus
import com.jfmart.backend.billing.BillingService
import com.jfmart.backend.delivery.ACTIVE_DELIVERY_STATUSES
import com.jfmart.backend.delivery.DeliveryRepository
import com.jfmart.backend.order.OrderRepository
import com.jfmart.backend.order.OrderStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/** The numbers on the admin's home page. */
data class DashboardResponse(
    val pendingApprovals: Int,
    val pendingOrders: Int,
    /** Confirmed orders that still need a delivery scheduled. */
    val awaitingDelivery: Int,
    val deliveriesToday: Long,
    val overdueClients: Int,
    val overdueBalance: BigDecimal,
    val totalOutstanding: BigDecimal
)

/** Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/dashboard")
class DashboardController(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val deliveryRepository: DeliveryRepository,
    private val billingService: BillingService
) {

    @GetMapping
    @Transactional(readOnly = true)
    fun summary(): DashboardResponse {
        val dues = billingService.duesByClient().values
        val awaitingDelivery = orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.CONFIRMED)
            .count { !deliveryRepository.existsByOrderIdAndStatusIn(it.id, ACTIVE_DELIVERY_STATUSES) }

        return DashboardResponse(
            pendingApprovals = userRepository.findByRoleAndStatus(Role.ROLE_CLIENT, UserStatus.PENDING_APPROVAL).size,
            pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING).toInt(),
            awaitingDelivery = awaitingDelivery,
            deliveriesToday = deliveryRepository.countByScheduledDateAndStatusIn(LocalDate.now(), ACTIVE_DELIVERY_STATUSES),
            overdueClients = dues.count { it.overdue.signum() > 0 },
            overdueBalance = dues.fold(BigDecimal.ZERO) { sum, d -> sum + d.overdue },
            totalOutstanding = dues.fold(BigDecimal.ZERO) { sum, d -> sum + d.outstanding }
        )
    }
}
