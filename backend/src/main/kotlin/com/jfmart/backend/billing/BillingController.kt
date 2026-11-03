package com.jfmart.backend.billing

import com.jfmart.backend.auth.AuthPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

/** The signed-in client's own account. The client id comes from the token, never from the URL. */
@RestController
@RequestMapping("/api/statement")
class ClientBillingController(private val billingService: BillingService) {

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    fun mine(@AuthenticationPrincipal principal: AuthPrincipal): StatementResponse {
        val clientId = principal.clientId ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not a client account")
        return billingService.statement(clientId)
    }
}

/** Invoices, payments and client statements for the admin. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
class AdminBillingController(private val billingService: BillingService) {

    @GetMapping("/invoices")
    fun invoices(
        @RequestParam(required = false) clientId: Long?,
        @RequestParam(required = false) status: PaymentStatus?,
        @RequestParam(required = false) overdue: Boolean?
    ): List<InvoiceResponse> = billingService.listInvoices(clientId, status, overdue)

    @GetMapping("/invoices/{invoiceNumber}")
    fun invoice(@PathVariable invoiceNumber: Long): InvoiceResponse = billingService.getInvoice(invoiceNumber)

    @PutMapping("/invoices/{invoiceNumber}/sales-invoice-number")
    fun setSalesInvoiceNumber(
        @PathVariable invoiceNumber: Long,
        @Valid @RequestBody request: SalesInvoiceNumberRequest
    ): InvoiceResponse = billingService.setSalesInvoiceNumber(invoiceNumber, request.salesInvoiceNumber)

    @PostMapping("/invoices/{invoiceNumber}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    fun recordPayment(
        @PathVariable invoiceNumber: Long,
        @Valid @RequestBody request: PaymentRequest
    ): InvoiceResponse = billingService.recordPayment(invoiceNumber, request)

    @GetMapping("/clients/{clientId}/statement")
    fun statement(@PathVariable clientId: Long): StatementResponse = billingService.statement(clientId)
}
