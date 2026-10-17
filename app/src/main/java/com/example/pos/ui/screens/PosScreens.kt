package com.example.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.CartItem
import com.example.pos.data.CreditLedgerUtils
import com.example.pos.data.Product
import com.example.pos.data.Sale
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
    var saleToEdit by remember { mutableStateOf<Sale?>(null) }

    // If cart has items, automatically switch to product selection
    LaunchedEffect(cart) {
        if (cart.isNotEmpty()) {
            isAddingNewSale = true
        }
    }

    if (isAddingNewSale) {
        // Product Selection View (Catalog view with Category Filters)
        val categories = listOf("All", "Groceries", "Household", "Beverages", "Snacks")

        val filteredProducts = products.filter { product ->
            val matchesCategory = (selectedCategory == "All") || product.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = product.name.contains(searchQuery, ignoreCase = true) ||
                    product.sku.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        val totalCartAmount = cart.sumOf { it.totalPrice }
        val totalCartItems = cart.sumOf { it.quantity }

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
                    Text(
                        text = "Select Products for Sale",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

            // Products Grid / List
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
                        onRemove = { viewModel.removeFromCart(product.id) }
                    )
                }
            }

            // Checkout Floating Bar
            if (cart.isNotEmpty()) {
                SurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    backgroundColor = SurfaceDark,
                    borderColor = EmeraldBright,
                    cornerRadius = 20.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$totalCartItems items in cart",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                            Text(
                                text = "KSh ${String.format("%,.0f", totalCartAmount)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldBright
                                )
                            )
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
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with "New Sale" action button
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

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Banner Card
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

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
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

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method Filter Chips
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

                Spacer(modifier = Modifier.height(16.dp))

                // Sales List
                if (sortedSales.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No sales records found",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sortedSales) { sale ->
                            SaleItemCard(
                                sale = sale,
                                onEdit = { saleToEdit = sale },
                                onDelete = { saleToDelete = sale }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Sale Confirmation Dialog
    if (saleToDelete != null) {
        val target = saleToDelete!!
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
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
                        viewModel.deleteSale(target.id)
                        saleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToDelete = null }) {
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
                                            text = "KSh ${String.format("%,.0f", cartItem.product.sellingPrice)} / ${cartItem.product.unit}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = EmeraldBright)
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
}

@Composable
fun ProductItemCard(
    product: Product,
    quantityInCart: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val isOutOfStock = product.stockQuantity <= 0
    val canAddMore = !isOutOfStock && quantityInCart < product.stockQuantity

    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canAddMore) { onAdd() },
        backgroundColor = SurfaceDark,
        borderColor = if (quantityInCart > 0) EmeraldBright else SurfaceBorder,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KSh ${String.format("%,.0f", product.sellingPrice)} / ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldBright
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isOutOfStock) "Out of Stock" else "Stock: ${product.stockQuantity}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isOutOfStock) ErrorRed else if (product.isLowStock) WarningYellow else TextMuted
                        )
                    )
                }
            }

            if (quantityInCart > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceDarkVariant)
                            .clickable { onRemove() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", style = MaterialTheme.typography.titleMedium.copy(color = EmeraldBright))
                    }

                    Text(
                        text = "$quantityInCart",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

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
                                color = if (canAddMore) EmeraldBright else TextMuted
                            )
                        )
                    }
                }
            } else {
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
                        text = sale.dateFormatted,
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
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
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
                    text = sale.items.joinToString { "${it.product.name} x${it.quantity}" },
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

                Spacer(modifier = Modifier.height(28.dp))

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

            // Complete Sale Button
            PrimaryGreenButton(
                text = if (isCreditSale) "Save Credit Sale" else "Complete Sale",
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
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = EmeraldBright,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sale Completed!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Receipt #${sale.id}",
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
