package com.longpham.springhtmxfullstacktest.product

import java.math.BigDecimal
import java.time.OffsetDateTime

abstract class Product {
    abstract val id: Long
    abstract val title: String
    abstract val handle: String
    abstract val productType: String?
    abstract val updatedAt: OffsetDateTime
    abstract val variants: List<ProductVariant>

    fun withVariants(variants: List<ProductVariant>): Product =
        create(id, title, handle, productType, updatedAt, variants)

    companion object {
        fun create(
            id: Long,
            title: String,
            handle: String,
            productType: String?,
            updatedAt: OffsetDateTime,
            variants: List<ProductVariant> = emptyList()
        ): Product = ProductImpl(id, title, handle, productType, updatedAt, variants)
    }

    private class ProductImpl(
        override val id: Long,
        override val title: String,
        override val handle: String,
        override val productType: String?,
        override val updatedAt: OffsetDateTime,
        override val variants: List<ProductVariant>
    ) : Product() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Product) return false
            return id == other.id &&
                    title == other.title &&
                    handle == other.handle &&
                    productType == other.productType &&
                    updatedAt == other.updatedAt &&
                    variants == other.variants
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + handle.hashCode()
            result = 31 * result + (productType?.hashCode() ?: 0)
            result = 31 * result + updatedAt.hashCode()
            result = 31 * result + variants.hashCode()
            return result
        }

        override fun toString(): String =
            "Product(id=$id, title='$title', handle='$handle', productType=$productType, updatedAt=$updatedAt, variants=$variants)"
    }
}

abstract class ProductVariant {
    abstract val id: Long
    abstract val productId: Long
    abstract val title: String
    abstract val sku: String?
    abstract val price: BigDecimal?

    companion object {
        fun create(
            id: Long,
            productId: Long,
            title: String,
            sku: String?,
            price: BigDecimal?
        ): ProductVariant = ProductVariantImpl(id, productId, title, sku, price)
    }

    private class ProductVariantImpl(
        override val id: Long,
        override val productId: Long,
        override val title: String,
        override val sku: String?,
        override val price: BigDecimal?
    ) : ProductVariant() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ProductVariant) return false
            return id == other.id &&
                    productId == other.productId &&
                    title == other.title &&
                    sku == other.sku &&
                    price == other.price
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + productId.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + (sku?.hashCode() ?: 0)
            result = 31 * result + (price?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String =
            "ProductVariant(id=$id, productId=$productId, title='$title', sku=$sku, price=$price)"
    }
}

