package com.example.pos.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pos.data.*
import com.example.pos.ui.components.FilterChipItem
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.utils.ReportPdfGenerator
import com.example.pos.utils.ReportShareHelper
import com.example.pos.viewmodel.PosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val shop by viewModel.shop.collectAsState()
    val products by viewModel.products.collectAsState()
    val salesHistory by viewModel.salesHistory.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val customerCredits by viewModel.customerCredits.collectAsState()

    var selectedScope by remember { mutableStateOf(ReportTimeScope.DAILY) }

    // Date state
    val currentCalendar = remember { mutableStateOf(Calendar.getInstance()) }
    var customStartEpoch by remember { mutableStateOf(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000) }
    var customEndEpoch by remember { mutableStateOf(System.currentTimeMillis()) }

    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showPreviewModal by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }

    // Compute Date Range depending on Scope
    val (startEpoch, endEpoch, periodText, isAtLatest) = remember(selectedScope, currentCalendar.value.timeInMillis, customStartEpoch, customEndEpoch) {
        val nowCal = Calendar.getInstance()
        when (selectedScope) {
            ReportTimeScope.DAILY -> {
                val (s, e, text) = ReportCalculator.getDayRange(currentCalendar.value)
                val atLatest = currentCalendar.value.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        currentCalendar.value.get(Calendar.DAY_OF_YEAR) >= nowCal.get(Calendar.DAY_OF_YEAR)
                Quadruple(s, e, text, atLatest)
            }
            ReportTimeScope.WEEKLY -> {
                val (s, e, text) = ReportCalculator.getWeekRange(currentCalendar.value)
                val atLatest = currentCalendar.value.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        currentCalendar.value.get(Calendar.WEEK_OF_YEAR) >= nowCal.get(Calendar.WEEK_OF_YEAR)
                Quadruple(s, e, text, atLatest)
            }
            ReportTimeScope.MONTHLY -> {
                val (s, e, text) = ReportCalculator.getMonthRange(currentCalendar.value)
                val atLatest = currentCalendar.value.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        currentCalendar.value.get(Calendar.MONTH) >= nowCal.get(Calendar.MONTH)
                Quadruple(s, e, text, atLatest)
            }
            ReportTimeScope.CUSTOM -> {
                val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                val text = "${sdf.format(Date(customStartEpoch))} – ${sdf.format(Date(customEndEpoch))}"
                Quadruple(customStartEpoch, customEndEpoch, text, false)
            }
        }
    }

    // Dynamic Financial Summary Calculation
    val summary = remember(
        selectedScope, startEpoch, endEpoch, periodText,
        salesHistory, expenses, customerCredits, products
    ) {
        ReportCalculator.computeSummary(
            scope = selectedScope,
            periodLabel = selectedScope.displayName,
            dateRangeText = periodText,
            startEpochMs = startEpoch,
            endEpochMs = endEpoch,
            allSales = salesHistory,
            allExpenses = expenses,
            allCredits = customerCredits,
            allProducts = products
        )
    }

    // Helper functions for date navigation
    fun navigatePrev() {
        val newCal = (currentCalendar.value.clone() as Calendar)
        when (selectedScope) {
            ReportTimeScope.DAILY -> newCal.add(Calendar.DAY_OF_YEAR, -1)
            ReportTimeScope.WEEKLY -> newCal.add(Calendar.WEEK_OF_YEAR, -1)
            ReportTimeScope.MONTHLY -> newCal.add(Calendar.MONTH, -1)
            ReportTimeScope.CUSTOM -> {}
        }
        currentCalendar.value = newCal
    }

    fun navigateNext() {
        if (isAtLatest) return
        val newCal = (currentCalendar.value.clone() as Calendar)
        when (selectedScope) {
            ReportTimeScope.DAILY -> newCal.add(Calendar.DAY_OF_YEAR, 1)
            ReportTimeScope.WEEKLY -> newCal.add(Calendar.WEEK_OF_YEAR, 1)
            ReportTimeScope.MONTHLY -> newCal.add(Calendar.MONTH, 1)
            ReportTimeScope.CUSTOM -> {}
        }
        currentCalendar.value = newCal
    }

    fun openDatePicker(onDateSelected: (Long) -> Unit) {
        val cal = currentCalendar.value
        val picker = DatePickerDialog(
            context,
            { _, year, month, day ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                onDateSelected(selectedCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        picker.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Reports & Statements",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 24.sp
                    )
                )
                Text(
                    text = "Executive business & financial summaries",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            // Sync Refresh Button
            IconButton(
                onClick = { viewModel.syncWithSupabase(force = true) },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceDarkVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Data",
                    tint = EmeraldBright,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Scope Switcher Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportTimeScope.entries.forEach { scope ->
                FilterChipItem(
                    label = scope.displayName,
                    isSelected = selectedScope == scope,
                    onClick = {
                        selectedScope = scope
                        currentCalendar.value = Calendar.getInstance()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Date Navigation Bar
        SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDark,
            borderColor = SurfaceBorder,
            cornerRadius = 18.dp
        ) {
            if (selectedScope == ReportTimeScope.CUSTOM) {
                // Custom Range Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDarkVariant)
                            .clickable {
                                openDatePicker { customStartEpoch = it }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("From Date", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                        Text(
                            text = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(customStartEpoch)),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDarkVariant)
                            .clickable {
                                openDatePicker { customEndEpoch = it }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("To Date", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                        Text(
                            text = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(customEndEpoch)),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                // Daily / Weekly / Monthly Arrow Navigator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navigatePrev() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Period",
                            tint = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                openDatePicker { selectedTime ->
                                    val newCal = Calendar.getInstance().apply { timeInMillis = selectedTime }
                                    currentCalendar.value = newCal
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = EmeraldBright,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = periodText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { navigateNext() },
                        enabled = !isAtLatest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Period",
                            tint = if (!isAtLatest) TextPrimary else TextMuted.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Executive Action Buttons: Preview & PDF Export
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Preview Button
            OutlinedButton(
                onClick = { showPreviewModal = true },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Preview Statement", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // Export PDF Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isGeneratingPdf = true
                        val file = withContext(Dispatchers.IO) {
                            ReportPdfGenerator.generatePdf(context, shop, summary)
                        }
                        generatedPdfFile = file
                        isGeneratingPdf = false
                        ReportShareHelper.sharePdf(context, file, "${shop.name} ${summary.scope.displayName} Report")
                    }
                },
                enabled = !isGeneratingPdf,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldBright,
                    contentColor = Color.White
                )
            ) {
                if (isGeneratingPdf) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Creating PDF...", fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. EXECUTIVE KPI MATRIX (4-Grid Cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Revenue Card
            ExecutiveKpiCard(
                title = "Total Revenue",
                amount = "KSh ${String.format("%,.0f", summary.revenue)}",
                subtitle = "${summary.transactionCount} transactions",
                accentColor = EmeraldBright,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )

            // Net Profit Card
            ExecutiveKpiCard(
                title = "Net Profit",
                amount = "KSh ${String.format("%,.0f", summary.netProfit)}",
                subtitle = "${String.format("%.1f", summary.netProfitMargin)}% margin",
                accentColor = if (summary.netProfit >= 0) EmeraldBright else ErrorRed,
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cost of Goods Sold Card
            ExecutiveKpiCard(
                title = "Cost of Goods (COGS)",
                amount = "KSh ${String.format("%,.0f", summary.cogs)}",
                subtitle = "Gross: KSh ${String.format("%,.0f", summary.grossProfit)}",
                accentColor = TextSecondary,
                icon = Icons.Default.Inventory2,
                modifier = Modifier.weight(1f)
            )

            // Total Expenses Card
            ExecutiveKpiCard(
                title = "Operating Expenses",
                amount = "KSh ${String.format("%,.0f", summary.totalExpenses)}",
                subtitle = "${summary.expensesByCategory.size} categories",
                accentColor = ErrorRed,
                icon = Icons.Default.Receipt,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. CASH FLOW & PAYMENT BREAKDOWN CARD
        SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDark,
            borderColor = SurfaceBorder,
            cornerRadius = 20.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sales Channels & Cash Flow",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = "Avg: KSh ${String.format("%,.0f", summary.averageBasketSize)}/order",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val total = if (summary.revenue > 0) summary.revenue else 1.0
                listOf("Cash", "M-Pesa", "Credit").forEach { method ->
                    val amt = summary.paymentBreakdown[method] ?: 0.0
                    val count = summary.paymentCounts[method] ?: 0
                    val ratio = (amt / total).toFloat().coerceIn(0f, 1f)
                    val methodColor = when (method) {
                        "M-Pesa" -> EmeraldBright
                        "Cash" -> Color(0xFF3B82F6)
                        else -> Color(0xFFF59E0B)
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$method ($count)",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "KSh ${String.format("%,.0f", amt)} (${(ratio * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall.copy(color = methodColor, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(SurfaceDarkVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(methodColor)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. CREDIT & RECEIVABLES SUMMARY
        SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDark,
            borderColor = SurfaceBorder,
            cornerRadius = 20.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Credit & Accounts Receivable",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningContainer)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Due Balances",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WarningYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ReportMetricRow("New Credit Issued", "KSh ${String.format("%,.0f", summary.creditSalesAmount)}", TextPrimary)
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = SurfaceBorder)

                ReportMetricRow("Repayments Collected", "KSh ${String.format("%,.0f", summary.creditCollections)}", EmeraldBright)
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = SurfaceBorder)

                ReportMetricRow("Outstanding Unpaid Debt", "KSh ${String.format("%,.0f", summary.creditOutstandingBalance)}", WarningYellow, isHighlight = true)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. TOP SELLING PRODUCTS SECTION (REAL DATA)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Top Selling Items in Period",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            )

            Text(
                text = "${summary.topSellingProducts.size} Items",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (summary.topSellingProducts.isEmpty()) {
            SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark,
                borderColor = SurfaceBorder,
                cornerRadius = 16.dp
            ) {
                Text(
                    text = "No sales recorded during this reporting interval.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    modifier = Modifier.padding(vertical = 14.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            summary.topSellingProducts.take(5).forEachIndexed { index, item ->
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (index == 0) EmeraldBright else EmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (index == 0) Color.White else EmeraldBright,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "${item.unitsSold} units sold  •  ${item.category}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }

                        Text(
                            text = "KSh ${String.format("%,.0f", item.revenue)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldBright,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // FORMAL DOCUMENT PREVIEW MODAL
    if (showPreviewModal) {
        FormalStatementPreviewDialog(
            shop = shop,
            summary = summary,
            onDismiss = { showPreviewModal = false },
            onShare = {
                coroutineScope.launch {
                    isGeneratingPdf = true
                    val file = withContext(Dispatchers.IO) {
                        ReportPdfGenerator.generatePdf(context, shop, summary)
                    }
                    isGeneratingPdf = false
                    ReportShareHelper.sharePdf(context, file, "${shop.name} ${summary.scope.displayName} Report")
                }
            }
        )
    }
}

@Composable
fun ExecutiveKpiCard(
    title: String,
    amount: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    SurfaceCard(
        modifier = modifier,
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder,
        cornerRadius = 18.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    fontSize = 18.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = accentColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun ReportMetricRow(
    label: String,
    amount: String,
    amountColor: Color,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isHighlight) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.5.sp)
            else MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 13.sp)
        )
        Text(
            text = amount,
            style = if (isHighlight) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = amountColor, fontSize = 16.sp)
            else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = amountColor, fontSize = 14.sp)
        )
    }
}

/**
 * High-fidelity in-app preview of the formal document
 */
@Composable
fun FormalStatementPreviewDialog(
    shop: Shop,
    summary: ReportFinancialSummary,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFFF8FAFC) // Formal paper background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top close & share bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF0F172A))
                    }
                    Text(
                        text = "Formal Statement Preview",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    )
                    Button(
                        onClick = onShare,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Document Paper Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Letterhead
                        Text(
                            text = shop.name.ifBlank { "RetailSync Store" },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = "Owner: ${shop.ownerName}  •  Tel: ${shop.phone}  •  ${shop.address}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569), fontSize = 11.sp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Period Banner
                        Text(
                            text = "FINANCIAL STATEMENT: ${summary.scope.displayName.uppercase()}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        )
                        Text(
                            text = "Date Interval: ${summary.dateRangeText}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Financial Table
                        Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))) {
                            PreviewTableRow("Total Gross Sales", "KSh ${String.format("%,.0f", summary.revenue)}", true)
                            PreviewTableRow("Cost of Goods Sold (COGS)", "- KSh ${String.format("%,.0f", summary.cogs)}")
                            PreviewTableRow("Gross Profit Margin", "KSh ${String.format("%,.0f", summary.grossProfit)} (${String.format("%.1f", summary.grossProfitMargin)}%)", true)
                            PreviewTableRow("Total Operating Expenses", "- KSh ${String.format("%,.0f", summary.totalExpenses)}")
                            PreviewTableRow("Net Profit", "KSh ${String.format("%,.0f", summary.netProfit)} (${String.format("%.1f", summary.netProfitMargin)}%)", isTotal = true)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Payment Methods Breakdown",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))) {
                            summary.paymentBreakdown.forEach { (method, amt) ->
                                PreviewTableRow(method, "KSh ${String.format("%,.0f", amt)}")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign-off line
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Box(modifier = Modifier.width(130.dp).height(1.dp).background(Color(0xFF94A3B8)))
                                Text("Store Manager", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                            }
                            Column {
                                Box(modifier = Modifier.width(130.dp).height(1.dp).background(Color(0xFF94A3B8)))
                                Text("Authorized Stamp", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewTableRow(label: String, value: String, isHighlight: Boolean = false, isTotal: Boolean = false) {
    val bg = if (isTotal) Color(0xFFE2E8F0) else if (isHighlight) Color(0xFFF8FAFC) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isTotal || isHighlight) FontWeight.Bold else FontWeight.Normal,
                color = Color(0xFF1E293B)
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isTotal) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isTotal) Color(0xFF059669) else Color(0xFF1E293B)
            )
        )
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
