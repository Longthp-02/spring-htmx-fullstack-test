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

    override
    fun fetchProducts(): List<ExternalProductDto> = restClient
        .get()
        .uri(productsUrl)
        .retrieve()
        .body(ExternalProductsResponse::class.java)
        ?.products
        .orEmpty()
}