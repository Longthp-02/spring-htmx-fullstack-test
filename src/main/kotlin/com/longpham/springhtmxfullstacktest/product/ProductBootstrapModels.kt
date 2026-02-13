package com.longpham.springhtmxfullstacktest.product

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalProductsResponse(
    val products: List<ExternalProductDto> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalProductDto(
    val id: Long,
    val title: String,
    val handle: String,
    @JsonProperty("product_type")
    val productType: String? = null,
    @JsonProperty("updated_at")
    val updatedAt: String,
    val variants: List<ExternalVariantDto> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalVariantDto(
    val id: Long,
    val title: String? = null,
    val sku: String? = null,
    val price: String? = null
)