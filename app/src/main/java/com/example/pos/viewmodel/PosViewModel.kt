package com.example.pos.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos.data.AppNotification
import com.example.pos.data.AuthSession
import com.example.pos.data.CartItem
import com.example.pos.data.CreditLedgerUtils
import com.example.pos.data.CustomerCredit
import com.example.pos.data.InventoryAlertKind
import com.example.pos.data.LowStockNotificationManager
import com.example.pos.data.Employee
import com.example.pos.data.EmployeePermissions
import com.example.pos.data.ApprovalRequest
import com.example.pos.data.Expense
import com.example.pos.data.PosStorageManager
import com.example.pos.data.Product
import com.example.pos.data.ProductImageStorage
import com.example.pos.data.Sale
import com.example.pos.data.Shop
import com.example.pos.data.StockInRecord
import com.example.pos.data.SupabaseClient
import com.example.pos.data.SupabaseService
import com.example.pos.data.ReminderConfig
import com.example.pos.data.ScheduledReminder
import com.example.pos.data.ReminderExecutionRecord
import com.example.pos.data.SyncOperation
import com.example.pos.data.NotificationCategory
import com.example.pos.data.ReminderDeliveryChannel
import com.example.pos.data.ReminderEventType
import com.example.pos.data.DeliveryStatus
import com.example.pos.data.ReminderSchedulerEngine
import com.example.pos.data.WhatsAppMessagingProvider
import com.example.pos.data.SmsMessagingProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PosViewModel(
    private val application: Application? = null,
    private val storageManager: PosStorageManager = PosStorageManager(application),
    private val supabase: SupabaseService? = SupabaseClient.service
) : ViewModel() {

    // Shop & Profile
    private val defaultShop = Shop()
    private val _shop = MutableStateFlow(storageManager.loadShop(defaultShop))
    val shop: StateFlow<Shop> = _shop.asStateFlow()

    private val initialSession = storageManager.loadAuthSession()
    private val isInitialSessionValid = initialSession?.isSessionValid() == true

    private val _isLoggedIn = MutableStateFlow(isInitialSessionValid)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Navigation state: if an active session is restored, launch directly into "main", avoiding login/splash interruption
    private val _currentScreen = MutableStateFlow(if (isInitialSessionValid) "main" else "splash")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Home, 1: Sales, 2: Stock, 3: Reports, 4: More
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Products & Stock
    private val defaultProducts = emptyList<Product>()
    private val _products = MutableStateFlow(storageManager.loadProducts(defaultProducts))
    val products: StateFlow<List<Product>> = _products.asStateFlow()
    private val _notifications = MutableStateFlow(storageManager.loadNotifications())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()
    private val _systemNotificationsEnabled = MutableStateFlow(storageManager.areSystemNotificationsEnabled())
    val systemNotificationsEnabled: StateFlow<Boolean> = _systemNotificationsEnabled.asStateFlow()

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
    private val defaultSalesHistory = emptyList<Sale>()
    private val _salesHistory = MutableStateFlow(storageManager.loadSales(defaultSalesHistory))
    val salesHistory: StateFlow<List<Sale>> = _salesHistory.asStateFlow()

    // Expenses
    private val defaultExpenses = emptyList<Expense>()
    private val _expenses = MutableStateFlow(storageManager.loadExpenses(defaultExpenses))
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    // Customer Credit Balances
    private val defaultCustomerCredits = emptyList<CustomerCredit>()
    private val _customerCredits = MutableStateFlow(storageManager.loadCustomerCredits(defaultCustomerCredits))
    val customerCredits: StateFlow<List<CustomerCredit>> = _customerCredits.asStateFlow()

    // Employees
    private val defaultEmployees = emptyList<Employee>()
    private val _employees = MutableStateFlow(storageManager.loadEmployees(defaultEmployees))
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    // Stock In History
    private val defaultStockInRecords = emptyList<StockInRecord>()
    private val _stockInRecords = MutableStateFlow(storageManager.loadStockInRecords(defaultStockInRecords))
    val stockInRecords: StateFlow<List<StockInRecord>> = _stockInRecords.asStateFlow()

    // Authorization & Current User Role
    private val _isOwner = MutableStateFlow(storageManager.loadCurrentUserRole() == "OWNER")
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _currentEmployee = MutableStateFlow<Employee?>(
        storageManager.loadActiveEmployeeId()?.let { activeId ->
            _employees.value.find { it.id == activeId }
        }
    )
    val currentEmployee: StateFlow<Employee?> = _currentEmployee.asStateFlow()

    // Pending Approval Lists
    private val _pendingEmployees = MutableStateFlow<List<Employee>>(emptyList())
    val pendingEmployees: StateFlow<List<Employee>> = _pendingEmployees.asStateFlow()

    private val _pendingStockIns = MutableStateFlow<List<StockInRecord>>(emptyList())
    val pendingStockIns: StateFlow<List<StockInRecord>> = _pendingStockIns.asStateFlow()

    private val _pendingNewProducts = MutableStateFlow<List<Product>>(emptyList())
    val pendingNewProducts: StateFlow<List<Product>> = _pendingNewProducts.asStateFlow()

    private val _pendingExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val pendingExpenses: StateFlow<List<Expense>> = _pendingExpenses.asStateFlow()

    private val _pendingCreditSales = MutableStateFlow<List<Sale>>(emptyList())
    val pendingCreditSales: StateFlow<List<Sale>> = _pendingCreditSales.asStateFlow()

    private val _totalPendingCount = MutableStateFlow(0)
    val totalPendingCount: StateFlow<Int> = _totalPendingCount.asStateFlow()

    // Reminder System & Persistent Schedules
    private val _reminderConfig = MutableStateFlow(storageManager.loadReminderConfig())
    val reminderConfig: StateFlow<ReminderConfig> = _reminderConfig.asStateFlow()

    private val _scheduledReminders = MutableStateFlow(storageManager.loadScheduledReminders())
    val scheduledReminders: StateFlow<List<ScheduledReminder>> = _scheduledReminders.asStateFlow()

    private val _reminderExecutionHistory = MutableStateFlow(storageManager.loadReminderExecutionHistory())
    val reminderExecutionHistory: StateFlow<List<ReminderExecutionRecord>> = _reminderExecutionHistory.asStateFlow()

    // Offline Persistent Sync Queue
    private val _pendingSyncOperations = MutableStateFlow(storageManager.loadPendingSyncOperations())
    val pendingSyncOperations: StateFlow<List<SyncOperation>> = _pendingSyncOperations.asStateFlow()

    private val whatsAppProvider = WhatsAppMessagingProvider(application)
    private val smsProvider = SmsMessagingProvider(application)
    private var reminderEvaluationJob: Job? = null

    private fun startReminderEvaluationLoop() {
        if (reminderEvaluationJob?.isActive == true) return
        reminderEvaluationJob = viewModelScope.launch {
            while (isActive) {
                if (_isLoggedIn.value && !getActiveShopId().isNullOrBlank()) {
                    evaluateAndProcessPendingReminders()
                }
                delay(60_000L)
            }
        }
    }

    fun getActiveShopId(): String? {
        return _shop.value.id.takeIf { it.isNotBlank() && it != "shop_default" && it != "shop_1" }
            ?: _currentEmployee.value?.shopId?.takeIf { it.isNotBlank() && it != "shop_default" && it != "shop_1" }
            ?: storageManager.loadShop(defaultShop).id.takeIf { it.isNotBlank() && it != "shop_default" && it != "shop_1" }
    }

    init {
        // Save initial defaults locally if not already stored
        storageManager.saveProducts(_products.value)
        storageManager.saveSales(_salesHistory.value)
        storageManager.saveExpenses(_expenses.value)
        storageManager.saveCustomerCredits(_customerCredits.value)
        storageManager.saveEmployees(_employees.value)
        storageManager.saveStockInRecords(_stockInRecords.value)
        storageManager.saveShop(_shop.value)

        val storedRole = storageManager.loadCurrentUserRole()
        val activeEmpId = storageManager.loadActiveEmployeeId()
        val activeEmp = _employees.value.find { it.id == activeEmpId }
        if (storedRole == "EMPLOYEE") {
            _isOwner.value = false
            _currentEmployee.value = activeEmp
            if (activeEmp?.status == "PENDING") {
                _currentScreen.value = "employee_waiting_approval"
            }
            if (activeEmp != null && !activeEmp.shopId.isNullOrBlank() && (_shop.value.id.isBlank() || _shop.value.id == "shop_default" || _shop.value.id == "shop_1")) {
                _shop.value = _shop.value.copy(id = activeEmp.shopId)
                storageManager.saveShop(_shop.value)
            }
        } else {
            _isOwner.value = true
            _currentEmployee.value = null
        }
        updatePendingLists()

        // Restore authenticated Supabase session if valid
        if (supabase != null) {
            initialSession?.let { session ->
                if (session.isSessionValid()) {
                    supabase.setTokens(
                        access = session.accessToken,
                        refresh = session.refreshToken,
                        expiresAt = session.tokenExpiresAt
                    )
                    // If token is expiring or expired, refresh silently in the background
                    if (session.refreshToken.isNotBlank() && session.isAccessTokenExpired()) {
                        viewModelScope.launch {
                            refreshSession(session.refreshToken, session.rememberMe)
                        }
                    }
                }
            }

            // Keep local persistent storage synchronized if Supabase refreshes the token
            supabase.onSessionRefreshedListener = { refreshedSession ->
                storageManager.saveAuthSession(refreshedSession)
            }
        }
        observeInventoryThresholds()

        startReminderEvaluationLoop()

        // Asynchronously pull live data from Supabase
        syncWithSupabase()
    }

    private fun observeInventoryThresholds() {
        viewModelScope.launch {
            var previous = _products.value.associateBy { it.id }
            // If there are no saved notifications yet, populate initial alerts for any current low or out of stock products
            if (_notifications.value.isEmpty()) {
                val current = _products.value
                val initialOut = current.filter { it.stockQuantity <= 0 }
                val initialLow = current.filter { it.stockQuantity > 0 && it.stockQuantity <= it.minStock }
                createInventoryNotification(InventoryAlertKind.OUT_OF_STOCK, initialOut)
                createInventoryNotification(InventoryAlertKind.LOW_STOCK, initialLow)
            }
            _products.collect { productsNow ->
                val current = productsNow.associateBy { it.id }
                val outOfStock = current.values.filter { product ->
                    product.stockQuantity <= 0 && (previous[product.id]?.stockQuantity ?: 1) > 0
                }
                val lowStock = current.values.filter { product ->
                    val old = previous[product.id]
                    product.stockQuantity > 0 && product.stockQuantity <= product.minStock &&
                        (old == null || old.stockQuantity > old.minStock ||
                            (old.minStock != product.minStock && old.stockQuantity > product.minStock))
                }

                createInventoryNotification(InventoryAlertKind.OUT_OF_STOCK, outOfStock)
                createInventoryNotification(InventoryAlertKind.LOW_STOCK, lowStock)

                // Auto-mark notifications as read if all affected products are now healthy in stock
                val currentNotifications = _notifications.value
                val hasStaleUnread = currentNotifications.any { notif ->
                    !notif.isRead && notif.productIds.isNotEmpty() && notif.productIds.all { id ->
                        val p = current[id]
                        p != null && !p.isLowStock
                    }
                }
                if (hasStaleUnread) {
                    _notifications.update { items ->
                        items.map { notif ->
                            if (!notif.isRead && notif.productIds.isNotEmpty()) {
                                val allRestocked = notif.productIds.all { id ->
                                    val p = current[id]
                                    p != null && !p.isLowStock
                                }
                                if (allRestocked) notif.copy(isRead = true) else notif
                            } else notif
                        }
                    }
                    storageManager.saveNotifications(_notifications.value)
                }

                previous = current
            }
        }
    }

    private fun createInventoryNotification(kind: InventoryAlertKind, products: List<Product>, force: Boolean = false) {
        if (products.isEmpty()) return

        // Prevent duplicate unread notifications for identical product sets and kind unless force is requested
        if (!force) {
            val currentUnread = _notifications.value.filter { !it.isRead && it.kind == kind }
            val newProductIds = products.map { it.id }.toSet()
            if (currentUnread.any { it.productIds.toSet() == newProductIds }) {
                return
            }
        }

        val isOut = kind == InventoryAlertKind.OUT_OF_STOCK
        val title = when {
            isOut && products.size == 1 -> "${products.first().name} is out of stock"
            isOut -> "${products.size} products are out of stock"
            products.size == 1 -> "${products.first().name} is low on stock"
            else -> "${products.size} products are low on stock"
        }
        val names = products.take(4).joinToString(", ") { it.name } +
            if (products.size > 4) " and ${products.size - 4} more" else ""
        val message = if (isOut) "$names. Restock these items." else "$names. Check your inventory and restock soon."
        val notification = AppNotification(
            id = "${kind.storageValue}_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}",
            kind = kind,
            title = title,
            message = message,
            productIds = products.map { it.id }
        )
        _notifications.update { (listOf(notification) + it).take(100) }
        storageManager.saveNotifications(_notifications.value)
        if (_systemNotificationsEnabled.value) {
            application?.let { LowStockNotificationManager.show(it, notification) }
        }
    }

    fun setSystemNotificationsEnabled(enabled: Boolean) {
        _systemNotificationsEnabled.value = enabled
        storageManager.setSystemNotificationsEnabled(enabled)
    }

    fun markAllNotificationsRead() {
        _notifications.update { items -> items.map { it.copy(isRead = true) } }
        storageManager.saveNotifications(_notifications.value)
    }

    fun markNotificationAsRead(notificationId: String) {
        _notifications.update { items ->
            items.map { if (it.id == notificationId) it.copy(isRead = true) else it }
        }
        storageManager.saveNotifications(_notifications.value)
    }

    fun markNotificationAsUnread(notificationId: String) {
        _notifications.update { items ->
            items.map { if (it.id == notificationId) it.copy(isRead = false) else it }
        }
        storageManager.saveNotifications(_notifications.value)
    }

    fun deleteNotification(notificationId: String) {
        _notifications.update { items -> items.filterNot { it.id == notificationId } }
        storageManager.saveNotifications(_notifications.value)
    }

    fun triggerTestNotification() {
        val lowStockProducts = _products.value.filter { it.isLowStock }
        val sampleProducts = if (lowStockProducts.isNotEmpty()) {
            lowStockProducts
        } else {
            _products.value.take(2)
        }
        if (sampleProducts.isEmpty()) {
            val demoNotification = AppNotification(
                id = "test_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}",
                kind = InventoryAlertKind.LOW_STOCK,
                title = "Test stock alert",
                message = "This is a test notification. Your notification system is working properly.",
                productIds = emptyList()
            )
            _notifications.update { (listOf(demoNotification) + it).take(100) }
            storageManager.saveNotifications(_notifications.value)
            if (_systemNotificationsEnabled.value) {
                application?.let { LowStockNotificationManager.show(it, demoNotification) }
            }
        } else {
            val kind = if (sampleProducts.any { it.stockQuantity <= 0 }) {
                InventoryAlertKind.OUT_OF_STOCK
            } else {
                InventoryAlertKind.LOW_STOCK
            }
            createInventoryNotification(kind, sampleProducts, force = true)
        }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
        storageManager.saveNotifications(emptyList())
    }

    // Guard against race conditions when local product updates are in flight
    private val pendingProductUpdates = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var lastSyncTime = 0L

    /**
     * Synchronize Products, Expenses, Sales, and Stock Records with Supabase.
     * Scoped to the authenticated user's shop for complete multi-tenant isolation.
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
            val activeEmail = storageManager.loadAuthSession()?.userEmail?.takeIf { it.isNotBlank() }
                ?: _shop.value.email.takeIf { it.isNotBlank() }

            val isEmployee = storageManager.loadCurrentUserRole() == "EMPLOYEE" || !_isOwner.value
            var activeShopId: String? = null

            if (isEmployee) {
                val emp = _currentEmployee.value
                    ?: (if (!activeEmail.isNullOrBlank()) service.getEmployeeByEmail(activeEmail).getOrNull() else null)
                if (emp != null) {
                    _currentEmployee.value = emp
                    _isOwner.value = false
                    activeShopId = emp.shopId
                    if (!activeShopId.isNullOrBlank() && (_shop.value.id == "shop_default" || _shop.value.id == "shop_1" || _shop.value.id.isBlank())) {
                        service.getShop(shopId = activeShopId).onSuccess { remoteShop ->
                            if (remoteShop != null) {
                                _shop.value = remoteShop
                                storageManager.saveShop(remoteShop)
                            }
                        }
                    }
                }
            } else {
                var currentShop = _shop.value
                service.getShop(
                    userEmail = activeEmail,
                    shopId = currentShop.id.takeIf { it != "shop_default" && it != "shop_1" }
                ).onSuccess { remoteShop ->
                    if (remoteShop != null) {
                        currentShop = remoteShop
                        _shop.value = remoteShop
                        storageManager.saveShop(remoteShop)
                    }
                }
                activeShopId = currentShop.id.takeIf { it != "shop_default" && it != "shop_1" }
            }

            if (activeShopId.isNullOrBlank()) {
                // Never query unpartitioned cross-tenant data without an active shop ID
                return@launch
            }

            // 1. Fetch live products from Supabase scoped to activeShopId
            service.getProducts(activeShopId).onSuccess { remoteProducts ->
                if (activeShopId != null || remoteProducts.isNotEmpty()) {
                    _products.update { currentList ->
                        val currentMap = currentList.associateBy { it.id }
                        val remoteIds = remoteProducts.mapTo(mutableSetOf()) { it.id }
                        val refreshed = remoteProducts.map { remote ->
                            val local = currentMap[remote.id]
                            val mergedSku = local?.sku?.takeIf { it.isNotBlank() }
                                ?: remote.sku.takeIf { it.isNotBlank() }
                                ?: "SKU-${remote.id.takeLast(4).uppercase()}"
                            val mergedImageUri = local?.imageUri ?: remote.imageUri
                            val mergedWholesalePrice = local?.wholesalePrice ?: remote.wholesalePrice
                            val mergedWholesaleQuantity = local?.wholesaleQuantity ?: remote.wholesaleQuantity
                            if (pendingProductUpdates.contains(remote.id)) {
                                currentMap[remote.id] ?: remote.copy(
                                    sku = mergedSku,
                                    imageUri = mergedImageUri,
                                    wholesalePrice = mergedWholesalePrice,
                                    wholesaleQuantity = mergedWholesaleQuantity
                                )
                            } else {
                                remote.copy(
                                    sku = mergedSku,
                                    imageUri = mergedImageUri,
                                    wholesalePrice = mergedWholesalePrice,
                                    wholesaleQuantity = mergedWholesaleQuantity
                                )
                            }
                        }
                        refreshed + currentList.filter {
                            pendingProductUpdates.contains(it.id) && it.id !in remoteIds
                        }
                    }
                    storageManager.saveProducts(_products.value)
                }
            }

            // 2. Fetch live expenses from Supabase scoped to activeShopId
            service.getExpenses(activeShopId).onSuccess { remoteExpenses ->
                if (activeShopId != null || remoteExpenses.isNotEmpty()) {
                    _expenses.value = remoteExpenses
                    storageManager.saveExpenses(remoteExpenses)
                }
            }

            // 3. Fetch customer credits from Supabase scoped to activeShopId
            service.getCustomerCredits(activeShopId).onSuccess { remoteCredits ->
                val mergedCredits = CreditLedgerUtils.mergeRemoteCredits(remoteCredits, _customerCredits.value)
                _customerCredits.value = mergedCredits
                storageManager.saveCustomerCredits(mergedCredits)
                mergedCredits.forEach { credit ->
                    if (credit.balance > 0.0 && !credit.status.equals("Settled", ignoreCase = true)) {
                        scheduleRemindersForCredit(credit, isUpdate = true)
                    } else {
                        cancelRemindersForCredit(credit.id)
                    }
                }
            }

            // 4. Fetch live sales scoped to activeShopId
            service.getSales(activeShopId).onSuccess { remoteSales ->
                if (activeShopId != null || remoteSales.isNotEmpty()) {
                    _salesHistory.value = remoteSales
                    storageManager.saveSales(remoteSales)
                }
            }

            // 5. Fetch stock in records scoped to activeShopId
            service.getStockInRecords(activeShopId).onSuccess { remoteStockIn ->
                if (activeShopId != null || remoteStockIn.isNotEmpty()) {
                    _stockInRecords.value = remoteStockIn
                    storageManager.saveStockInRecords(remoteStockIn)
                }
            }

            // 6. Fetch shop info if owner
            if (!isEmployee) {
                val userEmail = storageManager.loadRememberedEmail() ?: _shop.value.email
                service.getShop(userEmail).onSuccess { remoteShop ->
                    if (remoteShop != null && remoteShop.name.isNotBlank()) {
                        _shop.value = remoteShop
                        storageManager.saveShop(remoteShop)
                    }
                }
            }

            // 7. Fetch employees scoped to activeShopId
            service.getEmployees(activeShopId).onSuccess { remoteEmployees ->
                if (remoteEmployees.isNotEmpty()) {
                    _employees.value = remoteEmployees
                    storageManager.saveEmployees(remoteEmployees)
                    val activeId = storageManager.loadActiveEmployeeId()
                    if (activeId != null) {
                        _currentEmployee.value = remoteEmployees.find { it.id == activeId }
                    }
                }
            }

            updatePendingLists()

            // Process any durable pending offline sync operations now that connectivity is verified
            processPendingSyncQueue()

            // Evaluate pending scheduled reminders on sync/reconnect
            evaluateAndProcessPendingReminders()

            // Check overdue credit accounts summary for owner notification
            checkAndNotifyOverdueSummary()
        }
    }

    // Navigation & Auth actions
    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun login(email: String, pass: String, session: AuthSession? = null) {
        if (session != null) {
            storageManager.saveAuthSession(session)
            storageManager.saveRememberMePreference(session.rememberMe)
            if (session.rememberMe) {
                storageManager.saveRememberedEmail(email)
            } else {
                storageManager.clearRememberedEmail()
            }
            supabase?.setTokens(session.accessToken, session.refreshToken, session.tokenExpiresAt)
        } else {
            val fallbackSession = AuthSession(
                accessToken = "local_token_${System.currentTimeMillis()}",
                userEmail = email,
                rememberMe = true,
                loginTimestamp = System.currentTimeMillis()
            )
            storageManager.saveAuthSession(fallbackSession)
            storageManager.saveRememberedEmail(email)
        }
        _isLoggedIn.value = true

        // Clear cached shop data from any previous session so there is zero data bleeding
        storageManager.clearShopData()
        _products.value = emptyList()
        _salesHistory.value = emptyList()
        _expenses.value = emptyList()
        _stockInRecords.value = emptyList()
        _customerCredits.value = emptyList()
        _cart.value = emptyList()
        _notifications.value = emptyList()

        // Fetch shop & employees for this specific user immediately upon login
        viewModelScope.launch {
            val service = supabase
            if (service != null) {
                // Check if user is a shop owner first
                val remoteShop = service.getShop(userEmail = email).getOrNull()
                val matchedEmp = if (remoteShop == null) {
                    service.getEmployeeByEmail(email).getOrNull()
                } else null

                if (remoteShop != null) {
                    // Owner Login
                    _isOwner.value = true
                    _currentEmployee.value = null
                    _shop.value = remoteShop
                    storageManager.saveShop(remoteShop)
                    storageManager.saveCurrentUserRole("OWNER")
                    storageManager.saveActiveEmployeeId(null)
                    _currentScreen.value = "main"

                    val remoteEmployees = service.getEmployees(remoteShop.id).getOrNull() ?: emptyList()
                    _employees.value = remoteEmployees
                    storageManager.saveEmployees(remoteEmployees)
                } else if (matchedEmp != null) {
                    // Employee Login
                    _isOwner.value = false
                    _currentEmployee.value = matchedEmp
                    storageManager.saveCurrentUserRole("EMPLOYEE")
                    storageManager.saveActiveEmployeeId(matchedEmp.id)

                    val empShopId = matchedEmp.shopId
                    if (!empShopId.isNullOrBlank()) {
                        val empShop = service.getShop(shopId = empShopId).getOrNull()
                        if (empShop != null) {
                            _shop.value = empShop
                            storageManager.saveShop(empShop)
                        } else {
                            _shop.value = Shop(id = empShopId, name = "Store POS")
                            storageManager.saveShop(_shop.value)
                        }
                    }

                    if (matchedEmp.status == "PENDING") {
                        _currentScreen.value = "employee_waiting_approval"
                    } else {
                        _currentScreen.value = "main"
                    }
                } else {
                    val role = storageManager.loadCurrentUserRole()
                    _isOwner.value = (role != "EMPLOYEE")
                    _currentScreen.value = "main"
                }

                syncWithSupabase(force = true)
                updatePendingLists()
            } else {
                _isOwner.value = true
                _currentScreen.value = "main"
                syncWithSupabase(force = true)
                updatePendingLists()
            }
        }
    }

    fun refreshSession(refreshTokenStr: String, rememberMe: Boolean = true) {
        val service = supabase ?: return
        viewModelScope.launch {
            service.refreshSession(refreshTokenStr, rememberMe).onSuccess { newSession ->
                storageManager.saveAuthSession(newSession)
            }
        }
    }

    fun registerShop(shopName: String, ownerName: String, email: String, phone: String, address: String, category: String) {
        val existingShopId = _shop.value.id.takeIf { it.isNotBlank() && it != "shop_1" }
        val targetShopId = supabase?.getUserId()
            ?: existingShopId
            ?: "shop_${email.substringBefore('@').ifBlank { "main" }}"

        val newShop = Shop(
            id = targetShopId,
            name = shopName.trim().ifBlank { "My Shop" },
            ownerName = ownerName.trim().ifBlank { "Owner" },
            email = email.trim(),
            phone = phone.trim(),
            address = address.trim(),
            category = category.trim().ifBlank { "Retail & Grocery" }
        )
        _shop.value = newShop
        storageManager.saveShop(newShop)

        // Save persistent session for newly registered shop
        val regSession = AuthSession(
            accessToken = supabase?.getAccessToken() ?: "reg_token_${System.currentTimeMillis()}",
            refreshToken = supabase?.getRefreshToken().orEmpty(),
            userEmail = newShop.email,
            tokenExpiresAt = supabase?.getTokenExpiresAt() ?: 0L,
            rememberMe = true,
            loginTimestamp = System.currentTimeMillis()
        )
        storageManager.saveAuthSession(regSession)
        storageManager.saveRememberedEmail(newShop.email)

        _isLoggedIn.value = true
        _currentScreen.value = "main"

        // Clear cached shop data so newly registered shop starts with a 100% clean slate
        storageManager.clearShopData()
        _products.value = emptyList()
        _salesHistory.value = emptyList()
        _expenses.value = emptyList()
        _stockInRecords.value = emptyList()
        _customerCredits.value = emptyList()
        _cart.value = emptyList()
        _notifications.value = emptyList()

        viewModelScope.launch {
            SupabaseClient.service.saveShop(newShop)
            syncWithSupabase(force = true)
        }
    }

    fun onEmployeeRegistered(emp: Employee) {
        _isOwner.value = false
        _currentEmployee.value = emp
        if (!emp.shopId.isNullOrBlank()) {
            _shop.value = _shop.value.copy(id = emp.shopId)
            storageManager.saveShop(_shop.value)
        }
        _employees.update { list ->
            list.filterNot { it.id == emp.id || it.email.equals(emp.email, ignoreCase = true) } + emp
        }
        storageManager.saveEmployees(_employees.value)
        storageManager.saveCurrentUserRole("EMPLOYEE")
        storageManager.saveActiveEmployeeId(emp.id)
        _isLoggedIn.value = true
        _currentScreen.value = "employee_waiting_approval"
        updatePendingLists()
    }

    fun registerEmployee(shopId: String, name: String, phone: String, email: String, authUserId: String? = null) {
        val cleanEmail = email.trim().lowercase()
        val deterministicId = authUserId?.takeIf { it.isNotBlank() }?.let { "emp_$it" }
            ?: "emp_${cleanEmail.replace(Regex("[^a-zA-Z0-9]"), "_")}"

        val existing = _employees.value.find { it.email.equals(cleanEmail, ignoreCase = true) }
        val emp = existing?.copy(
            name = name.trim().ifBlank { existing.name },
            phone = phone.trim().ifBlank { existing.phone },
            authUserId = authUserId ?: existing.authUserId
        ) ?: Employee(
            id = deterministicId,
            name = name.trim(),
            role = "Cashier",
            phone = phone.trim(),
            email = email.trim(),
            isOwner = false,
            shopId = shopId.trim(),
            status = "PENDING",
            permissions = listOf(
                EmployeePermissions.PERM_RECORD_SALES,
                EmployeePermissions.PERM_STOCK_IN
            ),
            authUserId = authUserId
        )
        onEmployeeRegistered(emp)

        viewModelScope.launch {
            supabase?.upsertEmployee(emp, shopId.trim())
            updatePendingLists()
        }
    }

    fun refreshEmployeeStatus(onComplete: () -> Unit = {}) {
        val service = supabase ?: run { onComplete(); return }
        val empId = _currentEmployee.value?.id ?: storageManager.loadActiveEmployeeId()
        val currentEmail = _currentEmployee.value?.email ?: storageManager.loadRememberedEmail()
        val shopId = _currentEmployee.value?.shopId ?: _shop.value.id
        viewModelScope.launch {
            try {
                val res = service.getEmployees(shopId)
                res.onSuccess { list ->
                    val deduplicated = list.distinctBy { it.id }.distinctBy { it.email.lowercase() }
                    _employees.value = deduplicated
                    storageManager.saveEmployees(deduplicated)
                    val updated = deduplicated.find {
                        it.id == empId || (!currentEmail.isNullOrBlank() && it.email.equals(currentEmail, ignoreCase = true))
                    }
                    if (updated != null) {
                        _currentEmployee.value = updated
                        storageManager.saveActiveEmployeeId(updated.id)
                        if (updated.status == "ACTIVE") {
                            _currentScreen.value = "main"
                        }
                    } else if (empId != null && !currentEmail.isNullOrBlank()) {
                        _currentEmployee.value = null
                        storageManager.saveActiveEmployeeId(null)
                        _currentScreen.value = "login"
                    }
                    updatePendingLists()
                }
            } finally {
                onComplete()
            }
        }
    }

    // Permission Checkers
    fun canRecordSales(): Boolean {
        if (isOwner.value) return true
        val emp = _currentEmployee.value ?: return false
        return emp.status == "ACTIVE" && emp.permissions.contains(EmployeePermissions.PERM_RECORD_SALES)
    }

    fun canStockIn(): Boolean {
        if (isOwner.value) return true
        val emp = _currentEmployee.value ?: return false
        return emp.status == "ACTIVE" && emp.permissions.contains(EmployeePermissions.PERM_STOCK_IN)
    }

    fun canAddProduct(): Boolean {
        if (isOwner.value) return true
        val emp = _currentEmployee.value ?: return false
        return emp.status == "ACTIVE" && emp.permissions.contains(EmployeePermissions.PERM_NEW_PRODUCT)
    }

    fun canIssueCredit(): Boolean {
        if (isOwner.value) return true
        val emp = _currentEmployee.value ?: return false
        return emp.status == "ACTIVE" && emp.permissions.contains(EmployeePermissions.PERM_CREDIT_SALES)
    }

    fun canRecordExpense(): Boolean {
        if (isOwner.value) return true
        val emp = _currentEmployee.value ?: return false
        return emp.status == "ACTIVE" && emp.permissions.contains(EmployeePermissions.PERM_RECORD_EXPENSES)
    }

    // Owner Approval Actions
    fun approveEmployee(employeeId: String, permissions: List<String>) {
        val emp = _employees.value.find { it.id == employeeId } ?: return
        val activeShopId = getActiveShopId()
        val updated = emp.copy(
            status = "ACTIVE",
            permissions = permissions
        )
        val duplicateIds = _employees.value
            .filter { it.id != employeeId && it.email.equals(emp.email, ignoreCase = true) }
            .map { it.id }

        _employees.update { list ->
            list.filterNot { duplicateIds.contains(it.id) }.map { if (it.id == employeeId) updated else it }
        }
        storageManager.saveEmployees(_employees.value)
        viewModelScope.launch {
            supabase?.upsertEmployee(updated, activeShopId)
            duplicateIds.forEach { dupId ->
                supabase?.deleteEmployee(dupId)
            }
        }
        updatePendingLists()
    }

    fun rejectEmployee(employeeId: String) {
        val emp = _employees.value.find { it.id == employeeId }
        val empEmail = emp?.email
        val targetIds = _employees.value
            .filter { it.id == employeeId || (!empEmail.isNullOrBlank() && it.email.equals(empEmail, ignoreCase = true)) }
            .map { it.id }

        _employees.update { list -> list.filterNot { targetIds.contains(it.id) } }
        storageManager.saveEmployees(_employees.value)
        viewModelScope.launch {
            targetIds.forEach { id ->
                supabase?.deleteEmployee(id)
            }
        }
        updatePendingLists()
    }

    fun removeEmployee(employeeId: String) {
        val emp = _employees.value.find { it.id == employeeId } ?: return
        val empEmail = emp.email
        val targetIds = _employees.value
            .filter { it.id == employeeId || (empEmail.isNotBlank() && it.email.equals(empEmail, ignoreCase = true)) }
            .map { it.id }

        _employees.update { list -> list.filterNot { targetIds.contains(it.id) } }
        storageManager.saveEmployees(_employees.value)
        updatePendingLists()

        viewModelScope.launch {
            val service = supabase ?: return@launch
            targetIds.forEach { id ->
                service.deleteEmployee(id)
            }
        }
    }

    fun updateEmployeePermissions(employeeId: String, permissions: List<String>) {
        val emp = _employees.value.find { it.id == employeeId } ?: return
        val activeShopId = getActiveShopId()
        val updated = emp.copy(permissions = permissions)
        _employees.update { list -> list.map { if (it.id == employeeId) updated else it } }
        storageManager.saveEmployees(_employees.value)
        viewModelScope.launch {
            supabase?.upsertEmployee(updated, activeShopId)
        }
    }

    fun updateEmployeeStatus(employeeId: String, status: String) {
        val emp = _employees.value.find { it.id == employeeId } ?: return
        val activeShopId = getActiveShopId()
        val updated = emp.copy(status = status)
        _employees.update { list -> list.map { if (it.id == employeeId) updated else it } }
        storageManager.saveEmployees(_employees.value)
        viewModelScope.launch {
            supabase?.upsertEmployee(updated, activeShopId)
        }
        updatePendingLists()
    }

    fun revokeAllEmployeePermissions(employeeId: String) {
        updateEmployeePermissions(employeeId, emptyList())
    }

    fun approveStockIn(record: StockInRecord) {
        val activeShopId = getActiveShopId()
        _products.update { currentList ->
            currentList.map { p ->
                if (p.id == record.productId) {
                    p.copy(
                        stockQuantity = p.stockQuantity + record.quantityAdded,
                        buyingPrice = if (record.buyingPrice > 0) record.buyingPrice else p.buyingPrice
                    )
                } else p
            }
        }
        storageManager.saveProducts(_products.value)
        val updatedProduct = _products.value.find { it.id == record.productId }
        if (updatedProduct != null) {
            viewModelScope.launch { supabase?.upsertProduct(updatedProduct, activeShopId) }
        }

        val cleanSupplier = if (record.supplier.contains("orig:")) {
            record.supplier.substringAfter("orig:").ifBlank { "" }
        } else {
            record.supplier.removePrefix("PENDING_APPROVAL||").ifBlank { "" }
        }
        val approvedRecord = record.copy(
            id = if (record.id.startsWith("stk_pending_")) "stk_${record.id.removePrefix("stk_pending_")}" else record.id,
            supplier = cleanSupplier
        )
        _stockInRecords.update { list ->
            list.map { if (it.id == record.id) approvedRecord else it }
        }
        storageManager.saveStockInRecords(_stockInRecords.value)
        viewModelScope.launch {
            supabase?.deleteStockInRecord(record.id)
            supabase?.insertStockInRecord(approvedRecord, activeShopId)
        }
        updatePendingLists()
    }

    fun rejectStockIn(recordId: String) {
        _stockInRecords.update { list -> list.filterNot { it.id == recordId } }
        storageManager.saveStockInRecords(_stockInRecords.value)
        viewModelScope.launch { supabase?.deleteStockInRecord(recordId) }
        updatePendingLists()
    }

    fun approveNewProduct(product: Product) {
        val activeShopId = getActiveShopId()
        val cleanSupplier = if (product.supplier.contains("orig:")) {
            product.supplier.substringAfter("orig:").ifBlank { "" }
        } else {
            product.supplier.removePrefix("PENDING_APPROVAL||").ifBlank { "" }
        }
        val approvedProduct = product.copy(supplier = cleanSupplier)
        _products.update { list -> list.map { if (it.id == product.id) approvedProduct else it } }
        storageManager.saveProducts(_products.value)
        viewModelScope.launch {
            supabase?.upsertProduct(approvedProduct, activeShopId)
        }
        updatePendingLists()
    }

    fun rejectNewProduct(productId: String) {
        deleteProduct(productId)
        updatePendingLists()
    }

    fun approveCreditSale(sale: Sale) {
        val activeShopId = getActiveShopId()
        // 1. Deduct stock for items in the approved credit sale
        _products.update { currentList ->
            currentList.map { prod ->
                val sold = sale.items.find { it.product.id == prod.id }
                if (sold != null) {
                    prod.copy(stockQuantity = (prod.stockQuantity - sold.quantity).coerceAtLeast(0))
                } else prod
            }
        }
        storageManager.saveProducts(_products.value)
        viewModelScope.launch {
            sale.items.forEach { sold ->
                val p = _products.value.find { it.id == sold.product.id }
                if (p != null) supabase?.upsertProduct(p, activeShopId)
            }
        }

        // 2. Create customer credit ledger entry
        val creditRecord = CustomerCredit(
            id = "c_${System.currentTimeMillis()}",
            customerName = sale.customerName,
            customerPhone = "",
            totalOutstanding = sale.totalAmount,
            productsSummary = "${sale.items.size} Items | ${sale.items.joinToString { it.product.name }}",
            amountPaid = sale.paidAmount,
            balance = sale.balance,
            dueDate = sale.dueDate,
            status = if (sale.balance <= 0) "Settled" else "Pending",
            shopId = activeShopId
        )
        _customerCredits.update { listOf(creditRecord) + it }
        storageManager.saveCustomerCredits(_customerCredits.value)

        // 3. Mark sale as confirmed
        val confirmedSale = sale.copy(
            id = if (sale.id.startsWith("SL_PENDING_")) "SL-${(1093..9999).random()}" else sale.id,
            paymentMethod = "Credit"
        )
        _salesHistory.update { list -> list.map { if (it.id == sale.id) confirmedSale else it } }
        storageManager.saveSales(_salesHistory.value)

        viewModelScope.launch {
            supabase?.deleteSale(sale.id)
            supabase?.insertSale(confirmedSale, activeShopId)
            supabase?.upsertCustomerCredit(creditRecord, activeShopId)
        }
        updatePendingLists()
    }

    fun rejectCreditSale(saleId: String) {
        _salesHistory.update { list -> list.filterNot { it.id == saleId } }
        storageManager.saveSales(_salesHistory.value)
        viewModelScope.launch { supabase?.deleteSale(saleId) }
        updatePendingLists()
    }

    fun approveExpense(expense: Expense) {
        val activeShopId = getActiveShopId()
        val approvedCategory = if (expense.category.contains("orig:")) {
            expense.category.substringAfter("orig:").ifBlank { "General" }
        } else {
            expense.category.removePrefix("PENDING_APPROVAL||").ifBlank { "General" }
        }
        val approvedExpense = expense.copy(
            id = if (expense.id.startsWith("exp_pending_")) "exp_${expense.id.removePrefix("exp_pending_")}" else expense.id,
            category = approvedCategory
        )
        _expenses.update { list -> list.map { if (it.id == expense.id) approvedExpense else it } }
        storageManager.saveExpenses(_expenses.value)
        viewModelScope.launch {
            supabase?.deleteExpense(expense.id)
            supabase?.insertExpense(approvedExpense, activeShopId)
        }
        updatePendingLists()
    }

    fun rejectExpense(expenseId: String) {
        _expenses.update { list -> list.filterNot { it.id == expenseId } }
        storageManager.saveExpenses(_expenses.value)
        viewModelScope.launch { supabase?.deleteExpense(expenseId) }
        updatePendingLists()
    }

    fun updatePendingLists() {
        _pendingEmployees.value = _employees.value
            .filter { !it.isOwner && it.status == "PENDING" }
            .distinctBy { it.email.lowercase() }
        _pendingStockIns.value = _stockInRecords.value.filter { it.supplier.startsWith("PENDING_APPROVAL||") }
        _pendingNewProducts.value = _products.value.filter { it.supplier.startsWith("PENDING_APPROVAL||") }
        _pendingExpenses.value = _expenses.value.filter { it.category.startsWith("PENDING_APPROVAL||") }
        _pendingCreditSales.value = _salesHistory.value.filter {
            it.paymentMethod.startsWith("PENDING_APPROVAL||") ||
            it.paymentMethod == "PENDING_CREDIT" ||
            it.paymentMethod.startsWith("PENDING_CREDIT") ||
            it.id.startsWith("SL_PENDING_")
        }
        _totalPendingCount.value = _pendingEmployees.value.size +
            _pendingStockIns.value.size +
            _pendingNewProducts.value.size +
            _pendingExpenses.value.size +
            _pendingCreditSales.value.size
    }

    fun updateShopInformation(shop: Shop) {
        _shop.value = shop
        storageManager.saveShop(shop)
        viewModelScope.launch {
            SupabaseClient.service.saveShop(shop)
        }
    }

    fun updateDailyTarget(target: Double) {
        val updated = _shop.value.copy(dailyTarget = target)
        updateShopInformation(updated)
    }

    fun logout() {
        storageManager.clearAuthSession()
        storageManager.clearShopData()
        supabase?.clearSession()
        _isLoggedIn.value = false
        _isOwner.value = true
        _currentEmployee.value = null
        _currentScreen.value = "login"
        _shop.value = defaultShop
        _products.value = defaultProducts
        _salesHistory.value = emptyList()
        _expenses.value = emptyList()
        _stockInRecords.value = emptyList()
        _customerCredits.value = emptyList()
        _cart.value = emptyList()
        _notifications.value = emptyList()
        updatePendingLists()
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

    fun setCartQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            _cart.update { currentCart -> currentCart.filterNot { it.product.id == productId } }
            return
        }
        val product = _products.value.find { it.id == productId } ?: return
        if (product.stockQuantity <= 0) return
        val clampedQty = quantity.coerceAtMost(product.stockQuantity)
        if (clampedQty <= 0) return

        _cart.update { currentCart ->
            val existingIndex = currentCart.indexOfFirst { it.product.id == productId }
            if (existingIndex >= 0) {
                currentCart.toMutableList().apply {
                    this[existingIndex] = currentCart[existingIndex].copy(quantity = clampedQty)
                }
            } else {
                currentCart + CartItem(product, clampedQty)
            }
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

        val isPendingCredit = isCredit && !isOwner.value

        // Deduct inventory locally (only for confirmed sales, not pending approval credit sales)
        if (!isPendingCredit) {
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
        }

        val activeShopId = getActiveShopId()
        val activeOwnerName = _shop.value.ownerName.ifBlank { "Store Owner" }
        val saleEmployeeName = if (!isOwner.value) _currentEmployee.value?.name.orEmpty().ifBlank { "Employee" } else activeOwnerName

        val saleTimeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date())
        val newSale = Sale(
            id = if (isPendingCredit) "SL_PENDING_${(1093..9999).random()}" else "SL-${(1093..9999).random()}",
            items = currentCartItems.map { it.copy(product = it.product.copy(shopId = activeShopId)) },
            totalAmount = creditTotal,
            paymentMethod = if (isPendingCredit) "PENDING_APPROVAL||requester:$saleEmployeeName||orig:Credit" else paymentMethod,
            customerName = if (trimmedCustomerName.isNotBlank()) trimmedCustomerName else if (isCredit) "Credit Customer" else "Walk-in Customer",
            dateFormatted = "Today, $saleTimeFormat",
            timestamp = System.currentTimeMillis(),
            employeeName = saleEmployeeName,
            isCreditSale = isCredit,
            paidAmount = creditPaid,
            balance = creditBalance,
            dueDate = if (isCredit) creditRecord?.dueDate.orEmpty() else dueDate,
            shopId = activeShopId
        )

        _salesHistory.update { listOf(newSale) + it }
        storageManager.saveSales(_salesHistory.value)
        updatePendingLists()

        val finalCreditRecord = creditRecord?.copy(shopId = activeShopId)
        if (isCredit && !isPendingCredit && finalCreditRecord != null) {
            _customerCredits.update { listOf(finalCreditRecord) + it }
            storageManager.saveCustomerCredits(_customerCredits.value)
            scheduleRemindersForCredit(finalCreditRecord, isUpdate = false)
            enqueueSyncOperation(
                entityType = "credit",
                entityId = finalCreditRecord.id,
                action = "UPSERT",
                payloadJson = finalCreditRecord.id
            )
            val service = supabase
            if (service != null) {
                viewModelScope.launch {
                    try {
                        service.upsertCustomerCredit(finalCreditRecord, activeShopId)
                        removeSyncOperation(finalCreditRecord.id)
                    } catch (e: Exception) {
                        // Keep in sync queue
                    }
                }
            }
        }

        val productIds = currentCartItems.map { it.product.id }.toSet()
        if (!isPendingCredit) {
            pendingProductUpdates.addAll(productIds)
        }

        // Asynchronous cloud sync to Supabase
        val service = supabase
        if (service != null) {
            viewModelScope.launch {
                try {
                    if (!isPendingCredit) {
                        currentCartItems.forEach { cartItem ->
                            val updatedProd = _products.value.find { it.id == cartItem.product.id }
                            if (updatedProd != null) {
                                service.updateProductStock(updatedProd.id, updatedProd.stockQuantity, activeShopId)
                                service.upsertProduct(updatedProd, activeShopId)
                            }
                        }
                    }
                    service.insertSale(newSale, activeShopId)
                    if (finalCreditRecord != null && !isPendingCredit) {
                        service.upsertCustomerCredit(finalCreditRecord, activeShopId)
                    }
                } finally {
                    if (!isPendingCredit) {
                        pendingProductUpdates.removeAll(productIds)
                    }
                }
            }
        } else {
            if (!isPendingCredit) {
                pendingProductUpdates.removeAll(productIds)
            }
        }

        _completedSale.value = newSale
        _cart.value = emptyList()
        return newSale
    }

    fun deleteSale(saleId: String) {
        if (!isOwner.value) return
        val saleToDelete = _salesHistory.value.find { it.id == saleId }
        _salesHistory.update { list -> list.filterNot { it.id == saleId } }
        storageManager.saveSales(_salesHistory.value)

        if (saleToDelete != null) {
            // Restore inventory for the products in the deleted sale
            val productIds = saleToDelete.items.map { it.product.id }.toSet()
            _products.update { currentList ->
                currentList.map { product ->
                    val soldItem = saleToDelete.items.find { it.product.id == product.id }
                    if (soldItem != null) {
                        product.copy(stockQuantity = product.stockQuantity + soldItem.quantity)
                    } else product
                }
            }
            storageManager.saveProducts(_products.value)

            val service = supabase
            if (service != null) {
                val activeShopId = getActiveShopId()
                pendingProductUpdates.addAll(productIds)
                viewModelScope.launch {
                    try {
                        service.deleteSale(saleId)
                        saleToDelete.items.forEach { soldItem ->
                            val updatedProd = _products.value.find { it.id == soldItem.product.id }
                            if (updatedProd != null) {
                                service.updateProductStock(updatedProd.id, updatedProd.stockQuantity, activeShopId)
                                service.upsertProduct(updatedProd, activeShopId)
                            }
                        }
                    } finally {
                        pendingProductUpdates.removeAll(productIds)
                    }
                }
            }
        } else {
            val service = supabase ?: return
            viewModelScope.launch {
                service.deleteSale(saleId)
            }
        }
    }

    fun updateSale(updatedSale: Sale) {
        if (!isOwner.value) return
        val originalSale = _salesHistory.value.find { it.id == updatedSale.id }
        _salesHistory.update { list ->
            list.map { if (it.id == updatedSale.id) updatedSale else it }
        }
        storageManager.saveSales(_salesHistory.value)

        if (originalSale != null) {
            // Calculate item quantity differences: (newQty - oldQty)
            // positive delta = more sold -> deduct from stock
            // negative delta = fewer sold -> restore to stock
            val allProductIds = (originalSale.items.map { it.product.id } + updatedSale.items.map { it.product.id }).toSet()
            val oldItemMap = originalSale.items.associate { it.product.id to it.quantity }
            val newItemMap = updatedSale.items.associate { it.product.id to it.quantity }

            val hasInventoryChanges = allProductIds.any { id -> (oldItemMap[id] ?: 0) != (newItemMap[id] ?: 0) }

            if (hasInventoryChanges) {
                _products.update { currentList ->
                    currentList.map { product ->
                        val oldQty = oldItemMap[product.id] ?: 0
                        val newQty = newItemMap[product.id] ?: 0
                        val delta = newQty - oldQty
                        if (delta != 0) {
                            product.copy(stockQuantity = (product.stockQuantity - delta).coerceAtLeast(0))
                        } else product
                    }
                }
                storageManager.saveProducts(_products.value)
            }

            val service = supabase ?: return
            viewModelScope.launch {
                val activeShopId = getActiveShopId()
                pendingProductUpdates.addAll(allProductIds)
                try {
                    service.upsertSale(updatedSale, activeShopId)
                    if (hasInventoryChanges) {
                        allProductIds.forEach { pId ->
                            val updatedProd = _products.value.find { it.id == pId }
                            if (updatedProd != null) {
                                service.updateProductStock(updatedProd.id, updatedProd.stockQuantity, activeShopId)
                                service.upsertProduct(updatedProd, activeShopId)
                            }
                        }
                    }
                } finally {
                    pendingProductUpdates.removeAll(allProductIds)
                }
            }
        } else {
            val service = supabase ?: return
            viewModelScope.launch {
                val activeShopId = getActiveShopId()
                service.upsertSale(updatedSale, activeShopId)
            }
        }
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
        paymentMethod: String = "Cash",
        itemsMap: Map<String, Int> = emptyMap()
    ): Boolean {
        val normalizedName = customerName.trim()
        if (normalizedName.isBlank()) return false
        val normalizedPhone = CreditLedgerUtils.normalizePhoneNumber(phone) ?: return false
        val activeShopId = getActiveShopId()
        val created = CreditLedgerUtils.createCredit(
            id = "c_${System.currentTimeMillis()}",
            customerName = normalizedName,
            customerPhone = normalizedPhone,
            originalAmount = originalAmount,
            amountPaid = amountPaid,
            productsSummary = productsSummary.ifBlank { "1 Item | Product" },
            dueDate = dueDate.ifBlank { defaultCreditDueDate() },
            paymentMethod = paymentMethod
        ) ?: return false

        val creditRecord = created.copy(shopId = activeShopId)
        _customerCredits.update { listOf(creditRecord) + it }
        storageManager.saveCustomerCredits(_customerCredits.value)

        // Automatically schedule reminders for this credit sale
        scheduleRemindersForCredit(creditRecord, isUpdate = false)

        // Enqueue offline sync operation
        enqueueSyncOperation(
            entityType = "credit",
            entityId = creditRecord.id,
            action = "UPSERT",
            payloadJson = creditRecord.id
        )

        // Deduct inventory if items were selected
        if (itemsMap.isNotEmpty()) {
            val productIds = itemsMap.keys.toSet()
            val cartItems = mutableListOf<CartItem>()
            _products.update { currentList ->
                currentList.map { product ->
                    val qtySold = itemsMap[product.id] ?: 0
                    if (qtySold > 0) {
                        cartItems.add(CartItem(product.copy(shopId = activeShopId), qtySold))
                        product.copy(stockQuantity = (product.stockQuantity - qtySold).coerceAtLeast(0))
                    } else product
                }
            }
            storageManager.saveProducts(_products.value)

            // Also create a corresponding Sale record to keep Sales History and Reports in sync
            val newSale = Sale(
                id = "SL-${(1093..9999).random()}",
                items = cartItems,
                totalAmount = originalAmount,
                paymentMethod = paymentMethod,
                customerName = normalizedName,
                dateFormatted = "Just now",
                timestamp = System.currentTimeMillis(),
                employeeName = _shop.value.ownerName.ifBlank { "Store Owner" },
                isCreditSale = true,
                paidAmount = amountPaid,
                balance = (originalAmount - amountPaid).coerceAtLeast(0.0),
                dueDate = creditRecord.dueDate,
                shopId = activeShopId
            )
            _salesHistory.update { listOf(newSale) + it }
            storageManager.saveSales(_salesHistory.value)

            enqueueSyncOperation(
                entityType = "sale",
                entityId = newSale.id,
                action = "INSERT",
                payloadJson = newSale.id
            )

            val s = supabase ?: return true
            pendingProductUpdates.addAll(productIds)
            viewModelScope.launch {
                try {
                    s.upsertCustomerCredit(creditRecord, activeShopId)
                    s.insertSale(newSale, activeShopId)
                    removeSyncOperation(creditRecord.id)
                    removeSyncOperation(newSale.id)
                    itemsMap.keys.forEach { pId ->
                        val updatedProd = _products.value.find { it.id == pId }
                        if (updatedProd != null) {
                            s.updateProductStock(updatedProd.id, updatedProd.stockQuantity, activeShopId)
                            s.upsertProduct(updatedProd, activeShopId)
                        }
                    }
                } catch (e: Exception) {
                    // Retain in pending sync operations for background retry
                } finally {
                    pendingProductUpdates.removeAll(productIds)
                }
            }
        } else {
            val s = supabase ?: return true
            viewModelScope.launch {
                try {
                    s.upsertCustomerCredit(creditRecord, activeShopId)
                    removeSyncOperation(creditRecord.id)
                } catch (e: Exception) {
                    // Retain in pending sync queue
                }
            }
        }
        return true
    }

    fun addCustomerCredit(customerName: String, phone: String, productsSummary: String, amountPaid: Double, balance: Double, dueDate: String) {
        addCustomerCredit(
            customerName = customerName,
            phone = phone,
            productsSummary = productsSummary,
            originalAmount = amountPaid + balance,
            amountPaid = amountPaid,
            dueDate = dueDate
        )
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
            val activeShopId = getActiveShopId()
            val finalCredit = updatedCredit.copy(shopId = activeShopId ?: updatedCredit.shopId)
            _customerCredits.update { currentList ->
                currentList.map { if (it.id == creditId) finalCredit else it }
            }
            storageManager.saveCustomerCredits(_customerCredits.value)

            // If debt is fully paid, cancel all future pending reminders!
            if (finalCredit.balance <= 0.0) {
                cancelRemindersForCredit(finalCredit.id)
            }

            enqueueSyncOperation(
                entityType = "credit",
                entityId = finalCredit.id,
                action = "UPSERT",
                payloadJson = finalCredit.id
            )

            val s = supabase ?: return Result.success(finalCredit)
            viewModelScope.launch {
                try {
                    s.upsertCustomerCredit(finalCredit, activeShopId)
                    removeSyncOperation(finalCredit.id)
                } catch (e: Exception) {
                    // Retain in pending sync queue
                }
            }
            Result.success(finalCredit)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    // Add Product
    fun addProduct(
        name: String, category: String = "", sku: String = "", unit: String,
        buyingPrice: Double, sellingPrice: Double, quantity: Int, minStock: Int, supplier: String,
        imageUri: String? = null,
        wholesalePrice: Double? = null,
        wholesaleQuantity: Int? = null
    ): Boolean {
        val normalizedName = name.trim()
        val normalizedCategory = category.trim()
        val rawSku = sku.trim()
        val normalizedSku = if (rawSku.isBlank()) {
            val existing = _products.value.map { it.sku.trim().lowercase() }.toSet()
            var num = (_products.value.size + 1).coerceAtLeast(1)
            var cand = "SKU-${num.toString().padStart(4, '0')}"
            while (cand.lowercase() in existing) {
                num++
                cand = "SKU-${num.toString().padStart(4, '0')}"
            }
            cand
        } else rawSku
        val normalizedUnit = unit.trim().ifBlank { "pcs" }
        val hasWholesaleInput = wholesalePrice != null || wholesaleQuantity != null
        val wholesaleIsValid = !hasWholesaleInput || (
            wholesalePrice != null && wholesalePrice.isFinite() && wholesalePrice > 0 &&
            wholesaleQuantity != null && wholesaleQuantity >= 1
        )
        if (normalizedName.isBlank() ||
            !buyingPrice.isFinite() || buyingPrice < 0 ||
            !sellingPrice.isFinite() || sellingPrice <= 0 || quantity < 0 || minStock < 0 ||
            !wholesaleIsValid ||
            _products.value.any { it.sku.equals(normalizedSku, ignoreCase = true) }
        ) return false

        val activeShopId = getActiveShopId()
        val isPending = !isOwner.value
        val empName = _currentEmployee.value?.name?.ifBlank { "Employee" } ?: "Employee"
        val finalSupplier = if (isPending) {
            "PENDING_APPROVAL||requester:$empName||orig:${supplier.trim()}"
        } else {
            supplier.trim()
        }

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
            supplier = finalSupplier,
            iconType = normalizedCategory.lowercase().ifBlank { "box" },
            imageUri = imageUri,
            shopId = activeShopId,
            wholesalePrice = wholesalePrice?.takeIf { it > 0 },
            wholesaleQuantity = wholesaleQuantity?.takeIf { it > 0 }
        )
        _products.update { listOf(newProduct) + it }
        storageManager.saveProducts(_products.value)
        updatePendingLists()

        val service = supabase ?: return true
        pendingProductUpdates.add(newProduct.id)
        viewModelScope.launch {
            try {
                service.upsertProduct(newProduct, activeShopId)
            } finally {
                pendingProductUpdates.remove(newProduct.id)
            }
        }
        return true
    }

    fun updateProduct(
        productId: String,
        name: String, category: String = "", sku: String = "", unit: String,
        buyingPrice: Double, sellingPrice: Double, quantity: Int, minStock: Int, supplier: String,
        imageUri: String? = currentProductImageUri(productId),
        wholesalePrice: Double? = null,
        wholesaleQuantity: Int? = null
    ): Boolean {
        val currentProduct = _products.value.find { it.id == productId } ?: return false
        val normalizedName = name.trim()
        val normalizedCategory = category.trim()
        val rawSku = sku.trim()
        val normalizedSku = if (rawSku.isBlank()) {
            currentProduct.sku.ifBlank { "SKU-${productId.takeLast(4).uppercase()}" }
        } else rawSku
        val normalizedUnit = unit.trim().ifBlank { "pcs" }
        val hasWholesaleInput = wholesalePrice != null || wholesaleQuantity != null
        val wholesaleIsValid = !hasWholesaleInput || (
            wholesalePrice != null && wholesalePrice.isFinite() && wholesalePrice > 0 &&
            wholesaleQuantity != null && wholesaleQuantity >= 1
        )
        if (normalizedName.isBlank() ||
            !buyingPrice.isFinite() || buyingPrice < 0 ||
            !sellingPrice.isFinite() || sellingPrice <= 0 || quantity < 0 || minStock < 0 ||
            !wholesaleIsValid ||
            _products.value.any { it.id != productId && it.sku.isNotBlank() && it.sku.equals(normalizedSku, ignoreCase = true) }
        ) return false

        val activeShopId = getActiveShopId()
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
            iconType = normalizedCategory.lowercase().ifBlank { "box" },
            imageUri = imageUri,
            shopId = currentProduct.shopId ?: activeShopId,
            wholesalePrice = wholesalePrice?.takeIf { it > 0 },
            wholesaleQuantity = wholesaleQuantity?.takeIf { it > 0 }
        )
        _products.update { products -> products.map { if (it.id == productId) updatedProduct else it } }
        storageManager.saveProducts(_products.value)
        if (currentProduct.imageUri != updatedProduct.imageUri) {
            application?.let { ProductImageStorage.deleteIfOwned(it, currentProduct.imageUri) }
        }

        // Asynchronously update product in Supabase while safeguarding local state
        val service = supabase ?: return true
        pendingProductUpdates.add(productId)
        viewModelScope.launch {
            try {
                service.upsertProduct(updatedProduct, activeShopId)
            } finally {
                pendingProductUpdates.remove(productId)
            }
        }
        return true
    }

    // Delete Product
    fun deleteProduct(productId: String): Boolean {
        val target = _products.value.find { it.id == productId } ?: return false
        _products.update { products -> products.filter { it.id != productId } }
        storageManager.saveProducts(_products.value)
        _cart.update { items -> items.filter { it.product.id != productId } }
        application?.let { ProductImageStorage.deleteIfOwned(it, target.imageUri) }

        val service = supabase ?: return true
        pendingProductUpdates.add(productId)
        viewModelScope.launch {
            try {
                service.deleteProduct(productId)
            } finally {
                pendingProductUpdates.remove(productId)
            }
        }
        return true
    }

    private fun currentProductImageUri(productId: String): String? =
        _products.value.firstOrNull { it.id == productId }?.imageUri

    // Stock In / Restocking
    fun recordStockIn(
        productId: String,
        quantityAdded: Int,
        buyingPrice: Double,
        sellingPrice: Double = 0.0,
        supplier: String,
        employeeName: String
    ) {
        if (quantityAdded <= 0) return

        val isPending = !isOwner.value
        val finalSupplier = if (isPending) {
            "PENDING_APPROVAL||requester:${employeeName.ifBlank { _currentEmployee.value?.name ?: "Employee" }}||orig:${supplier.trim()}"
        } else {
            supplier.trim()
        }
        val recordId = if (isPending) "stk_pending_${System.currentTimeMillis()}" else "stk_${System.currentTimeMillis()}"

        if (!isPending) {
            _products.update { currentList ->
                currentList.map { p ->
                    if (p.id == productId) {
                        p.copy(
                            stockQuantity = p.stockQuantity + quantityAdded,
                            buyingPrice = if (buyingPrice > 0) buyingPrice else p.buyingPrice,
                            sellingPrice = if (sellingPrice > 0) sellingPrice else p.sellingPrice,
                            supplier = if (supplier.isNotBlank()) supplier else p.supplier
                        )
                    } else p
                }
            }
            storageManager.saveProducts(_products.value)
        }

        val prodName = _products.value.find { it.id == productId }?.name ?: "Product"
        val activeShopId = getActiveShopId()
        val record = StockInRecord(
            id = recordId,
            productId = productId,
            productName = prodName,
            quantityAdded = quantityAdded,
            buyingPrice = buyingPrice,
            supplier = finalSupplier,
            dateFormatted = "Today, 10:00 AM",
            employeeName = employeeName.ifBlank { _currentEmployee.value?.name ?: _shop.value.ownerName.ifBlank { "Store Owner" } },
            shopId = activeShopId
        )
        _stockInRecords.update { listOf(record) + it }
        storageManager.saveStockInRecords(_stockInRecords.value)
        updatePendingLists()

        if (!isPending) {
            // Auto-resolve notifications for this product if its stock is now healthy
            val restockedProduct = _products.value.find { it.id == productId }
            if (restockedProduct != null && !restockedProduct.isLowStock) {
                _notifications.update { items ->
                    items.map { notif ->
                        if (!notif.isRead && productId in notif.productIds) {
                            val allHealthy = notif.productIds.all { pId ->
                                val p = _products.value.find { it.id == pId }
                                p != null && !p.isLowStock
                            }
                            if (allHealthy) notif.copy(isRead = true) else notif
                        } else notif
                    }
                }
                storageManager.saveNotifications(_notifications.value)
            }

            // Asynchronously update product inventory and insert stock-in record in Supabase
            val updatedProduct = _products.value.find { it.id == productId }
            val service = supabase ?: return
            viewModelScope.launch {
                pendingProductUpdates.add(productId)
                try {
                    service.insertStockInRecord(record, activeShopId)
                    if (updatedProduct != null) {
                        service.upsertProduct(updatedProduct, activeShopId)
                    }
                } finally {
                    pendingProductUpdates.remove(productId)
                }
            }
        } else {
            val service = supabase ?: return
            viewModelScope.launch {
                service.insertStockInRecord(record, activeShopId)
            }
        }
    }

    // Expenses
    fun addExpense(title: String, category: String, amount: Double): Boolean {
        val normalizedTitle = title.trim()
        val normalizedCategory = category.trim()
        val safeAmount = amount.coerceAtLeast(0.0)
        if (normalizedTitle.isBlank() || normalizedCategory.isBlank() || safeAmount <= 0.0) return false

        val isPending = !isOwner.value
        val empName = _currentEmployee.value?.name?.ifBlank { "Employee" } ?: "Employee"
        val finalCategory = if (isPending) "PENDING_APPROVAL||requester:$empName||orig:$normalizedCategory" else normalizedCategory
        val expenseId = if (isPending) "exp_pending_${System.currentTimeMillis()}" else "exp_${System.currentTimeMillis()}"

        val activeShopId = getActiveShopId()
        val newExpense = Expense(
            id = expenseId,
            title = normalizedTitle,
            category = finalCategory,
            amount = safeAmount,
            dateFormatted = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")),
            iconType = normalizedCategory.lowercase(),
            shopId = activeShopId
        )
        _expenses.update { listOf(newExpense) + it }
        storageManager.saveExpenses(_expenses.value)
        updatePendingLists()

        val service = supabase ?: return true
        viewModelScope.launch { service.insertExpense(newExpense, activeShopId) }
        return true
    }

    fun updateExpense(expenseId: String, title: String, category: String, amount: Double): Boolean {
        val normalizedTitle = title.trim()
        val normalizedCategory = category.trim()
        if (normalizedTitle.isBlank() || normalizedCategory.isBlank()) return false

        val currentExpense = _expenses.value.find { it.id == expenseId } ?: return false
        val activeShopId = getActiveShopId()
        val updatedExpense = currentExpense.copy(
            title = normalizedTitle,
            category = normalizedCategory,
            amount = amount.coerceAtLeast(0.0),
            iconType = normalizedCategory.lowercase(),
            shopId = activeShopId ?: currentExpense.shopId
        )

        _expenses.update { currentList ->
            currentList.map { if (it.id == expenseId) updatedExpense else it }
        }
        storageManager.saveExpenses(_expenses.value)
        val service = supabase ?: return true
        viewModelScope.launch { service.upsertExpense(updatedExpense, activeShopId) }
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
    fun addEmployee(
        name: String,
        role: String,
        phone: String,
        email: String,
        permissions: List<String> = listOf(
            EmployeePermissions.PERM_RECORD_SALES,
            EmployeePermissions.PERM_STOCK_IN
        )
    ) {
        val activeShopId = getActiveShopId()
        val cleanEmail = email.trim().lowercase()
        val deterministicId = if (cleanEmail.isNotBlank()) {
            "emp_${cleanEmail.replace(Regex("[^a-zA-Z0-9]"), "_")}"
        } else {
            "emp_${System.currentTimeMillis()}"
        }

        val emp = Employee(
            id = deterministicId,
            name = name.trim(),
            role = role.trim().ifBlank { "Cashier" },
            phone = phone.trim(),
            email = email.trim(),
            shopId = activeShopId,
            status = "ACTIVE",
            permissions = permissions
        )
        _employees.update { list ->
            list.filterNot { it.id == emp.id || (cleanEmail.isNotBlank() && it.email.equals(cleanEmail, ignoreCase = true)) } + emp
        }
        storageManager.saveEmployees(_employees.value)

        viewModelScope.launch {
            SupabaseClient.service.upsertEmployee(emp, activeShopId)
        }
    }

    // ==========================================
    // OFFLINE SYNC QUEUE MANAGEMENT
    // ==========================================

    fun enqueueSyncOperation(
        entityType: String,
        entityId: String,
        action: String,
        payloadJson: String
    ) {
        val op = SyncOperation(
            id = "sync_${System.currentTimeMillis()}_${(100..999).random()}",
            entityType = entityType,
            entityId = entityId,
            action = action,
            payloadJson = payloadJson,
            shopId = getActiveShopId()
        )
        _pendingSyncOperations.update { current ->
            val filtered = current.filterNot { it.entityId == entityId && it.action == action }
            filtered + op
        }
        storageManager.savePendingSyncOperations(_pendingSyncOperations.value)
    }

    fun removeSyncOperation(operationId: String) {
        _pendingSyncOperations.update { it.filterNot { op -> op.id == operationId || op.entityId == operationId } }
        storageManager.savePendingSyncOperations(_pendingSyncOperations.value)
    }

    fun processPendingSyncQueue() {
        val service = supabase ?: return
        val pending = _pendingSyncOperations.value
        if (pending.isEmpty()) return

        val activeShopId = getActiveShopId()
        viewModelScope.launch {
            pending.forEach { op ->
                try {
                    when (op.entityType.lowercase()) {
                        "credit" -> {
                            val credit = _customerCredits.value.find { it.id == op.entityId }
                            if (credit != null) {
                                service.upsertCustomerCredit(credit, activeShopId)
                                removeSyncOperation(op.id)
                            }
                        }
                        "sale" -> {
                            val sale = _salesHistory.value.find { it.id == op.entityId }
                            if (sale != null) {
                                service.insertSale(sale, activeShopId)
                                removeSyncOperation(op.id)
                            }
                        }
                        "expense" -> {
                            val expense = _expenses.value.find { it.id == op.entityId }
                            if (expense != null) {
                                service.upsertExpense(expense, activeShopId)
                                removeSyncOperation(op.id)
                            }
                        }
                        "product" -> {
                            val product = _products.value.find { it.id == op.entityId }
                            if (product != null) {
                                service.upsertProduct(product, activeShopId)
                                service.updateProductStock(product.id, product.stockQuantity, activeShopId)
                                removeSyncOperation(op.id)
                            }
                        }
                        "stock_in" -> {
                            val stockIn = _stockInRecords.value.find { it.id == op.entityId }
                            if (stockIn != null) {
                                service.insertStockInRecord(stockIn, activeShopId)
                                removeSyncOperation(op.id)
                            }
                        }
                        else -> {
                            removeSyncOperation(op.id)
                        }
                    }
                } catch (e: Exception) {
                    _pendingSyncOperations.update { list ->
                        list.map {
                            if (it.id == op.id) it.copy(attempts = it.attempts + 1, lastError = e.message)
                            else it
                        }
                    }
                    storageManager.savePendingSyncOperations(_pendingSyncOperations.value)
                }
            }
        }
    }

    // ==========================================
    // AUTOMATED CREDIT REMINDER ENGINE
    // ==========================================

    fun updateReminderConfig(newConfig: ReminderConfig) {
        _reminderConfig.value = newConfig
        storageManager.saveReminderConfig(newConfig)
        _customerCredits.value
            .filter { it.balance > 0.0 && !it.status.equals("Settled", ignoreCase = true) }
            .forEach { scheduleRemindersForCredit(it, isUpdate = true) }
    }

    fun scheduleRemindersForCredit(credit: CustomerCredit, isUpdate: Boolean = false) {
        if (credit.balance <= 0.0 || credit.status.equals("Settled", ignoreCase = true)) {
            cancelRemindersForCredit(credit.id)
            return
        }
        val config = _reminderConfig.value
        val parsedDueDate = CreditLedgerUtils.parseDueDate(credit.dueDate) ?: return

        val zoneId = ReminderSchedulerEngine.parseZoneId(config.timeZoneId)
        val calculatedEvents = ReminderSchedulerEngine.generateSchedule(parsedDueDate, config)

        val priorForCredit = _scheduledReminders.value.filter { it.creditId == credit.id }
        val newSchedules = calculatedEvents.map { event ->
            val epochMillis = event.scheduledDateTime.atZone(zoneId).toInstant().toEpochMilli()
            val scheduleId = "rem_${credit.id}_${event.type.storageValue}_${parsedDueDate.toEpochDay()}"
            priorForCredit.firstOrNull { it.id == scheduleId && it.status !in setOf(DeliveryStatus.QUEUED, DeliveryStatus.ATTEMPTED) }
                ?: ScheduledReminder(
                id = scheduleId,
                creditId = credit.id,
                customerName = credit.customerName,
                customerPhone = credit.customerPhone,
                reminderType = event.type,
                scheduledTimeMillis = epochMillis,
                remainingBalanceAtScheduling = credit.balance,
                shopId = credit.shopId ?: getActiveShopId(),
                status = DeliveryStatus.QUEUED
            )
        }

        _scheduledReminders.update { current ->
            val kept = if (isUpdate) {
                current.filterNot { it.creditId == credit.id && it.status in setOf(DeliveryStatus.QUEUED, DeliveryStatus.ATTEMPTED) }
            } else current
            (kept + newSchedules).distinctBy { it.id }
        }
        storageManager.saveScheduledReminders(_scheduledReminders.value)
    }

    fun cancelRemindersForCredit(creditId: String) {
        _scheduledReminders.update { current ->
            current.map {
                if (it.creditId == creditId && (it.status == DeliveryStatus.QUEUED || it.status == DeliveryStatus.ATTEMPTED)) {
                    it.copy(status = DeliveryStatus.CANCELLED)
                } else it
            }
        }
        storageManager.saveScheduledReminders(_scheduledReminders.value)
    }

    fun evaluateAndProcessPendingReminders(currentTimeMillis: Long = System.currentTimeMillis()) {
        val config = _reminderConfig.value
        if (!config.autoSendEnabled) return

        val schedules = _scheduledReminders.value
        val credits = _customerCredits.value.associateBy { it.id }
        val shopName = _shop.value.name.ifBlank { "RetailSync Store" }
        val zoneId = ReminderSchedulerEngine.parseZoneId(config.timeZoneId)
        val currentDate = java.time.Instant.ofEpochMilli(currentTimeMillis).atZone(zoneId).toLocalDate()

        val pendingReminders = schedules.filter {
            it.status == DeliveryStatus.QUEUED &&
            it.scheduledTimeMillis <= currentTimeMillis
        }

        if (pendingReminders.isEmpty()) return

        // Claim queued reminders synchronously before network work so overlapping syncs cannot double-send.
        val claimedIds = pendingReminders.mapTo(mutableSetOf()) { it.id }
        _scheduledReminders.update { list ->
            list.map { if (it.id in claimedIds && it.status == DeliveryStatus.QUEUED) it.copy(status = DeliveryStatus.ATTEMPTED) else it }
        }
        storageManager.saveScheduledReminders(_scheduledReminders.value)

        viewModelScope.launch {
            for (reminder in pendingReminders) {
                val credit = credits[reminder.creditId]
                if (credit == null) {
                    updateReminderStatus(reminder.id, DeliveryStatus.CANCELLED)
                    continue
                }

                // Verify delivery eligibility
                if (!ReminderSchedulerEngine.shouldDeliverReminder(
                        credit = credit,
                        event = reminder.reminderType,
                        currentBalance = credit.balance,
                        currentDate = currentDate,
                        hasAlreadySentEvent = false
                    )
                ) {
                    updateReminderStatus(reminder.id, DeliveryStatus.CANCELLED)
                    continue
                }

                // Working hours policy check
                val currentLocalDateTime = java.time.Instant.ofEpochMilli(currentTimeMillis).atZone(zoneId).toLocalDateTime()
                val (adjustedDateTime, isDeferred) = ReminderSchedulerEngine.applyWorkingHoursPolicy(currentLocalDateTime, config)
                if (isDeferred) {
                    val deferredMillis = adjustedDateTime.atZone(zoneId).toInstant().toEpochMilli()
                    _scheduledReminders.update { list ->
                        list.map {
                            if (it.id == reminder.id) it.copy(
                                status = DeliveryStatus.QUEUED,
                                scheduledTimeMillis = deferredMillis
                            ) else it
                        }
                    }
                    storageManager.saveScheduledReminders(_scheduledReminders.value)
                    continue
                }

                val overdueDays = ReminderSchedulerEngine.calculateOverdueDays(credit.dueDate, currentDate)
                val messageText = config.templates.render(
                    type = reminder.reminderType,
                    customerName = credit.customerName,
                    balance = credit.balance,
                    dueDate = credit.dueDate,
                    overdueDays = overdueDays,
                    shopName = shopName
                )

                var whatsAppStatus = DeliveryStatus.SKIPPED
                var whatsAppError: String? = null
                var smsStatus = DeliveryStatus.SKIPPED
                var smsError: String? = null

                if (config.defaultChannel == ReminderDeliveryChannel.WHATSAPP || config.defaultChannel == ReminderDeliveryChannel.BOTH) {
                    val res = whatsAppProvider.sendMessage(
                        recipientPhone = credit.customerPhone,
                        messageText = messageText,
                        shopId = getActiveShopId(),
                        metadata = mapOf("customer_name" to credit.customerName)
                    )
                    whatsAppStatus = res.status
                    whatsAppError = res.errorMessage
                }

                if (config.defaultChannel == ReminderDeliveryChannel.SMS || config.defaultChannel == ReminderDeliveryChannel.BOTH) {
                    val res = smsProvider.sendMessage(
                        recipientPhone = credit.customerPhone,
                        messageText = messageText,
                        shopId = getActiveShopId(),
                        metadata = mapOf("customer_name" to credit.customerName)
                    )
                    smsStatus = res.status
                    smsError = res.errorMessage
                }

                val overallStatus = when {
                    whatsAppStatus == DeliveryStatus.DELIVERED || smsStatus == DeliveryStatus.DELIVERED -> DeliveryStatus.DELIVERED
                    whatsAppStatus == DeliveryStatus.ACCEPTED || smsStatus == DeliveryStatus.ACCEPTED -> DeliveryStatus.ACCEPTED
                    whatsAppStatus == DeliveryStatus.QUEUED || smsStatus == DeliveryStatus.QUEUED -> DeliveryStatus.QUEUED
                    else -> DeliveryStatus.FAILED
                }

                val channelSummary = "WhatsApp: ${whatsAppStatus.name} • SMS: ${smsStatus.name}"

                val executionRecord = ReminderExecutionRecord(
                    id = "exec_${System.currentTimeMillis()}_${(100..999).random()}",
                    creditId = credit.id,
                    customerName = credit.customerName,
                    customerPhone = credit.customerPhone,
                    reminderType = reminder.reminderType,
                    scheduledTimeMillis = reminder.scheduledTimeMillis,
                    executionTimeMillis = System.currentTimeMillis(),
                    balanceAtExecution = credit.balance,
                    renderedMessage = messageText,
                    whatsAppStatus = whatsAppStatus,
                    whatsAppError = whatsAppError,
                    smsStatus = smsStatus,
                    smsError = smsError,
                    shopId = getActiveShopId()
                )
                _reminderExecutionHistory.update { listOf(executionRecord) + it }
                storageManager.saveReminderExecutionHistory(_reminderExecutionHistory.value)

                updateReminderStatus(reminder.id, overallStatus)

                // Notify store owner
                val category = if (reminder.reminderType == ReminderEventType.ADVANCE || reminder.reminderType == ReminderEventType.ADVANCE_WEEK || reminder.reminderType == ReminderEventType.DUE_DATE) {
                    NotificationCategory.CREDIT_REMINDER
                } else {
                    NotificationCategory.OVERDUE_CREDIT
                }

                val notifTitle = when (reminder.reminderType) {
                    ReminderEventType.ADVANCE -> "Advance Reminder: ${credit.customerName}"
                    ReminderEventType.ADVANCE_WEEK -> "One Week Reminder: ${credit.customerName}"
                    ReminderEventType.DUE_DATE -> "Payment Due Today: ${credit.customerName}"
                    ReminderEventType.FIRST_OVERDUE -> "Overdue Alert: ${credit.customerName}"
                    ReminderEventType.REPEATED_OVERDUE -> "Repeated Overdue Alert: ${credit.customerName}"
                }

                val notifBody = "${credit.customerName} owes KSh ${String.format(java.util.Locale.US, "%,.2f", credit.balance)} (Due: ${credit.dueDate}). ($channelSummary)"
                val channelErrors = buildList {
                    whatsAppError?.takeIf { whatsAppStatus == DeliveryStatus.FAILED }?.let { add("WhatsApp: $it") }
                    smsError?.takeIf { smsStatus == DeliveryStatus.FAILED }?.let { add("SMS: $it") }
                }
                val notificationMessage = if (channelErrors.isEmpty()) {
                    notifBody
                } else {
                    "$notifBody Details: ${channelErrors.joinToString("; ")}"
                }

                val appNotification = AppNotification(
                    id = "notif_rem_${System.currentTimeMillis()}_${(100..999).random()}",
                    title = notifTitle,
                    message = notificationMessage,
                    category = category,
                    creditSaleId = credit.id,
                    customerName = credit.customerName,
                    deliveryDetails = channelSummary,
                    createdAt = System.currentTimeMillis()
                )
                _notifications.update { listOf(appNotification) + it }
                storageManager.saveNotifications(_notifications.value)
            }
        }
    }

    private fun updateReminderStatus(reminderId: String, status: DeliveryStatus) {
        _scheduledReminders.update { list ->
            list.map { if (it.id == reminderId) it.copy(status = status) else it }
        }
        storageManager.saveScheduledReminders(_scheduledReminders.value)
    }

    fun checkAndNotifyOverdueSummary() {
        val overdueCredits = _customerCredits.value.filter {
            it.balance > 0 && CreditLedgerUtils.resolveCreditStatus(it).equals("Overdue", ignoreCase = true)
        }
        if (overdueCredits.isEmpty()) return

        val totalOverdueAmount = overdueCredits.sumOf { it.balance }
        val count = overdueCredits.size

        // Only send summary notification if one hasn't been sent in the last 24 hours
        val lastSummary = _notifications.value.find { it.category == NotificationCategory.OVERDUE_CREDIT && it.creditSaleId == null }
        val oneDayMillis = 24 * 60 * 60 * 1000L
        if (lastSummary != null && (System.currentTimeMillis() - lastSummary.createdAt) < oneDayMillis) {
            return
        }

        val summaryNotif = AppNotification(
            id = "notif_overdue_summary_${System.currentTimeMillis()}",
            title = "Overdue Credit Alert: $count Accounts",
            message = "$count credit customers have overdue balances totaling KSh ${String.format(java.util.Locale.US, "%,.2f", totalOverdueAmount)}. Review Credit Sales to send reminders or record repayments.",
            category = NotificationCategory.OVERDUE_CREDIT,
            creditSaleId = null,
            createdAt = System.currentTimeMillis()
        )
        _notifications.update { listOf(summaryNotif) + it }
        storageManager.saveNotifications(_notifications.value)
        if (_systemNotificationsEnabled.value) {
            application?.let { LowStockNotificationManager.show(it, summaryNotif) }
        }
    }

    /**
     * Sends a reminder immediately for the given credit using the current reminder configuration.
     * Returns a concise summary string for UI display.
     */
    suspend fun sendNowNow(credit: CustomerCredit): String {
        val config = _reminderConfig.value
        // Use current date for overdue calculation
        val overdueDays = ReminderSchedulerEngine.calculateOverdueDays(
            credit.dueDate,
            java.time.LocalDate.now()
        )
        val messageText = config.templates.render(
            type = ReminderEventType.DUE_DATE,
            customerName = credit.customerName,
            balance = credit.balance,
            dueDate = credit.dueDate,
            overdueDays = overdueDays,
            shopName = shop.value.name
        )

        var whatsappStatus = DeliveryStatus.SKIPPED
        var whatsappError: String? = null
        var smsStatus = DeliveryStatus.SKIPPED
        var smsError: String? = null

        if (config.defaultChannel == ReminderDeliveryChannel.WHATSAPP || config.defaultChannel == ReminderDeliveryChannel.BOTH) {
            val res = whatsAppProvider.sendMessage(
                recipientPhone = credit.customerPhone,
                messageText = messageText,
                shopId = getActiveShopId()
            )
            whatsappStatus = res.status
            whatsappError = res.errorMessage
        }
        if (config.defaultChannel == ReminderDeliveryChannel.SMS || config.defaultChannel == ReminderDeliveryChannel.BOTH) {
            val res = smsProvider.sendMessage(
                recipientPhone = credit.customerPhone,
                messageText = messageText,
                shopId = getActiveShopId()
            )
            smsStatus = res.status
            smsError = res.errorMessage
        }

        val parts = mutableListOf<String>()
        if (whatsappStatus != DeliveryStatus.SKIPPED) {
            parts.add("WhatsApp: ${whatsappStatus.name}" + (whatsappError?.let { " ($it)" } ?: ""))
        }
        if (smsStatus != DeliveryStatus.SKIPPED) {
            parts.add("SMS: ${smsStatus.name}" + (smsError?.let { " ($it)" } ?: ""))
        }
        return if (parts.isEmpty()) {
            "No channel enabled for sending."
        } else {
            "Reminder sent – ${parts.joinToString(" • ")}" 
        }
    }
}
