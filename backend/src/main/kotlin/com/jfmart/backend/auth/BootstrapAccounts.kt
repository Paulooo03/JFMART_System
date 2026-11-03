package com.jfmart.backend.auth

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Creates the single admin and single courier account on first start.
 * Passwords come from ADMIN_PASSWORD / COURIER_PASSWORD; a role is skipped if it already has an
 * account or its password is not set.
 */
@Component
class BootstrapAccounts(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${jfmart.bootstrap.admin-username}") private val adminUsername: String,
    @Value("\${jfmart.bootstrap.admin-password}") private val adminPassword: String,
    @Value("\${jfmart.bootstrap.courier-username}") private val courierUsername: String,
    @Value("\${jfmart.bootstrap.courier-password}") private val courierPassword: String
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        create(Role.ROLE_ADMIN, adminUsername, adminPassword)
        create(Role.ROLE_DELIVERY, courierUsername, courierPassword)
    }

    private fun create(role: Role, username: String, password: String) {
        if (userRepository.existsByRole(role)) return
        if (password.isBlank()) {
            log.warn("No {} account exists and no bootstrap password is set; skipping", role)
            return
        }
        userRepository.save(
            User(
                username = username,
                passwordHash = passwordEncoder.encode(password)!!,
                role = role,
                status = UserStatus.ACTIVE
            )
        )
        log.info("Created {} account '{}'", role, username)
    }
}
