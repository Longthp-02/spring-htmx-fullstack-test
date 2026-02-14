package com.longpham.springhtmxfullstacktest.product

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Repository
class ProductRepository(private val jdbcClient: JdbcClient) : ProductPersistence {

    data class UpsertResult(val inserted: Int, val updated: Int)

    fun findAllProducts(): List<Product> {
        val products = jdbcClient
            .sql(
                """
                select id, title, handle, product_type, updated_at
                from products
                order by updated_at desc
                """.trimIndent()
            )
            .query { rs, _ ->
                Product.create(
                    id = rs.getLong("id"),
                    title = rs.getString("title"),
                    handle = rs.getString("handle"),
                    productType = rs.getString("product_type"),
                    updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java)
                )
            }
            .list()

        if (products.isEmpty()) return products

        return products.map { product ->
            product.withVariants(findVariantsByProductId(product.id))
        }
    }

    fun findById(id: Long): Product? {
        val product = jdbcClient
            .sql(
                """
                select id, title, handle, product_type, updated_at
                from products
                where id = :id
                """.trimIndent()
            )
            .param("id", id)
            .query { rs, _ ->
                Product.create(
                    id = rs.getLong("id"),
                    title = rs.getString("title"),
                    handle = rs.getString("handle"),
                    productType = rs.getString("product_type"),
                    updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java)
                )
            }
            .optional()
            .orElse(null)
            ?: return null

        return product.withVariants(findVariantsByProductId(id))
    }

    fun searchByTitle(query: String): List<Product> {
        val products = jdbcClient
            .sql(
                """
                select id, title, handle, product_type, updated_at
                from products
                where title ilike :query
                order by updated_at desc
                """.trimIndent()
            )
            .param("query", "%$query%")
            .query { rs, _ ->
                Product.create(
                    id = rs.getLong("id"),
                    title = rs.getString("title"),
                    handle = rs.getString("handle"),
                    productType = rs.getString("product_type"),
                    updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java)
                )
            }
            .list()

        if (products.isEmpty()) return products

        return products.map { product ->
            product.withVariants(findVariantsByProductId(product.id))
        }
    }

    @Transactional
    fun insertManualProduct(
        title: String,
        handle: String,
        productType: String?,
        updatedAt: OffsetDateTime
    ): Product {
        val generatedId = jdbcClient
            .sql(
                """
                insert into products (title, handle, product_type, updated_at)
                values (:title, :handle, :productType, :updatedAt)
                returning id
                """.trimIndent()
            )
            .param("title", title)
            .param("handle", handle)
            .param("productType", productType)
            .param("updatedAt", updatedAt)
            .query(Long::class.java)
            .single()

        return Product.create(
            id = generatedId,
            title = title,
            handle = handle,
            productType = productType,
            updatedAt = updatedAt
        )
    }

    @Transactional
    fun insertManualProduct(
        id: Long,
        title: String,
        handle: String,
        productType: String?,
        updatedAt: OffsetDateTime
    ): Product {
        jdbcClient
            .sql(
                """
                insert into products (id, title, handle, product_type, updated_at)
                values (:id, :title, :handle, :productType, :updatedAt)
                """.trimIndent()
            )
            .param("id", id)
            .param("title", title)
            .param("handle", handle)
            .param("productType", productType)
            .param("updatedAt", updatedAt)
            .update()

        return Product.create(
            id = id,
            title = title,
            handle = handle,
            productType = productType,
            updatedAt = updatedAt
        )
    }

    @Transactional
    fun deleteProduct(id: Long): Boolean {
        val rows = jdbcClient
            .sql("delete from products where id = :id")
            .param("id", id)
            .update()

        return rows > 0
    }

    @Transactional
    override fun upsertProductsFromExternal(products: List<Product>): UpsertResult {
        if (products.isEmpty()) return UpsertResult(inserted = 0, updated = 0)

        val uniqueProducts = products.distinctBy { it.id }
        var inserted = 0
        var updated = 0

        for (product in uniqueProducts) {
            val exists = jdbcClient
                .sql("select exists(select 1 from products where id = :id)")
                .param("id", product.id)
                .query(Boolean::class.java)
                .single()

            if (exists) updated++ else inserted++

            jdbcClient
                .sql(
                    """
                    insert into products (id, title, handle, product_type, updated_at)
                    values (:id, :title, :handle, :productType, :updatedAt)
                    on conflict (id)
                    do update set
                        title = excluded.title,
                        handle = excluded.handle,
                        product_type = excluded.product_type,
                        updated_at = excluded.updated_at
                    """.trimIndent()
                )
                .param("id", product.id)
                .param("title", product.title)
                .param("handle", product.handle)
                .param("productType", product.productType)
                .param("updatedAt", product.updatedAt)
                .update()

            replaceVariants(product.id, product.variants)
        }

        alignManualIdSequenceToExternalRange()

        return UpsertResult(inserted = inserted, updated = updated)
    }

    @Transactional
    fun replaceVariants(productId: Long, variants: List<ProductVariant>) {
        jdbcClient
            .sql("delete from product_variants where product_id = :productId")
            .param("productId", productId)
            .update()

        for (variant in variants) {
            jdbcClient
                .sql(
                    """
                    insert into product_variants (id, product_id, title, sku, price)
                    values (:id, :productId, :title, :sku, :price)
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

    @Transactional
    fun deleteAllProducts() {
        jdbcClient.sql("delete from product_variants").update()
        jdbcClient.sql("delete from products").update()
    }

    private fun findVariantsByProductId(productId: Long): List<ProductVariant> = jdbcClient
        .sql(
            """
            select id, product_id, title, sku, price
            from product_variants
            where product_id = :productId
            order by id
            """.trimIndent()
        )
        .param("productId", productId)
        .query { rs, _ ->
            ProductVariant.create(
                id = rs.getLong("id"),
                productId = rs.getLong("product_id"),
                title = rs.getString("title"),
                sku = rs.getString("sku"),
                price = rs.getBigDecimal("price")
            )
        }
        .list()

    private fun alignManualIdSequenceToExternalRange() {
        val maxExternalLikeId = jdbcClient
            .sql(
                """
                select max(id)
                from products
                where id <= :upperBound
                """.trimIndent()
            )
            .param("upperBound", EXTERNAL_ID_UPPER_BOUND)
            .query(Long::class.java)
            .optional()
            .orElse(null)
            ?: return

        jdbcClient
            .sql("select setval('products_manual_id_seq', :maxId, true)")
            .param("maxId", maxExternalLikeId)
            .query(Long::class.java)
            .single()
    }

    companion object {
        private const val EXTERNAL_ID_UPPER_BOUND = 99_999_999_999_999L
    }
}
