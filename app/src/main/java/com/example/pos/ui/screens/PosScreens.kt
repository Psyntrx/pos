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
    var search by remember { mutableStateOf("") }

    val filteredProducts = remember(products, search) {
        if (search.isBlank()) products
        else products.filter {
            it.name.contains(search, ignoreCase = true) || it.sku.contains(search, ignoreCase = true)
        }
    }

    val subtotal = cart.sumOf { it.totalPrice }
    val total = subtotal

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Sale",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                if (cart.isNotEmpty()) {
                    Text(
                        text = "Clear Cart",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable { viewModel.clearCart() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search bar
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search for products...", color = TextSecondary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = EmeraldBright)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                colors = com.example.pos.ui.components.posTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Product Selection List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    val cartItem = cart.find { it.product.id == product.id }
                    val quantityInCart = cartItem?.quantity ?: 0

                    PosProductItemRow(
                        product = product,
                        quantityInCart = quantityInCart,
                        onAdd = { viewModel.addToCart(product) },
                        onRemove = { viewModel.removeFromCart(product.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(200.dp))
                }
            }
        }

        // Sleek Floating Bezelled Summary Bar
        Surface(
            color = SurfaceDark,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Subtotal",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Text(
                        text = "KSh ${String.format("%,.0f", subtotal)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "KSh ${String.format("%,.0f", total)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldBright
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryGreenButton(
                    text = if (cart.isEmpty()) "Select Products" else "Complete Sale (${cart.sumOf { it.quantity }})",
                    onClick = onProceedToPayment,
                    enabled = cart.isNotEmpty()
                )
            }
        }
    }
}

@Composable
fun PosProductItemRow(
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
        borderColor = if (quantityInCart > 0) EmeraldBright.copy(alpha = 0.5f) else SurfaceBorder,
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isOutOfStock) SurfaceDarkVariant.copy(alpha = 0.5f) else SurfaceDarkVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGroceryStore,
                        contentDescription = null,
                        tint = if (isOutOfStock) TextMuted else EmeraldBright,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isOutOfStock) TextSecondary else TextPrimary
                        )
                    )
                    val stockText = if (isOutOfStock) "Out of Stock" else "Stock: ${product.stockQuantity}"
                    val stockColor = if (isOutOfStock) ErrorRed else TextSecondary
                    Text(
                        text = "KSh ${String.format("%,.0f", product.sellingPrice)} • $stockText",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            color = stockColor
                        )
                    )
                }
            }

            // Quantity Stepper
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

                Spacer(modifier = Modifier.height(28.dp))

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

                Spacer(modifier = Modifier.height(32.dp))

                // Payment Methods
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(24.dp))

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
                            customerName = customerInput,
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
                        text = "Transaction ID: ${sale.id}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "KSh ${String.format("%,.0f", sale.totalAmount)}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldBright
                        )
                    )

                    Text(
                        text = "Paid via ${sale.paymentMethod}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryDarkButton(
                            text = "Print",
                            onClick = { },
                            icon = Icons.Default.Print,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryDarkButton(
                            text = "Share",
                            onClick = { },
                            icon = Icons.Default.Share,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                PrimaryGreenButton(
                    text = "New Sale",
                    onClick = onDismiss
                )
            }
        )
    }
}
