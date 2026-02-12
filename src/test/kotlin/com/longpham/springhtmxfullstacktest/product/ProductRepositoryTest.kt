package com.longpham.springhtmxfullstacktest.product

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@Testcontainers
class ProductRepositoryTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17")
    }

    @Autowired
    lateinit var productRepository: ProductRepository

    @BeforeEach
    fun setUp() {
        productRepository.deleteAllProducts()
    }

    @Test
    fun `findAllProducts returns empty list when no products exist`() {
        val products = productRepository.findAllProducts()
        assertTrue(products.isEmpty())
    }

    @Test
    fun `insertManualProduct creates a new product`() {
        val now = OffsetDateTime.now()
        val product = productRepository.insertManualProduct(
            id = 1L,
            title = "Test Product",
            handle = "test-product",
            productType = "Electronics",
            updatedAt = now
        )

        assertEquals(1L, product.id)
        assertEquals("Test Product", product.title)
        assertEquals("test-product", product.handle)
        assertEquals("Electronics", product.productType)
    }

    @Test
    fun `findAllProducts returns products with variants`() {
        // Insert a product
        val now = OffsetDateTime.now()
        productRepository.insertManualProduct(
            id = 1L,
            title = "Test Product",
            handle = "test-product",
            productType = "Electronics",
            updatedAt = now
        )

        // Insert variants
        val variants = listOf(
            ProductVariant.create(id = 101L, productId = 1L, title = "Small", sku = "SKU-001", price = BigDecimal("19.99")),
            ProductVariant.create(id = 102L, productId = 1L, title = "Large", sku = "SKU-002", price = BigDecimal("29.99"))
        )
        productRepository.replaceVariants(1L, variants)

        // Find all products
        val products = productRepository.findAllProducts()

        assertEquals(1, products.size)
        assertEquals(2, products[0].variants.size)
        assertEquals("Small", products[0].variants[0].title)
        assertEquals("Large", products[0].variants[1].title)
    }

    @Test
    fun `findById returns product with variants`() {
        val now = OffsetDateTime.now()
        productRepository.insertManualProduct(
            id = 1L,
            title = "Test Product",
            handle = "test-product",
            productType = "Electronics",
            updatedAt = now
        )

        val variants = listOf(
            ProductVariant.create(id = 101L, productId = 1L, title = "Default", sku = "SKU-001", price = BigDecimal("9.99"))
        )
        productRepository.replaceVariants(1L, variants)

        val product = productRepository.findById(1L)

        assertNotNull(product)
        assertEquals("Test Product", product.title)
        assertEquals(1, product.variants.size)
        assertEquals("Default", product.variants[0].title)
    }

    @Test
    fun `findById returns null for non-existent product`() {
        val product = productRepository.findById(999L)
        assertNull(product)
    }

    @Test
    fun `deleteProduct removes the product`() {
        val now = OffsetDateTime.now()
        productRepository.insertManualProduct(
            id = 1L,
            title = "Test Product",
            handle = "test-product",
            productType = null,
            updatedAt = now
        )

        val deleted = productRepository.deleteProduct(1L)
        assertTrue(deleted)

        val product = productRepository.findById(1L)
        assertNull(product)
    }

    @Test
    fun `deleteProduct returns false for non-existent product`() {
        val deleted = productRepository.deleteProduct(999L)
        assertTrue(!deleted)
    }

    @Test
    fun `searchByTitle finds products by partial title match`() {
        val now = OffsetDateTime.now()
        productRepository.insertManualProduct(1L, "Apple iPhone 15", "iphone-15", "Phone", now)
        productRepository.insertManualProduct(2L, "Samsung Galaxy", "galaxy", "Phone", now)
        productRepository.insertManualProduct(3L, "Apple MacBook", "macbook", "Laptop", now)

        val results = productRepository.searchByTitle("Apple")

        assertEquals(2, results.size)
        assertTrue(results.all { it.title.contains("Apple") })
    }

    @Test
    fun `searchByTitle is case insensitive`() {
        val now = OffsetDateTime.now()
        productRepository.insertManualProduct(1L, "Apple iPhone 15", "iphone-15", "Phone", now)

        val results = productRepository.searchByTitle("apple")

        assertEquals(1, results.size)
        assertEquals("Apple iPhone 15", results[0].title)
    }

    @Test
    fun `upsertProductsFromExternal inserts new products`() {
        val now = OffsetDateTime.now()
        val products = listOf(
            Product.create(id = 1L, title = "Product 1", handle = "product-1", productType = "Type A", updatedAt = now),
            Product.create(id = 2L, title = "Product 2", handle = "product-2", productType = "Type B", updatedAt = now)
        )

        productRepository.upsertProductsFromExternal(products)

        val allProducts = productRepository.findAllProducts()
        assertEquals(2, allProducts.size)
    }

    @Test
    fun `upsertProductsFromExternal updates existing products`() {
        val now = OffsetDateTime.now()
        productRepository.insertManualProduct(1L, "Original Title", "product-1", "Type A", now)

        val later = now.plusHours(1)
        val updatedProducts = listOf(
            Product.create(id = 1L, title = "Updated Title", handle = "product-1-updated", productType = "Type B", updatedAt = later)
        )

        productRepository.upsertProductsFromExternal(updatedProducts)

        val product = productRepository.findById(1L)
        assertNotNull(product)
        assertEquals("Updated Title", product.title)
        assertEquals("product-1-updated", product.handle)
        assertEquals("Type B", product.productType)
    }

    @Test
    fun `upsertProductsFromExternal handles products with variants`() {
        val now = OffsetDateTime.now()
        val variants = listOf(
            ProductVariant.create(id = 101L, productId = 1L, title = "Variant 1", sku = "SKU-1", price = BigDecimal("10.00")),
            ProductVariant.create(id = 102L, productId = 1L, title = "Variant 2", sku = "SKU-2", price = BigDecimal("20.00"))
        )
        val products = listOf(
            Product.create(id = 1L, title = "Product with Variants", handle = "product-1", productType = "Type A", updatedAt = now, variants = variants)
        )

        productRepository.upsertProductsFromExternal(products)

        val product = productRepository.findById(1L)
        assertNotNull(product)
        assertEquals(2, product.variants.size)
    }

    @Test
    fun `replaceVariants deletes existing variants and inserts new ones`() {
        val now = OffsetDateTime.now()
        productRepository.insertManualProduct(1L, "Test Product", "test-product", null, now)

        // Insert initial variants
        val initialVariants = listOf(
            ProductVariant.create(id = 101L, productId = 1L, title = "Old Variant", sku = "OLD-SKU", price = BigDecimal("5.00"))
        )
        productRepository.replaceVariants(1L, initialVariants)

        // Replace with new variants
        val newVariants = listOf(
            ProductVariant.create(id = 201L, productId = 1L, title = "New Variant 1", sku = "NEW-SKU-1", price = BigDecimal("15.00")),
            ProductVariant.create(id = 202L, productId = 1L, title = "New Variant 2", sku = "NEW-SKU-2", price = BigDecimal("25.00"))
        )
        productRepository.replaceVariants(1L, newVariants)

        val product = productRepository.findById(1L)
        assertNotNull(product)
        assertEquals(2, product.variants.size)
        assertEquals("New Variant 1", product.variants[0].title)
        assertEquals("New Variant 2", product.variants[1].title)
    }

    @Test
    fun `product with null productType is handled correctly`() {
        val now = OffsetDateTime.now()
        productRepository.insertManualProduct(
            id = 1L,
            title = "Test Product",
            handle = "test-product",
            productType = null,
            updatedAt = now
        )

        val product = productRepository.findById(1L)
        assertNotNull(product)
        assertNull(product.productType)
    }

    @Test
    fun `variant with null sku and price is handled correctly`() {
        val now = OffsetDateTime.now()
        productRepository.insertManualProduct(1L, "Test Product", "test-product", null, now)

        val variants = listOf(
            ProductVariant.create(id = 101L, productId = 1L, title = "Default Variant", sku = null, price = null)
        )
        productRepository.replaceVariants(1L, variants)

        val product = productRepository.findById(1L)
        assertNotNull(product)
        assertEquals(1, product.variants.size)
        assertNull(product.variants[0].sku)
        assertNull(product.variants[0].price)
    }
}

