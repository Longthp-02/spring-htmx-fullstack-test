package com.longpham.springhtmxfullstacktest.product

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProductBootstrapService(
    private val externalProductSource: ExternalProductSource,
    private val productBootstrapMapper: ProductBootstrapMapper,
    private val productPersistence: ProductPersistence
) {
    private val logger = LoggerFactory.getLogger(ProductBootstrapService::class.java)

    fun bootstrapProducts() {
        val externalProducts = externalProductSource.fetchProducts()
        val products = productBootstrapMapper.toDomainProducts(externalProducts)

        if (products.isEmpty()) {
            logger.info("Product bootstrap completed with no products to persist")
            return
        }

        val result = productPersistence.upsertProductsFromExternal(products)
        logger.info(
            "Product bootstrap completed: inserted={}, updated={}, totalSaved={}",
            result.inserted,
            result.updated,
            products.size
        )
    }
}
