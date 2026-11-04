package com.jfmart.backend.client

import com.jfmart.backend.auth.AuthPrincipal
import com.jfmart.backend.auth.UserRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

data class ClientProfileResponse(
    val clientId: Long,
    val username: String?,
    val hospitalName: String,
    val contactPerson: String,
    val phoneNumber: String,
    val email: String,
    val deliveryAddress: String,
    val department: String?
)

/** What a client may change about themselves; the hospital name is changed by the admin only. */
data class UpdateClientProfileRequest(
    @field:NotBlank @field:Size(max = 256) val contactPerson: String,
    @field:NotBlank @field:Size(max = 32) val phoneNumber: String,
    @field:NotBlank @field:Email @field:Size(max = 256) val email: String,
    @field:NotBlank @field:Size(max = 512) val deliveryAddress: String,
    @field:Size(max = 256) val department: String? = null
)

data class AdminUpdateClientRequest(
    @field:NotBlank @field:Size(max = 256) val hospitalName: String,
    @field:NotBlank @field:Size(max = 256) val contactPerson: String,
    @field:NotBlank @field:Size(max = 32) val phoneNumber: String,
    @field:NotBlank @field:Email @field:Size(max = 256) val email: String,
    @field:NotBlank @field:Size(max = 512) val deliveryAddress: String,
    @field:Size(max = 256) val department: String? = null
)

private fun Client.toProfile(username: String?) =
    ClientProfileResponse(id, username, hospitalName, contactPerson, phoneNumber, email, deliveryAddress, department)

/** The signed-in client's own details. The client id comes from the token, never from the URL. */
@RestController
@RequestMapping("/api/clients/me")
class ClientProfileController(
    private val clientRepository: ClientRepository
) {

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    @Transactional(readOnly = true)
    fun get(@AuthenticationPrincipal principal: AuthPrincipal): ClientProfileResponse =
        findClient(principal).toProfile(principal.username)

    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    @Transactional
    fun update(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @Valid @RequestBody request: UpdateClientProfileRequest
    ): ClientProfileResponse {
        val client = findClient(principal)
        client.contactPerson = request.contactPerson.trim()
        client.phoneNumber = request.phoneNumber.trim()
        client.email = request.email.trim()
        client.deliveryAddress = request.deliveryAddress.trim()
        client.department = request.department?.trim()?.takeIf { it.isNotEmpty() }
        return client.toProfile(principal.username)
    }

    private fun findClient(principal: AuthPrincipal): Client {
        val clientId = principal.clientId ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not a client account")
        return clientRepository.findById(clientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found") }
    }
}

/** Admin edits to any client's details. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/clients")
class AdminClientProfileController(
    private val clientRepository: ClientRepository,
    private val userRepository: UserRepository
) {

    @PutMapping("/{clientId}")
    @Transactional
    fun update(
        @PathVariable clientId: Long,
        @Valid @RequestBody request: AdminUpdateClientRequest
    ): ClientProfileResponse {
        val client = clientRepository.findById(clientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found") }
        client.hospitalName = request.hospitalName.trim()
        client.contactPerson = request.contactPerson.trim()
        client.phoneNumber = request.phoneNumber.trim()
        client.email = request.email.trim()
        client.deliveryAddress = request.deliveryAddress.trim()
        client.department = request.department?.trim()?.takeIf { it.isNotEmpty() }
        return client.toProfile(userRepository.findFirstByClientId(clientId)?.username)
    }
}
