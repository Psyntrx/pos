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
                items(filteredProducts) { product ->
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
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
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
                        text = "Stock: ${product.stockQuantity}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (product.isLowStock) WarningYellow else TextMuted
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
                            .background(EmeraldContainer)
                            .clickable { onAdd() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium.copy(color = EmeraldBright))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer)
                        .clickable { onAdd() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge.copy(color = EmeraldBright))
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

    // Standard customer field
    var customerInput by remember { mutableStateOf("") }

    // Credit payment fields
    var partialAmountInput by remember { mutableStateOf("") }
    var creditCustomerName by remember { mutableStateOf("") }
    var creditCustomerPhone by remember { mutableStateOf("") }
    var creditDueDate by remember { mutableStateOf("In 14 Days") }

    var validationError by remember { mutableStateOf<String?>(null) }

    val isCredit = selectedPaymentMethod == "Credit"
    val paidAmountNum = if (isCredit) (partialAmountInput.toDoubleOrNull() ?: 0.0) else totalAmount
    val remainingBalance = (totalAmount - paidAmountNum).coerceAtLeast(0.0)

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
                        text = "KSh ${String.format("%,.0f", if (totalAmount > 0) totalAmount else 1010.0)}",
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
                                    validationError = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == method,
                                onClick = {
                                    selectedPaymentMethod = method
                                    validationError = null
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = EmeraldBright,
                                    unselectedColor = TextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = method,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isCredit) {
                    // Credit Payment Specific Fields
                    SurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = SurfaceDark,
                        borderColor = SurfaceBorder,
                        cornerRadius = 20.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Credit Details",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldBright
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Partial Amount Field
                            OutlinedTextField(
                                value = partialAmountInput,
                                onValueChange = {
                                    partialAmountInput = it
                                    validationError = null
                                },
                                label = { Text("Partial Amount Paid (KSh)", color = TextSecondary) },
                                placeholder = { Text("0.00", color = TextMuted) },
                                leadingIcon = {
                                    Text(
                                        "KSh",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = EmeraldBright,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                                colors = com.example.pos.ui.components.posTextFieldColors(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Remaining Balance Display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Remaining Credit Balance:",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                                Text(
                                    text = "KSh ${String.format("%,.0f", remainingBalance)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WarningYellow
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Customer Name Field (Mandatory)
                            OutlinedTextField(
                                value = creditCustomerName,
                                onValueChange = {
                                    creditCustomerName = it
                                    validationError = null
                                },
                                label = { Text("Customer Name *", color = TextSecondary) },
                                placeholder = { Text("Mandatory for credit sales", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldBright) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                                colors = com.example.pos.ui.components.posTextFieldColors(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Customer Phone Field (Mandatory)
                            OutlinedTextField(
                                value = creditCustomerPhone,
                                onValueChange = {
                                    creditCustomerPhone = it
                                    validationError = null
                                },
                                label = { Text("Customer Phone Number *", color = TextSecondary) },
                                placeholder = { Text("Mandatory (e.g. +254 712 345 678)", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldBright) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                                colors = com.example.pos.ui.components.posTextFieldColors(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Due Date Field
                            OutlinedTextField(
                                value = creditDueDate,
                                onValueChange = { creditDueDate = it },
                                label = { Text("Payment Due Date", color = TextSecondary) },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldBright) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                                colors = com.example.pos.ui.components.posTextFieldColors(),
                                singleLine = true
                            )
                        }
                    }
                } else {
                    // Standard Customer (Optional) Input for Cash/M-Pesa
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

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = validationError!!,
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Complete Sale Button
            PrimaryGreenButton(
                text = "Complete Sale",
                onClick = {
                    validationError = null
                    if (isCredit) {
                        if (creditCustomerName.isBlank()) {
                            validationError = "Customer name is mandatory for credit sales."
                            return@PrimaryGreenButton
                        }
                        if (creditCustomerPhone.isBlank()) {
                            validationError = "Customer phone number is mandatory for credit sales."
                            return@PrimaryGreenButton
                        }
                        val partialAmount = partialAmountInput.toDoubleOrNull() ?: 0.0
                        if (partialAmount < 0 || partialAmount > totalAmount) {
                            validationError = "Partial amount must be between 0 and KSh ${String.format("%,.0f", totalAmount)}"
                            return@PrimaryGreenButton
                        }
                        viewModel.completeSale(
                            paymentMethod = "Credit",
                            customerName = creditCustomerName.trim(),
                            customerPhone = creditCustomerPhone.trim(),
                            isCredit = true,
                            paidAmount = partialAmount,
                            dueDate = creditDueDate.ifBlank { "In 14 Days" }
                        )
                    } else {
                        viewModel.completeSale(
                            paymentMethod = selectedPaymentMethod,
                            customerName = customerInput.trim(),
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
