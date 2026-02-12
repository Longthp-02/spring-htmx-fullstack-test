package com.longpham.springhtmxfullstacktest.product

import java.math.BigDecimal
import java.time.OffsetDateTime

data class Product(
    val id: Long,
    val title: String,
    val handle: String,
    val productType: String?,
    val updatedAt: OffsetDateTime,
    val variants: List<ProductVariant> = emptyList()
)

data class ProductVariant(
    val id: Long,
    val productId: Long,
    val title: String,
    val sku: String?,
    val price: BigDecimal?
)

