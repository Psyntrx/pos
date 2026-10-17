package com.example.pos

import android.content.Context
import com.example.pos.data.PosStorageManager
import com.example.pos.viewmodel.PosViewModel
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class PosViewModelProductTest {

    private lateinit var application: android.app.Application

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        application.getSharedPreferences("pos_data_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun creditDurationMigrationPreservesDayChoicesWithoutTreatingOneMonthAsOneDay() {
        val preferences = application.getSharedPreferences("pos_data_prefs", Context.MODE_PRIVATE)
        preferences.edit().putInt("key_credit_duration_months", 60).apply()

        val migratedDays = PosStorageManager(application).loadCreditDurationDays()

        assertEquals(60, migratedDays)
        assertEquals(60, preferences.getInt("key_credit_duration_months", 0))
        assertEquals(60, preferences.getInt("key_credit_duration_days", 0))

        preferences.edit().clear().putInt("key_credit_duration_months", 1).apply()
        val defaultDays = PosStorageManager(application).loadCreditDurationDays()

        assertEquals(14, defaultDays)
        assertEquals(1, preferences.getInt("key_credit_duration_months", 0))
    }

    @Test
    fun addProductRejectsDuplicateSku() {
        val viewModel = PosViewModel(application, supabase = null)
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
        val viewModel = PosViewModel(application, supabase = null)
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
        val viewModel = PosViewModel(application, supabase = null)

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

    @Test
    fun completeSaleCreditRequiresNameAndValidPhone() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Test Item",
            category = "General",
            sku = "TEST-CREDIT-01",
            unit = "pcs",
            buyingPrice = 50.0,
            sellingPrice = 100.0,
            quantity = 10,
            minStock = 2,
            supplier = "Local"
        )
        val product = viewModel.products.value.first()
        viewModel.addToCart(product)

        // Blank customer name rejected
        try {
            viewModel.completeSale(
                paymentMethod = "Credit",
                customerName = "   ",
                customerPhone = "0712345678",
                isCredit = true
            )
            assertTrue("Expected IllegalArgumentException for blank customer name", false)
        } catch (_: IllegalArgumentException) {}

        // Invalid customer phone rejected
        try {
            viewModel.completeSale(
                paymentMethod = "Credit",
                customerName = "Alice",
                customerPhone = "invalid-phone",
                isCredit = true
            )
            assertTrue("Expected IllegalArgumentException for invalid customer phone", false)
        } catch (_: IllegalArgumentException) {}

        // Valid credit sale succeeds
        val initialStock = product.stockQuantity
        val initialCreditsCount = viewModel.customerCredits.value.size
        val sale = viewModel.completeSale(
            paymentMethod = "Credit",
            customerName = "  Alice Wambui  ",
            customerPhone = "0712 345 678",
            isCredit = true,
            paidAmount = 50.0,
            dueDate = "Oct 30, 2026"
        )

        assertEquals("Alice Wambui", sale.customerName)
        assertTrue(sale.isCreditSale)
        assertEquals(50.0, sale.paidAmount, 0.0)
        assertEquals(product.sellingPrice - 50.0, sale.balance, 0.0)

        // Verifies stock deducted
        val updatedProduct = viewModel.products.value.first { it.id == product.id }
        assertEquals(initialStock - 1, updatedProduct.stockQuantity)

        // Verifies credit ledger entry created with normalized phone
        assertEquals(initialCreditsCount + 1, viewModel.customerCredits.value.size)
        val creditRecord = viewModel.customerCredits.value.first()
        assertEquals("Alice Wambui", creditRecord.customerName)
        assertEquals("+254712345678", creditRecord.customerPhone)
        assertEquals(product.sellingPrice, creditRecord.totalOutstanding, 0.0)
        assertEquals(50.0, creditRecord.amountPaid, 0.0)
        assertEquals(product.sellingPrice - 50.0, creditRecord.balance, 0.0)
    }

    @Test
    fun completeSaleCashDoesNotRequireCustomerPhone() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Cash Item",
            category = "General",
            sku = "TEST-CASH-01",
            unit = "pcs",
            buyingPrice = 50.0,
            sellingPrice = 100.0,
            quantity = 10,
            minStock = 2,
            supplier = "Local"
        )
        val product = viewModel.products.value.first()
        viewModel.addToCart(product)

        val initialCreditsCount = viewModel.customerCredits.value.size
        val sale = viewModel.completeSale(
            paymentMethod = "Cash",
            customerName = "",
            customerPhone = "",
            isCredit = false
        )

        assertEquals("Walk-in Customer", sale.customerName)
        assertFalse(sale.isCreditSale)
        assertEquals(initialCreditsCount, viewModel.customerCredits.value.size)
    }

    @Test
    fun addToCartPreventsExceedingStock() {
        val viewModel = PosViewModel(application, supabase = null)
        // Add a product with stock = 2
        viewModel.addProduct(
            name = "Limited Item",
            category = "General",
            sku = "LTD-01",
            unit = "pcs",
            buyingPrice = 50.0,
            sellingPrice = 100.0,
            quantity = 2,
            minStock = 1,
            supplier = ""
        )
        val limitedProduct = viewModel.products.value.first { it.sku == "LTD-01" }

        viewModel.addToCart(limitedProduct)
        viewModel.addToCart(limitedProduct)
        viewModel.addToCart(limitedProduct) // 3rd attempt exceeds stock

        val cartItem = viewModel.cart.value.first { it.product.id == limitedProduct.id }
        assertEquals(2, cartItem.quantity)
    }

    @Test
    fun recordStockInIncrementsStockQuantity() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Flour",
            category = "Household",
            sku = "FLOUR-01",
            unit = "2kg",
            buyingPrice = 100.0,
            sellingPrice = 140.0,
            quantity = 0,
            minStock = 3,
            supplier = ""
        )
        val product = viewModel.products.value.first { it.sku == "FLOUR-01" }
        assertEquals(0, product.stockQuantity)

        viewModel.recordStockIn(
            productId = product.id,
            quantityAdded = 15,
            buyingPrice = 105.0,
            sellingPrice = 145.0,
            supplier = "New Supplier",
            employeeName = "Owner"
        )

        val updated = viewModel.products.value.first { it.id == product.id }
        assertEquals(15, updated.stockQuantity)
        assertEquals(105.0, updated.buyingPrice, 0.001)
        assertEquals(145.0, updated.sellingPrice, 0.001)
        assertEquals("New Supplier", updated.supplier)
    }

    @Test
    fun updateProductAllowsBlankSkuAndUpdatesQuantity() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Sugar",
            category = "Groceries",
            sku = "SUGAR-01",
            unit = "1kg",
            buyingPrice = 120.0,
            sellingPrice = 150.0,
            quantity = 0,
            minStock = 5,
            supplier = ""
        )
        val product = viewModel.products.value.first { it.sku == "SUGAR-01" }
        assertEquals(0, product.stockQuantity)

        val success = viewModel.updateProduct(
            productId = product.id,
            name = "Sugar 1kg",
            category = "Groceries",
            sku = "",
            unit = "1kg",
            buyingPrice = 120.0,
            sellingPrice = 155.0,
            quantity = 25,
            minStock = 5,
            supplier = "Sweet Co"
        )

        assertTrue(success)
        val updated = viewModel.products.value.first { it.id == product.id }
        assertEquals(25, updated.stockQuantity)
        assertTrue(updated.sku.isNotBlank())
        assertEquals(155.0, updated.sellingPrice, 0.001)
    }
}

