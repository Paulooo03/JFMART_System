package com.jfmart.backend.auth

import jakarta.persistence.*

enum class Role { ROLE_ADMIN, ROLE_CLIENT, ROLE_DELIVERY }

enum class UserStatus { PENDING_APPROVAL, ACTIVE, DISABLED }

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 256)
    var username: String = "",

    @Column(name = "password_hash", nullable = false, length = 256)
    var passwordHash: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var role: Role = Role.ROLE_CLIENT,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: UserStatus = UserStatus.PENDING_APPROVAL,

    // null for admin and courier accounts
    @Column(name = "client_id")
    var clientId: Long? = null
)
