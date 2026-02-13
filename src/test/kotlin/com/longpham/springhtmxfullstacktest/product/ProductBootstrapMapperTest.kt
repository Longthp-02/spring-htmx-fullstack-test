package com.longpham.springhtmxfullstacktest.product

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProductBootstrapMapperTest {

    private val mapper = ProductBootstrapMapper()

    @Test
    fun `toDomainProducts deduplicates and limits to 50`() {
        val duplicate = ExternalProductDto(
            id = 1L,
            title = "First",
            handle = "first",
            productType = "Type",
            updatedAt = "2026-02-13T06:03:56+01:00",
            variants = emptyList()
        )

        val products = buildList {
            add(duplicate)
            add(duplicate.copy(title = "Should be ignored"))
            for (id in 2L..60L) {
                add(
                    ExternalProductDto(
                        id = id,
                        title = "Product $id",
                        handle = "product-$id",
                        productType = null,
                        updatedAt = "2026-02-13T06:03:56+01:00",
                        variants = emptyList()
                    )
                )
            }
        }

        val result = mapper.toDomainProducts(products)

        assertEquals(50, result.size)
        assertEquals(1L, result.first().id)
        assertEquals("First", result.first().title)
        assertEquals(50L, result.last().id)
    }

    @Test
    fun `toDomainProducts maps variants with minimal rules`() {
        val products = listOf(
            ExternalProductDto(
                id = 10L,
                title = "Mapped Product",
                handle = "mapped-product",
                productType = "Leggings",
                updatedAt = "2026-02-13T06:03:56+01:00",
                variants = listOf(
                    ExternalVariantDto(id = 101L, title = "  XS  ", sku = "SKU-XS", price = "199.90"),
                    ExternalVariantDto(id = 101L, title = "Duplicate", sku = "DUP", price = "999.99"),
                    ExternalVariantDto(id = 102L, title = "   ", sku = "SKU-BLANK", price = "100.00"),
                    ExternalVariantDto(id = 103L, title = "S", sku = null, price = "not-a-number")
                )
            )
        )

        val result = mapper.toDomainProducts(products)

        assertEquals(1, result.size)
        assertEquals(2, result[0].variants.size)

        val first = result[0].variants[0]
        assertEquals(101L, first.id)
        assertEquals("XS", first.title)
        assertEquals("SKU-XS", first.sku)
        assertEquals("199.90", first.price?.toPlainString())

        val second = result[0].variants[1]
        assertEquals(103L, second.id)
        assertEquals("S", second.title)
        assertNull(second.price)
    }
}