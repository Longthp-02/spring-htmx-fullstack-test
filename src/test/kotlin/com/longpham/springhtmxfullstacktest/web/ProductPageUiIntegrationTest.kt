package com.longpham.springhtmxfullstacktest.web

import com.longpham.springhtmxfullstacktest.product.Product
import com.longpham.springhtmxfullstacktest.product.ProductRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

@WebMvcTest(ProductPageController::class)
class ProductPageUiIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var productRepository: ProductRepository

    @Test
    fun `GET root renders stable htmx target container with empty placeholder`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"products-table-container\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("hx-get=\"/products/table\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Click <strong>Load products</strong>")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<table class=\"products-table\">"))))
    }

    @Test
    fun `GET products table returns table fragment`() {
        `when`(productRepository.findAllProducts()).thenReturn(emptyList())

        mockMvc.perform(get("/products/table"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<table class=\"products-table\">")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hx-delete"))))
    }

    @Test
    fun `POST products returns updated table with inserted product`() {
        val now = OffsetDateTime.now()
        val inserted = Product.create(
            id = 8_568_688_738_653L,
            title = "UI Tee",
            handle = "ui-tee",
            productType = "Shirt",
            updatedAt = now
        )

        `when`(productRepository.findAllProducts()).thenReturn(listOf(inserted))

        mockMvc.perform(
            post("/products")
                .param("title", "UI Tee")
                .param("handle", "ui-tee")
                .param("productType", "Shirt")
        )
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("UI Tee")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("ui-tee")))
    }
}