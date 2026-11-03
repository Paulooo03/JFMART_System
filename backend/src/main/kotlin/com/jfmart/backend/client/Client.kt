package com.jfmart.backend.client

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "clients_list")
class Client(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "hospital_name", nullable = false, length = 256)
    var hospitalName: String = "",

    @Column(name = "contact_person", nullable = false, length = 256)
    var contactPerson: String = "",

    @Column(name = "phone_number", nullable = false, length = 32)
    var phoneNumber: String = "",

    @Column(nullable = false, length = 256)
    var email: String = "",

    @Column(name = "delivery_address", nullable = false, length = 512)
    var deliveryAddress: String = "",

    @Column(length = 256)
    var department: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
