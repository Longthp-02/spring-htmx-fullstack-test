package com.longpham.springhtmxfullstacktest.product

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductBootstrapServiceTest {

    @Test
    fun `bootstrapProducts persists mapped products`() {
        val source = FakeExternalProductSource(
            listOf(
                ExternalProductDto(
                    id = 1L,
                    title = "A",
                    handle = "a",
                    updatedAt = "2026-02-13T06:03:56+01:00",
                    variants = emptyList()
                )
            )
        )
        val mapper = ProductBootstrapMapper()
        val persistence = FakeProductPersistence()
        val service = ProductBootstrapService(source, mapper, persistence)

        service.bootstrapProducts()

        assertEquals(1, persistence.savedProducts.size)
        assertEquals(1L, persistence.savedProducts.first().id)
    }

    @Test
    fun `bootstrapProducts skips persistence when mapped products are empty`() {
        val source = FakeExternalProductSource(emptyList())
        val mapper = ProductBootstrapMapper()
        val persistence = FakeProductPersistence()
        val service = ProductBootstrapService(source, mapper, persistence)

        service.bootstrapProducts()

        assertTrue(persistence.savedProducts.isEmpty())
        assertEquals(0, persistence.callCount)
    }

    private class FakeExternalProductSource(
        private val products: List<ExternalProductDto>
    ) : ExternalProductSource {
        override fun fetchProducts(): List<ExternalProductDto> = products
    }

    private class FakeProductPersistence : ProductPersistence {
        val savedProducts = mutableListOf<Product>()
        var callCount: Int = 0

        override fun upsertProductsFromExternal(products: List<Product>): ProductRepository.UpsertResult {
            callCount++
            savedProducts.addAll(products)
            return ProductRepository.UpsertResult(inserted = products.size, updated = 0)
        }
    }
}
