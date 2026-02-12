package com.longpham.springhtmxfullstacktest.product

import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType

object ProductTable {
    val TABLE = DSL.table("products")

    val ID = DSL.field("id", SQLDataType.BIGINT)
    val TITLE = DSL.field("title", SQLDataType.VARCHAR)
    val HANDLE = DSL.field("handle", SQLDataType.VARCHAR)
    val PRODUCT_TYPE = DSL.field("product_type", SQLDataType.VARCHAR)
    val UPDATED_AT = DSL.field("updated_at", SQLDataType.OFFSETDATETIME)

    val allFields = listOf(ID, TITLE, HANDLE, PRODUCT_TYPE, UPDATED_AT)
}

object ProductVariantTable {
    val TABLE = DSL.table("product_variants")

    val ID = DSL.field("id", SQLDataType.BIGINT)
    val PRODUCT_ID = DSL.field("product_id", SQLDataType.BIGINT)
    val TITLE = DSL.field("title", SQLDataType.VARCHAR)
    val SKU = DSL.field("sku", SQLDataType.VARCHAR)
    val PRICE = DSL.field("price", SQLDataType.DECIMAL)

    val allFields = listOf(ID, PRODUCT_ID, TITLE, SKU, PRICE)
}