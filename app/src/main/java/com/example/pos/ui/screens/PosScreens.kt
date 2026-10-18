package com.example.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.CartItem
import com.example.pos.data.CreditLedgerUtils
import com.example.pos.data.Product
import com.example.pos.data.Sale
import com.example.pos.ui.components.FilterChipItem
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.SecondaryDarkButton
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDateSelector(
    selectedDueDate: String,
    onDueDateSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember { (1..14).map { "In $it ${if (it == 1) "Day" else "Days"}" } }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Payment Due Date", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary))
        Spacer(modifier = Modifier.height(6.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedDueDate.ifBlank { "In 14 Days" },
                onValueChange = {},
                readOnly = true,
                label = { Text("Due Date (Max 14 Days)", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldBright) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                colors = com.example.pos.ui.components.posTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SurfaceDark)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (selectedDueDate == option) EmeraldBright else TextPrimary,
                                fontWeight = if (selectedDueDate == option) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onDueDateSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

fun formatSaleTimestamp(sale: Sale): String {
    if (sale.timestamp <= 0L) {
        return if (sale.dateFormatted.isNotBlank() && !sale.dateFormatted.equals("Just now", ignoreCase = true)) {
            sale.dateFormatted
        } else {
            "Today"
        }
    }
    return try {
        val saleCal = java.util.Calendar.getInstance().apply { timeInMillis = sale.timestamp }
        val nowCal = java.util.Calendar.getInstance()
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(saleCal.time)

        val isSameYear = saleCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR)
        val isSameDay = isSameYear && saleCal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR)

        val yesterdayCal = (nowCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterdayCal.get(java.util.Calendar.YEAR) == saleCal.get(java.util.Calendar.YEAR) &&
                yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR) == saleCal.get(java.util.Calendar.DAY_OF_YEAR)

        when {
            isSameDay -> "Today, $timeFormat"
            isYesterday -> "Yesterday, $timeFormat"
            isSameYear -> java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.US).format(saleCal.time)
            else -> java.text.SimpleDateFormat("MMM d, yyyy, h:mm a", java.util.Locale.US).format(saleCal.time)
        }
    } catch (e: Exception) {
        if (sale.dateFormatted.isNotBlank() && !sale.dateFormatted.equals("Just now", ignoreCase = true)) {
            sale.dateFormatted
        } else {
            "Today"
        }
    }
}

