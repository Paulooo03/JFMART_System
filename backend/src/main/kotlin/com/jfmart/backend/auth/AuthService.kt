package com.jfmart.backend.auth

import com.jfmart.backend.client.Client
import com.jfmart.backend.client.ClientRepository
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

data class SignUpRequest(
    @field:NotBlank @field:Size(max = 256) val username: String,
    @field:NotBlank @field:Size(min = 8, max = 72) val password: String,
    @field:NotBlank @field:Size(max = 256) val hospitalName: String,
    @field:NotBlank @field:Size(max = 256) val contactPerson: String,
    @field:NotBlank @field:Size(max = 32) val phoneNumber: String,
    @field:NotBlank @field:Email @field:Size(max = 256) val email: String,
    @field:NotBlank @field:Size(max = 512) val deliveryAddress: String,
    @field:Size(max = 256) val department: String? = null
)

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)

data class LoginResponse(
    val token: String,
    val role: Role,
    val userId: Long,
    val clientId: Long?
)

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val clientRepository: ClientRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    /** Creates the client record and a login that stays PENDING_APPROVAL until an admin approves it. */
    @Transactional
    fun signUp(request: SignUpRequest) {
        if (userRepository.existsByUsername(request.username)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken")
        }
        val client = clientRepository.save(
            Client(
                hospitalName = request.hospitalName,
                contactPerson = request.contactPerson,
                phoneNumber = request.phoneNumber,
                email = request.email,
                deliveryAddress = request.deliveryAddress,
                department = request.department?.takeIf { it.isNotBlank() }
            )
        )
        userRepository.save(
            User(
                username = request.username,
                passwordHash = passwordEncoder.encode(request.password)!!,
                role = Role.ROLE_CLIENT,
                status = UserStatus.PENDING_APPROVAL,
                clientId = client.id
            )
        )
    }

    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByUsername(request.username).orElse(null)
        if (user == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password")
        }
        when (user.status) {
            UserStatus.PENDING_APPROVAL ->
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Account is pending admin approval")
            UserStatus.DISABLED ->
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled")
            UserStatus.ACTIVE -> Unit
        }
        return LoginResponse(jwtService.issue(user), user.role, user.id, user.clientId)
    }
}
