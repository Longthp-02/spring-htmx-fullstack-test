package com.longpham.springhtmxfullstacktest.web

import com.longpham.springhtmxfullstacktest.product.Product
import com.longpham.springhtmxfullstacktest.product.ProductRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
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
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"product-search-input\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("hx-get=\"/products/table\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("hx-trigger=\"keyup changed delay:300ms\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Export CSV")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Click <strong>Load products</strong>")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<table class=\"products-table\">"))))
    }

    @Test
    fun `GET products table returns table fragment`() {
        val product = Product.create(
            id = 42L,
            title = "Demo",
            handle = "demo",
            productType = "Type",
            updatedAt = OffsetDateTime.now()
        )
        `when`(productRepository.findAllProducts()).thenReturn(listOf(product))

        mockMvc.perform(get("/products/table"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<table class=\"products-table\">")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Delete")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/products/42/edit")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-product-id=\"42\"")))
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

    @Test
    fun `DELETE product returns refreshed table and applies query filter when provided`() {
        val searched = Product.create(
            id = 7L,
            title = "Filtered Product",
            handle = "filtered-product",
            productType = "Type",
            updatedAt = OffsetDateTime.now()
        )

        `when`(productRepository.deleteProduct(7L)).thenReturn(true)
        `when`(productRepository.searchByTitle("filter")).thenReturn(listOf(searched))

        mockMvc.perform(delete("/products/7").param("q", "filter"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Filtered Product")))

        verify(productRepository).deleteProduct(7L)
        verify(productRepository).searchByTitle("filter")
    }

    @Test
    fun `GET edit page renders update form for selected product`() {
        val product = Product.create(
            id = 11L,
            title = "To Edit",
            handle = "to-edit",
            productType = "Type",
            updatedAt = OffsetDateTime.now()
        )
        `when`(productRepository.findById(11L)).thenReturn(product)

        mockMvc.perform(get("/products/11/edit"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Update Product")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/products/11/edit\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"To Edit\"")))
    }

    @Test
    fun `GET products export returns downloadable csv for filtered query`() {
        val product = Product.create(
            id = 9L,
            title = "CSV Product",
            handle = "csv-product",
            productType = "Type",
            updatedAt = OffsetDateTime.now()
        )
        `when`(productRepository.searchByTitle("csv")).thenReturn(listOf(product))

        mockMvc.perform(get("/products/export.csv").param("q", "csv"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=products.csv"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id,title,handle,product_type,updated_at,variant_count")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"CSV Product\"")))
    }
}
