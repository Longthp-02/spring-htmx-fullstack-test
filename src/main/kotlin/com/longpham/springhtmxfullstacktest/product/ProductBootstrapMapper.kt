package com.longpham.springhtmxfullstacktest.product

import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class ProductBootstrapMapper {

    fun toDomainProducts(
        externalProducts: List<ExternalProductDto>,
        maxProducts: Int = DEFAULT_MAX_PRODUCTS
    ): List<Product> = externalProducts
        .asSequence()
        .distinctBy { it.id }
        .take(maxProducts)
        .map { it.toDomainProduct() }
        .toList()

    private fun ExternalProductDto.toDomainProduct(): Product = Product.create(
        id = id,
        title = title,
        handle = handle,
        productType = productType,
        updatedAt = OffsetDateTime.parse(updatedAt),
        variants = variants
            .distinctBy { it.id }
            .mapNotNull { it.toDomainVariant(id) }
    )

    private fun ExternalVariantDto.toDomainVariant(productId: Long): ProductVariant? {
        val normalizedTitle = title?.trim().orEmpty()
        if (normalizedTitle.isEmpty()) return null

        return ProductVariant.create(
            id = id,
            productId = productId,
            title = normalizedTitle,
            sku = sku,
            price = price?.toBigDecimalOrNull()
        )
    }

    companion object {
        const val DEFAULT_MAX_PRODUCTS = 50
    }
}