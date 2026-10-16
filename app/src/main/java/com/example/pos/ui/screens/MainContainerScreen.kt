package com.example.pos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.pos.ui.components.CustomBottomNavBar
import com.example.pos.ui.theme.BgDark
import com.example.pos.ui.theme.AppColorTheme
import com.example.pos.ui.theme.ThemeMode
import com.example.pos.viewmodel.PosViewModel

@Composable
fun MainContainerScreen(
    viewModel: PosViewModel,
    notificationNavigationRequest: Int = 0,
    themeMode: ThemeMode,
    colorTheme: AppColorTheme,
    customAccentColor: Color,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorThemeChange: (AppColorTheme) -> Unit,
    onCustomAccentColorChange: (Color) -> Unit,
    profilePhotoUri: String?,
    onProfilePhotoChange: (String?) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotificationsCount = notifications.count { !it.isRead }
    val hasUnreadNotifications = unreadNotificationsCount > 0
    var activeModal by remember {
        mutableStateOf(if (notificationNavigationRequest > 0) "notifications" else null)
    } // "payment", "add_product", "stock_in", "credit_sales", "expenses", "employees", "notifications"
    var preselectedStockInProductId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notificationNavigationRequest) {
        if (notificationNavigationRequest > 0) activeModal = "notifications"
    }

    val completedSale by viewModel.completedSale.collectAsState()

    val isModalOpen = activeModal != null || completedSale != null
    val isNotOnHome = selectedTab != 0

    // Intercept Back Press to handle hierarchical back navigation:
    // 1. Close active modal/dialog if open
    // 2. Return to Dashboard (Home tab) if on another tab
    // 3. Exit app only when on Dashboard with no modals open
    BackHandler(enabled = isModalOpen || isNotOnHome) {
        if (completedSale != null) {
            viewModel.dismissReceiptModal()
            activeModal = null
            preselectedStockInProductId = null
        } else if (activeModal != null) {
            activeModal = null
            preselectedStockInProductId = null
        } else if (selectedTab != 0) {
            viewModel.selectTab(0)
        }
    }

    // Show sale receipt modal when a sale completes
    if (completedSale != null) {
        SaleSuccessDialog(
            viewModel = viewModel,
            onDismiss = {
                viewModel.dismissReceiptModal()
                activeModal = null
                preselectedStockInProductId = null
                viewModel.selectTab(0) // Return to home
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (activeModal == null) {
                CustomBottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        },
        containerColor = BgDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgDark)
        ) {
            if (activeModal != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    when (activeModal) {
                        "payment" -> PaymentScreen(
                            viewModel = viewModel,
                            onBack = { activeModal = null }
                        )
                        "add_product" -> AddProductScreen(
                            viewModel = viewModel,
                            onClose = { activeModal = null }
                        )
                        "stock_in" -> StockInScreen(
                            viewModel = viewModel,
                            initialProductId = preselectedStockInProductId,
                            onClose = {
                                activeModal = null
                                preselectedStockInProductId = null
                            }
                        )
                        "credit_sales" -> CreditSalesScreen(
                            viewModel = viewModel,
                            onClose = { activeModal = null }
                        )
                        "expenses" -> ExpensesScreen(
                            viewModel = viewModel,
                            onClose = { activeModal = null }
                        )
                        "employees" -> EmployeesScreen(
                            viewModel = viewModel,
                            onClose = { activeModal = null }
                        )
                        "notifications" -> NotificationCenterScreen(
                            viewModel = viewModel,
                            onClose = { activeModal = null },
                            onViewInventory = {
                                activeModal = null
                                viewModel.selectTab(2)
                            },
                            onOpenStockIn = { productId ->
                                preselectedStockInProductId = productId
                                activeModal = "stock_in"
                            }
                        )
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        profilePhotoUri = profilePhotoUri,
                        onNavigateToTab = { viewModel.selectTab(it) },
                        onOpenStockIn = { productId ->
                            preselectedStockInProductId = productId
                            activeModal = "stock_in"
                        },
                        onOpenCreditSales = { activeModal = "credit_sales" },
                        onOpenExpenses = { activeModal = "expenses" },
                        onOpenNotifications = { activeModal = "notifications" },
                        hasUnreadNotifications = hasUnreadNotifications,
                        unreadNotificationsCount = unreadNotificationsCount
                    )
                    1 -> NewSaleScreen(
                        viewModel = viewModel,
                        onProceedToPayment = { activeModal = "payment" }
                    )
                    2 -> ProductsScreen(
                        viewModel = viewModel,
                        onOpenAddProduct = { activeModal = "add_product" }
                    )
                    3 -> ReportsScreen(
                        viewModel = viewModel
                    )
                    4 -> SettingsScreen(
                        viewModel = viewModel,
                        themeMode = themeMode,
                        colorTheme = colorTheme,
                        customAccentColor = customAccentColor,
                        onThemeModeChange = onThemeModeChange,
                        onColorThemeChange = onColorThemeChange,
                        onCustomAccentColorChange = onCustomAccentColorChange,
                        profilePhotoUri = profilePhotoUri,
                        onProfilePhotoChange = onProfilePhotoChange,
                        onOpenEmployees = { activeModal = "employees" },
                        onOpenCreditReminders = { activeModal = "credit_sales" },
                        onOpenExpenses = { activeModal = "expenses" },
                        onOpenNotifications = { activeModal = "notifications" }
                    )
                }
            }
        }
    }
}
