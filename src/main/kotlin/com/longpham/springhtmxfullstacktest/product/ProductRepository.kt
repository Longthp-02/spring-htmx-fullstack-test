package com.longpham.springhtmxfullstacktest.product

import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Repository
class ProductRepository(private val dslContext: DSLContext) : ProductPersistence {

    data class UpsertResult(val inserted: Int, val updated: Int)

    private fun Record.toProduct(): Product = Product.create(
        id = get(ProductTable.ID)!!,
        title = get(ProductTable.TITLE)!!,
        handle = get(ProductTable.HANDLE)!!,
        productType = get(ProductTable.PRODUCT_TYPE),
        updatedAt = get(ProductTable.UPDATED_AT)!!
    )

    private fun Record.toProductVariant(): ProductVariant = ProductVariant.create(
        id = get(ProductVariantTable.ID)!!,
        productId = get(ProductVariantTable.PRODUCT_ID)!!,
        title = get(ProductVariantTable.TITLE)!!,
        sku = get(ProductVariantTable.SKU),
        price = get(ProductVariantTable.PRICE)
    )

    fun findAllProducts(): List<Product> {
        val products = dslContext
            .select(ProductTable.allFields)
            .from(ProductTable.TABLE)
            .orderBy(ProductTable.UPDATED_AT.desc())
            .fetch { it.toProduct() }

        if (products.isEmpty()) return products

        val productIds = products.map { it.id }
        val variants = findVariantsByProductIds(productIds)
        val variantsByProductId = variants.groupBy { it.productId }

        return products.map { product ->
            product.withVariants(variantsByProductId[product.id] ?: emptyList())
        }
    }

    fun findById(id: Long): Product? {
        val product = dslContext
            .select(ProductTable.allFields)
            .from(ProductTable.TABLE)
            .where(ProductTable.ID.eq(id))
            .fetchOne { it.toProduct() } ?: return null

        val variants = dslContext
            .select(ProductVariantTable.allFields)
            .from(ProductVariantTable.TABLE)
            .where(ProductVariantTable.PRODUCT_ID.eq(id))
            .orderBy(ProductVariantTable.ID)
            .fetch { it.toProductVariant() }

        return product.withVariants(variants)
    }

    fun searchByTitle(query: String): List<Product> {
        val products = dslContext
            .select(ProductTable.allFields)
            .from(ProductTable.TABLE)
            .where(ProductTable.TITLE.likeIgnoreCase("%$query%"))
            .orderBy(ProductTable.UPDATED_AT.desc())
            .fetch { it.toProduct() }

        if (products.isEmpty()) return products

        val productIds = products.map { it.id }
        val variants = findVariantsByProductIds(productIds)
        val variantsByProductId = variants.groupBy { it.productId }

        return products.map { product ->
            product.withVariants(variantsByProductId[product.id] ?: emptyList())
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
        dslContext
            .insertInto(ProductTable.TABLE)
            .columns(ProductTable.allFields)
            .values(id, title, handle, productType, updatedAt)
            .execute()

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
        val rowsAffected = dslContext
            .deleteFrom(ProductTable.TABLE)
            .where(ProductTable.ID.eq(id))
            .execute()
        return rowsAffected > 0
    }

    @Transactional
    override
    fun upsertProductsFromExternal(products: List<Product>): UpsertResult {
        if (products.isEmpty()) return UpsertResult(inserted = 0, updated = 0)

        val uniqueProducts = products.distinctBy { it.id }
        val externalIds = uniqueProducts.map { it.id }
        val existingIds = dslContext
            .select(ProductTable.ID)
            .from(ProductTable.TABLE)
            .where(ProductTable.ID.`in`(externalIds))
            .fetch(ProductTable.ID)
            .toSet()

        val inserted = externalIds.count { it !in existingIds }
        val updated = externalIds.size - inserted

        for (product in uniqueProducts) {
            dslContext
                .insertInto(ProductTable.TABLE)
                .columns(ProductTable.allFields)
                .values(product.id, product.title, product.handle, product.productType, product.updatedAt)
                .onConflict(ProductTable.ID)
                .doUpdate()
                .set(ProductTable.TITLE, DSL.excluded(ProductTable.TITLE))
                .set(ProductTable.HANDLE, DSL.excluded(ProductTable.HANDLE))
                .set(ProductTable.PRODUCT_TYPE, DSL.excluded(ProductTable.PRODUCT_TYPE))
                .set(ProductTable.UPDATED_AT, DSL.excluded(ProductTable.UPDATED_AT))
                .execute()

            replaceVariants(product.id, product.variants)
        }

        return UpsertResult(inserted = inserted, updated = updated)
    }

    @Transactional
    fun replaceVariants(productId: Long, variants: List<ProductVariant>) {
        dslContext
            .deleteFrom(ProductVariantTable.TABLE)
            .where(ProductVariantTable.PRODUCT_ID.eq(productId))
            .execute()

        for (variant in variants) {
            dslContext
                .insertInto(ProductVariantTable.TABLE)
                .columns(ProductVariantTable.allFields)
                .values(variant.id, productId, variant.title, variant.sku, variant.price)
                .execute()
        }
    }

    @Transactional
    fun deleteAllProducts() {
        dslContext.deleteFrom(ProductVariantTable.TABLE).execute()
        dslContext.deleteFrom(ProductTable.TABLE).execute()
    }

    private fun findVariantsByProductIds(productIds: List<Long>): List<ProductVariant> {
        if (productIds.isEmpty()) return emptyList()

        return dslContext
            .select(ProductVariantTable.allFields)
            .from(ProductVariantTable.TABLE)
            .where(ProductVariantTable.PRODUCT_ID.`in`(productIds))
            .orderBy(ProductVariantTable.PRODUCT_ID, ProductVariantTable.ID)
            .fetch { it.toProductVariant() }
    }
}

