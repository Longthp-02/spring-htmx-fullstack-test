package com.longpham.springhtmxfullstacktest.product

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["product.bootstrap.enabled"], havingValue = "true", matchIfMissing = true)
class ProductBootstrapScheduler(
    private val productBootstrapService: ProductBootstrapService
) {
    private val logger = LoggerFactory.getLogger(ProductBootstrapScheduler::class.java)

    @Scheduled(initialDelay = 0, fixedDelay = Long.MAX_VALUE)
    fun bootstrapProducts() {
        runCatching {
            productBootstrapService.bootstrapProducts()
        }.onFailure { error ->
            logger.warn("Product bootstrap failed: {}", error.message, error)
        }
    }
}