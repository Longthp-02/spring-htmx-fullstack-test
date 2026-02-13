package com.longpham.springhtmxfullstacktest.product

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductBootstrapClientIntegrationTest {

    @Test
    fun `fetchProducts parses payload from configured url`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "products": [
                            {
                              "id": 1001,
                              "title": "Mock Product",
                              "handle": "mock-product",
                              "product_type": "Leggings",
                              "updated_at": "2026-02-13T06:03:56+01:00",
                              "variants": [
                                {
                                  "id": 5001,
                                  "title": "Black / S",
                                  "sku": "SKU-BLACK-S",
                                  "price": "299.00",
                                  "unknown_variant_field": "ignored"
                                }
                              ],
                              "unknown_product_field": "ignored"
                            }
                          ]
                        }
                        """.trimIndent()
                    )
            )
            server.start()

            val client = ProductBootstrapClient(
                restClientBuilder = RestClient.builder(),
                productsUrl = server.url("/products.json").toString()
            )

            val products = client.fetchProducts()

            assertEquals(1, products.size)
            assertEquals(1001L, products[0].id)
            assertEquals("Mock Product", products[0].title)
            assertEquals("mock-product", products[0].handle)
            assertEquals("Leggings", products[0].productType)
            assertEquals("2026-02-13T06:03:56+01:00", products[0].updatedAt)
            assertEquals(1, products[0].variants.size)
            assertEquals(5001L, products[0].variants[0].id)
            assertEquals("Black / S", products[0].variants[0].title)
            assertEquals("SKU-BLACK-S", products[0].variants[0].sku)
            assertEquals("299.00", products[0].variants[0].price)

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/products.json", request.path)
        }
    }

    @Test
    fun `fetchProducts returns empty list when payload has no products`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody("{}")
            )
            server.start()

            val client = ProductBootstrapClient(
                restClientBuilder = RestClient.builder(),
                productsUrl = server.url("/products.json").toString()
            )

            val products = client.fetchProducts()

            assertTrue(products.isEmpty())
        }
    }
}