package com.jfmart.backend.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

data class ApiError(val message: String)

/** Every API error body is `{"message": "..."}` so the apps can show it directly. */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException::class)
    fun status(e: ResponseStatusException): ResponseEntity<ApiError> =
        ResponseEntity.status(e.statusCode).body(ApiError(e.reason ?: "Request failed"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalid(e: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val first = e.bindingResult.fieldErrors.firstOrNull()
        val message = if (first == null) "Invalid request" else "${first.field}: ${first.defaultMessage}"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError(message))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun unreadable(e: HttpMessageNotReadableException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError("Malformed request body"))
}
