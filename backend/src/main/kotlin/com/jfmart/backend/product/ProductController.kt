package com.jfmart.backend.product

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

data class ProductRequest(
    @field:NotBlank @field:Size(max = 256) val name: String,
    @field:NotNull @field:DecimalMin("0.00") @field:Digits(integer = 13, fraction = 2) val price: BigDecimal,
    @field:NotBlank @field:Size(max = 64) val unit: String,
    val active: Boolean = true
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val unit: String,
    val active: Boolean
)

private fun Product.toResponse() = ProductResponse(id, name, price, unit, active)

/** Price list for clients (active products only). */
@RestController
@RequestMapping("/api/products")
class ProductController(private val productRepository: ProductRepository) {

    @GetMapping
    fun list(@RequestParam(defaultValue = "") search: String): List<ProductResponse> =
        productRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderByName(search.trim()).map { it.toResponse() }
}

/** Product and price management. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/products")
class AdminProductController(private val productRepository: ProductRepository) {

    @GetMapping
    fun list(@RequestParam(defaultValue = "") search: String): List<ProductResponse> =
        productRepository.findByNameContainingIgnoreCaseOrderByName(search.trim()).map { it.toResponse() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: ProductRequest): ProductResponse =
        productRepository.save(
            Product(name = request.name.trim(), price = request.price, unit = request.unit.trim(), active = request.active)
        ).toResponse()

    /** Replaces the product's fields. Set `active` to false instead of deleting, so past orders keep their reference. */
    @PutMapping("/{id}")
    @Transactional
    fun update(@PathVariable id: Long, @Valid @RequestBody request: ProductRequest): ProductResponse {
        val product = productRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found") }
        product.name = request.name.trim()
        product.price = request.price
        product.unit = request.unit.trim()
        product.active = request.active
        return product.toResponse()
    }
}
