package com.longpham.springhtmxfullstacktest.web

import com.longpham.springhtmxfullstacktest.product.ProductRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.OffsetDateTime

@Controller
class ProductPageController(
    private val productRepository: ProductRepository
) {

    @GetMapping("/")
    fun index(): String = "index"

    @GetMapping("/products/table")
    fun productsTable(model: Model): String {
        populateProducts(model)
        return TABLE_FRAGMENT
    }

    @PostMapping("/products")
    fun addProduct(
        @RequestParam title: String,
        @RequestParam(required = false) handle: String?,
        @RequestParam(required = false) productType: String?,
        model: Model
    ): String {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isNotEmpty()) {
            val normalizedHandle = handle
                ?.trim()
                .takeUnless { it.isNullOrBlank() }
                ?: toHandle(normalizedTitle)

            productRepository.insertManualProduct(
                title = normalizedTitle,
                handle = normalizedHandle,
                productType = productType?.trim()?.ifBlank { null },
                updatedAt = OffsetDateTime.now()
            )
        }

        populateProducts(model)
        return TABLE_FRAGMENT
    }

    private fun populateProducts(model: Model) {
        model.addAttribute("products", productRepository.findAllProducts())
    }

    private fun toHandle(input: String): String {
        val slug = input
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        return if (slug.isBlank()) "manual-product-${System.currentTimeMillis()}" else slug
    }

    companion object {
        private const val TABLE_FRAGMENT = "fragments/products-table :: productsTable"
    }
}