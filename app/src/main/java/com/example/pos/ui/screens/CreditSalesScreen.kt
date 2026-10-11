package com.example.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.SecondaryDarkButton
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel

@Composable
fun CreditSalesScreen(
    viewModel: PosViewModel,
    onClose: () -> Unit
) {

    // light and dark mode toggle for creditsales and expenses
    var isLightMode by remember { mutableStateOf(false) }

    // Temporary, screen-local theme preview.
    // Existing color references below can remain unchanged.
    val BgDark = if (isLightMode) com.example.pos.ui.theme.BgLight
    else com.example.pos.ui.theme.BgDark
    val SurfaceDark = if (isLightMode) com.example.pos.ui.theme.SurfaceLight
    else com.example.pos.ui.theme.SurfaceDark
    val SurfaceDarkVariant = if (isLightMode) com.example.pos.ui.theme.SurfaceLightVariant
    else com.example.pos.ui.theme.SurfaceDarkVariant
    val SurfaceBorder = if (isLightMode) com.example.pos.ui.theme.SurfaceLightBorder
    else com.example.pos.ui.theme.SurfaceBorder
    val TextPrimary = if (isLightMode) com.example.pos.ui.theme.TextLightPrimary
    else com.example.pos.ui.theme.TextPrimary
    val TextSecondary = if (isLightMode) com.example.pos.ui.theme.TextLightSecondary
    else com.example.pos.ui.theme.TextSecondary
    val TextMuted = if (isLightMode) com.example.pos.ui.theme.TextLightMuted
    else com.example.pos.ui.theme.TextMuted
    val EmeraldBright = if (isLightMode) com.example.pos.ui.theme.EmeraldLightBright
    else com.example.pos.ui.theme.EmeraldBright
    val EmeraldContainer = if (isLightMode) com.example.pos.ui.theme.EmeraldLightContainer
    else com.example.pos.ui.theme.EmeraldContainer

    // continuation of original code is below

    val customerCredits by viewModel.customerCredits.collectAsState()
    val totalOutstanding = customerCredits.sumOf { it.totalOutstanding }

    var customerName by remember { mutableStateOf("Samuel Njuguna") }
    var customerPhone by remember { mutableStateOf("+254 722 111 222") }
    var productsSummary by remember { mutableStateOf("2 Items | Unga, Cooking Oil") }
    var amountPaid by remember { mutableStateOf("500") }
    var dueDate by remember { mutableStateOf("Sep 30, 2026") }

    val calculatedBalance = (8460.0 - (amountPaid.toDoubleOrNull() ?: 500.0)).coerceAtLeast(0.0)

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
                // Button for dark and light mode
                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = { isLightMode = !isLightMode }
                ) {
                    Text(
                        text = if (isLightMode) "Dark ☾" else "Light ☀",
                        color = EmeraldBright
                    )
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
                            imageVector = Icons.Default.Home,
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

            key(isLightMode) {
                // Form inputs
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldBright) },
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
                    value = productsSummary,
                    onValueChange = { productsSummary = it },
                    label = { Text("Products", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = EmeraldBright) },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = amountPaid,
                        onValueChange = { amountPaid = it },
                        label = { Text("Amount Paid", color = TextSecondary) },
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
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = "KSh ${String.format("%,.0f", calculatedBalance)}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Balance", color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(color = if (isLightMode) WarningLightYellow else WarningYellow),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDarkVariant,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = EmeraldBright,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = if (isLightMode) WarningLightYellow else WarningYellow,
                            unfocusedTextColor = if (isLightMode) WarningLightYellow else WarningYellow,
                            focusedLabelColor = TextSecondary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = if (isLightMode) WarningLightYellow else WarningYellow
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
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
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryGreenButton(
                    text = "Save Credit Sale",
                    onClick = {
                        onClose()
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Credit Ledger List
                Text(
                    text = "Active Credit Ledger",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                customerCredits.forEach { c ->
                    SurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
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
                                Column {
                                    Text(
                                        text = c.customerName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = c.productsSummary,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                Text(
                                    text = "KSh ${String.format("%,.0f", c.balance)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLightMode) WarningLightYellow else WarningYellow
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Due: ${c.dueDate}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                )

                                SecondaryDarkButton(
                                    text = "Send Reminder",
                                    onClick = { },
                                    icon = Icons.Default.Send,
                                    modifier = Modifier.height(36.dp),
                                    containerColor = if (isLightMode) Color(0xFFF5F2EB) else SurfaceDarkVariant,
                                    contentColor = TextPrimary,
                                    borderColor = SurfaceBorder,
                                    iconTint = if (isLightMode) EmeraldLightBright else EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
