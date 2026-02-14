package com.longpham.springhtmxfullstacktest.web

import com.longpham.springhtmxfullstacktest.product.Product
import com.longpham.springhtmxfullstacktest.product.ProductRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
    fun productsTable(
        @RequestParam(required = false, name = "q") query: String?,
        model: Model
    ): String {
        model.addAttribute("products", loadProducts(query))
        return TABLE_FRAGMENT
    }

    @GetMapping("/products/export.csv", produces = ["text/csv"])
    fun exportProductsCsv(
        @RequestParam(required = false, name = "q") query: String?
    ): ResponseEntity<String> {
        val products = loadProducts(query)
        val csv = buildCsv(products)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products.csv")
            .body(csv)
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

        model.addAttribute("products", productRepository.findAllProducts())
        return TABLE_FRAGMENT
    }

    @DeleteMapping("/products/{id}")
    fun deleteProduct(
        @PathVariable id: Long,
        @RequestParam(required = false, name = "q") query: String?,
        model: Model
    ): String {
        productRepository.deleteProduct(id)
        model.addAttribute("products", loadProducts(query))
        return TABLE_FRAGMENT
    }

    @GetMapping("/products/{id}/edit")
    fun editProductPage(@PathVariable id: Long, model: Model): String {
        val product = productRepository.findById(id) ?: return "redirect:/"
        model.addAttribute("product", product)
        return "products-edit"
    }

    @PostMapping("/products/{id}/edit")
    fun updateProduct(
        @PathVariable id: Long,
        @RequestParam title: String,
        @RequestParam handle: String,
        @RequestParam(required = false) productType: String?
    ): String {
        productRepository.updateProduct(
            id = id,
            title = title.trim(),
            handle = handle.trim(),
            productType = productType?.trim()?.ifBlank { null },
            updatedAt = OffsetDateTime.now()
        )
        return "redirect:/products/$id/edit"
    }

    private fun loadProducts(query: String?): List<Product> {
        val normalizedQuery = query?.trim().orEmpty()
        return if (normalizedQuery.isBlank()) {
            productRepository.findAllProducts()
        } else {
            productRepository.searchByTitle(normalizedQuery)
        }
    }

    private fun buildCsv(products: List<Product>): String {
        val header = "id,title,handle,product_type,updated_at,variant_count"
        val rows = products.map { product ->
            listOf(
                product.id.toString(),
                csvCell(product.title),
                csvCell(product.handle),
                csvCell(product.productType ?: ""),
                csvCell(product.updatedAt.toString()),
                product.variants.size.toString()
            ).joinToString(",")
        }

        return (listOf(header) + rows).joinToString("\n")
    }

    private fun csvCell(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""

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
