package com.longpham.springhtmxfullstacktest.product

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class ProductBootstrapClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${product.bootstrap.url:https://famme.no/products.json}")
    private val productsUrl: String
) : ExternalProductSource {
    private val restClient = restClientBuilder.build()
    private val requestUrl = withLimit(productsUrl)

    override
    fun fetchProducts(): List<ExternalProductDto> = restClient
        .get()
        .uri(requestUrl)
        .retrieve()
        .body(ExternalProductsResponse::class.java)
        ?.products
        .orEmpty()

    private fun withLimit(url: String): String {
        if (url.contains("limit=")) return url
        return if (url.contains("?")) "$url&limit=250" else "$url?limit=250"
    }
}
