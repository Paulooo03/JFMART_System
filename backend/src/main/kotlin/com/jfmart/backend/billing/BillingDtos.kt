package com.jfmart.backend.billing

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class PaymentRequest(
    @field:NotNull @field:DecimalMin("0.01") @field:Digits(integer = 13, fraction = 2) val amountPaid: BigDecimal,
    @field:NotBlank @field:Size(max = 64) val paymentMethod: String,
    @field:NotNull @field:PastOrPresent val paymentDate: LocalDate,
    @field:Size(max = 100) val referenceNumber: String? = null
)

data class SalesInvoiceNumberRequest(
    @field:NotBlank @field:Size(max = 64) val salesInvoiceNumber: String
)

data class PaymentResponse(
    val id: Long,
    val invoiceNumber: Long,
    val amountPaid: BigDecimal,
    val paymentMethod: String,
    val paymentDate: LocalDate,
    val referenceNumber: String?
)

data class InvoiceResponse(
    val invoiceNumber: Long,
    val salesInvoiceNumber: String?,
    val orderId: Long,
    val orderReference: String?,
    val clientId: Long,
    val clientName: String?,
    val startDate: LocalDate,
    val dueDate: LocalDate,
    val amount: BigDecimal,
    val amountPaid: BigDecimal,
    val balance: BigDecimal,
    val paymentStatus: PaymentStatus,
    val overdue: Boolean,
    val payments: List<PaymentResponse>
)

/** A client's account: totals plus every invoice with its payments. Used by the client app and the admin. */
data class StatementResponse(
    val clientId: Long,
    val clientName: String,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val outstandingBalance: BigDecimal,
    val overdueBalance: BigDecimal,
    /** Earliest due date among invoices that still have a balance; in the past means overdue. */
    val nextDueDate: LocalDate?,
    val invoices: List<InvoiceResponse>
)
