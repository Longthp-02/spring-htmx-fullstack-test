package com.longpham.springhtmxfullstacktest.product

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/debug")
class DebugController(private val productRepository: ProductRepository) {

    @GetMapping("/products")
    fun getAllProducts(): List<Product> {
        return productRepository.findAllProducts()
    }
}

