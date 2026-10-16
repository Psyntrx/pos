package com.example.pos.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.CartItem
import com.example.pos.data.CreditLedgerUtils
import com.example.pos.data.CustomerCredit
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.SecondaryDarkButton
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun CreditSalesScreen(
    viewModel: PosViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val customerCredits by viewModel.customerCredits.collectAsState()
    val products by viewModel.products.collectAsState()
    val shop by viewModel.shop.collectAsState()
    val activeCredits = customerCredits.filter { it.balance > 0.0 && !it.status.equals("Settled", ignoreCase = true) }
    val pastCredits = customerCredits.filter { it.balance <= 0.0 || it.status.equals("Settled", ignoreCase = true) }
    val totalOutstanding = activeCredits.sumOf { it.balance }

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerNameError by remember { mutableStateOf<String?>(null) }
    var customerPhoneError by remember { mutableStateOf<String?>(null) }

    var selectedCreditProducts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var showProductPickerDialog by remember { mutableStateOf(false) }
    var productPickerSearch by remember { mutableStateOf("") }

    var amountPaid by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(viewModel.defaultCreditDueDate()) }
    var searchQuery by remember { mutableStateOf("") }
    var paymentTarget by remember { mutableStateOf<CustomerCredit?>(null) }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var paymentReferenceCode by remember { mutableStateOf("") }
    var paymentNotes by remember { mutableStateOf("") }
    var reminderTarget by remember { mutableStateOf<CustomerCredit?>(null) }
    var historyTarget by remember { mutableStateOf<CustomerCredit?>(null) }
    var selectedCreditDuration by remember { mutableStateOf(viewModel.getCreditDurationDays()) }
    var customDays by remember { mutableStateOf("14") }
    var initialPaymentMethod by remember { mutableStateOf("Cash") }
    var showCustomDuration by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val filteredActiveCredits = remember(activeCredits, searchQuery) {
        CreditLedgerUtils.filterActiveCredits(activeCredits, searchQuery)
    }
    val filteredPastCredits = remember(pastCredits, searchQuery) {
        CreditLedgerUtils.filterPastCredits(pastCredits, searchQuery)
    }
    val selectedProducts = products.filter { selectedCreditProducts.containsKey(it.id) }
    val originalAmount = CreditLedgerUtils.calculateSaleTotal(
        selectedProducts.map { product -> CartItem(product, selectedCreditProducts[product.id] ?: 0) }
    ) ?: 0.0
    val productsSummary = selectedProducts.joinToString { product ->
        "${product.name} x ${selectedCreditProducts[product.id]}"
    }
    val parsedAmountPaid = if (amountPaid.isBlank()) 0.0 else amountPaid.toDoubleOrNull() ?: Double.NaN
    val calculatedBalance = CreditLedgerUtils.calculateOutstandingBalance(
        originalAmount,
        parsedAmountPaid
    )

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Credit Sales",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Credit duration",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 14).forEach { days ->
                    FilterChip(
                        selected = selectedCreditDuration == days,
                        onClick = {
                            selectedCreditDuration = days
                            showCustomDuration = false
                            viewModel.setCreditDurationDays(days)
                            dueDate = viewModel.defaultCreditDueDate()
                        },
                        label = { Text("${days}d") }
                    )
                }
                listOf(30, 60).forEach { days ->
                    FilterChip(
                        selected = selectedCreditDuration == days,
                        onClick = {
                            selectedCreditDuration = days
                            showCustomDuration = false
                            viewModel.setCreditDurationDays(days)
                            dueDate = viewModel.defaultCreditDueDate()
                        },
                        label = { Text("${days}d") }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = showCustomDuration || selectedCreditDuration !in listOf(7, 14, 30, 60, -1),
                    onClick = {
                        showCustomDuration = true
                        selectedCreditDuration = 0
                    },
                    label = { Text("Custom days") }
                )
                FilterChip(
                    selected = selectedCreditDuration == -1,
                    onClick = {
                        selectedCreditDuration = -1
                        showCustomDuration = false
                        showDatePicker = true
                    },
                    label = { Text("Custom date") }
                )
            }
            if (showCustomDuration) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customDays,
                        onValueChange = { customDays = it.filter(Char::isDigit) },
                        label = { Text("Days") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Button(onClick = {
                        val days = customDays.toIntOrNull()?.takeIf { it > 0 }
                        if (days != null) {
                            selectedCreditDuration = days
                            showCustomDuration = false
                            viewModel.setCreditDurationDays(days)
                            dueDate = viewModel.defaultCreditDueDate()
                        }
                    }) {
                        Text("Apply")
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Total Outstanding Banner Card
            SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark,
                borderColor = SurfaceBorder,
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = EmeraldBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Total Outstanding",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                        Text(
                            text = "KSh ${String.format("%,.0f", totalOutstanding)}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form inputs
            OutlinedTextField(
                value = customerName,
                onValueChange = {
                    customerName = it
                    if (customerNameError != null) customerNameError = null
                },
                label = { Text("Customer *", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldBright) },
                isError = customerNameError != null,
                supportingText = {
                    if (customerNameError != null) {
                        Text(customerNameError!!, color = ErrorRed)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = TextStyle(color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = EmeraldBright
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = customerPhone,
                onValueChange = {
                    customerPhone = it
                    if (customerPhoneError != null) customerPhoneError = null
                },
                label = { Text("Customer Phone *", color = TextSecondary) },
                placeholder = { Text("e.g. 0712345678 or +254...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldBright) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = customerPhoneError != null,
                supportingText = {
                    if (customerPhoneError != null) {
                        Text(customerPhoneError!!, color = ErrorRed)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = TextStyle(color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = EmeraldBright
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Credit sale items", color = TextSecondary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(6.dp))
            SecondaryDarkButton(
                text = if (selectedProducts.isEmpty()) "Select Products from Catalog" else "Add / Adjust Products (${selectedCreditProducts.values.sum()})",
                onClick = {
                    productPickerSearch = ""
                    showProductPickerDialog = true
                },
                icon = Icons.Default.AddShoppingCart,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                containerColor = SurfaceDarkVariant,
                contentColor = TextPrimary,
                borderColor = SurfaceBorder,
                iconTint = EmeraldPrimary
            )

            if (selectedProducts.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No products selected. Tap above to pick products from catalog.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                    modifier = Modifier.align(Alignment.Start)
                )
            } else {
                selectedProducts.forEach { product ->
                    val quantity = selectedCreditProducts[product.id] ?: 0
                    val canIncrease = quantity < product.stockQuantity
                    SurfaceCard(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        backgroundColor = SurfaceDark,
                        borderColor = SurfaceBorder,
                        cornerRadius = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${quantity} × KSh ${String.format("%,.2f", product.sellingPrice)} = KSh ${String.format("%,.2f", product.sellingPrice * quantity)} (Stock: ${product.stockQuantity})",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = {
                                selectedCreditProducts = selectedCreditProducts.toMutableMap().apply {
                                    if (quantity <= 1) remove(product.id) else this[product.id] = quantity - 1
                                }
                                amountPaid = ""
                            }) {
                                Icon(if (quantity <= 1) Icons.Default.Delete else Icons.Default.Remove, contentDescription = "Decrease quantity", tint = WarningYellow)
                            }
                            Text(quantity.toString(), color = TextPrimary, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = {
                                    if (canIncrease) {
                                        selectedCreditProducts = selectedCreditProducts.toMutableMap().apply {
                                            this[product.id] = quantity + 1
                                        }
                                        amountPaid = ""
                                    }
                                },
                                enabled = canIncrease
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase quantity", tint = if (canIncrease) EmeraldBright else TextMuted)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = "KSh ${String.format("%,.2f", originalAmount)}",
                onValueChange = {},
                readOnly = true,
                label = { Text("Original sale total", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = TextStyle(color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = EmeraldBright
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountPaid,
                    onValueChange = { amountPaid = it },
                    label = { Text("Amount paid now (KSh)", color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDarkVariant,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = EmeraldBright,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = TextSecondary,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = EmeraldBright
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = calculatedBalance?.let { "KSh ${String.format("%,.2f", it)}" } ?: "Enter valid amounts",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Outstanding balance", color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(color = WarningYellow),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDarkVariant,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = EmeraldBright,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = WarningYellow,
                        unfocusedTextColor = WarningYellow,
                        focusedLabelColor = TextSecondary,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = WarningYellow
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Initial payment method", color = TextSecondary, modifier = Modifier.align(Alignment.Start))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Cash", "M-Pesa").forEach { method ->
                    FilterChip(
                        selected = initialPaymentMethod == method,
                        onClick = { initialPaymentMethod = method },
                        label = { Text(method) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = dueDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Due Date", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldBright) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = TextStyle(color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = EmeraldBright
                ),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Choose due date", tint = EmeraldBright)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryGreenButton(
                text = if (isSaving) "Saving..." else "Save Credit Sale",
                enabled = !isSaving,
                onClick = {
                    val trimmedName = customerName.trim()
                    var hasError = false
                    if (trimmedName.isBlank()) {
                        customerNameError = "Customer name is required"
                        hasError = true
                    }
                    val normalizedPhone = CreditLedgerUtils.normalizePhoneNumber(customerPhone)
                    if (normalizedPhone == null) {
                        customerPhoneError = "Valid phone number is required (e.g. 0712345678 or +254...)"
                        hasError = true
                    }
                    if (selectedProducts.isEmpty()) {
                        snackbarMessage = "Add at least one product."
                        hasError = true
                    }
                    if (hasError) return@PrimaryGreenButton

                    isSaving = true
                    val saved = viewModel.addCustomerCredit(
                        customerName = trimmedName,
                        phone = normalizedPhone!!,
                        productsSummary = productsSummary,
                        originalAmount = originalAmount,
                        amountPaid = parsedAmountPaid,
                        dueDate = dueDate,
                        paymentMethod = initialPaymentMethod
                    )
                    isSaving = false
                    if (saved) {
                        snackbarMessage = "Credit record saved successfully."
                        customerName = ""
                        customerPhone = ""
                        customerNameError = null
                        customerPhoneError = null
                        selectedCreditProducts = emptyMap()
                        amountPaid = ""
                        selectedCreditDuration = viewModel.getCreditDurationDays()
                        showCustomDuration = false
                        dueDate = viewModel.defaultCreditDueDate()
                    } else {
                        snackbarMessage = "Invalid credit entry. Please verify amounts and dates."
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Credit Ledger List
            Text(
                text = "Search customer or phone",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search customer or phone", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldBright) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Active Credits",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredActiveCredits.isEmpty()) {
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceDark,
                    borderColor = SurfaceBorder,
                    cornerRadius = 18.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active credit records match that search.",
                            color = TextSecondary
                        )
                    }
                }
            } else {
                filteredActiveCredits.forEach { c ->
                    val resolvedStatus = CreditLedgerUtils.resolveCreditStatus(c)
                    val dueLabel = if (c.dueDate.isNotBlank()) "Due: ${c.dueDate}" else "Due: Not set"
                    SurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { historyTarget = c },
                        backgroundColor = SurfaceDark,
                        borderColor = SurfaceBorder,
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = c.customerName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    if (c.customerPhone.isNotBlank()) {
                                        Text(
                                            text = c.customerPhone,
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                    }
                                    Text(
                                        text = resolvedStatus,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (resolvedStatus == "Overdue") ErrorRed else EmeraldBright
                                        )
                                    )
                                    Text(
                                        text = "Original: KSh ${String.format("%,.2f", c.totalOutstanding)}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                    Text(
                                        text = "Repaid: KSh ${String.format("%,.2f", c.amountPaid)}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                }

                                Text(
                                    text = "KSh ${String.format("%,.0f", c.balance)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WarningYellow
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dueLabel,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryDarkButton(
                                    text = "Record Payment",
                                    onClick = {
                                        paymentTarget = c
                                        paymentAmount = ""
                                        paymentMethod = "Cash"
                                        paymentReferenceCode = ""
                                        paymentNotes = ""
                                    },
                                    icon = Icons.Default.Payments,
                                    modifier = Modifier.height(36.dp),
                                    containerColor = SurfaceDarkVariant,
                                    contentColor = TextPrimary,
                                    borderColor = SurfaceBorder,
                                    iconTint = EmeraldPrimary
                                )
                                SecondaryDarkButton(
                                    text = "Send Reminder",
                                    onClick = { reminderTarget = c },
                                    icon = Icons.AutoMirrored.Filled.Send,
                                    modifier = Modifier.height(36.dp),
                                    containerColor = SurfaceDarkVariant,
                                    contentColor = TextPrimary,
                                    borderColor = SurfaceBorder,
                                    iconTint = EmeraldPrimary
                                )
                                SecondaryDarkButton(
                                    text = "View History",
                                    onClick = { historyTarget = c },
                                    icon = Icons.Default.History,
                                    modifier = Modifier.height(36.dp),
                                    containerColor = SurfaceDarkVariant,
                                    contentColor = TextPrimary,
                                    borderColor = SurfaceBorder,
                                    iconTint = EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Past Records",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredPastCredits.isEmpty()) {
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceDark,
                    borderColor = SurfaceBorder,
                    cornerRadius = 18.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No settled or past credit records match that search.",
                            color = TextSecondary
                        )
                    }
                }
            } else {
                filteredPastCredits.forEach { c ->
                    SurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { historyTarget = c },
                        backgroundColor = SurfaceDark,
                        borderColor = SurfaceBorder,
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = c.customerName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    if (c.customerPhone.isNotBlank()) {
                                        Text(
                                            text = c.customerPhone,
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                    }
                                    Text(
                                        text = c.status.ifBlank { "Settled" },
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                    )
                                    Text(
                                        text = "Original: KSh ${String.format("%,.2f", c.totalOutstanding)}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                    Text(
                                        text = "Repaid: KSh ${String.format("%,.2f", c.amountPaid)}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                }
                                Text(
                                    text = "KSh ${String.format("%,.2f", c.balance)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Settled: ${c.settledAt ?: c.dueDate}",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SecondaryDarkButton(
                                text = "View History",
                                onClick = { historyTarget = c },
                                icon = Icons.Default.History,
                                modifier = Modifier.height(36.dp),
                                containerColor = SurfaceDarkVariant,
                                contentColor = TextPrimary,
                                borderColor = SurfaceBorder,
                                iconTint = EmeraldPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (paymentTarget != null) {
            AlertDialog(
                onDismissRequest = { paymentTarget = null },
                containerColor = SurfaceDark,
                title = { Text("Record Payment", color = TextPrimary) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Customer: ${paymentTarget!!.customerName}", color = TextSecondary)
                        Text("Outstanding: KSh ${String.format("%,.0f", paymentTarget!!.balance)}", color = WarningYellow)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = paymentAmount,
                            onValueChange = { paymentAmount = it },
                            label = { Text("Amount received in KES") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Payment method", color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Cash", "M-Pesa").forEach { method ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { paymentMethod = method },
                                    label = { Text(method) }
                                )
                            }
                        }
                        if (paymentMethod == "M-Pesa") {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = paymentReferenceCode,
                                onValueChange = { paymentReferenceCode = it },
                                label = { Text("M-Pesa ref/code (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = paymentNotes,
                            onValueChange = { paymentNotes = it },
                            label = { Text("Notes (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = paymentAmount.toDoubleOrNull() ?: 0.0
                            val result = viewModel.recordRepayment(
                                paymentTarget!!.id,
                                amount,
                                paymentMethod,
                                paymentReferenceCode,
                                paymentNotes
                            )
                            paymentTarget = null
                            paymentAmount = ""
                            paymentReferenceCode = ""
                            paymentNotes = ""
                            snackbarMessage = result.getOrNull()?.let { "Payment recorded successfully." } ?: result.exceptionOrNull()?.message ?: "Payment failed."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldBright, contentColor = TextOnPrimary)
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { paymentTarget = null; paymentAmount = ""; paymentReferenceCode = ""; paymentNotes = "" }) { Text("Cancel", color = TextSecondary) }
                }
            )
        }

        if (historyTarget != null) {
            val target = historyTarget!!
            val historyRecords = remember(target) { CreditLedgerUtils.getDetailedPaymentHistory(target) }
            val resolvedStatus = remember(target) { CreditLedgerUtils.resolveCreditStatus(target) }
            val isSettled = resolvedStatus == "Settled"
            val isOverdue = resolvedStatus == "Overdue"
            val initialDeposit = remember(target) { CreditLedgerUtils.getInitialDeposit(target) }
            val subsequent = remember(target) { CreditLedgerUtils.getSubsequentRepayments(target) }

            AlertDialog(
                onDismissRequest = { historyTarget = null },
                containerColor = SurfaceDark,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Credit Details", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Surface(
                            color = when {
                                isSettled -> EmeraldContainer
                                isOverdue -> ErrorRed.copy(alpha = 0.2f)
                                else -> WarningYellow.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = resolvedStatus,
                                color = when {
                                    isSettled -> EmeraldBright
                                    isOverdue -> ErrorRed
                                    else -> WarningYellow
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = target.customerName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        if (target.customerPhone.isNotBlank()) {
                            Text(
                                text = target.customerPhone,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                        if (target.productsSummary.isNotBlank()) {
                            Text(
                                text = "Items: ${target.productsSummary}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = SurfaceDarkVariant,
                            borderColor = SurfaceBorder,
                            cornerRadius = 12.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Original Credit:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text("KSh ${String.format("%,.2f", target.totalOutstanding)}", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Credit Created:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text(CreditLedgerUtils.formatCreationTimestamp(target.createdAt), color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Initial Deposit:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        if (initialDeposit > 0.0) "KSh ${String.format("%,.2f", initialDeposit)}" else "None (KSh 0.00)",
                                        color = if (initialDeposit > 0.0) EmeraldBright else TextMuted,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (subsequent > 0.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Subsequent Repaid:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                        Text("KSh ${String.format("%,.2f", subsequent)}", color = EmeraldBright, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Repaid:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text("KSh ${String.format("%,.2f", target.amountPaid)}", color = EmeraldBright, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Current Balance:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "KSh ${String.format("%,.2f", target.balance)}",
                                        color = if (isSettled) EmeraldBright else WarningYellow,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Due Date:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text(target.dueDate.ifBlank { "Not set" }, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                }
                                if (isSettled && !target.settledAt.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Settlement Date:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                        Text(target.settledAt ?: target.dueDate, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        if (!isSettled && target.balance > 0.0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SecondaryDarkButton(
                                    text = "Record Payment",
                                    onClick = {
                                        val cur = target
                                        historyTarget = null
                                        paymentTarget = cur
                                        paymentAmount = ""
                                        paymentMethod = "Cash"
                                        paymentReferenceCode = ""
                                        paymentNotes = ""
                                    },
                                    icon = Icons.Default.Payments,
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    containerColor = SurfaceDarkVariant,
                                    contentColor = TextPrimary,
                                    borderColor = SurfaceBorder,
                                    iconTint = EmeraldPrimary
                                )
                                SecondaryDarkButton(
                                    text = "Send Reminder",
                                    onClick = {
                                        val cur = target
                                        historyTarget = null
                                        reminderTarget = cur
                                    },
                                    icon = Icons.AutoMirrored.Filled.Send,
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    containerColor = SurfaceDarkVariant,
                                    contentColor = TextPrimary,
                                    borderColor = SurfaceBorder,
                                    iconTint = EmeraldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Payment History (${historyRecords.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (historyRecords.isEmpty()) {
                            SurfaceCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = SurfaceDarkVariant,
                                borderColor = SurfaceBorder,
                                cornerRadius = 10.dp
                            ) {
                                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "No payments recorded yet.\nFull balance of KSh ${String.format("%,.2f", target.totalOutstanding)} is outstanding.",
                                        color = TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                historyRecords.forEach { record ->
                                    SurfaceCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        backgroundColor = SurfaceDarkVariant,
                                        borderColor = SurfaceBorder,
                                        cornerRadius = 10.dp
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = if (record.isInitialPayment) EmeraldContainer else SurfaceBorder,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = when {
                                                                record.isLegacySnapshot -> "Legacy Snapshot"
                                                                record.isInitialPayment -> "Deposit"
                                                                else -> "Repayment"
                                                            },
                                                            color = if (record.isInitialPayment) EmeraldBright else TextPrimary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = record.method,
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                                Text(
                                                    text = "KSh ${String.format("%,.2f", record.amount)}",
                                                    color = EmeraldBright,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = record.formattedDate,
                                                color = TextMuted,
                                                style = MaterialTheme.typography.labelSmall
                                            )

                                            if (record.referenceCode.isNotBlank() || record.notes.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val refText = if (record.referenceCode.isNotBlank()) "Ref: ${record.referenceCode}" else ""
                                                val noteText = if (record.notes.isNotBlank()) "Note: ${record.notes}" else ""
                                                val combined = listOf(refText, noteText).filter { it.isNotBlank() }.joinToString(" • ")
                                                Text(
                                                    text = combined,
                                                    color = TextSecondary,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }

                                            if (record.balanceAfterPayment >= 0.0) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Balance after payment: KSh ${String.format("%,.2f", record.balanceAfterPayment)}",
                                                    color = WarningYellow,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { historyTarget = null }) {
                        Text("Close", color = EmeraldBright)
                    }
                }
            )
        }

        if (reminderTarget != null) {
            val target = reminderTarget!!
            val clipboardManager = LocalClipboardManager.current
            var reminderMessageText by remember(target.id) {
                mutableStateOf(
                    CreditLedgerUtils.buildReminderMessage(
                        customerName = target.customerName,
                        balance = target.balance,
                        dueDate = target.dueDate,
                        shopName = shop.name
                    )
                )
            }
            val normalizedPhone = remember(target.customerPhone) {
                CreditLedgerUtils.normalizePhoneNumber(target.customerPhone) ?: target.customerPhone.trim()
            }
            val cleanDigits = normalizedPhone.replace("+", "").trim()
            val status = remember(target) { CreditLedgerUtils.resolveCreditStatus(target) }

            fun launchShareChooser() {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, reminderMessageText)
                    putExtra(Intent.EXTRA_SUBJECT, "Payment Reminder - ${shop.name.ifBlank { "POS" }}")
                    if (normalizedPhone.isNotBlank()) {
                        putExtra("address", normalizedPhone)
                        putExtra("android.intent.extra.PHONE_NUMBER", normalizedPhone)
                    }
                }
                val chooser = Intent.createChooser(sendIntent, "Send reminder")
                try {
                    context.startActivity(chooser)
                    reminderTarget = null
                } catch (e: Exception) {
                    clipboardManager.setText(AnnotatedString(reminderMessageText))
                    snackbarMessage = "No messaging application available. Reminder copied to clipboard."
                    reminderTarget = null
                }
            }

            fun launchWhatsApp() {
                if (cleanDigits.isBlank()) {
                    launchShareChooser()
                    return
                }
                val uri = Uri.parse("https://wa.me/$cleanDigits?text=${Uri.encode(reminderMessageText)}")
                val waIntent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    context.startActivity(waIntent)
                    reminderTarget = null
                } catch (e: Exception) {
                    launchShareChooser()
                }
            }

            fun launchSms() {
                if (normalizedPhone.isBlank()) {
                    launchShareChooser()
                    return
                }
                val smsUri = Uri.parse("smsto:${Uri.encode(normalizedPhone)}")
                val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                    putExtra("sms_body", reminderMessageText)
                }
                try {
                    context.startActivity(smsIntent)
                    reminderTarget = null
                } catch (e: Exception) {
                    launchShareChooser()
                }
            }

            AlertDialog(
                onDismissRequest = { reminderTarget = null },
                containerColor = SurfaceDark,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Send Reminder", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Surface(
                            color = if (status == "Overdue") ErrorRed.copy(alpha = 0.2f) else EmeraldContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = status,
                                color = if (status == "Overdue") ErrorRed else EmeraldBright,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = target.customerName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        if (target.customerPhone.isNotBlank()) {
                            Text(
                                text = target.customerPhone,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Balance: KSh ${String.format("%,.2f", target.balance)}",
                                color = WarningYellow,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (target.dueDate.isNotBlank()) {
                                Text("Due: ${target.dueDate}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Editable message:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = reminderMessageText,
                            onValueChange = { reminderMessageText = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = TextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDarkVariant,
                                unfocusedContainerColor = SurfaceDarkVariant,
                                focusedBorderColor = EmeraldBright,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Choose send option:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecondaryDarkButton(
                                text = "WhatsApp",
                                onClick = { launchWhatsApp() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                containerColor = SurfaceDarkVariant,
                                contentColor = EmeraldBright,
                                borderColor = EmeraldBright.copy(alpha = 0.5f)
                            )
                            SecondaryDarkButton(
                                text = "SMS",
                                onClick = { launchSms() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                containerColor = SurfaceDarkVariant,
                                contentColor = TextPrimary,
                                borderColor = SurfaceBorder
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecondaryDarkButton(
                                text = "Share Chooser",
                                onClick = { launchShareChooser() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                containerColor = SurfaceDarkVariant,
                                contentColor = TextPrimary,
                                borderColor = SurfaceBorder
                            )
                            SecondaryDarkButton(
                                text = "Copy Text",
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(reminderMessageText))
                                    snackbarMessage = "Reminder copied to clipboard."
                                    reminderTarget = null
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                containerColor = SurfaceDarkVariant,
                                contentColor = TextPrimary,
                                borderColor = SurfaceBorder
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { reminderTarget = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        if (showProductPickerDialog) {
            val pickerFilteredProducts = remember(products, productPickerSearch) {
                if (productPickerSearch.isBlank()) products
                else products.filter {
                    it.name.contains(productPickerSearch, ignoreCase = true) ||
                    it.sku.contains(productPickerSearch, ignoreCase = true) ||
                    it.category.contains(productPickerSearch, ignoreCase = true)
                }
            }

            AlertDialog(
                onDismissRequest = { showProductPickerDialog = false },
                containerColor = SurfaceDark,
                title = {
                    Text("Select Products", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = productPickerSearch,
                            onValueChange = { productPickerSearch = it },
                            placeholder = { Text("Search catalog...", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldBright) },
                            trailingIcon = {
                                if (productPickerSearch.isNotBlank()) {
                                    IconButton(onClick = { productPickerSearch = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = TextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDarkVariant,
                                unfocusedContainerColor = SurfaceDarkVariant,
                                focusedBorderColor = EmeraldBright,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (pickerFilteredProducts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No products match your search.", color = TextSecondary)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pickerFilteredProducts, key = { it.id }) { product ->
                                    val currentQty = selectedCreditProducts[product.id] ?: 0
                                    val isOutOfStock = product.stockQuantity <= 0
                                    val canAddMore = !isOutOfStock && currentQty < product.stockQuantity

                                    SurfaceCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = canAddMore) {
                                                selectedCreditProducts = selectedCreditProducts.toMutableMap().apply {
                                                    this[product.id] = currentQty + 1
                                                }
                                                amountPaid = ""
                                            },
                                        backgroundColor = SurfaceDarkVariant,
                                        borderColor = if (currentQty > 0) EmeraldBright.copy(alpha = 0.5f) else SurfaceBorder,
                                        cornerRadius = 10.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(product.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "KSh ${String.format("%,.0f", product.sellingPrice)} / ${product.unit}",
                                                    color = EmeraldBright,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                val stockText = if (isOutOfStock) "Out of Stock" else "Stock: ${product.stockQuantity}"
                                                val stockColor = if (isOutOfStock) ErrorRed else TextMuted
                                                Text(stockText, color = stockColor, style = MaterialTheme.typography.labelSmall)
                                            }

                                            if (currentQty > 0) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(SurfaceDark)
                                                            .clickable {
                                                                selectedCreditProducts = selectedCreditProducts.toMutableMap().apply {
                                                                    if (currentQty <= 1) remove(product.id)
                                                                    else this[product.id] = currentQty - 1
                                                                }
                                                                amountPaid = ""
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("-", color = EmeraldBright, fontWeight = FontWeight.Bold)
                                                    }
                                                    Text(
                                                        "$currentQty",
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(if (canAddMore) EmeraldContainer else SurfaceDark)
                                                            .clickable(enabled = canAddMore) {
                                                                selectedCreditProducts = selectedCreditProducts.toMutableMap().apply {
                                                                    this[product.id] = currentQty + 1
                                                                }
                                                                amountPaid = ""
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            "+",
                                                            color = if (canAddMore) EmeraldBright else TextMuted,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (canAddMore) EmeraldContainer else SurfaceDark)
                                                        .clickable(enabled = canAddMore) {
                                                            selectedCreditProducts = selectedCreditProducts.toMutableMap().apply {
                                                                this[product.id] = 1
                                                            }
                                                            amountPaid = ""
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "+ Add",
                                                        color = if (canAddMore) EmeraldBright else TextMuted,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    PrimaryGreenButton(
                        text = "Done (${selectedCreditProducts.values.sum()} added)",
                        onClick = { showProductPickerDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }

        if (showDatePicker) {
            val initialDate = CreditLedgerUtils.parseDueDate(dueDate) ?: LocalDate.now().plusDays(14)
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDate = CreditLedgerUtils.formatDueDate(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            )
                            selectedCreditDuration = -1
                            showCustomDuration = false
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Snackbar feedback
        if (snackbarMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = SurfaceDarkVariant,
                contentColor = TextPrimary,
                action = {
                    TextButton(onClick = { snackbarMessage = null }) {
                        Text("Dismiss", color = EmeraldBright)
                    }
                }
            ) {
                Text(snackbarMessage!!)
            }
        }
    }
}
