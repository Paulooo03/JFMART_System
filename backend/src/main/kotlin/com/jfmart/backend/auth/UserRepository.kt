package com.jfmart.backend.auth

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>
    fun existsByUsername(username: String): Boolean
    fun existsByRole(role: Role): Boolean
    fun findFirstByClientId(clientId: Long): User?
    fun findByRoleAndStatus(role: Role, status: UserStatus): List<User>
}
