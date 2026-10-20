package com.jfmart.entity

import jakarta.persistence.*

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

    @Column(nullable = false, length = 256)
    var role: String = "" // "ROLE_ADMIN", "ROLE_CLIENT", "ROLE_DELIVERY"
)