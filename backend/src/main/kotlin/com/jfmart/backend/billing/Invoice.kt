package com.jfmart.backend.billing

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

enum class PaymentStatus { UNPAID, PARTIALLY_PAID, PAID }

/** One invoice per delivered order. Overdue is computed from the due date, not stored. */
@Entity
@Table(name = "invoices")
class Invoice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_number")
    val invoiceNumber: Long = 0,

    /** Number from the paper sales invoice series; entered by the admin. */
    @Column(name = "sales_invoice_number", unique = true, length = 64)
    var salesInvoiceNumber: String? = null,

    @Column(name = "client_id", nullable = false)
    val clientId: Long = 0,

    @Column(name = "order_id", nullable = false)
    val orderId: Long = 0,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate = LocalDate.now(),

    @Column(name = "due_date", nullable = false)
    val dueDate: LocalDate = LocalDate.now(),

    @Column(nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    var paymentStatus: PaymentStatus = PaymentStatus.UNPAID
)

@Entity
@Table(name = "payments")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "invoice_id", nullable = false)
    val invoiceId: Long = 0,

    @Column(name = "client_id", nullable = false)
    val clientId: Long = 0,

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    val amountPaid: BigDecimal = BigDecimal.ZERO,

    @Column(name = "payment_method", nullable = false, length = 64)
    val paymentMethod: String = "",

    @Column(name = "payment_date", nullable = false)
    val paymentDate: LocalDate = LocalDate.now(),

    @Column(name = "reference_number", length = 100)
    val referenceNumber: String? = null
)
