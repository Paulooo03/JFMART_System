package com.jfmart.backend.billing

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal

/** A per-client total from a grouped query. */
interface ClientAmount {
    val clientId: Long
    val total: BigDecimal
}

interface InvoiceRepository : JpaRepository<Invoice, Long> {
    fun findAllByOrderByStartDateDescInvoiceNumberDesc(): List<Invoice>
    fun findByClientIdOrderByStartDateDescInvoiceNumberDesc(clientId: Long): List<Invoice>
    fun existsByOrderId(orderId: Long): Boolean
    fun existsBySalesInvoiceNumberAndInvoiceNumberNot(salesInvoiceNumber: String, invoiceNumber: Long): Boolean

    // Row lock so two payments recorded at the same moment cannot overpay an invoice
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByInvoiceNumber(invoiceNumber: Long): Invoice?

    @Query("select i.clientId as clientId, sum(i.amount) as total from Invoice i group by i.clientId")
    fun totalsByClient(): List<ClientAmount>
}

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByInvoiceIdInOrderByPaymentDateAscIdAsc(invoiceIds: Collection<Long>): List<Payment>
    fun findByInvoiceId(invoiceId: Long): List<Payment>

    @Query("select p.clientId as clientId, sum(p.amountPaid) as total from Payment p group by p.clientId")
    fun totalsByClient(): List<ClientAmount>
}
