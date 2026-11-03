package com.jfmart.backend.export

import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Excel records per client. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/clients")
class ExportController(private val exportService: ExportService) {

    @GetMapping("/{clientId}/export")
    fun export(@PathVariable clientId: Long): ResponseEntity<ByteArray> {
        val file = exportService.clientWorkbook(clientId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName).build().toString())
            .body(file.bytes)
    }
}