@Composable
fun NewSaleScreen(
    viewModel: PosViewModel,
    onProceedToPayment: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val salesHistory by viewModel.salesHistory.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isAddingNewSale by remember { mutableStateOf(false) }
    var selectedMethodFilter by remember { mutableStateOf("All") }
    var saleToDelete by remember { mutableStateOf<Sale?>(null) }
    var showFinalDeleteConfirm by remember { mutableStateOf(false) }
    var saleToEdit by remember { mutableStateOf<Sale?>(null) }

    // If cart has items, automatically switch to product selection
    LaunchedEffect(cart) {
        if (cart.isNotEmpty()) {
            isAddingNewSale = true
        }
    }

    var editingQuantityProduct by remember { mutableStateOf<Product?>(null) }

    if (isAddingNewSale) {
        val productCategories = remember(products) {
            products.map { it.category.trim() }.filter { it.isNotBlank() }.distinct()
        }
        val filterCategories = remember(productCategories) {
            listOf("All", "Wholesale", "Retail") + productCategories.filter {
                !it.equals("All", true) && !it.equals("Wholesale", true) && !it.equals("Retail", true)
            }
        }

        val filteredProducts = products.filter { product ->
            if (product.supplier.startsWith("PENDING_APPROVAL||")) return@filter false
            val inCartQty = cart.find { it.product.id == product.id }?.quantity ?: 0
            val matchesCategory = when (selectedCategory.lowercase()) {
                "all" -> true
                "wholesale" -> product.hasWholesalePricing || product.isWholesaleFor(inCartQty)
                "retail" -> !product.hasWholesalePricing && !product.isWholesaleFor(inCartQty)
                else -> product.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.sku.contains(searchQuery, ignoreCase = true) ||
                    product.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        val totalCartAmount = cart.sumOf { it.totalPrice }
        val totalCartItems = cart.sumOf { it.quantity }
        val wholesaleCartItemsCount = cart.count { it.isWholesale }
        val totalWholesaleSavings = cart.sumOf { it.wholesaleSavings }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isAddingNewSale = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Select Products for Sale",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Wholesale pricing activates automatically when quantity threshold is reached",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search products by name, SKU...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = EmeraldBright) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category & Pricing Mode Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterCategories) { cat ->
                        FilterChipItem(
                            label = if (cat == "Wholesale") "⚡ Wholesale" else cat,
                            isSelected = selectedCategory.equals(cat, ignoreCase = true),
                            onClick = { viewModel.selectCategory(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Products List
                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No products found",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (selectedCategory != "All" || searchQuery.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = {
                                    viewModel.selectCategory("All")
                                    viewModel.updateSearchQuery("")
                                }) {
                                    Text("Clear filters", color = EmeraldBright)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            val cartItem = cart.find { it.product.id == product.id }
                            val qtyInCart = cartItem?.quantity ?: 0

                            ProductItemCard(
                                product = product,
                                quantityInCart = qtyInCart,
                                onAdd = { viewModel.addToCart(product) },
                                onRemove = { viewModel.removeFromCart(product.id) },
                                onSetQuantity = { qty -> viewModel.setCartQuantity(product.id, qty) },
                                onOpenQuantityDialog = { editingQuantityProduct = product }
                            )
                        }
                    }
                }

                // Checkout Floating Bar
                if (cart.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        backgroundColor = SurfaceDark,
                        borderColor = EmeraldBright,
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$totalCartItems items in cart",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                    if (wholesaleCartItemsCount > 0) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            color = EmeraldContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "$wholesaleCartItemsCount Wholesale",
                                                color = EmeraldBright,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "KSh ${String.format("%,.0f", totalCartAmount)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldBright
                                    )
                                )
                                if (totalWholesaleSavings > 0) {
                                    Text(
                                        text = "Saved KSh ${String.format("%,.0f", totalWholesaleSavings)} with wholesale",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = WarningYellow,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = onProceedToPayment,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldBright),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Charge", color = BgDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Sales History View (Recent Transactions sorted by date)
        val sortedSales = remember(salesHistory, searchQuery, selectedMethodFilter) {
            salesHistory
                .filter { sale ->
                    val matchesSearch = searchQuery.isBlank() ||
                            sale.id.contains(searchQuery, ignoreCase = true) ||
                            sale.customerName.contains(searchQuery, ignoreCase = true)
                    val matchesMethod = selectedMethodFilter == "All" ||
                            sale.paymentMethod.equals(selectedMethodFilter, ignoreCase = true)
                    matchesSearch && matchesMethod
                }
                .sortedByDescending { it.timestamp }
        }

        val totalRevenue = salesHistory.sumOf { it.totalAmount }
        val totalCount = salesHistory.size

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Header with "New Sale" action button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sales Transactions",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "$totalCount total sales recorded",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }

                        Button(
                            onClick = { isAddingNewSale = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldBright),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = BgDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Sale", color = BgDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Stats Banner Card
                item {
                    SurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = SurfaceDark,
                        borderColor = SurfaceBorder,
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Revenue", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                Text(
                                    "KSh ${String.format("%,.0f", totalRevenue)}",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldBright
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = EmeraldBright)
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search receipt ID or customer...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = EmeraldBright) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = com.example.pos.ui.components.posTextFieldColors(),
                        singleLine = true
                    )
                }

                // Payment Method Filter Chips
                item {
                    val methodFilters = listOf("All", "Cash", "M-Pesa", "Credit")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        methodFilters.forEach { filter ->
                            val isSelected = selectedMethodFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMethodFilter = filter },
                                label = {
                                    Text(
                                        text = filter,
                                        color = if (isSelected) BgDark else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceDark,
                                    selectedContainerColor = EmeraldBright
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = SurfaceBorder,
                                    selectedBorderColor = EmeraldBright
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }

                // Sales Transactions List
                if (sortedSales.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No sales records found",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    items(sortedSales, key = { it.id }) { sale ->
                        SaleItemCard(
                            sale = sale,
                            onEdit = { saleToEdit = sale },
                            onDelete = {
                                saleToDelete = sale
                                showFinalDeleteConfirm = false
                            }
                        )
                    }
                }
            }
        }
    }

    // 1. Initial Delete Sale Confirmation Dialog
    if (saleToDelete != null && !showFinalDeleteConfirm) {
        val target = saleToDelete!!
        AlertDialog(
            onDismissRequest = {
                saleToDelete = null
                showFinalDeleteConfirm = false
            },
            containerColor = SurfaceDark,
            title = {
                Text("Delete Sale", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete sale #${target.id}? This will remove it permanently.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinalDeleteConfirm = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldBright),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = BgDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    saleToDelete = null
                    showFinalDeleteConfirm = false
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 2. Secondary Confirmation Dialog ("just to be sure sure")
    if (saleToDelete != null && showFinalDeleteConfirm) {
        val target = saleToDelete!!
        AlertDialog(
            onDismissRequest = {
                saleToDelete = null
                showFinalDeleteConfirm = false
            },
            containerColor = SurfaceDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningYellow,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Deletion", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Are you sure sure you want to permanently delete sale #${target.id}? This action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSale(target.id)
                        saleToDelete = null
                        showFinalDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldBright),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Delete", color = BgDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    saleToDelete = null
                    showFinalDeleteConfirm = false
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Edit Sale Dialog
    if (saleToEdit != null) {
        val target = saleToEdit!!
        var editedCustomerName by remember { mutableStateOf(target.customerName) }
        var editedPaymentMethod by remember { mutableStateOf(target.paymentMethod) }
        var editedPaidAmountInput by remember { mutableStateOf(if (target.isCreditSale) target.paidAmount.toString() else "") }
        var editedDueDate by remember { mutableStateOf(if (target.dueDate.isNotBlank()) target.dueDate else "In 14 Days") }
        var editedItems by remember { mutableStateOf(target.items) }

        val recalculatedTotal = editedItems.sumOf { it.totalPrice }
        val isCredit = editedPaymentMethod == "Credit"
        val paidAmountNum = if (isCredit) {
            editedPaidAmountInput.toDoubleOrNull() ?: recalculatedTotal
        } else recalculatedTotal
        val calculatedBalance = (recalculatedTotal - paidAmountNum).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { saleToEdit = null },
            containerColor = SurfaceDark,
            title = {
                Text("Edit Sale #${target.id}", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Customer Name
                    OutlinedTextField(
                        value = editedCustomerName,
                        onValueChange = { editedCustomerName = it },
                        label = { Text("Customer Name", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = com.example.pos.ui.components.posTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Payment Method
                    Text("Payment Method", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "M-Pesa", "Credit").forEach { method ->
                            FilterChip(
                                selected = editedPaymentMethod == method,
                                onClick = { editedPaymentMethod = method },
                                label = { Text(method, color = if (editedPaymentMethod == method) BgDark else TextPrimary) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceDarkVariant,
                                    selectedContainerColor = EmeraldBright
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Items & Quantities Editor
                    Text("Products & Quantities", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (editedItems.isEmpty()) {
                        Text("No items selected in sale", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    } else {
                        editedItems.forEachIndexed { index, cartItem ->
                            SurfaceCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                backgroundColor = SurfaceDarkVariant,
                                borderColor = SurfaceBorder,
                                cornerRadius = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cartItem.product.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                        )
                                        Text(
                                            text = "KSh ${String.format("%,.0f", cartItem.unitPrice)} / ${cartItem.product.unit}${if (cartItem.isWholesale) " (Wholesale)" else " (Retail)"}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = if (cartItem.isWholesale) EmeraldBright else TextSecondary)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Decrease quantity button
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(SurfaceDark)
                                                .clickable {
                                                    val updatedList = editedItems.toMutableList()
                                                    if (cartItem.quantity > 1) {
                                                        updatedList[index] = cartItem.copy(quantity = cartItem.quantity - 1)
                                                    } else {
                                                        updatedList.removeAt(index)
                                                    }
                                                    editedItems = updatedList
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("-", color = EmeraldBright, fontWeight = FontWeight.Bold)
                                        }

                                        Text("${cartItem.quantity}", color = TextPrimary, fontWeight = FontWeight.Bold)

                                        // Increase quantity button
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldContainer)
                                                .clickable {
                                                    val updatedList = editedItems.toMutableList()
                                                    updatedList[index] = cartItem.copy(quantity = cartItem.quantity + 1)
                                                    editedItems = updatedList
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", color = EmeraldBright, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recalculated Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount:", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        Text(
                            "KSh ${String.format("%,.0f", recalculatedTotal)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldBright)
                        )
                    }

                    if (isCredit) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Amount Paid Input (for partial payment adjustment)
                        OutlinedTextField(
                            value = editedPaidAmountInput,
                            onValueChange = { editedPaidAmountInput = it },
                            label = { Text("Partial Amount Paid (KSh)", color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = com.example.pos.ui.components.posTextFieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Credit Balance:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                            Text(
                                "KSh ${String.format("%,.0f", calculatedBalance)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = WarningYellow)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Due Date Selector (Max 14 Days)
                        DueDateSelector(
                            selectedDueDate = editedDueDate,
                            onDueDateSelected = { editedDueDate = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTotal = recalculatedTotal
                        val finalPaid = if (isCredit) paidAmountNum else finalTotal
                        val finalBalance = if (isCredit) calculatedBalance else 0.0

                        viewModel.updateSale(
                            target.copy(
                                customerName = editedCustomerName.trim(),
                                paymentMethod = editedPaymentMethod,
                                items = editedItems,
                                totalAmount = finalTotal,
                                isCreditSale = isCredit,
                                paidAmount = finalPaid,
                                balance = finalBalance,
                                dueDate = if (isCredit) editedDueDate else target.dueDate
                            )
                        )
                        saleToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldBright)
                ) {
                    Text("Save Changes", color = BgDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToEdit = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Set Quantity Dialog
    if (editingQuantityProduct != null) {
        val prod = editingQuantityProduct!!
        val currentInCart = cart.find { it.product.id == prod.id }?.quantity ?: 0
        var typedQty by remember(prod.id) { mutableStateOf(if (currentInCart > 0) currentInCart.toString() else "1") }

        AlertDialog(
            onDismissRequest = { editingQuantityProduct = null },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Set Quantity: ${prod.name}",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Available stock: ${prod.stockQuantity} ${prod.unit}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = SurfaceDarkVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Retail Price", color = TextSecondary, fontSize = 10.sp)
                                Text("KSh ${String.format("%,.0f", prod.sellingPrice)}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        if (prod.hasWholesalePricing) {
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Wholesale (≥${prod.wholesaleQuantity})", color = EmeraldBright, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Text("KSh ${String.format("%,.0f", prod.wholesalePrice)}", color = EmeraldBright, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    if (prod.hasWholesalePricing) {
                        FilledTonalButton(
                            onClick = {
                                typedQty = (prod.wholesaleQuantity ?: 1).toString()
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SurfaceDarkVariant,
                                contentColor = EmeraldBright
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Set Wholesale Min (${prod.wholesaleQuantity} ${prod.unit})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedTextField(
                        value = typedQty,
                        onValueChange = { typedQty = it.filter(Char::isDigit) },
                        label = { Text("Quantity (${prod.unit})") },
                        placeholder = { Text("Enter quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = com.example.pos.ui.components.posTextFieldColors(),
                        singleLine = true
                    )

                    val parsed = typedQty.toIntOrNull() ?: 0
                    if (parsed > 0) {
                        val isWs = prod.isWholesaleFor(parsed)
                        val price = prod.priceForQuantity(parsed)
                        val total = price * parsed
                        val savings = if (isWs) (prod.sellingPrice - price) * parsed else 0.0

                        Surface(
                            color = if (isWs) EmeraldContainer else SurfaceDarkVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isWs) "Wholesale Mode Applied" else "Retail Mode",
                                        color = if (isWs) EmeraldBright else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$parsed × KSh ${String.format("%,.0f", price)}",
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "KSh ${String.format("%,.0f", total)}",
                                        color = if (isWs) EmeraldBright else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (savings > 0) {
                                        Text(
                                            text = "Save KSh ${String.format("%,.0f", savings)}",
                                            color = EmeraldBright,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = typedQty.toIntOrNull() ?: 0
                        viewModel.setCartQuantity(prod.id, qty)
                        editingQuantityProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldBright),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply", color = BgDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (currentInCart > 0) {
                            viewModel.setCartQuantity(prod.id, 0)
                        }
                        editingQuantityProduct = null
                    }
                ) {
                    Text(if (currentInCart > 0) "Remove" else "Cancel", color = if (currentInCart > 0) ErrorRed else TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    quantityInCart: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onSetQuantity: (Int) -> Unit = {},
    onOpenQuantityDialog: () -> Unit = {}
) {
    val isOutOfStock = product.stockQuantity <= 0
    val canAddMore = !isOutOfStock && quantityInCart < product.stockQuantity
    val isWholesale = product.isWholesaleFor(quantityInCart)
    val currentUnitPrice = product.priceForQuantity(quantityInCart)
    val itemSubtotal = currentUnitPrice * quantityInCart
    val wholesaleSavings = if (isWholesale) (product.sellingPrice - currentUnitPrice) * quantityInCart else 0.0

    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canAddMore) { onAdd() },
        backgroundColor = SurfaceDark,
        borderColor = when {
            isWholesale -> EmeraldBright
            quantityInCart > 0 -> EmeraldBright.copy(alpha = 0.7f)
            else -> SurfaceBorder
        },
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isWholesale) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Store, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "WHOLESALE",
                                        color = EmeraldBright,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Pricing display
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        if (isWholesale) {
                            Text(
                                text = "KSh ${String.format("%,.0f", currentUnitPrice)} / ${product.unit}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldBright
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KSh ${String.format("%,.0f", product.sellingPrice)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            )
                        } else {
                            Text(
                                text = "KSh ${String.format("%,.0f", product.sellingPrice)} / ${product.unit}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldBright
                                )
                            )
                            if (product.hasWholesalePricing) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "WS: KSh ${String.format("%,.0f", product.wholesalePrice)} (≥${product.wholesaleQuantity})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = WarningYellow,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    // Stock information
                    val remainingStock = (product.stockQuantity - quantityInCart).coerceAtLeast(0)
                    val stockText = when {
                        isOutOfStock -> "Out of Stock"
                        remainingStock <= 0 -> "0 left (in cart)"
                        quantityInCart > 0 -> "Stock: $remainingStock left"
                        else -> "Stock: ${product.stockQuantity}"
                    }
                    val stockColor = when {
                        isOutOfStock || remainingStock <= 0 -> ErrorRed
                        remainingStock <= product.minStock -> WarningYellow
                        else -> TextMuted
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stockText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = stockColor,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Quantity controls
                if (quantityInCart > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceDarkVariant)
                                .clickable { onRemove() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", style = MaterialTheme.typography.titleMedium.copy(color = EmeraldBright, fontWeight = FontWeight.Bold))
                        }

                        // Tappable quantity badge to directly edit quantity
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenQuantityDialog() },
                            color = if (isWholesale) EmeraldContainer else SurfaceDarkVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$quantityInCart",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWholesale) EmeraldBright else TextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (canAddMore) EmeraldContainer else SurfaceDarkVariant)
                                .clickable(enabled = canAddMore) { onAdd() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (canAddMore) EmeraldBright else TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick wholesale button if wholesale tier exists and stock is sufficient
                        if (product.hasWholesalePricing && product.stockQuantity >= (product.wholesaleQuantity ?: 0)) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSetQuantity(product.wholesaleQuantity ?: 1) },
                                color = SurfaceDarkVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WarningYellow.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Store, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+${product.wholesaleQuantity}",
                                        color = WarningYellow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (canAddMore) EmeraldContainer else SurfaceDarkVariant)
                                .clickable(enabled = canAddMore) { onAdd() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = if (canAddMore) EmeraldBright else TextMuted
                                )
                            )
                        }
                    }
                }
            }

            // Subtotal / Wholesale notification banner for item in cart
            if (quantityInCart > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isWholesale) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Wholesale mode (${quantityInCart} ${product.unit} @ KSh ${String.format("%,.0f", currentUnitPrice)})",
                                color = EmeraldBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "KSh ${String.format("%,.0f", itemSubtotal)} (Save KSh ${String.format("%,.0f", wholesaleSavings)})",
                            color = EmeraldBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        if (product.hasWholesalePricing) {
                            val itemsNeeded = (product.wholesaleQuantity ?: 0) - quantityInCart
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onSetQuantity(product.wholesaleQuantity ?: 0) }
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Add $itemsNeeded more for wholesale (tap to set)",
                                    color = WarningYellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "Retail mode (${quantityInCart} ${product.unit})",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "Subtotal: KSh ${String.format("%,.0f", itemSubtotal)}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaleItemCard(
    sale: Sale,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder,
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Receipt #${sale.id}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = formatSaleTimestamp(sale),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (sale.paymentMethod == "Credit") WarningYellow.copy(alpha = 0.2f) else EmeraldContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = sale.paymentMethod,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (sale.paymentMethod == "Credit") WarningYellow else EmeraldBright
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Customer: ${sale.customerName.ifBlank { "Walk-in Customer" }}",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )

            if (sale.items.isNotEmpty()) {
                Text(
                    text = sale.items.joinToString { "${it.product.name} x${it.quantity}${if (it.isWholesale) " (WS)" else ""}" },
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Amount",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Text(
                    text = "KSh ${String.format("%,.0f", sale.totalAmount)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldBright
                    )
                )
            }
        }
    }
}

@Composable
fun PaymentScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit
) {
    val cart by viewModel.cart.collectAsState()
    val cartTotal = cart.sumOf { it.totalPrice }
    val initialTotalAmount = remember { cartTotal }
    val totalAmount = if (cartTotal > 0) cartTotal else initialTotalAmount

    var selectedPaymentMethod by remember { mutableStateOf("M-Pesa") }
    val isCreditSale = selectedPaymentMethod == "Credit"

    // Walk-in / non-credit customer
    var customerInput by remember { mutableStateOf("") }

    // Credit-specific fields
    var creditCustomerName by remember { mutableStateOf("") }
    var creditCustomerPhone by remember { mutableStateOf("") }
    var creditCustomerNameError by remember { mutableStateOf<String?>(null) }
    var creditCustomerPhoneError by remember { mutableStateOf<String?>(null) }

    var creditDurationDays by remember { mutableStateOf(viewModel.getCreditDurationDays()) }
    var creditDueDate by remember { mutableStateOf(viewModel.defaultCreditDueDate()) }

    var creditDepositInput by remember { mutableStateOf("") }
    var creditDepositError by remember { mutableStateOf<String?>(null) }

    val parsedDeposit = if (creditDepositInput.isBlank()) 0.0 else creditDepositInput.toDoubleOrNull() ?: 0.0
    val remainingBalance = CreditLedgerUtils.calculateOutstandingBalance(totalAmount, parsedDeposit) ?: totalAmount

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Payment",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Total Amount Display
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "KSh ${String.format("%,.0f", totalAmount)}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldBright
                        )
                    )
                }

                if (cart.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    SurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = SurfaceDark,
                        borderColor = SurfaceBorder,
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Order Items (${cart.sumOf { it.quantity }})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                                val wsCount = cart.count { it.isWholesale }
                                if (wsCount > 0) {
                                    Surface(
                                        color = EmeraldContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "$wsCount Wholesale",
                                            color = EmeraldBright,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            cart.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.product.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = if (item.isWholesale) EmeraldContainer else SurfaceDarkVariant,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (item.isWholesale) "Wholesale" else "Retail",
                                                    color = if (item.isWholesale) EmeraldBright else TextMuted,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${item.quantity} ${item.product.unit} × KSh ${String.format("%,.0f", item.unitPrice)}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "KSh ${String.format("%,.0f", item.totalPrice)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                        )
                                        if (item.isWholesale && item.wholesaleSavings > 0) {
                                            Text(
                                                text = "Saved KSh ${String.format("%,.0f", item.wholesaleSavings)}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = EmeraldBright, fontSize = 10.sp)
                                            )
                                        }
                                    }
                                }
                            }

                            val totalWholesaleSavings = cart.sumOf { it.wholesaleSavings }
                            if (totalWholesaleSavings > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Wholesale Savings",
                                        style = MaterialTheme.typography.bodySmall.copy(color = EmeraldBright, fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "- KSh ${String.format("%,.0f", totalWholesaleSavings)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = EmeraldBright, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Payment Methods
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                val methods = listOf("Cash", "M-Pesa", "Credit")

                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceDark,
                    borderColor = SurfaceBorder,
                    cornerRadius = 20.dp
                ) {
                    methods.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPaymentMethod = method
                                    creditCustomerNameError = null
                                    creditCustomerPhoneError = null
                                    creditDepositError = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == method,
                                onClick = {
                                    selectedPaymentMethod = method
                                    creditCustomerNameError = null
                                    creditCustomerPhoneError = null
                                    creditDepositError = null
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = EmeraldBright,
                                    unselectedColor = TextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (method == "Credit") "Credit Sale" else method,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isCreditSale) {
                    // Credit Customer Name (Mandatory)
                    OutlinedTextField(
                        value = creditCustomerName,
                        onValueChange = {
                            creditCustomerName = it
                            if (creditCustomerNameError != null) creditCustomerNameError = null
                        },
                        label = { Text("Customer Name *") },
                        placeholder = { Text("Enter customer name", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldBright) },
                        isError = creditCustomerNameError != null,
                        supportingText = {
                            if (creditCustomerNameError != null) {
                                Text(creditCustomerNameError!!, color = ErrorRed)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                        colors = com.example.pos.ui.components.posTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Credit Customer Phone (Mandatory)
                    OutlinedTextField(
                        value = creditCustomerPhone,
                        onValueChange = {
                            creditCustomerPhone = it
                            if (creditCustomerPhoneError != null) creditCustomerPhoneError = null
                        },
                        label = { Text("Customer Phone *") },
                        placeholder = { Text("e.g. 0712345678 or +254...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldBright) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = creditCustomerPhoneError != null,
                        supportingText = {
                            if (creditCustomerPhoneError != null) {
                                Text(creditCustomerPhoneError!!, color = ErrorRed)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                        colors = com.example.pos.ui.components.posTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Credit Duration
                    Text(
                        text = "Credit Duration",
                        style = MaterialTheme.typography.titleSmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(7, 14, 30, 60).forEach { days ->
                            FilterChip(
                                selected = creditDurationDays == days,
                                onClick = {
                                    creditDurationDays = days
                                    creditDueDate = CreditLedgerUtils.calculateDueDate(days)
                                },
                                label = { Text("${days}d") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Due Date display
                    OutlinedTextField(
                        value = creditDueDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Due Date", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldBright) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                        colors = com.example.pos.ui.components.posTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Deposit / Initial Payment (Optional)
                    OutlinedTextField(
                        value = creditDepositInput,
                        onValueChange = {
                            creditDepositInput = it
                            if (creditDepositError != null) creditDepositError = null
                        },
                        label = { Text("Initial Deposit (Optional)") },
                        placeholder = { Text("0.00", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = creditDepositError != null,
                        supportingText = {
                            if (creditDepositError != null) {
                                Text(creditDepositError!!, color = ErrorRed)
                            } else {
                                Text("Remaining balance: KSh ${String.format("%,.0f", remainingBalance)}", color = WarningYellow)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                        colors = com.example.pos.ui.components.posTextFieldColors(),
                        singleLine = true
                    )
                } else {
                    // Customer (Optional) Input for Cash / M-Pesa
                    OutlinedTextField(
                        value = customerInput,
                        onValueChange = { customerInput = it },
                        label = { Text("Customer (Optional)") },
                        placeholder = { Text("Name or phone number", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldBright) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                        colors = com.example.pos.ui.components.posTextFieldColors(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val canRecord = viewModel.canRecordSales()
            if (!canRecord) {
                SurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    backgroundColor = ErrorRed.copy(alpha = 0.12f),
                    borderColor = ErrorRed.copy(alpha = 0.4f),
                    cornerRadius = 14.dp
                ) {
                    Text(
                        "Your permission to record sales has been revoked by the store owner.",
                        color = ErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Complete Sale Button
            PrimaryGreenButton(
                text = if (!canRecord) "Permission Revoked" else if (isCreditSale) "Save Credit Sale" else "Complete Sale",
                enabled = canRecord,
                onClick = {
                    if (isCreditSale) {
                        val trimmedName = creditCustomerName.trim()
                        var hasError = false
                        if (trimmedName.isBlank()) {
                            creditCustomerNameError = "Customer name is required"
                            hasError = true
                        }
                        val normalizedPhone = CreditLedgerUtils.normalizePhoneNumber(creditCustomerPhone)
                        if (normalizedPhone == null) {
                            creditCustomerPhoneError = "Valid phone number is required (e.g. 0712345678 or +254...)"
                            hasError = true
                        }
                        val deposit = creditDepositInput.toDoubleOrNull() ?: 0.0
                        if (deposit < 0.0 || deposit > totalAmount) {
                            creditDepositError = "Deposit cannot exceed total amount"
                            hasError = true
                        }
                        if (hasError) return@PrimaryGreenButton

                        viewModel.completeSale(
                            paymentMethod = "Credit",
                            customerName = trimmedName,
                            customerPhone = normalizedPhone!!,
                            isCredit = true,
                            paidAmount = deposit,
                            dueDate = creditDueDate
                        )
                    } else {
                        viewModel.completeSale(
                            paymentMethod = selectedPaymentMethod,
                            customerName = customerInput.trim(),
                            customerPhone = "",
                            isCredit = false
                        )
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun SaleSuccessDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val completedSale by viewModel.completedSale.collectAsState()

    if (completedSale != null) {
        val sale = completedSale!!

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = SurfaceDark,
            title = null,
            text = {
                val isPendingApproval = sale.id.startsWith("SL_PENDING_") || sale.paymentMethod.startsWith("PENDING_")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isPendingApproval) WarningYellow.copy(alpha = 0.15f) else EmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPendingApproval) Icons.Default.Info else Icons.Default.CheckCircle,
                            contentDescription = if (isPendingApproval) "Pending" else "Success",
                            tint = if (isPendingApproval) WarningYellow else EmeraldBright,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isPendingApproval) "Credit Sale Submitted!" else "Sale Completed!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isPendingApproval) WarningYellow else TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isPendingApproval) "Submitted for Owner Approval • #${sale.id}" else "Receipt #${sale.id}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = SurfaceDarkVariant,
                        borderColor = SurfaceBorder,
                        cornerRadius = 16.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Amount", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                                Text(
                                    "KSh ${String.format("%,.0f", sale.totalAmount)}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = EmeraldBright)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Payment Method", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                                Text(
                                    sale.paymentMethod,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                )
                            }

                            if (sale.isCreditSale) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Amount Paid", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                                    Text(
                                        "KSh ${String.format("%,.0f", sale.paidAmount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = EmeraldBright)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Credit Balance", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                                    Text(
                                        "KSh ${String.format("%,.0f", sale.balance)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = WarningYellow)
                                    )
                                }
                            }

                            if (sale.customerName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Customer", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                                    Text(
                                        sale.customerName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = TextPrimary)
                                    )
                                }
                            }

                            if (sale.items.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Items Purchased",
                                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                sale.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.product.name} × ${item.quantity}${if (item.isWholesale) " (Wholesale)" else ""}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "KSh ${String.format("%,.0f", item.totalPrice)}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = EmeraldBright, fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                }
                                val totalSavings = sale.items.sumOf { it.wholesaleSavings }
                                if (totalSavings > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Wholesale Discount", style = MaterialTheme.typography.bodySmall.copy(color = EmeraldBright))
                                        Text("- KSh ${String.format("%,.0f", totalSavings)}", style = MaterialTheme.typography.bodySmall.copy(color = EmeraldBright, fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PrimaryGreenButton(
                        text = "Done",
                        onClick = onDismiss
                    )
                }
            }
        )
    }
}
