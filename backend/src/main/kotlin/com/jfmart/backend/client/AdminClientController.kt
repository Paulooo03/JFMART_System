package com.jfmart.backend.client

import com.jfmart.backend.auth.Role
import com.jfmart.backend.auth.UserRepository
import com.jfmart.backend.auth.UserStatus
import com.jfmart.backend.billing.BillingService
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

data class PendingClientResponse(
    val userId: Long,
    val username: String,
    val clientId: Long,
    val hospitalName: String,
    val contactPerson: String,
    val phoneNumber: String,
    val email: String,
    val deliveryAddress: String,
    val department: String?
)

data class ClientSummaryResponse(
    val userId: Long,
    val username: String,
    val accountStatus: UserStatus,
    val clientId: Long,
    val hospitalName: String,
    val contactPerson: String,
    val phoneNumber: String,
    val email: String,
    val deliveryAddress: String,
    val department: String?,
    val outstandingBalance: BigDecimal
)

/** Client management for the admin. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/clients")
class AdminClientController(
    private val userRepository: UserRepository,
    private val clientRepository: ClientRepository,
    private val billingService: BillingService
) {

    /** All client accounts with their outstanding balance, optionally filtered by account status. */
    @GetMapping
    @Transactional(readOnly = true)
    fun list(@RequestParam(required = false) status: UserStatus?): List<ClientSummaryResponse> {
        val users = userRepository.findAll()
            .filter { it.role == Role.ROLE_CLIENT && it.clientId != null && (status == null || it.status == status) }
        val clients = clientRepository.findAllById(users.mapNotNull { it.clientId }).associateBy { it.id }
        val balances = billingService.outstandingByClient()
        return users.mapNotNull { user ->
            val client = clients[user.clientId] ?: return@mapNotNull null
            ClientSummaryResponse(
                userId = user.id,
                username = user.username,
                accountStatus = user.status,
                clientId = client.id,
                hospitalName = client.hospitalName,
                contactPerson = client.contactPerson,
                phoneNumber = client.phoneNumber,
                email = client.email,
                deliveryAddress = client.deliveryAddress,
                department = client.department,
                outstandingBalance = balances[client.id] ?: BigDecimal.ZERO
            )
        }.sortedBy { it.hospitalName.lowercase() }
    }

    @GetMapping("/pending")
    fun pending(): List<PendingClientResponse> {
        val users = userRepository.findByRoleAndStatus(Role.ROLE_CLIENT, UserStatus.PENDING_APPROVAL)
        val clients = clientRepository.findAllById(users.mapNotNull { it.clientId }).associateBy { it.id }
        return users.mapNotNull { user ->
            val client = clients[user.clientId] ?: return@mapNotNull null
            PendingClientResponse(
                userId = user.id,
                username = user.username,
                clientId = client.id,
                hospitalName = client.hospitalName,
                contactPerson = client.contactPerson,
                phoneNumber = client.phoneNumber,
                email = client.email,
                deliveryAddress = client.deliveryAddress,
                department = client.department
            )
        }
    }

    @PostMapping("/{userId}/approve")
    @Transactional
    fun approve(@PathVariable userId: Long) = setStatus(userId, UserStatus.ACTIVE)

    @PostMapping("/{userId}/reject")
    @Transactional
    fun reject(@PathVariable userId: Long) = setStatus(userId, UserStatus.DISABLED)

    private fun setStatus(userId: Long, status: UserStatus) {
        val user = userRepository.findById(userId)
            .filter { it.role == Role.ROLE_CLIENT }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Client account not found") }
        user.status = status
    }
}
