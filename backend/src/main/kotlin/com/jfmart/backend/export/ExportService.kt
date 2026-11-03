package com.jfmart.backend.export

import com.jfmart.backend.billing.BillingService
import com.jfmart.backend.client.ClientRepository
import com.jfmart.backend.order.OrderRepository
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ExportFile(val fileName: String, val bytes: ByteArray)

/** Builds the per-client Excel workbook of records the admin keeps today by hand. */
@Service
class ExportService(
    private val billingService: BillingService,
    private val orderRepository: OrderRepository,
    private val clientRepository: ClientRepository
) {

    @Transactional(readOnly = true)
    fun clientWorkbook(clientId: Long): ExportFile {
        val client = clientRepository.findById(clientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found") }
        val statement = billingService.statement(clientId)
        val salesNumberByOrder = statement.invoices.associate { it.orderId to it.salesInvoiceNumber }
        val orders = orderRepository.findByClientIdOrderByCreatedAtDesc(clientId)

        val out = ByteArrayOutputStream()
        XSSFWorkbook().use { workbook ->
            val styles = Styles(workbook)

            SheetWriter(workbook.createSheet("Summary"), styles).apply {
                row("Client / hospital", client.hospitalName)
                row("Contact person", client.contactPerson)
                row("Contact number", client.phoneNumber)
                row("Email", client.email)
                row("Delivery address", client.deliveryAddress)
                row("Department", client.department)
                row("Exported on", LocalDate.now())
                row()
                row("Total invoiced", statement.totalInvoiced)
                row("Total paid", statement.totalPaid)
                row("Outstanding balance", statement.outstandingBalance)
                row("Overdue balance", statement.overdueBalance)
                finish(2)
            }

            SheetWriter(workbook.createSheet("Invoices"), styles).apply {
                header("Sales invoice no.", "Order ref.", "Invoice date", "Due date", "Amount", "Paid", "Balance", "Status", "Overdue")
                statement.invoices.forEach {
                    row(
                        it.salesInvoiceNumber, it.orderReference, it.startDate, it.dueDate,
                        it.amount, it.amountPaid, it.balance, it.paymentStatus.name, if (it.overdue) "Yes" else "No"
                    )
                }
                finish(9)
            }

            SheetWriter(workbook.createSheet("Payments"), styles).apply {
                header("Payment date", "Sales invoice no.", "Order ref.", "Method", "Reference no.", "Amount")
                statement.invoices.flatMap { inv -> inv.payments.map { inv to it } }
                    .sortedBy { it.second.paymentDate }
                    .forEach { (inv, p) ->
                        row(p.paymentDate, inv.salesInvoiceNumber, inv.orderReference, p.paymentMethod, p.referenceNumber, p.amountPaid)
                    }
                finish(6)
            }

            SheetWriter(workbook.createSheet("Orders"), styles).apply {
                header(
                    "Order ref.", "Order date", "Status", "Delivered", "Sales invoice no.",
                    "Item", "Qty", "Unit", "Unit price", "Line total"
                )
                orders.forEach { order ->
                    order.items.forEach { item ->
                        row(
                            order.referenceNumber, order.createdAt, order.status.name, order.deliveredAt,
                            salesNumberByOrder[order.id], item.itemName, item.quantity, item.unit,
                            item.unitPrice, item.lineTotal()
                        )
                    }
                }
                finish(10)
            }

            workbook.write(out)
        }

        val slug = client.hospitalName.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').ifEmpty { "client" }
        return ExportFile("JFMART-$slug-${LocalDate.now()}.xlsx", out.toByteArray())
    }

    private class Styles(workbook: XSSFWorkbook) {
        private val formats = workbook.createDataFormat()
        val header: CellStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply { bold = true })
        }
        val money: CellStyle = workbook.createCellStyle().apply { dataFormat = formats.getFormat("#,##0.00") }
        val date: CellStyle = workbook.createCellStyle().apply { dataFormat = formats.getFormat("yyyy-mm-dd") }
        val dateTime: CellStyle = workbook.createCellStyle().apply { dataFormat = formats.getFormat("yyyy-mm-dd hh:mm") }
    }

    /** Writes values row by row; text is always stored as plain strings, never as formulas. */
    private class SheetWriter(private val sheet: Sheet, private val styles: Styles) {
        private var next = 0

        fun header(vararg titles: String) {
            val row = sheet.createRow(next++)
            titles.forEachIndexed { i, title ->
                row.createCell(i).apply {
                    setCellValue(title)
                    cellStyle = styles.header
                }
            }
        }

        fun row(vararg values: Any?) {
            val row = sheet.createRow(next++)
            values.forEachIndexed { i, value ->
                val cell = row.createCell(i)
                when (value) {
                    null -> Unit
                    is BigDecimal -> { cell.setCellValue(value.toDouble()); cell.cellStyle = styles.money }
                    is LocalDate -> { cell.setCellValue(value); cell.cellStyle = styles.date }
                    is LocalDateTime -> { cell.setCellValue(value); cell.cellStyle = styles.dateTime }
                    is Number -> cell.setCellValue(value.toDouble())
                    else -> cell.setCellValue(value.toString())
                }
            }
        }

        fun finish(columns: Int) {
            (0 until columns).forEach { sheet.autoSizeColumn(it) }
        }
    }
}
