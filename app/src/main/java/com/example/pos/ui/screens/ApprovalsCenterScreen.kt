package com.example.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.Employee
import com.example.pos.data.EmployeePermissions
import com.example.pos.data.Expense
import com.example.pos.data.Product
import com.example.pos.data.Sale
import com.example.pos.data.StockInRecord
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel

@Composable
fun ApprovalsCenterScreen(
    viewModel: PosViewModel,
    onClose: () -> Unit
) {
    val pendingEmployeesRaw by viewModel.pendingEmployees.collectAsState()
    val pendingEmployees = remember(pendingEmployeesRaw) {
        pendingEmployeesRaw.distinctBy { it.email.lowercase() }
    }
    val pendingStockIns by viewModel.pendingStockIns.collectAsState()
    val pendingNewProducts by viewModel.pendingNewProducts.collectAsState()
    val pendingExpenses by viewModel.pendingExpenses.collectAsState()
    val pendingCreditSales by viewModel.pendingCreditSales.collectAsState()

    val totalPending = pendingEmployees.size + pendingStockIns.size +
            pendingNewProducts.size + pendingExpenses.size + pendingCreditSales.size

    var selectedFilter by remember { mutableStateOf("ALL") }

    LaunchedEffect(Unit) {
        viewModel.syncWithSupabase(force = true)
    }

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
                    text = "Pending Approvals",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                if (totalPending > 0) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(WarningYellow)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = totalPending.toString(),
                            color = BgDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.syncWithSupabase(force = true) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EmeraldBright)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTabChip("All ($totalPending)", selectedFilter == "ALL") { selectedFilter = "ALL" }
                FilterTabChip("Employees (${pendingEmployees.size})", selectedFilter == "EMPLOYEES") { selectedFilter = "EMPLOYEES" }
                FilterTabChip("Restock (${pendingStockIns.size})", selectedFilter == "STOCK_IN") { selectedFilter = "STOCK_IN" }
                FilterTabChip("Products (${pendingNewProducts.size})", selectedFilter == "PRODUCTS") { selectedFilter = "PRODUCTS" }
                FilterTabChip("Credit Sales (${pendingCreditSales.size})", selectedFilter == "CREDIT") { selectedFilter = "CREDIT" }
                FilterTabChip("Expenses (${pendingExpenses.size})", selectedFilter == "EXPENSES") { selectedFilter = "EXPENSES" }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (totalPending == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldBright,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All Caught Up!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No pending employee or inventory approvals.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Pending Employees
                    if (selectedFilter in listOf("ALL", "EMPLOYEES")) {
                        items(pendingEmployees, key = { it.id }) { emp ->
                            PendingEmployeeCard(
                                employee = emp,
                                onApprove = { perms ->
                                    viewModel.approveEmployee(emp.id, perms)
                                },
                                onReject = {
                                    viewModel.rejectEmployee(emp.id)
                                }
                            )
                        }
                    }

                    // Pending Stock Ins
                    if (selectedFilter in listOf("ALL", "STOCK_IN")) {
                        items(pendingStockIns, key = { it.id }) { stk ->
                            PendingStockInCard(
                                record = stk,
                                onApprove = { viewModel.approveStockIn(stk) },
                                onReject = { viewModel.rejectStockIn(stk.id) }
                            )
                        }
                    }

                    // Pending New Products
                    if (selectedFilter in listOf("ALL", "PRODUCTS")) {
                        items(pendingNewProducts, key = { it.id }) { prod ->
                            PendingProductCard(
                                product = prod,
                                onApprove = { viewModel.approveNewProduct(prod) },
                                onReject = { viewModel.rejectNewProduct(prod.id) }
                            )
                        }
                    }

                    // Pending Credit Sales
                    if (selectedFilter in listOf("ALL", "CREDIT")) {
                        items(pendingCreditSales, key = { it.id }) { sale ->
                            PendingCreditSaleCard(
                                sale = sale,
                                onApprove = { viewModel.approveCreditSale(sale) },
                                onReject = { viewModel.rejectCreditSale(sale.id) }
                            )
                        }
                    }

                    // Pending Expenses
                    if (selectedFilter in listOf("ALL", "EXPENSES")) {
                        items(pendingExpenses, key = { it.id }) { exp ->
                            PendingExpenseCard(
                                expense = exp,
                                onApprove = { viewModel.approveExpense(exp) },
                                onReject = { viewModel.rejectExpense(exp.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) EmeraldBright else SurfaceDarkVariant)
            .border(
                1.dp,
                if (isSelected) EmeraldBright else SurfaceBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) BgDark else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PendingEmployeeCard(
    employee: Employee,
    onApprove: (List<String>) -> Unit,
    onReject: () -> Unit
) {
    var permSales by remember { mutableStateOf(true) }
    var permStockIn by remember { mutableStateOf(true) }
    var permNewProduct by remember { mutableStateOf(false) }
    var permCredit by remember { mutableStateOf(false) }
    var permExpenses by remember { mutableStateOf(false) }

    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = WarningYellow.copy(alpha = 0.5f),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(WarningYellow.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("New Employee Signup", color = WarningYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(employee.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Phone: ${employee.phone.ifBlank { "N/A" }} • Email: ${employee.email.ifBlank { "N/A" }}", color = TextSecondary, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SurfaceBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Text("Assign Permissions:", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))

            PermissionCheckbox("Record Sales (POS)", permSales) { permSales = it }
            PermissionCheckbox("Stock In / Restock", permStockIn) { permStockIn = it }
            PermissionCheckbox("Add New Products", permNewProduct) { permNewProduct = it }
            PermissionCheckbox("Issue Credit Sales", permCredit) { permCredit = it }
            PermissionCheckbox("Record Expenses", permExpenses) { permExpenses = it }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ErrorRed.copy(alpha = 0.5f))
                    )
                ) {
                    Text("Decline", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }

                PrimaryGreenButton(
                    text = "Approve",
                    onClick = {
                        val perms = mutableListOf<String>()
                        if (permSales) perms.add(EmployeePermissions.PERM_RECORD_SALES)
                        if (permStockIn) perms.add(EmployeePermissions.PERM_STOCK_IN)
                        if (permNewProduct) perms.add(EmployeePermissions.PERM_NEW_PRODUCT)
                        if (permCredit) perms.add(EmployeePermissions.PERM_CREDIT_SALES)
                        if (permExpenses) perms.add(EmployeePermissions.PERM_RECORD_EXPENSES)
                        onApprove(perms)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = EmeraldBright,
                checkmarkColor = BgDark,
                uncheckedColor = TextMuted
            ),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = TextPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun PendingStockInCard(
    record: StockInRecord,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = WarningYellow.copy(alpha = 0.5f),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddBox, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Stock In Verification", color = WarningYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(record.productName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Quantity Added", color = TextSecondary, fontSize = 11.sp)
                    Text("+${record.quantityAdded}", color = EmeraldBright, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column {
                    Text("Buying / Cost Price", color = TextSecondary, fontSize = 11.sp)
                    Text("KSh ${"%,.0f".format(record.buyingPrice)}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column {
                    Text("Requested By", color = TextSecondary, fontSize = 11.sp)
                    Text(record.employeeName, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Reject", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }

                PrimaryGreenButton(
                    text = "Approve & Add Stock",
                    onClick = onApprove,
                    modifier = Modifier.weight(1.3f).height(44.dp)
                )
            }
        }
    }
}

@Composable
private fun PendingProductCard(
    product: Product,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val requesterName = if (product.supplier.contains("requester:")) {
        product.supplier.substringAfter("requester:").substringBefore("||")
    } else "Employee"

    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = WarningYellow.copy(alpha = 0.5f),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(WarningYellow.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("New Product Request", color = WarningYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(product.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Requested by $requesterName", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Category", color = TextSecondary, fontSize = 11.sp)
                    Text(product.category, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                Column {
                    Text("Initial Stock", color = TextSecondary, fontSize = 11.sp)
                    Text("${product.stockQuantity} ${product.unit}", color = EmeraldBright, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column {
                    Text("Cost Price", color = TextSecondary, fontSize = 11.sp)
                    Text("KSh ${"%,.0f".format(product.buyingPrice)}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column {
                    Text("Selling Price", color = TextSecondary, fontSize = 11.sp)
                    Text("KSh ${"%,.0f".format(product.sellingPrice)}", color = EmeraldBright, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Reject", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }

                PrimaryGreenButton(
                    text = "Approve to Catalog",
                    onClick = onApprove,
                    modifier = Modifier.weight(1.3f).height(44.dp)
                )
            }
        }
    }
}

@Composable
private fun PendingCreditSaleCard(
    sale: Sale,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val requesterName = if (sale.paymentMethod.contains("requester:")) {
        sale.paymentMethod.substringAfter("requester:").substringBefore("||")
    } else sale.employeeName.ifBlank { "Employee" }

    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = WarningYellow.copy(alpha = 0.5f),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(WarningYellow.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Credit Sale Request", color = WarningYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(sale.customerName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Requested by $requesterName • Due: ${sale.dueDate.ifBlank { "N/A" }}", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Sale", color = TextSecondary, fontSize = 11.sp)
                    Text("KSh ${"%,.0f".format(sale.totalAmount)}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column {
                    Text("Deposit Paid", color = TextSecondary, fontSize = 11.sp)
                    Text("KSh ${"%,.0f".format(sale.paidAmount)}", color = EmeraldBright, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column {
                    Text("Balance Due", color = TextSecondary, fontSize = 11.sp)
                    Text("KSh ${"%,.0f".format(sale.balance)}", color = WarningYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Reject", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }

                PrimaryGreenButton(
                    text = "Approve Credit Sale",
                    onClick = onApprove,
                    modifier = Modifier.weight(1.3f).height(44.dp)
                )
            }
        }
    }
}

@Composable
private fun PendingExpenseCard(
    expense: Expense,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val displayCategory = expense.category.removePrefix("PENDING_APPROVAL||")
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = WarningYellow.copy(alpha = 0.5f),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(WarningYellow.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Expense Request", color = WarningYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(expense.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Category", color = TextSecondary, fontSize = 11.sp)
                    Text(displayCategory, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                Column {
                    Text("Amount", color = TextSecondary, fontSize = 11.sp)
                    Text("KSh ${"%,.0f".format(expense.amount)}", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column {
                    Text("Date", color = TextSecondary, fontSize = 11.sp)
                    Text(expense.dateFormatted, color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Reject", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }

                PrimaryGreenButton(
                    text = "Approve Expense",
                    onClick = onApprove,
                    modifier = Modifier.weight(1.3f).height(44.dp)
                )
            }
        }
    }
}
