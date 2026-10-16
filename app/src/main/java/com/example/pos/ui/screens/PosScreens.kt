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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
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
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.SecondaryDarkButton
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel

@Composable
fun NewSaleScreen(
    viewModel: PosViewModel,
    onProceedToPayment: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search product name or SKU...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = EmeraldBright) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = com.example.pos.ui.components.posTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Categories Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(category) },
                        label = {
                            Text(
                                text = category,
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
fun PaymentScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit
) {
    val cart by viewModel.cart.collectAsState()
    val totalAmount = cart.sumOf { it.totalPrice }

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
