package com.jfmart.backend.billing

import com.jfmart.backend.client.ClientRepository
import com.jfmart.backend.order.Order
import com.jfmart.backend.order.OrderRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate

data class ClientDues(val outstanding: BigDecimal, val overdue: BigDecimal, val nextDueDate: LocalDate?)

@Service
class BillingService(
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val clientRepository: ClientRepository,
    @Value("\${jfmart.billing.payment-terms-days}") private val paymentTermsDays: Long
) {

    /** Called when an order is delivered. Starts the payment term; safe to call twice for the same order. */
    @Transactional
    fun createInvoiceForDeliveredOrder(order: Order, deliveredOn: LocalDate) {
        if (invoiceRepository.existsByOrderId(order.id)) return
        val amount = order.totalAmount ?: BigDecimal.ZERO
        invoiceRepository.save(
            Invoice(
                clientId = order.clientId,
                orderId = order.id,
                startDate = deliveredOn,
                dueDate = deliveredOn.plusDays(paymentTermsDays),
                amount = amount,
                paymentStatus = statusFor(amount, BigDecimal.ZERO)
            )
        )
    }

    @Transactional(readOnly = true)
    fun listInvoices(clientId: Long?, status: PaymentStatus?, overdue: Boolean?): List<InvoiceResponse> {
        val invoices = if (clientId == null) invoiceRepository.findAllByOrderByStartDateDescInvoiceNumberDesc()
        else invoiceRepository.findByClientIdOrderByStartDateDescInvoiceNumberDesc(clientId)
        return toResponses(invoices).filter {
            (status == null || it.paymentStatus == status) && (overdue == null || it.overdue == overdue)
        }
    }

    @Transactional(readOnly = true)
    fun getInvoice(invoiceNumber: Long): InvoiceResponse =
        toResponses(listOf(findInvoice(invoiceNumber))).single()

    @Transactional
    fun setSalesInvoiceNumber(invoiceNumber: Long, value: String): InvoiceResponse {
        val invoice = findInvoice(invoiceNumber)
        val number = value.trim()
        if (invoiceRepository.existsBySalesInvoiceNumberAndInvoiceNumberNot(number, invoiceNumber)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Sales invoice number $number is already used")
        }
        invoice.salesInvoiceNumber = number
        return toResponses(listOf(invoice)).single()
    }

    @Transactional
    fun recordPayment(invoiceNumber: Long, request: PaymentRequest): InvoiceResponse {
        val invoice = invoiceRepository.findWithLockByInvoiceNumber(invoiceNumber)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found")
        val paid = paymentRepository.findByInvoiceId(invoiceNumber).fold(BigDecimal.ZERO) { sum, p -> sum + p.amountPaid }
        val balance = invoice.amount - paid
        if (request.amountPaid > balance) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Payment exceeds the outstanding balance of $balance")
        }
        paymentRepository.save(
            Payment(
                invoiceId = invoice.invoiceNumber,
                clientId = invoice.clientId,
                amountPaid = request.amountPaid,
                paymentMethod = request.paymentMethod.trim(),
                paymentDate = request.paymentDate,
                referenceNumber = request.referenceNumber?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
        invoice.paymentStatus = statusFor(invoice.amount, paid + request.amountPaid)
        return toResponses(listOf(invoice)).single()
    }

    @Transactional(readOnly = true)
    fun statement(clientId: Long): StatementResponse {
        val client = clientRepository.findById(clientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found") }
        val invoices = toResponses(invoiceRepository.findByClientIdOrderByStartDateDescInvoiceNumberDesc(clientId))
        val open = invoices.filter { it.balance.signum() > 0 }
        return StatementResponse(
            clientId = client.id,
            clientName = client.hospitalName,
            totalInvoiced = invoices.fold(BigDecimal.ZERO) { sum, i -> sum + i.amount },
            totalPaid = invoices.fold(BigDecimal.ZERO) { sum, i -> sum + i.amountPaid },
            outstandingBalance = open.fold(BigDecimal.ZERO) { sum, i -> sum + i.balance },
            overdueBalance = open.filter { it.overdue }.fold(BigDecimal.ZERO) { sum, i -> sum + i.balance },
            nextDueDate = open.minOfOrNull { it.dueDate },
            invoices = invoices
        )
    }

    /** What each invoiced client owes: the balance, the overdue part, and the earliest unpaid due date. */
    @Transactional(readOnly = true)
    fun duesByClient(): Map<Long, ClientDues> {
        val today = LocalDate.now()
        val paid = paymentRepository.totalsByInvoice().associate { it.invoiceId to it.total }
        return invoiceRepository.findAll().groupBy { it.clientId }.mapValues { (_, invoices) ->
            var outstanding = BigDecimal.ZERO
            var overdue = BigDecimal.ZERO
            var nextDue: LocalDate? = null
            invoices.forEach { invoice ->
                val balance = invoice.amount - (paid[invoice.invoiceNumber] ?: BigDecimal.ZERO)
                if (balance.signum() > 0) {
                    outstanding += balance
                    if (invoice.dueDate.isBefore(today)) overdue += balance
                    val earliest = nextDue
                    if (earliest == null || invoice.dueDate.isBefore(earliest)) nextDue = invoice.dueDate
                }
            }
            ClientDues(outstanding, overdue, nextDue)
        }
    }

    // ---- helpers ----

    private fun findInvoice(invoiceNumber: Long): Invoice =
        invoiceRepository.findById(invoiceNumber)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found") }

    private fun statusFor(amount: BigDecimal, paid: BigDecimal) = when {
        paid >= amount -> PaymentStatus.PAID
        paid.signum() > 0 -> PaymentStatus.PARTIALLY_PAID
        else -> PaymentStatus.UNPAID
    }

    private fun toResponses(invoices: List<Invoice>): List<InvoiceResponse> {
        if (invoices.isEmpty()) return emptyList()
        val today = LocalDate.now()
        val paymentsByInvoice = paymentRepository
            .findByInvoiceIdInOrderByPaymentDateAscIdAsc(invoices.map { it.invoiceNumber })
            .groupBy { it.invoiceId }
        val orders = orderRepository.findAllById(invoices.map { it.orderId }.toSet()).associateBy { it.id }
        val names = clientRepository.findAllById(invoices.map { it.clientId }.toSet()).associate { it.id to it.hospitalName }

        return invoices.map { invoice ->
            val payments = paymentsByInvoice[invoice.invoiceNumber].orEmpty()
            val paid = payments.fold(BigDecimal.ZERO) { sum, p -> sum + p.amountPaid }
            val balance = invoice.amount - paid
            InvoiceResponse(
                invoiceNumber = invoice.invoiceNumber,
                salesInvoiceNumber = invoice.salesInvoiceNumber,
                orderId = invoice.orderId,
                orderReference = orders[invoice.orderId]?.referenceNumber,
                clientId = invoice.clientId,
                clientName = names[invoice.clientId],
                startDate = invoice.startDate,
                dueDate = invoice.dueDate,
                amount = invoice.amount,
                amountPaid = paid,
                balance = balance,
                paymentStatus = invoice.paymentStatus,
                overdue = balance.signum() > 0 && invoice.dueDate.isBefore(today),
                payments = payments.map {
                    PaymentResponse(it.id, it.invoiceId, it.amountPaid, it.paymentMethod, it.paymentDate, it.referenceNumber)
                }
            )
        }
    }
}
