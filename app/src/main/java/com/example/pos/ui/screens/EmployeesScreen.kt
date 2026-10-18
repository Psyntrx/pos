package com.example.pos.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.Employee
import com.example.pos.data.EmployeePermissions
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel

@Composable
fun EmployeesScreen(
    viewModel: PosViewModel,
    onClose: () -> Unit
) {
    val employeesRaw by viewModel.employees.collectAsState()
    val shop by viewModel.shop.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Deduplicate employees by email so the owner never sees duplicates
    val employees = remember(employeesRaw) {
        employeesRaw.distinctBy { it.id }.distinctBy { it.email.lowercase() }
    }

    var selectedEmployeeForManage by remember { mutableStateOf<Employee?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val activeCount = employees.count { !it.isOwner && it.status == "ACTIVE" }
    val pendingCount = employees.count { !it.isOwner && it.status == "PENDING" }

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
                Column {
                    Text(
                        text = "Employees & Permissions",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Manage staff access, revoke permissions & remove employees",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shop Code Invite Banner Card
            SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark,
                borderColor = SurfaceBorder,
                cornerRadius = 18.dp
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Staff Onboarding Shop Code", color = TextSecondary, fontSize = 11.sp)
                                Text(shop.id, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(shop.id))
                                Toast.makeText(context, "Shop Code copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldBright),
                            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(EmeraldBright.copy(alpha = 0.5f))
                            )
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Share this code with your employees. They install the POS app on their phones and choose 'Staff / Employee' during signup.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Stats Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StaffStatBadge("Total: ${employees.size}", SurfaceDarkVariant, TextPrimary)
                StaffStatBadge("Active: $activeCount", EmeraldContainer, EmeraldBright)
                if (pendingCount > 0) {
                    StaffStatBadge("Pending: $pendingCount", WarningYellow.copy(alpha = 0.15f), WarningYellow)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Employees List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(employees, key = { it.id }) { emp ->
                    EmployeeCardItem(
                        employee = emp,
                        onClick = {
                            if (!emp.isOwner) {
                                selectedEmployeeForManage = emp
                            }
                        }
                    )
                }
            }
        }

        // Add Employee FAB
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
            Icon(Icons.Default.Add, contentDescription = "Add Employee", modifier = Modifier.size(28.dp))
        }
    }

    // Manage Employee & Permissions Dialog / Sheet
    selectedEmployeeForManage?.let { employee ->
        ManageEmployeeDialog(
            employee = employee,
            onDismiss = { selectedEmployeeForManage = null },
            onSavePermissions = { updatedPerms ->
                viewModel.updateEmployeePermissions(employee.id, updatedPerms)
                selectedEmployeeForManage = null
                Toast.makeText(context, "Permissions updated for ${employee.name}", Toast.LENGTH_SHORT).show()
            },
            onToggleStatus = { newStatus ->
                viewModel.updateEmployeeStatus(employee.id, newStatus)
                selectedEmployeeForManage = null
                val msg = if (newStatus == "SUSPENDED") "Employee suspended" else "Employee activated"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onRevokeAll = {
                viewModel.revokeAllEmployeePermissions(employee.id)
                selectedEmployeeForManage = null
                Toast.makeText(context, "All permissions revoked for ${employee.name}", Toast.LENGTH_SHORT).show()
            },
            onRemoveEmployee = {
                viewModel.removeEmployee(employee.id)
                selectedEmployeeForManage = null
                Toast.makeText(context, "${employee.name} removed from shop", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add Employee Manual Dialog
    if (showAddDialog) {
        AddEmployeeManualDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, role, phone, email, perms ->
                viewModel.addEmployee(name, role, phone, email, perms)
                showAddDialog = false
                Toast.makeText(context, "Employee $name saved", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun StaffStatBadge(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun EmployeeCardItem(
    employee: Employee,
    onClick: () -> Unit
) {
    val statusColor = when (employee.status) {
        "ACTIVE" -> EmeraldBright
        "PENDING" -> WarningYellow
        "SUSPENDED" -> ErrorRed
        else -> TextMuted
    }
    val statusBg = statusColor.copy(alpha = 0.15f)

    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !employee.isOwner) { onClick() },
        backgroundColor = SurfaceDark,
        borderColor = if (employee.status == "SUSPENDED") ErrorRed.copy(alpha = 0.3f) else SurfaceBorder,
        cornerRadius = 18.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (employee.isOwner) EmeraldContainer else statusBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (employee.isOwner) Icons.Default.Shield else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (employee.isOwner) EmeraldBright else statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = employee.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = if (employee.isOwner) "Store Owner" else "${employee.role} • ${employee.phone.ifBlank { employee.email }}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (employee.isOwner) EmeraldContainer else statusBg)
                            .border(1.dp, if (employee.isOwner) EmeraldBright.copy(alpha = 0.3f) else statusColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (employee.isOwner) "OWNER" else employee.status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (employee.isOwner) EmeraldBright else statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    if (!employee.isOwner) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Manage",
                                color = EmeraldBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Permissions summary for employees
            if (!employee.isOwner) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                val perms = employee.permissions
                val permText = if (employee.status == "SUSPENDED") {
                    "Access Suspended (Permissions Inactive)"
                } else if (perms.isEmpty()) {
                    "No permissions assigned (Restricted access)"
                } else {
                    val names = mutableListOf<String>()
                    if (perms.contains(EmployeePermissions.PERM_RECORD_SALES)) names.add("Sales")
                    if (perms.contains(EmployeePermissions.PERM_STOCK_IN)) names.add("Stock In")
                    if (perms.contains(EmployeePermissions.PERM_NEW_PRODUCT)) names.add("New Products")
                    if (perms.contains(EmployeePermissions.PERM_CREDIT_SALES)) names.add("Credit")
                    if (perms.contains(EmployeePermissions.PERM_RECORD_EXPENSES)) names.add("Expenses")
                    "${perms.size} Granted: " + names.joinToString(", ")
                }

                Text(
                    text = permText,
                    color = if (employee.status == "SUSPENDED" || perms.isEmpty()) ErrorRed.copy(alpha = 0.8f) else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Dialog to view employee details, toggle status (active vs suspended),
 * edit or revoke granular permissions, and remove employee completely.
 */
@Composable
private fun ManageEmployeeDialog(
    employee: Employee,
    onDismiss: () -> Unit,
    onSavePermissions: (List<String>) -> Unit,
    onToggleStatus: (String) -> Unit,
    onRevokeAll: () -> Unit,
    onRemoveEmployee: () -> Unit
) {
    var selectedPerms by remember { mutableStateOf(employee.permissions.toSet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val permissionItems = listOf(
        Triple(
            EmployeePermissions.PERM_RECORD_SALES,
            "Record Sales",
            "Allow employee to ring up sales and process customer checkout"
        ),
        Triple(
            EmployeePermissions.PERM_STOCK_IN,
            "Stock In / Restock",
            "Allow recording inventory arrivals directly without owner approval"
        ),
        Triple(
            EmployeePermissions.PERM_NEW_PRODUCT,
            "Add New Products",
            "Allow creating brand new catalog items without owner approval"
        ),
        Triple(
            EmployeePermissions.PERM_CREDIT_SALES,
            "Issue Credit Sales",
            "Allow selling goods on customer credit/debt without owner approval"
        ),
        Triple(
            EmployeePermissions.PERM_RECORD_EXPENSES,
            "Record Expenses",
            "Allow logging shop operational expenses and disbursements"
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Manage Staff Member",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = employee.name,
                        style = MaterialTheme.typography.bodyMedium.copy(color = EmeraldBright, fontWeight = FontWeight.SemiBold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Info Summary Card
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceDarkVariant,
                    borderColor = SurfaceBorder,
                    cornerRadius = 14.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Email", color = TextSecondary, fontSize = 11.sp)
                            Text(employee.email.ifBlank { "N/A" }, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Phone", color = TextSecondary, fontSize = 11.sp)
                            Text(employee.phone.ifBlank { "N/A" }, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                employee.status,
                                color = if (employee.status == "ACTIVE") EmeraldBright else if (employee.status == "SUSPENDED") ErrorRed else WarningYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Suspend / Reactivate Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (employee.status == "SUSPENDED") "Account is Suspended" else "Account is Active",
                        color = if (employee.status == "SUSPENDED") ErrorRed else EmeraldBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedButton(
                        onClick = {
                            val nextStatus = if (employee.status == "SUSPENDED") "ACTIVE" else "SUSPENDED"
                            onToggleStatus(nextStatus)
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (employee.status == "SUSPENDED") EmeraldBright else WarningYellow
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (employee.status == "SUSPENDED") EmeraldBright.copy(alpha = 0.5f) else WarningYellow.copy(alpha = 0.5f)
                            )
                        )
                    ) {
                        Icon(
                            imageVector = if (employee.status == "SUSPENDED") Icons.Default.Check else Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (employee.status == "SUSPENDED") "Reactivate Staff" else "Suspend Staff",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Permissions Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assigned Permissions",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Row {
                        TextButton(
                            onClick = { selectedPerms = emptySet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Revoke All", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(
                            onClick = { selectedPerms = EmployeePermissions.ALL_PERMISSIONS.toSet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Select All", color = EmeraldBright, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Permissions Checkboxes
                permissionItems.forEach { (permKey, title, desc) ->
                    val isChecked = selectedPerms.contains(permKey)
                    SurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedPerms = if (isChecked) selectedPerms - permKey else selectedPerms + permKey
                            },
                        backgroundColor = if (isChecked) EmeraldContainer.copy(alpha = 0.35f) else SurfaceDarkVariant,
                        borderColor = if (isChecked) EmeraldBright.copy(alpha = 0.4f) else SurfaceBorder,
                        cornerRadius = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedPerms = if (checked) selectedPerms + permKey else selectedPerms - permKey
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = EmeraldBright,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = BgDark
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Danger Zone: Remove Employee
                Text("Danger Zone", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ErrorRed.copy(alpha = 0.5f))
                    )
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove Employee from Shop", color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            PrimaryGreenButton(
                text = "Save Permissions",
                onClick = { onSavePermissions(selectedPerms.toList()) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    )

    // Remove Confirmation Alert Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Remove Employee?",
                    style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${employee.name} from this shop? They will immediately lose all access to your shop data and will be logged out of their device.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 13.sp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onRemoveEmployee()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Remove Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * Manual dialog to add employee if owner does not want them to self-register via shop code.
 */
@Composable
private fun AddEmployeeManualDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, role: String, phone: String, email: String, permissions: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Cashier") }
    var selectedPerms by remember {
        mutableStateOf(
            setOf(
                EmployeePermissions.PERM_RECORD_SALES,
                EmployeePermissions.PERM_STOCK_IN
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Add Employee Manually", style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *", color = TextSecondary) },
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

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number", color = TextSecondary) },
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

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = TextSecondary) },
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

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role (e.g. Cashier, Sales Clerk)", color = TextSecondary) },
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

                Spacer(modifier = Modifier.height(14.dp))
                Text("Default Permissions", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                listOf(
                    EmployeePermissions.PERM_RECORD_SALES to "Record Sales",
                    EmployeePermissions.PERM_STOCK_IN to "Stock In / Restock",
                    EmployeePermissions.PERM_NEW_PRODUCT to "Add New Products",
                    EmployeePermissions.PERM_CREDIT_SALES to "Issue Credit Sales",
                    EmployeePermissions.PERM_RECORD_EXPENSES to "Record Expenses"
                ).forEach { (permKey, title) ->
                    val isChecked = selectedPerms.contains(permKey)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPerms = if (isChecked) selectedPerms - permKey else selectedPerms + permKey
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedPerms = if (checked) selectedPerms + permKey else selectedPerms - permKey
                            },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldBright, uncheckedColor = TextSecondary)
                        )
                        Text(title, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            PrimaryGreenButton(
                text = "Save Employee",
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(name, role, phone, email, selectedPerms.toList())
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
