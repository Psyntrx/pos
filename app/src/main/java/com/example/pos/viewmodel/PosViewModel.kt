package com.example.pos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos.data.CartItem
import com.example.pos.data.CreditLedgerUtils
import com.example.pos.data.CustomerCredit
import com.example.pos.data.Employee
import com.example.pos.data.Expense
import com.example.pos.data.PosStorageManager
import com.example.pos.data.Product
import com.example.pos.data.Sale
import com.example.pos.data.Shop
import com.example.pos.data.StockInRecord
import com.example.pos.data.SupabaseClient
import com.example.pos.data.SupabaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PosViewModel(
    application: Application,
    private val supabase: SupabaseService? = SupabaseClient.service
) : AndroidViewModel(application) {

    private val storageManager = PosStorageManager(application)

    // Shop & Profile
    private val defaultShop = Shop()
    private val _shop = MutableStateFlow(storageManager.loadShop(defaultShop))
    val shop: StateFlow<Shop> = _shop.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Navigation state
    private val _currentScreen = MutableStateFlow("splash") // "splash", "login", "forgot_password", "register_step1", "register_step2", "main"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Home, 1: Sales, 2: Inventory, 3: Reports, 4: More
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Products & Inventory
    private val defaultProducts = listOf(
        Product("p1", "Unga", "Groceries", "UG001", "pcs", 110.0, 150.0, 20, 10, "Pemba Millers", "groceries"),
        Product("p2", "Sukari", "Groceries", "SK001", "kg", 110.0, 140.0, 35, 15, "Mumias Sugar", "sugar"),
        Product("p3", "Cooking Oil", "Groceries", "CO001", "L", 340.0, 420.0, 12, 10, "Bidco Africa", "oil"),
        Product("p4", "Rice", "Groceries", "RI001", "kg", 260.0, 350.0, 8, 10, "Mwea Farmers", "rice"),
        Product("p5", "Detergent", "Household", "DE001", "pkt", 200.0, 280.0, 18, 5, "Unilever", "household"),
        Product("p6", "Fresh Milk", "Beverages", "MK001", "pkt", 50.0, 70.0, 4, 10, "Brookside", "beverage"),
        Product("p7", "Soda 500ml", "Beverages", "SD001", "btl", 45.0, 65.0, 40, 12, "Coca Cola", "beverage"),
        Product("p8", "Potato Chips", "Snacks", "SN001", "pkt", 80.0, 120.0, 25, 8, "Tropical", "snack")
    )
    private val _products = MutableStateFlow(storageManager.loadProducts(defaultProducts))
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // POS Cart
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // Payment & Completed Sale State
    private val _completedSale = MutableStateFlow<Sale?>(null)
    val completedSale: StateFlow<Sale?> = _completedSale.asStateFlow()

    // Sales History
    private val defaultSalesHistory = listOf(
        Sale("SL-1092", listOf(
            CartItem(Product("p1", "Unga", "Groceries", "UG001", "pcs", 110.0, 150.0, 20, 10), 3),
            CartItem(Product("p3", "Cooking Oil", "Groceries", "CO001", "L", 340.0, 420.0, 12, 10), 1),
            CartItem(Product("p2", "Sukari", "Groceries", "SK001", "kg", 110.0, 140.0, 35, 15), 1)
        ), 1010.0, "M-Pesa", "John Doe", "Today, 10:42 AM", System.currentTimeMillis() - 3600000),
        Sale("SL-1091", listOf(
            CartItem(Product("p7", "Soda 500ml", "Beverages", "SD001", "btl", 45.0, 65.0, 40, 12), 4)
        ), 260.0, "Cash", "Walk-in Customer", "Today, 09:15 AM", System.currentTimeMillis() - 7200000),
        Sale("SL-1090", listOf(
            CartItem(Product("p4", "Rice", "Groceries", "RI001", "kg", 260.0, 350.0, 8, 10), 5)
        ), 1750.0, "M-Pesa", "Sarah M.", "Yesterday, 04:30 PM", System.currentTimeMillis() - 86400000)
    )
    private val _salesHistory = MutableStateFlow(storageManager.loadSales(defaultSalesHistory))
    val salesHistory: StateFlow<List<Sale>> = _salesHistory.asStateFlow()

    // Expenses
    private val _expenses = MutableStateFlow(storageManager.loadExpenses(emptyList()))
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    // Customer Credit Balances
    private val _customerCredits = MutableStateFlow(storageManager.loadCustomerCredits(emptyList()))
    val customerCredits: StateFlow<List<CustomerCredit>> = _customerCredits.asStateFlow()

    // Employees
    private val defaultEmployees = listOf(
        Employee("emp1", "Faith W.", "Owner", "+254 712 345 678", "faith@store.com", isOwner = true),
        Employee("emp2", "Kevin Otieno", "Sales Clerk", "+254 720 998 877", "kevin@store.com"),
        Employee("emp3", "Amina Hassan", "Manager", "+254 735 112 233", "amina@store.com")
    )
    private val _employees = MutableStateFlow(storageManager.loadEmployees(defaultEmployees))
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    // Stock In History
    private val defaultStockInRecords = listOf(
        StockInRecord("stk1", "p1", "Unga", 50, 110.0, "Pemba Millers", "Sep 28, 2026 09:41 AM", "Faith"),
        StockInRecord("stk2", "p3", "Cooking Oil", 20, 340.0, "Bidco Africa", "Sep 25, 2026 02:15 PM", "Amina")
    )
    private val _stockInRecords = MutableStateFlow(storageManager.loadStockInRecords(defaultStockInRecords))
    val stockInRecords: StateFlow<List<StockInRecord>> = _stockInRecords.asStateFlow()

    init {
        if (_expenses.value.isEmpty()) {
            _expenses.value = CreditLedgerUtils.buildDemoExpenses()
        }

        storageManager.saveProducts(_products.value)
        storageManager.saveSales(_salesHistory.value)
        storageManager.saveExpenses(_expenses.value)
        storageManager.saveCustomerCredits(_customerCredits.value)
        storageManager.saveEmployees(_employees.value)
        storageManager.saveStockInRecords(_stockInRecords.value)
        storageManager.saveShop(_shop.value)
        syncWithSupabase()
    }

    // Guard against race conditions when local product updates are in flight
    private val pendingProductUpdates = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var lastSyncTime = 0L

    /**
     * Synchronize Products, Expenses, Sales, and Stock Records with Supabase.
     * Offline-first: Falls back seamlessly to local storage cache if network is unavailable.
     * Includes a throttle guard so rapid UI transitions do not trigger redundant or racing requests.
     */
    fun syncWithSupabase(force: Boolean = false) {
        val service = supabase ?: return

        val now = System.currentTimeMillis()
        if (!force && (now - lastSyncTime < 15_000L)) {
            return
        }
        lastSyncTime = now

        viewModelScope.launch {
            service.getProducts().onSuccess { remoteProducts ->
                if (remoteProducts.isNotEmpty()) {
                    _products.update { currentList ->
                        val currentMap = currentList.associateBy { it.id }
                        remoteProducts.map { remote ->
                            if (pendingProductUpdates.contains(remote.id)) {
                                currentMap[remote.id] ?: remote
                            } else {
                                remote
                            }
                        }
                    }
                    storageManager.saveProducts(_products.value)
                }
            }

            service.getExpenses().onSuccess { remoteExpenses ->
                _expenses.value = remoteExpenses
                storageManager.saveExpenses(remoteExpenses)
            }

            service.getCustomerCredits().onSuccess { remoteCredits ->
                val mergedCredits = CreditLedgerUtils.mergeRemoteCredits(remoteCredits, _customerCredits.value)
                _customerCredits.value = mergedCredits
                storageManager.saveCustomerCredits(mergedCredits)
            }

            service.getSales().onSuccess { remoteSales ->
                if (remoteSales.isNotEmpty()) {
                    _salesHistory.value = remoteSales
                    storageManager.saveSales(remoteSales)
                }
            }

            service.getStockInRecords().onSuccess { remoteStockIn ->
                if (remoteStockIn.isNotEmpty()) {
                    _stockInRecords.value = remoteStockIn
                    storageManager.saveStockInRecords(remoteStockIn)
                }
            }

            service.getShop().onSuccess { remoteShop ->
                if (remoteShop != null) {
                    _shop.value = remoteShop
                    storageManager.saveShop(remoteShop)
                }
            }
        }
    }

    // Navigation & Auth actions
    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun login(email: String, pass: String) {
        _isLoggedIn.value = true
        _currentScreen.value = "main"
    }

    fun registerShop(shopName: String, ownerName: String, email: String, phone: String, address: String, category: String) {
        val newShop = Shop(
            name = shopName.ifBlank { "Faith's Store" },
            ownerName = ownerName.ifBlank { "Faith" },
            email = email.ifBlank { "faith@store.com" },
            phone = phone.ifBlank { "+254 712 345 678" },
            address = address.ifBlank { "Nairobi, Kenya" },
            category = category.ifBlank { "Retail & Grocery" }
        )
        _shop.value = newShop
        storageManager.saveShop(newShop)
        _isLoggedIn.value = true
        _currentScreen.value = "main"
    }

    fun updateShopInformation(shop: Shop) {
        _shop.value = shop
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentScreen.value = "login"
    }

    // Category & Search
    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Cart operations
    fun addToCart(product: Product) {
        if (product.stockQuantity <= 0) return
        _cart.update { currentCart ->
            val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }
            if (existingIndex >= 0) {
                val existing = currentCart[existingIndex]
                if (existing.quantity < product.stockQuantity) {
                    currentCart.toMutableList().apply {
                        this[existingIndex] = existing.copy(quantity = existing.quantity + 1)
                    }
                } else currentCart
            } else {
                currentCart + CartItem(product, 1)
            }
        }
    }

    fun removeFromCart(productId: String) {
        _cart.update { currentCart ->
            val existingIndex = currentCart.indexOfFirst { it.product.id == productId }
            if (existingIndex >= 0) {
                val existing = currentCart[existingIndex]
                if (existing.quantity > 1) {
                    currentCart.toMutableList().apply {
                        this[existingIndex] = existing.copy(quantity = existing.quantity - 1)
                    }
                } else {
                    currentCart.filterNot { it.product.id == productId }
                }
            } else currentCart
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // Process Sale & Automatic Stock Deduction
    fun completeSale(
        paymentMethod: String,
        customerName: String = "",
        customerPhone: String = "",
        isCredit: Boolean = false,
        paidAmount: Double = 0.0,
        dueDate: String = ""
    ): Sale {
        val currentCartItems = _cart.value
        val total = currentCartItems.sumOf { it.totalPrice }
        val creditTotal = if (isCredit) {
            CreditLedgerUtils.normalizeMoney(total)
                ?: throw IllegalArgumentException("Credit sale total is invalid.")
        } else {
            total
        }
        val creditPaid = if (isCredit) {
            CreditLedgerUtils.normalizeMoney(paidAmount)
                ?: throw IllegalArgumentException("Initial payment is invalid.")
        } else {
            total
        }
        val creditBalance = if (isCredit) {
            CreditLedgerUtils.calculateOutstandingBalance(creditTotal, creditPaid)
                ?: throw IllegalArgumentException("Initial payment exceeds the credit sale total.")
        } else {
            0.0
        }
        val trimmedCustomerName = customerName.trim()
        if (isCredit && trimmedCustomerName.isBlank()) {
            throw IllegalArgumentException("Customer name is required for credit sales.")
        }
        val normalizedCustomerPhone = if (isCredit) {
            CreditLedgerUtils.normalizePhoneNumber(customerPhone)
                ?: throw IllegalArgumentException("Valid phone number is required for credit sales.")
        } else {
            customerPhone.trim()
        }

        val creditRecord = if (isCredit) {
            CreditLedgerUtils.createCredit(
                id = "c_${System.currentTimeMillis()}",
                customerName = trimmedCustomerName,
                customerPhone = normalizedCustomerPhone,
                originalAmount = creditTotal,
                amountPaid = creditPaid,
                productsSummary = "${currentCartItems.size} Items | ${currentCartItems.joinToString { it.product.name }}",
                dueDate = dueDate.ifBlank { defaultCreditDueDate() },
                paymentMethod = paymentMethod
            ) ?: throw IllegalArgumentException("Credit sale due date or details are invalid.")
        } else {
            null
        }

        // Deduct inventory
        _products.update { currentList ->
            currentList.map { product ->
                val cartItem = currentCartItems.find { it.product.id == product.id }
                if (cartItem != null) {
                    val newQty = (product.stockQuantity - cartItem.quantity).coerceAtLeast(0)
                    product.copy(stockQuantity = newQty)
                } else {
                    product
                }
            }
        }
        storageManager.saveProducts(_products.value)

        val newSale = Sale(
            id = "SL-${(1093..9999).random()}",
            items = currentCartItems,
            totalAmount = creditTotal,
            paymentMethod = paymentMethod,
            customerName = if (trimmedCustomerName.isNotBlank()) trimmedCustomerName else if (isCredit) "Credit Customer" else "Walk-in Customer",
            dateFormatted = "Just now",
            timestamp = System.currentTimeMillis(),
            isCreditSale = isCredit,
            paidAmount = creditPaid,
            balance = creditBalance,
            dueDate = if (isCredit) creditRecord?.dueDate.orEmpty() else dueDate
        )

        _salesHistory.update { listOf(newSale) + it }
        storageManager.saveSales(_salesHistory.value)

        if (isCredit && creditRecord != null) {
            _customerCredits.update { listOf(creditRecord) + it }
            storageManager.saveCustomerCredits(_customerCredits.value)
            val service = supabase
            if (service != null) {
                viewModelScope.launch { service.upsertCustomerCredit(creditRecord) }
            }
        }

        _completedSale.value = newSale
        _cart.value = emptyList()
        return newSale
    }

    fun dismissReceiptModal() {
        _completedSale.value = null
    }

    fun defaultCreditDueDate(): String {
        val days = storageManager.loadCreditDurationDays(CreditLedgerUtils.defaultCreditDurationDays())
        return CreditLedgerUtils.calculateDueDate(days)
    }

    fun getCreditDurationDays(): Int = storageManager.loadCreditDurationDays(CreditLedgerUtils.defaultCreditDurationDays())

    fun setCreditDurationDays(days: Int) {
        storageManager.saveCreditDurationDays(days.coerceAtLeast(1))
    }

    fun addCustomerCredit(
        customerName: String,
        phone: String,
        productsSummary: String,
        originalAmount: Double,
        amountPaid: Double,
        dueDate: String,
        paymentMethod: String = "Cash"
    ): Boolean {
        val normalizedName = customerName.trim()
        if (normalizedName.isBlank()) return false
        val normalizedPhone = CreditLedgerUtils.normalizePhoneNumber(phone) ?: return false
        val creditRecord = CreditLedgerUtils.createCredit(
            id = "c_${System.currentTimeMillis()}",
            customerName = normalizedName,
            customerPhone = normalizedPhone,
            originalAmount = originalAmount,
            amountPaid = amountPaid,
            productsSummary = productsSummary.ifBlank { "1 Item | Product" },
            dueDate = dueDate.ifBlank { defaultCreditDueDate() },
            paymentMethod = paymentMethod
        ) ?: return false

        _customerCredits.update { listOf(creditRecord) + it }
        storageManager.saveCustomerCredits(_customerCredits.value)
        val service = supabase ?: return true
        viewModelScope.launch { service.upsertCustomerCredit(creditRecord) }
        return true
    }

    fun recordRepayment(
        creditId: String,
        amount: Double,
        method: String,
        referenceCode: String = "",
        notes: String = ""
    ): Result<CustomerCredit> {
        val currentCredit = _customerCredits.value.find { it.id == creditId }
            ?: return Result.failure(IllegalArgumentException("Credit record not found."))

        return try {
            val updatedCredit = CreditLedgerUtils.applyRepayment(currentCredit, amount, method, referenceCode, notes)
            _customerCredits.update { currentList ->
                currentList.map { if (it.id == creditId) updatedCredit else it }
            }
            storageManager.saveCustomerCredits(_customerCredits.value)
            val service = supabase ?: return Result.success(updatedCredit)
            viewModelScope.launch { service.upsertCustomerCredit(updatedCredit) }
            Result.success(updatedCredit)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    // Add Product
    fun addProduct(
        name: String, category: String, sku: String, unit: String,
        buyingPrice: Double, sellingPrice: Double, quantity: Int, minStock: Int, supplier: String
    ): Boolean {
        val normalizedName = name.trim()
        val normalizedCategory = category.trim()
        val normalizedSku = sku.trim()
        val normalizedUnit = unit.trim()
        if (normalizedName.isBlank() || normalizedCategory.isBlank() || normalizedSku.isBlank() ||
            normalizedUnit.isBlank() || !buyingPrice.isFinite() || buyingPrice < 0 ||
            !sellingPrice.isFinite() || sellingPrice <= 0 || quantity < 0 || minStock < 0 ||
            _products.value.any { it.sku.equals(normalizedSku, ignoreCase = true) }
        ) return false

        val newProduct = Product(
            id = "p_${System.currentTimeMillis()}",
            name = normalizedName,
            category = normalizedCategory,
            sku = normalizedSku,
            unit = normalizedUnit,
            buyingPrice = buyingPrice,
            sellingPrice = sellingPrice,
            stockQuantity = quantity,
            minStock = minStock,
            supplier = supplier.trim(),
            iconType = normalizedCategory.lowercase()
        )
        _products.update { listOf(newProduct) + it }
        return true
    }

    fun updateProduct(
        productId: String,
        name: String, category: String, sku: String, unit: String,
        buyingPrice: Double, sellingPrice: Double, quantity: Int, minStock: Int, supplier: String
    ): Boolean {
        val currentProduct = _products.value.find { it.id == productId } ?: return false
        val normalizedName = name.trim()
        val normalizedCategory = category.trim()
        val normalizedSku = sku.trim()
        val normalizedUnit = unit.trim()
        if (normalizedName.isBlank() || normalizedCategory.isBlank() || normalizedSku.isBlank() ||
            normalizedUnit.isBlank() || !buyingPrice.isFinite() || buyingPrice < 0 ||
            !sellingPrice.isFinite() || sellingPrice <= 0 || quantity < 0 || minStock < 0 ||
            _products.value.any { it.id != productId && it.sku.equals(normalizedSku, ignoreCase = true) }
        ) return false

        val updatedProduct = currentProduct.copy(
            name = normalizedName,
            category = normalizedCategory,
            sku = normalizedSku,
            unit = normalizedUnit,
            buyingPrice = buyingPrice,
            sellingPrice = sellingPrice,
            stockQuantity = quantity,
            minStock = minStock,
            supplier = supplier.trim(),
            iconType = normalizedCategory.lowercase()
        )
        _products.update { products -> products.map { if (it.id == productId) updatedProduct else it } }
        storageManager.saveProducts(_products.value)

        // Asynchronously update product in Supabase while safeguarding local state
        val service = supabase ?: return true
        viewModelScope.launch {
            pendingProductUpdates.add(productId)
            try {
                service.upsertProduct(updatedProduct)
            } finally {
                pendingProductUpdates.remove(productId)
            }
        }
        return true
    }

    // Stock In / Restocking
    fun recordStockIn(
        productId: String,
        quantityAdded: Int,
        buyingPrice: Double,
        sellingPrice: Double = 0.0,
        supplier: String,
        employeeName: String
    ) {
        _products.update { currentList ->
            currentList.map { p ->
                if (p.id == productId) {
                    p.copy(
                        buyingPrice = if (buyingPrice > 0) buyingPrice else p.buyingPrice,
                        sellingPrice = if (sellingPrice > 0) sellingPrice else p.sellingPrice,
                        supplier = if (supplier.isNotBlank()) supplier else p.supplier
                    )
                } else p
            }
        }
        storageManager.saveProducts(_products.value)

        val prodName = _products.value.find { it.id == productId }?.name ?: "Product"
        val record = StockInRecord(
            id = "stk_${System.currentTimeMillis()}",
            productId = productId,
            productName = prodName,
            quantityAdded = quantityAdded,
            buyingPrice = buyingPrice,
            supplier = supplier,
            dateFormatted = "Today, 10:00 AM",
            employeeName = employeeName
        )
        _stockInRecords.update { listOf(record) + it }
        storageManager.saveStockInRecords(_stockInRecords.value)

        // Asynchronously update product inventory and insert stock-in record in Supabase
        val updatedProduct = _products.value.find { it.id == productId }
        val service = supabase ?: return
        viewModelScope.launch {
            pendingProductUpdates.add(productId)
            try {
                service.insertStockInRecord(record)
                if (updatedProduct != null) {
                    service.upsertProduct(updatedProduct)
                }
            } finally {
                pendingProductUpdates.remove(productId)
            }
        }
    }

    // Expenses
    fun addExpense(title: String, category: String, amount: Double): Boolean {
        val normalizedTitle = title.trim()
        val normalizedCategory = category.trim()
        val safeAmount = amount.coerceAtLeast(0.0)
        if (normalizedTitle.isBlank() || normalizedCategory.isBlank() || safeAmount <= 0.0) return false

        val newExpense = Expense(
            id = "exp_${System.currentTimeMillis()}",
            title = normalizedTitle,
            category = normalizedCategory,
            amount = safeAmount,
            dateFormatted = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")),
            iconType = normalizedCategory.lowercase()
        )
        _expenses.update { listOf(newExpense) + it }
        storageManager.saveExpenses(_expenses.value)
        val service = supabase ?: return true
        viewModelScope.launch { service.insertExpense(newExpense) }
        return true
    }

    fun updateExpense(expenseId: String, title: String, category: String, amount: Double): Boolean {
        val normalizedTitle = title.trim()
        val normalizedCategory = category.trim()
        if (normalizedTitle.isBlank() || normalizedCategory.isBlank()) return false

        val currentExpense = _expenses.value.find { it.id == expenseId } ?: return false
        val updatedExpense = currentExpense.copy(
            title = normalizedTitle,
            category = normalizedCategory,
            amount = amount.coerceAtLeast(0.0),
            iconType = normalizedCategory.lowercase()
        )

        _expenses.update { currentList ->
            currentList.map { if (it.id == expenseId) updatedExpense else it }
        }
        storageManager.saveExpenses(_expenses.value)
        val service = supabase ?: return true
        viewModelScope.launch { service.upsertExpense(updatedExpense) }
        return true
    }

    fun deleteExpenses(ids: Set<String>) {
        if (ids.isEmpty()) return
        _expenses.update { currentList -> currentList.filterNot { ids.contains(it.id) } }
        storageManager.saveExpenses(_expenses.value)
        val service = supabase ?: return
        ids.forEach { id ->
            viewModelScope.launch { service.deleteExpense(id) }
        }
    }

    // Add Employee
    fun addEmployee(name: String, role: String, phone: String, email: String) {
        val emp = Employee(
            id = "emp_${System.currentTimeMillis()}",
            name = name,
            role = role,
            phone = phone,
            email = email
        )
        _employees.update { it + emp }
        storageManager.saveEmployees(_employees.value)
    }
}
