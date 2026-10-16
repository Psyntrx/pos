package com.example.pos

import com.example.pos.viewmodel.PosViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PosViewModelProductTest {
    @Test
    fun addProductRejectsDuplicateSku() {
        val viewModel = PosViewModel()
        viewModel.addProduct(
            name = "Existing Product",
            category = "Groceries",
            sku = "UG001",
            unit = "pcs",
            buyingPrice = 100.0,
            sellingPrice = 150.0,
            quantity = 10,
            minStock = 2,
            supplier = ""
        )
        val initialCount = viewModel.products.value.size

        val saved = viewModel.addProduct(
            name = "New product",
            category = "Groceries",
            sku = "ug001",
            unit = "pcs",
            buyingPrice = 10.0,
            sellingPrice = 15.0,
            quantity = 5,
            minStock = 2,
            supplier = ""
        )

        assertFalse(saved)
        assertEquals(initialCount, viewModel.products.value.size)
    }

    @Test
    fun addProductRejectsInvalidPriceAndStock() {
        val viewModel = PosViewModel()
        val initialCount = viewModel.products.value.size

        val saved = viewModel.addProduct(
            name = "New product",
            category = "Groceries",
            sku = "UNIQUE-01",
            unit = "pcs",
            buyingPrice = 10.0,
            sellingPrice = 0.0,
            quantity = -1,
            minStock = 2,
            supplier = ""
        )

        assertFalse(saved)
        assertEquals(initialCount, viewModel.products.value.size)
    }

    @Test
    fun addProductTrimsFieldsAndAddsValidProduct() {
        val viewModel = PosViewModel()

        val saved = viewModel.addProduct(
            name = "  Tea  ",
            category = "  Beverages  ",
            sku = "  TEA-001  ",
            unit = "  box  ",
            buyingPrice = 20.0,
            sellingPrice = 30.0,
            quantity = 12,
            minStock = 3,
            supplier = "  Local supplier  "
        )

        assertTrue(saved)
        val product = viewModel.products.value.first()
        assertEquals("Tea", product.name)
        assertEquals("Beverages", product.category)
        assertEquals("TEA-001", product.sku)
        assertEquals("box", product.unit)
        assertEquals("Local supplier", product.supplier)
        assertEquals(12, product.stockQuantity)
    }
}
