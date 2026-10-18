package com.example.pos

import android.content.Context
import com.example.pos.data.CartItem
import com.example.pos.data.PosStorageManager
import com.example.pos.data.Sale
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

    @Test
    fun addProductAllowsBlankCategoryAndAutoGeneratesSku() {
        val viewModel = PosViewModel(application, supabase = null)
        val saved = viewModel.addProduct(
            name = "Apples",
            category = "   ",
            sku = "",
            unit = "kg",
            buyingPrice = 50.0,
            sellingPrice = 80.0,
            quantity = 20,
            minStock = 5,
            supplier = "Farm Fresh"
        )

        assertTrue(saved)
        val product = viewModel.products.value.first()
        assertEquals("Apples", product.name)
        assertEquals("", product.category)
        assertTrue(product.sku.isNotBlank())
        assertEquals("kg", product.unit)
    }

    @Test
    fun updateProductAllowsBlankCategory() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Notebook",
            category = "Stationery",
            sku = "NOTE-01",
            unit = "pcs",
            buyingPrice = 30.0,
            sellingPrice = 50.0,
            quantity = 10,
            minStock = 2,
            supplier = ""
        )
        val product = viewModel.products.value.first()

        val success = viewModel.updateProduct(
            productId = product.id,
            name = "Notebook A5",
            category = "",
            sku = "NOTE-01",
            unit = "pcs",
            buyingPrice = 30.0,
            sellingPrice = 55.0,
            quantity = 15,
            minStock = 2,
            supplier = "Local"
        )

        assertTrue(success)
        val updated = viewModel.products.value.first { it.id == product.id }
        assertEquals("Notebook A5", updated.name)
        assertEquals("", updated.category)
        assertEquals(55.0, updated.sellingPrice, 0.001)
    }

    @Test
    fun deleteProductRemovesProductAndCartEntry() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Item to Delete",
            category = "Groceries",
            sku = "DEL-01",
            unit = "pcs",
            buyingPrice = 10.0,
            sellingPrice = 20.0,
            quantity = 5,
            minStock = 1,
            supplier = ""
        )
        val product = viewModel.products.value.first { it.sku == "DEL-01" }
        viewModel.addToCart(product)
        assertTrue(viewModel.cart.value.any { it.product.id == product.id })

        val deleted = viewModel.deleteProduct(product.id)
        assertTrue(deleted)
        assertFalse(viewModel.products.value.any { it.id == product.id })
        assertFalse(viewModel.cart.value.any { it.product.id == product.id })

        // Deleting non-existent product returns false
        assertFalse(viewModel.deleteProduct("non-existent-id"))
    }

    @Test
    fun recordStockInAutoResolvesNotificationWhenStockBecomesHealthy() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Test Item",
            category = "Groceries",
            sku = "NOTIF-TEST-01",
            unit = "pcs",
            buyingPrice = 50.0,
            sellingPrice = 100.0,
            quantity = 1,
            minStock = 5,
            supplier = ""
        )
        val product = viewModel.products.value.first { it.sku == "NOTIF-TEST-01" }

        // Trigger test notification or verify threshold notification
        viewModel.triggerTestNotification()
        val unreadBefore = viewModel.notifications.value.filter { !it.isRead }
        assertTrue(unreadBefore.isNotEmpty())

        // Restock product above minStock
        viewModel.recordStockIn(
            productId = product.id,
            quantityAdded = 10,
            buyingPrice = 50.0,
            sellingPrice = 100.0,
            supplier = "Local",
            employeeName = "Owner"
        )

        // Verifies the notification containing this product was auto-resolved if all items became healthy
        val unreadForProduct = viewModel.notifications.value.filter {
            !it.isRead && product.id in it.productIds
        }
        assertTrue("Notifications for restocked healthy product should be auto-marked as read", unreadForProduct.isEmpty())
    }

    @Test
    fun markAllNotificationsReadAndClearNotificationsWork() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.triggerTestNotification()
        assertTrue(viewModel.notifications.value.isNotEmpty())

        viewModel.markAllNotificationsRead()
        assertTrue(viewModel.notifications.value.all { it.isRead })

        viewModel.clearNotifications()
        assertTrue(viewModel.notifications.value.isEmpty())
    }

    @Test
    fun completeSaleDeductsProductStockAndPersists() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Sugar",
            category = "Groceries",
            sku = "SUGAR-01",
            unit = "kg",
            buyingPrice = 100.0,
            sellingPrice = 150.0,
            quantity = 10,
            minStock = 2,
            supplier = "Local"
        )
        val product = viewModel.products.value.first { it.sku == "SUGAR-01" }
        viewModel.addToCart(product)
        viewModel.addToCart(product)
        viewModel.addToCart(product) // 3 items in cart

        val sale = viewModel.completeSale(
            paymentMethod = "Cash",
            customerName = "Jane Doe",
            customerPhone = "0712345678"
        )
        assertTrue(sale.id.isNotBlank())

        val updatedProduct = viewModel.products.value.first { it.id == product.id }
        assertEquals(7, updatedProduct.stockQuantity)

        val storageProducts = PosStorageManager(application).loadProducts()
        val persistedProduct = storageProducts.first { it.id == product.id }
        assertEquals(7, persistedProduct.stockQuantity)
    }

    @Test
    fun deleteSaleRestoresProductStock() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Flour",
            category = "Groceries",
            sku = "FLOUR-01",
            unit = "kg",
            buyingPrice = 120.0,
            sellingPrice = 160.0,
            quantity = 15,
            minStock = 3,
            supplier = "Local"
        )
        val product = viewModel.products.value.first { it.sku == "FLOUR-01" }
        viewModel.addToCart(product)
        viewModel.addToCart(product) // 2 items in cart

        val sale = viewModel.completeSale(paymentMethod = "Cash")
        assertTrue(sale.id.isNotBlank())
        assertEquals(13, viewModel.products.value.first { it.id == product.id }.stockQuantity)

        viewModel.deleteSale(sale.id)

        val restoredProduct = viewModel.products.value.first { it.id == product.id }
        assertEquals(15, restoredProduct.stockQuantity)

        val persisted = PosStorageManager(application).loadProducts().first { it.id == product.id }
        assertEquals(15, persisted.stockQuantity)
    }

    @Test
    fun updateSaleAdjustsStockDelta() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Milk",
            category = "Groceries",
            sku = "MILK-01",
            unit = "ltr",
            buyingPrice = 50.0,
            sellingPrice = 65.0,
            quantity = 20,
            minStock = 5,
            supplier = "Dairy"
        )
        val product = viewModel.products.value.first { it.sku == "MILK-01" }
        viewModel.addToCart(product)
        viewModel.addToCart(product)
        viewModel.addToCart(product) // 3 items in cart

        val sale = viewModel.completeSale(paymentMethod = "Cash")
        assertTrue(sale.id.isNotBlank())
        assertEquals(17, viewModel.products.value.first { it.id == product.id }.stockQuantity)

        // Update sale from 3 items to 5 items (+2 more sold)
        val saleWith5Items = sale.copy(
            items = listOf(CartItem(product = product, quantity = 5)),
            totalAmount = 5 * 65.0
        )
        viewModel.updateSale(saleWith5Items)
        assertEquals(15, viewModel.products.value.first { it.id == product.id }.stockQuantity)

        // Update sale from 5 items to 2 items (-3 fewer sold -> 3 restored)
        val saleWith2Items = sale.copy(
            items = listOf(CartItem(product = product, quantity = 2)),
            totalAmount = 2 * 65.0
        )
        viewModel.updateSale(saleWith2Items)
        assertEquals(18, viewModel.products.value.first { it.id == product.id }.stockQuantity)
    }

    @Test
    fun addCustomerCreditWithItemsDeductsProductStock() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Cooking Oil",
            category = "Groceries",
            sku = "OIL-01",
            unit = "ltr",
            buyingPrice = 200.0,
            sellingPrice = 250.0,
            quantity = 10,
            minStock = 2,
            supplier = "Local"
        )
        val product = viewModel.products.value.first { it.sku == "OIL-01" }

        val saved = viewModel.addCustomerCredit(
            customerName = "John Doe",
            phone = "0712345678",
            productsSummary = "4 x Cooking Oil",
            originalAmount = 1000.0,
            amountPaid = 200.0,
            dueDate = "Nov 1, 2026",
            paymentMethod = "Cash",
            itemsMap = mapOf(product.id to 4)
        )
        assertTrue(saved)

        val updatedProduct = viewModel.products.value.first { it.id == product.id }
        assertEquals(6, updatedProduct.stockQuantity)

        val creditSale = viewModel.salesHistory.value.firstOrNull { it.customerName == "John Doe" }
        assertTrue(creditSale != null)
        assertTrue(creditSale!!.isCreditSale)
        assertEquals(1000.0, creditSale.totalAmount, 0.001)
        assertEquals(200.0, creditSale.paidAmount, 0.001)
        assertEquals(800.0, creditSale.balance, 0.001)
    }

    @Test
    fun addProductWithRetailAndWholesalePricingSavesCorrectly() {
        val viewModel = PosViewModel(application, supabase = null)
        val saved = viewModel.addProduct(
            name = "Sugar 1kg",
            category = "Groceries",
            sku = "SUGAR-01",
            unit = "kg",
            buyingPrice = 120.0,
            sellingPrice = 150.0,
            quantity = 50,
            minStock = 5,
            supplier = "Sweet Mills",
            wholesalePrice = 135.0,
            wholesaleQuantity = 10
        )
        assertTrue(saved)

        val product = viewModel.products.value.first { it.sku == "SUGAR-01" }
        assertEquals(150.0, product.retailPrice, 0.001)
        assertEquals(150.0, product.sellingPrice, 0.001)
        assertEquals(135.0, product.wholesalePrice!!, 0.001)
        assertEquals(10, product.wholesaleQuantity!!)
        assertTrue(product.hasWholesalePricing)

        // Threshold checks
        assertFalse(product.isWholesaleFor(1))
        assertFalse(product.isWholesaleFor(9))
        assertTrue(product.isWholesaleFor(10))
        assertTrue(product.isWholesaleFor(25))

        // Price checks
        assertEquals(150.0, product.priceForQuantity(1), 0.001)
        assertEquals(150.0, product.priceForQuantity(9), 0.001)
        assertEquals(135.0, product.priceForQuantity(10), 0.001)
        assertEquals(135.0, product.priceForQuantity(25), 0.001)
    }

    @Test
    fun cartAutomaticallySwitchesToWholesalePriceWhenThresholdReached() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Soap Bar",
            category = "Household",
            sku = "SOAP-01",
            unit = "pcs",
            buyingPrice = 30.0,
            sellingPrice = 50.0,
            quantity = 100,
            minStock = 10,
            supplier = "Clean Co",
            wholesalePrice = 40.0,
            wholesaleQuantity = 5
        )
        val product = viewModel.products.value.first { it.sku == "SOAP-01" }

        // Add 4 items (below threshold 5) -> Retail mode
        repeat(4) { viewModel.addToCart(product) }
        val cartItemRetail = viewModel.cart.value.first { it.product.id == product.id }
        assertEquals(4, cartItemRetail.quantity)
        assertFalse(cartItemRetail.isWholesale)
        assertEquals(50.0, cartItemRetail.unitPrice, 0.001)
        assertEquals(200.0, cartItemRetail.totalPrice, 0.001)
        assertEquals(0.0, cartItemRetail.wholesaleSavings, 0.001)

        // Add 5th item -> reaches threshold 5 -> switches automatically to Wholesale mode!
        viewModel.addToCart(product)
        val cartItemWholesale = viewModel.cart.value.first { it.product.id == product.id }
        assertEquals(5, cartItemWholesale.quantity)
        assertTrue(cartItemWholesale.isWholesale)
        assertEquals(40.0, cartItemWholesale.unitPrice, 0.001)
        assertEquals(200.0, cartItemWholesale.totalPrice, 0.001) // 5 * 40 = 200
        assertEquals(50.0, cartItemWholesale.wholesaleSavings, 0.001) // (50 - 40) * 5 = 50 saved
    }

    @Test
    fun setCartQuantityDirectlyUpdatesModeAndSavings() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Mineral Water",
            category = "Beverages",
            sku = "WATER-01",
            unit = "bottle",
            buyingPrice = 20.0,
            sellingPrice = 35.0,
            quantity = 100,
            minStock = 5,
            supplier = "Springs",
            wholesalePrice = 28.0,
            wholesaleQuantity = 12
        )
        val product = viewModel.products.value.first { it.sku == "WATER-01" }

        // Set quantity directly to 20 (>= 12) -> Wholesale mode
        viewModel.setCartQuantity(product.id, 20)
        val cartItem = viewModel.cart.value.first { it.product.id == product.id }
        assertEquals(20, cartItem.quantity)
        assertTrue(cartItem.isWholesale)
        assertEquals(28.0, cartItem.unitPrice, 0.001)
        assertEquals(560.0, cartItem.totalPrice, 0.001)
        assertEquals(140.0, cartItem.wholesaleSavings, 0.001) // (35 - 28) * 20 = 140

        // Set quantity to 5 (< 12) -> Retail mode
        viewModel.setCartQuantity(product.id, 5)
        val cartItemReduced = viewModel.cart.value.first { it.product.id == product.id }
        assertEquals(5, cartItemReduced.quantity)
        assertFalse(cartItemReduced.isWholesale)
        assertEquals(35.0, cartItemReduced.unitPrice, 0.001)
        assertEquals(175.0, cartItemReduced.totalPrice, 0.001)
        assertEquals(0.0, cartItemReduced.wholesaleSavings, 0.001)
    }

    @Test
    fun completeSaleWithWholesaleItemsCalculatesCorrectTotalAndDeductsStock() {
        val viewModel = PosViewModel(application, supabase = null)
        viewModel.addProduct(
            name = "Cement 50kg",
            category = "Hardware",
            sku = "CEMENT-01",
            unit = "bag",
            buyingPrice = 650.0,
            sellingPrice = 750.0,
            quantity = 50,
            minStock = 10,
            supplier = "Bamburi",
            wholesalePrice = 700.0,
            wholesaleQuantity = 10
        )
        val product = viewModel.products.value.first { it.sku == "CEMENT-01" }

        // Customer buys 10 bags (wholesale)
        viewModel.setCartQuantity(product.id, 10)
        val sale = viewModel.completeSale(paymentMethod = "Cash", customerName = "Builder Bob")

        // Total should be 10 * 700 = 7000 (not 7500)
        assertEquals(7000.0, sale.totalAmount, 0.001)
        assertEquals(1, sale.items.size)
        assertTrue(sale.items.first().isWholesale)
        assertEquals(700.0, sale.items.first().unitPrice, 0.001)
        assertEquals(500.0, sale.items.first().wholesaleSavings, 0.001)

        // Inventory should be 40 left (50 - 10)
        assertEquals(40, viewModel.products.value.first { it.id == product.id }.stockQuantity)
    }
}



