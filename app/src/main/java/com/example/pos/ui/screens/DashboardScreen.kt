package com.example.pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.Product
import com.example.pos.ui.components.RetailSyncHeader
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel
import kotlin.math.atan2
import kotlin.math.sqrt

private data class CategorySalesData(
    val name: String,
    val percentage: Float, // 0f to 1f
    val amount: Double,
    val color: Color
)

private data class DailySalesBarData(
    val dayLabel: String,
    val amount: Double,
    val isToday: Boolean = false
)

@Composable
fun DashboardScreen(
    viewModel: PosViewModel,
    profilePhotoUri: String?,
    onNavigateToTab: (Int) -> Unit,
    onOpenStockIn: (String?) -> Unit,
    onOpenCreditSales: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenNotifications: () -> Unit,
    hasUnreadNotifications: Boolean
) {
    val shop by viewModel.shop.collectAsState()
    val products by viewModel.products.collectAsState()
    val salesHistory by viewModel.salesHistory.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    val totalSalesToday = if (salesHistory.isNotEmpty()) {
        salesHistory.filter { it.dateFormatted.contains("Today") || it.dateFormatted.contains("Just now") }
            .sumOf { it.totalAmount }
            .let { if (it > 0) it else 24580.0 }
    } else 24580.0

    val grossProfit = 18320.0
    val totalExpenses = expenses.sumOf { it.amount }
    val netProfit = (grossProfit - totalExpenses).coerceAtLeast(0.0)
    val totalTransactions = salesHistory.size + 9
    val lowStockProducts = products.filter { it.isLowStock }

    // Target Progress Calculation
    val dailyTarget = 30000.0
    val targetRatio = (totalSalesToday / dailyTarget).toFloat().coerceIn(0f, 1f)

    // Animated target progress
    var animationTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationTrigger = true
        viewModel.syncWithSupabase()
    }
    val animatedTargetProgress by animateFloatAsState(
        targetValue = if (animationTrigger) targetRatio else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "targetProgress"
    )

    // Category Doughnut Data
    val categoriesData = remember {
        listOf(
            CategorySalesData("Groceries", 0.58f, 14250.0, Color(0xFF10B981)),
            CategorySalesData("Beverages", 0.24f, 5900.0, Color(0xFF8B5CF6)),
            CategorySalesData("Snacks", 0.18f, 4430.0, Color(0xFFF59E0B))
        )
    }
    var selectedCategoryIndex by remember { mutableStateOf<Int?>(null) }

    // Weekly Sales Trend Data
    val weeklyData = remember {
        listOf(
            DailySalesBarData("Mon", 14200.0),
            DailySalesBarData("Tue", 18500.0),
            DailySalesBarData("Wed", 24580.0, isToday = true),
            DailySalesBarData("Thu", 9400.0),
            DailySalesBarData("Fri", 21300.0),
            DailySalesBarData("Sat", 26800.0),
            DailySalesBarData("Sun", 16400.0)
        )
    }
    var selectedDayIndex by remember { mutableStateOf<Int?>(2) } // Default to Wed (Today)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Header Bar
        RetailSyncHeader(
            shopName = shop.name,
            ownerName = shop.ownerName,
            profilePhotoUri = profilePhotoUri,
            onNotificationClick = onOpenNotifications,
            onProfileClick = { onNavigateToTab(4) },
            hasUnreadNotifications = hasUnreadNotifications
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 1. Today's Sales Hero Card with Interactive Doughnut Chart
            SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark,
                borderColor = SurfaceBorder,
                cornerRadius = 24.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Column: Sales Metric & Target Progress
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Today's Sales",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "KSh ${String.format("%,.0f", totalSalesToday)}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    fontSize = 26.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Target Progress Bar
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Target: KSh ${String.format("%,.0f", dailyTarget)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "${(animatedTargetProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldBright,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Animated Track
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SurfaceDarkVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedTargetProgress)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(EmeraldBright)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right Column: Interactive Category Doughnut Chart
                        Box(
                            modifier = Modifier
                                .size(105.dp)
                                .align(Alignment.CenterVertically),
                            contentAlignment = Alignment.Center
                        ) {
                            InteractiveDoughnutChart(
                                categories = categoriesData,
                                selectedIndex = selectedCategoryIndex,
                                onSelectCategory = { index ->
                                    selectedCategoryIndex = if (selectedCategoryIndex == index) null else index
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selected Category Detail Pill (Shows exact category sales on tap)
                    AnimatedVisibility(
                        visible = selectedCategoryIndex != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        selectedCategoryIndex?.let { idx ->
                            val cat = categoriesData[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cat.color.copy(alpha = 0.15f))
                                    .border(1.dp, cat.color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(cat.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${cat.name} Sales:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                                Text(
                                    text = "KSh ${String.format("%,.0f", cat.amount)} (${(cat.percentage * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = cat.color,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    if (selectedCategoryIndex == null) {
                        // Category Legend Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            categoriesData.forEachIndexed { idx, cat ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedCategoryIndex = idx }
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(cat.color)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${cat.name} ${(cat.percentage * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Metrics 2x2 Grid (Financial Snapshot)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Gross Profit
                FinancialCard(
                    title = "Gross Profit",
                    value = "KSh ${String.format("%,.0f", grossProfit)}",
                    badgeText = "Gross",
                    badgeColor = EmeraldBright,
                    badgeBgColor = EmeraldContainer,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                // Expenses (Clickable to open Expenses Modal)
                FinancialCard(
                    title = "Expenses",
                    value = "KSh ${String.format("%,.0f", totalExpenses)}",
                    badgeText = "Track",
                    badgeColor = TextSecondary,
                    badgeBgColor = SurfaceDarkVariant,
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    onClick = onOpenExpenses,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Net Profit (Prominently styled)
                FinancialCard(
                    title = "Net Profit",
                    value = "KSh ${String.format("%,.0f", netProfit)}",
                    badgeText = "+ 18%",
                    badgeColor = EmeraldBright,
                    badgeBgColor = EmeraldContainer,
                    icon = Icons.Default.AccountBalanceWallet,
                    highlightBorder = true,
                    modifier = Modifier.weight(1f)
                )

                // Transactions
                FinancialCard(
                    title = "Transactions",
                    value = "$totalTransactions sales",
                    badgeText = "Today",
                    badgeColor = TextMuted,
                    badgeBgColor = SurfaceDarkVariant,
                    icon = Icons.Outlined.Receipt,
                    onClick = { onNavigateToTab(3) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Compact Action Pills Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary "+ New Sale" Button
                Button(
                    onClick = { onNavigateToTab(1) },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New Sale",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // "Stock In" Pill
                SecondaryActionPill(
                    label = "Stock In",
                    icon = Icons.Default.AddBox,
                    onClick = { onOpenStockIn(null) }
                )

                // "Expenses" Pill
                SecondaryActionPill(
                    label = "Expenses",
                    icon = Icons.Default.Receipt,
                    onClick = onOpenExpenses
                )

                // "Credit" Pill
                SecondaryActionPill(
                    label = "Credit",
                    icon = Icons.Default.CreditCard,
                    onClick = onOpenCreditSales
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 4. Interactive Weekly Sales Performance Bar Chart
            SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark,
                borderColor = SurfaceBorder,
                cornerRadius = 24.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Sales Trend",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        )

                        Text(
                            text = "Tap bar for value",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Floating Tooltip for Selected Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        selectedDayIndex?.let { idx ->
                            val selectedDay = weeklyData[idx]
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDarkVariant)
                                    .border(1.dp, EmeraldBright.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedDay.dayLabel}: ",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = "KSh ${String.format("%,.0f", selectedDay.amount)}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = EmeraldBright,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (selectedDay.isToday) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(EmeraldContainer)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = EmeraldBright,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bars Container
                    val maxWeeklySales = remember { weeklyData.maxOf { it.amount } }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyData.forEachIndexed { index, dayData ->
                            val isSelected = selectedDayIndex == index
                            val barRatio = (dayData.amount / maxWeeklySales).toFloat().coerceIn(0.12f, 1f)

                            val animatedBarHeight by animateFloatAsState(
                                targetValue = if (animationTrigger) barRatio else 0.05f,
                                animationSpec = tween(
                                    durationMillis = 600 + (index * 80),
                                    easing = FastOutSlowInEasing
                                ),
                                label = "bar_${dayData.dayLabel}"
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedDayIndex = index },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Dedicated Bar Container (bar fills up to 100% of available space above label)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(if (isSelected) 24.dp else 18.dp)
                                            .fillMaxHeight(animatedBarHeight)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                            .background(
                                                when {
                                                    isSelected -> EmeraldBright
                                                    dayData.isToday -> EmeraldBright.copy(alpha = 0.75f)
                                                    else -> EmeraldPrimary.copy(alpha = 0.35f)
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 1.dp else 0.dp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Day Label - Guaranteed baseline visibility
                                Text(
                                    text = dayData.dayLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) EmeraldBright else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 5. Low Stock Items (Clean Product Cards, NOT an ad banner)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Low Stock Items",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningContainer)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${lowStockProducts.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WarningYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Text(
                    text = "Manage",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldBright,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onNavigateToTab(2) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (lowStockProducts.isEmpty()) {
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceDark,
                    borderColor = SurfaceBorder,
                    cornerRadius = 16.dp
                ) {
                    Text(
                        text = "All products are healthy in stock",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                        modifier = Modifier.padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Horizontal scrolling cards for low stock items
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(lowStockProducts) { product ->
                        LowStockProductCard(
                            product = product,
                            onRestock = { onOpenStockIn(product.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Recent Transactions Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                )

                Text(
                    text = "See All",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldBright,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onNavigateToTab(3) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            salesHistory.take(3).forEach { sale ->
                SurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    backgroundColor = SurfaceDark,
                    borderColor = SurfaceBorder,
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDarkVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = EmeraldBright,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = sale.id,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "${sale.items.size} items • ${sale.paymentMethod}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "KSh ${String.format("%,.0f", sale.totalAmount)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldBright,
                                    fontSize = 14.sp
                                )
                            )
                            Text(
                                text = sale.dateFormatted,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Interactive Doughnut Chart showing categories with tap inspection.
 */
@Composable
private fun InteractiveDoughnutChart(
    categories: List<CategorySalesData>,
    selectedIndex: Int?,
    onSelectCategory: (Int) -> Unit
) {
    var animationTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationTrigger = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "doughnutProgress"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    val radius = sqrt(dx * dx + dy * dy)
                    val outerRadius = size.width / 2f
                    val innerRadius = outerRadius * 0.55f

                    // If tap is inside doughnut ring
                    if (radius in innerRadius..outerRadius) {
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angle < 0) angle += 360f

                        // Determine which slice was clicked
                        var accumAngle = 0f
                        for (i in categories.indices) {
                            val sweep = categories[i].percentage * 360f
                            if (angle >= accumAngle && angle < accumAngle + sweep) {
                                onSelectCategory(i)
                                break
                            }
                            accumAngle += sweep
                        }
                    } else {
                        // Tapped center or outside -> clear selection
                        onSelectCategory(-1)
                    }
                }
            }
    ) {
        val strokeWidthNormal = 14.dp.toPx()
        val strokeWidthSelected = 18.dp.toPx()
        val arcPadding = strokeWidthSelected / 2f
        val arcSize = Size(size.width - arcPadding * 2f, size.height - arcPadding * 2f)
        val arcTopLeft = Offset(arcPadding, arcPadding)

        var startAngle = -90f // Start from 12 o'clock

        categories.forEachIndexed { index, cat ->
            val isSelected = selectedIndex == index
            val hasSelection = selectedIndex != null
            val sweep = cat.percentage * 360f * animatedProgress

            val stroke = if (isSelected) strokeWidthSelected else strokeWidthNormal
            val color = when {
                isSelected -> cat.color
                hasSelection -> cat.color.copy(alpha = 0.35f)
                else -> cat.color
            }

            // Draw arc slice with small gap spacing
            val gap = 3f
            drawArc(
                color = color,
                startAngle = startAngle + (gap / 2f),
                sweepAngle = (sweep - gap).coerceAtLeast(0.1f),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            startAngle += sweep
        }
    }
}

/**
 * Clean Financial Snapshot Card
 */
@Composable
private fun FinancialCard(
    title: String,
    value: String,
    badgeText: String,
    badgeColor: Color,
    badgeBgColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    highlightBorder: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    SurfaceCard(
        modifier = modifier,
        backgroundColor = SurfaceDark,
        borderColor = if (highlightBorder) EmeraldBright.copy(alpha = 0.5f) else SurfaceBorder,
        cornerRadius = 18.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDarkVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = EmeraldBright,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Secondary Pill Action Button
 */
@Composable
private fun SecondaryActionPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDarkVariant)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldBright,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Clean Product Low Stock Item Card (NOT an ad banner)
 */
@Composable
private fun LowStockProductCard(
    product: Product,
    onRestock: () -> Unit
) {
    SurfaceCard(
        modifier = Modifier.width(200.dp),
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder,
        cornerRadius = 18.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDarkVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGroceryStore,
                        contentDescription = null,
                        tint = EmeraldBright,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.stockQuantity} ${product.unit} left",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ErrorRed,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Alert threshold: ${product.minStock} ${product.unit}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 10.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Direct Restock Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldContainer)
                    .border(1.dp, EmeraldBright.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable { onRestock() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Restock",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
