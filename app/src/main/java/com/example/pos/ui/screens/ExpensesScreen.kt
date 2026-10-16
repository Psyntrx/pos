package com.example.pos.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.CreditLedgerUtils
import com.example.pos.data.Expense
import com.example.pos.ui.components.FilterChipItem
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: PosViewModel,
    onClose: () -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    var filterPeriod by remember { mutableStateOf("All") }
    val filteredExpenses = remember(expenses, filterPeriod) {
        CreditLedgerUtils.filterExpenses(expenses, filterPeriod)
    }
    var showAddDialog by remember { mutableStateOf(false) }

    // Selection mode state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedExpenseIds by remember { mutableStateOf(setOf<String>()) }

    // Single-tap details / options dialog state
    var clickedExpenseForDetails by remember { mutableStateOf<Expense?>(null) }

    // Delete confirmation dialog state
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Placeholder action message state (for edit/delete feedback)
    var placeholderMessage by remember { mutableStateOf<String?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

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
            // Header (Dynamic based on selection mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    IconButton(onClick = {
                        isSelectionMode = false
                        selectedExpenseIds = emptySet()
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel Selection",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedExpenseIds.size} selected",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    // Select All
                    IconButton(onClick = {
                        selectedExpenseIds = expenses.map { it.id }.toSet()
                    }) {
                        Icon(
                            Icons.Default.SelectAll,
                            contentDescription = "Select All",
                            tint = TextPrimary
                        )
                    }

                    // Delete Action
                    if (selectedExpenseIds.isNotEmpty()) {
                        IconButton(onClick = {
                            showDeleteConfirmation = true
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = ErrorRed
                            )
                        }
                    }
                } else {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Expenses",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val periods = listOf("All", "This Week", "This Month")
                items(periods) { period ->
                    FilterChipItem(
                        label = period,
                        isSelected = filterPeriod == period,
                        onClick = { filterPeriod = period },
                        backgroundColor = SurfaceDarkVariant,
                        borderColor = SurfaceBorder,
                        selectedBackgroundColor = EmeraldBright,
                        selectedTextColor = BgDark,
                        unselectedTextColor = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expenses List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (filteredExpenses.isEmpty()) {
                    item {
                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = SurfaceDark,
                            borderColor = SurfaceBorder,
                            cornerRadius = 18.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No expenses found for this period.",
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(filteredExpenses) { exp ->
                        val isSelected = selectedExpenseIds.contains(exp.id)
                        SurfaceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedExpenseIds = if (isSelected) {
                                                selectedExpenseIds - exp.id
                                            } else {
                                                selectedExpenseIds + exp.id
                                            }
                                            if (selectedExpenseIds.isEmpty()) {
                                                isSelectionMode = false
                                            }
                                        } else {
                                            clickedExpenseForDetails = exp
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                        }
                                        selectedExpenseIds = if (isSelected) {
                                            selectedExpenseIds - exp.id
                                        } else {
                                            selectedExpenseIds + exp.id
                                        }
                                        if (selectedExpenseIds.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    }
                                ),
                            backgroundColor = if (isSelected) EmeraldContainer.copy(alpha = 0.4f) else SurfaceDark,
                            borderColor = if (isSelected) EmeraldBright else SurfaceBorder,
                            cornerRadius = 18.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelectionMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedExpenseIds = if (checked) {
                                                    selectedExpenseIds + exp.id
                                                } else {
                                                    selectedExpenseIds - exp.id
                                                }
                                                if (selectedExpenseIds.isEmpty()) {
                                                    isSelectionMode = false
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = EmeraldBright,
                                                uncheckedColor = TextSecondary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(EmeraldContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(exp.iconType),
                                            contentDescription = null,
                                            tint = EmeraldBright,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = exp.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 15.sp
                                            )
                                        )
                                        Text(
                                            text = exp.dateFormatted,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = "KSh ${String.format("%,.0f", exp.amount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Add Expense FAB (hidden when in selection mode)
        if (!isSelectionMode) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldBright,
                contentColor = TextOnPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 20.dp)
                    .size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Expense",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Add Expense Dialog with Category Selector
        if (showAddDialog) {
            var title by remember { mutableStateOf("") }
            var amount by remember { mutableStateOf("") }
            var category by remember { mutableStateOf("Rent") }
            var categoryMenuExpanded by remember { mutableStateOf(false) }
            val categories = listOf("Rent", "Electricity", "Transport", "Salaries", "Internet", "Repairs", "Food", "Healthcare", "Education", "Water", "Shopping", "Utility", "Other")

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = SurfaceDark,
                title = {
                    Text(
                        text = "Add Expense",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary)
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title e.g. Office Rent", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDarkVariant,
                                unfocusedContainerColor = SurfaceDark,
                                focusedBorderColor = EmeraldBright,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = categoryMenuExpanded,
                            onExpandedChange = { categoryMenuExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category", color = TextSecondary) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceDarkVariant,
                                    unfocusedContainerColor = SurfaceDark,
                                    focusedBorderColor = EmeraldBright,
                                    unfocusedBorderColor = SurfaceBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                categories.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = TextPrimary) },
                                        onClick = {
                                            category = option
                                            categoryMenuExpanded = false
                                        },
                                        leadingIcon = if (category.equals(option, true)) {
                                            { Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldBright) }
                                        } else null
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount (KSh)", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDarkVariant,
                                unfocusedContainerColor = SurfaceDark,
                                focusedBorderColor = EmeraldBright,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    PrimaryGreenButton(
                        text = "Save Expense",
                        onClick = {
                            val wasSaved = viewModel.addExpense(
                                title,
                                category,
                                amount.toDoubleOrNull() ?: 0.0
                            )
                            if (wasSaved) {
                                showAddDialog = false
                            }
                        }
                    )
                }
            )
        }

        if (editingExpense != null) {
            var editTitle by remember { mutableStateOf(editingExpense!!.title) }
            var editAmount by remember { mutableStateOf(editingExpense!!.amount.toString()) }
            var editCategory by remember { mutableStateOf(editingExpense!!.category) }
            var editCategoryMenuExpanded by remember { mutableStateOf(false) }
            val categories = listOf("Rent", "Electricity", "Transport", "Salaries", "Internet", "Repairs", "Food", "Healthcare", "Education", "Water", "Shopping", "Utility", "Other")
            AlertDialog(
                onDismissRequest = { editingExpense = null },
                containerColor = SurfaceDark,
                title = { Text("Edit Expense", color = TextPrimary) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(modifier = Modifier.height(12.dp))
                        ExposedDropdownMenuBox(expanded = editCategoryMenuExpanded, onExpandedChange = { editCategoryMenuExpanded = it }) {
                            OutlinedTextField(value = editCategory, onValueChange = {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editCategoryMenuExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable))
                            ExposedDropdownMenu(expanded = editCategoryMenuExpanded, onDismissRequest = { editCategoryMenuExpanded = false }) {
                                categories.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { editCategory = option; editCategoryMenuExpanded = false })
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = editAmount, onValueChange = { editAmount = it }, label = { Text("Amount (KSh)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                },
                confirmButton = {
                    PrimaryGreenButton(text = "Save", onClick = {
                        val updated = viewModel.updateExpense(editingExpense!!.id, editTitle, editCategory, editAmount.toDoubleOrNull() ?: 0.0)
                        if (updated) {
                            editingExpense = null
                            placeholderMessage = "Expense updated."
                        }
                    })
                },
                dismissButton = {
                    TextButton(onClick = { editingExpense = null }) { Text("Cancel", color = TextSecondary) }
                }
            )
        }

        // Single-tap Expense Details & Options Dialog
        if (clickedExpenseForDetails != null) {
            val exp = clickedExpenseForDetails!!
            AlertDialog(
                onDismissRequest = { clickedExpenseForDetails = null },
                containerColor = SurfaceDark,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(exp.iconType),
                                contentDescription = null,
                                tint = EmeraldBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = exp.title,
                            style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary)
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Amount: KSh ${String.format("%,.0f", exp.amount)}",
                            style = MaterialTheme.typography.titleMedium.copy(color = EmeraldBright, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Category: ${exp.category}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Date: ${exp.dateFormatted}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clickedExpenseForDetails = null
                                editingExpense = exp
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Edit")
                        }
                        Button(
                            onClick = {
                                clickedExpenseForDetails = null
                                viewModel.deleteExpenses(setOf(exp.id))
                                placeholderMessage = "Expense deleted successfully."
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = TextOnPrimary)
                        ) {
                            Text("Delete")
                        }
                    }
                }
            )
        }

        // Delete Confirmation Dialog (for selection mode delete)
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                containerColor = SurfaceDark,
                title = {
                    Text(
                        text = "Delete Expenses",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary)
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete ${selectedExpenseIds.size} selected expense(s)? (Placeholder action)",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteExpenses(selectedExpenseIds)
                            showDeleteConfirmation = false
                            placeholderMessage = "Deleted ${selectedExpenseIds.size} expense(s)."
                            isSelectionMode = false
                            selectedExpenseIds = emptySet()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = TextOnPrimary)
                    ) {
                        Text("Confirm Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Snackbar placeholder message
        if (placeholderMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = SurfaceDarkVariant,
                contentColor = TextPrimary,
                action = {
                    TextButton(onClick = { placeholderMessage = null }) {
                        Text("Dismiss", color = EmeraldBright)
                    }
                }
            ) {
                Text(placeholderMessage!!)
            }
        }
    }
}

fun getCategoryIcon(iconType: String): ImageVector {
    return when (iconType.lowercase().trim()) {
        "rent", "housing" -> Icons.Default.Home
        "electricity", "power" -> Icons.Default.Lightbulb
        "transport", "logistics", "travel" -> Icons.Default.DirectionsCar
        "salaries", "payroll", "wage" -> Icons.Default.AccountBalanceWallet
        "wifi", "internet" -> Icons.Default.Wifi
        "repairs", "maintenance", "fix" -> Icons.Default.Build
        "food", "groceries", "dining" -> Icons.Default.Restaurant
        "healthcare", "health", "medical" -> Icons.Default.Favorite
        "education", "school", "books" -> Icons.Default.School
        "water", "utility_water" -> Icons.Default.WaterDrop
        "shopping", "retail" -> Icons.Default.ShoppingBag
        "utility" -> Icons.Default.ElectricalServices
        else -> Icons.Default.Receipt
    }
}
