package com.longpham.springhtmxfullstacktest.product

interface ExternalProductSource {
    fun fetchProducts(): List<ExternalProductDto>
}

interface ProductPersistence {
    fun upsertProductsFromExternal(products: List<Product>): ProductRepository.UpsertResult
}