package com.longpham.springhtmxfullstacktest.product

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.OffsetDateTime

@Repository
class ProductRepository(private val jdbcClient: JdbcClient) {

    private val productRowMapper: (ResultSet, Int) -> Product = { rs, _ ->
        Product(
            id = rs.getLong("id"),
            title = rs.getString("title"),
            handle = rs.getString("handle"),
            productType = rs.getString("product_type"),
            updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java)
        )
    }

    private val variantRowMapper: (ResultSet, Int) -> ProductVariant = { rs, _ ->
        ProductVariant(
            id = rs.getLong("id"),
            productId = rs.getLong("product_id"),
            title = rs.getString("title"),
            sku = rs.getString("sku"),
            price = rs.getBigDecimal("price")
        )
    }

    fun findAllProducts(): List<Product> {
        val products = jdbcClient.sql("SELECT * FROM products ORDER BY updated_at DESC")
            .query(productRowMapper)
            .list()

        if (products.isEmpty()) return products

        val productIds = products.map { it.id }
        val variants = findVariantsByProductIds(productIds)
        val variantsByProductId = variants.groupBy { it.productId }

        return products.map { product ->
            product.copy(variants = variantsByProductId[product.id] ?: emptyList())
        }
    }

    fun findById(id: Long): Product? {
        val product = jdbcClient.sql("SELECT * FROM products WHERE id = :id")
            .param("id", id)
            .query(productRowMapper)
            .optional()
            .orElse(null) ?: return null

        val variants = jdbcClient.sql("SELECT * FROM product_variants WHERE product_id = :productId ORDER BY id")
            .param("productId", id)
            .query(variantRowMapper)
            .list()

        return product.copy(variants = variants)
    }

    fun searchByTitle(query: String): List<Product> {
        val products = jdbcClient.sql(
            """
            SELECT * FROM products 
            WHERE title ILIKE :query 
            ORDER BY updated_at DESC
            """.trimIndent()
        )
            .param("query", "%$query%")
            .query(productRowMapper)
            .list()

        if (products.isEmpty()) return products

        val productIds = products.map { it.id }
        val variants = findVariantsByProductIds(productIds)
        val variantsByProductId = variants.groupBy { it.productId }

        return products.map { product ->
            product.copy(variants = variantsByProductId[product.id] ?: emptyList())
        }
    }

    @Transactional
    fun insertManualProduct(
        id: Long,
        title: String,
        handle: String,
        productType: String?,
        updatedAt: OffsetDateTime = OffsetDateTime.now()
    ): Product {
        jdbcClient.sql(
            """
            INSERT INTO products (id, title, handle, product_type, updated_at)
            VALUES (:id, :title, :handle, :productType, :updatedAt)
            """.trimIndent()
        )
            .param("id", id)
            .param("title", title)
            .param("handle", handle)
            .param("productType", productType)
            .param("updatedAt", updatedAt)
            .update()

        return Product(
            id = id,
            title = title,
            handle = handle,
            productType = productType,
            updatedAt = updatedAt
        )
    }

    @Transactional
    fun deleteProduct(id: Long): Boolean {
        val rowsAffected = jdbcClient.sql("DELETE FROM products WHERE id = :id")
            .param("id", id)
            .update()
        return rowsAffected > 0
    }

    @Transactional
    fun upsertProductsFromExternal(products: List<Product>) {
        if (products.isEmpty()) return

        for (product in products) {
            jdbcClient.sql(
                """
                INSERT INTO products (id, title, handle, product_type, updated_at)
                VALUES (:id, :title, :handle, :productType, :updatedAt)
                ON CONFLICT (id) DO UPDATE SET
                    title = EXCLUDED.title,
                    handle = EXCLUDED.handle,
                    product_type = EXCLUDED.product_type,
                    updated_at = EXCLUDED.updated_at
                """.trimIndent()
            )
                .param("id", product.id)
                .param("title", product.title)
                .param("handle", product.handle)
                .param("productType", product.productType)
                .param("updatedAt", product.updatedAt)
                .update()

            if (product.variants.isNotEmpty()) {
                replaceVariants(product.id, product.variants)
            }
        }
    }

    @Transactional
    fun replaceVariants(productId: Long, variants: List<ProductVariant>) {
        // Delete existing variants
        jdbcClient.sql("DELETE FROM product_variants WHERE product_id = :productId")
            .param("productId", productId)
            .update()

        // Insert new variants
        for (variant in variants) {
            jdbcClient.sql(
                """
                INSERT INTO product_variants (id, product_id, title, sku, price)
                VALUES (:id, :productId, :title, :sku, :price)
                """.trimIndent()
            )
                .param("id", variant.id)
                .param("productId", productId)
                .param("title", variant.title)
                .param("sku", variant.sku)
                .param("price", variant.price)
                .update()
        }
    }

    private fun findVariantsByProductIds(productIds: List<Long>): List<ProductVariant> {
        if (productIds.isEmpty()) return emptyList()

        return jdbcClient.sql(
            """
            SELECT * FROM product_variants 
            WHERE product_id IN (:productIds) 
            ORDER BY product_id, id
            """.trimIndent()
        )
            .param("productIds", productIds)
            .query(variantRowMapper)
            .list()
    }
}

