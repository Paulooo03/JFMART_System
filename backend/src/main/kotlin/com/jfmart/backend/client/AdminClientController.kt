package com.jfmart.backend.client

import com.jfmart.backend.auth.Role
import com.jfmart.backend.auth.UserRepository
import com.jfmart.backend.auth.UserStatus
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

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

/** Approval queue for self sign-ups. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/clients")
class AdminClientController(
    private val userRepository: UserRepository,
    private val clientRepository: ClientRepository
) {

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
