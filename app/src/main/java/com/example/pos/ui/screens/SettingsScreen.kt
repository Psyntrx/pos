package com.example.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
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
import com.example.pos.ui.components.SecondaryDarkButton
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel

@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenEmployees: () -> Unit,
    onOpenCreditReminders: () -> Unit,
    onOpenExpenses: () -> Unit
) {
    val shop by viewModel.shop.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Owner Profile Card
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
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SurfaceDarkVariant)
                        .border(1.5.dp, EmeraldBright, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = EmeraldBright,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = shop.ownerName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Owner",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EmeraldBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = shop.email,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AppearanceSettingsCard(
            selectedMode = themeMode,
            onModeSelected = onThemeModeChange
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Settings Menu Items
        SettingsMenuItem(
            label = "Shop Information",
            icon = Icons.Default.Store,
            onClick = { }
        )

        SettingsMenuItem(
            label = "Employees & Permissions",
            icon = Icons.Default.People,
            onClick = onOpenEmployees
        )

        SettingsMenuItem(
            label = "Expenses Tracker",
            icon = Icons.Default.Receipt,
            onClick = onOpenExpenses
        )

        SettingsMenuItem(
            label = "Notifications",
            icon = Icons.Default.Notifications,
            onClick = { }
        )

        SettingsMenuItem(
            label = "Credit Reminders",
            icon = Icons.Default.Receipt,
            onClick = onOpenCreditReminders
        )

        SettingsMenuItem(
            label = "Help & Support",
            icon = Icons.Default.HelpOutline,
            onClick = { }
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Log Out Button
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ErrorContainer,
                contentColor = ErrorRed
            ),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed
                )
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun AppearanceSettingsCard(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder,
        cornerRadius = 20.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BrightnessAuto, contentDescription = null, tint = EmeraldBright)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Appearance", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Choose how RetailSync looks", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val isSelected = mode == selectedMode
                    val icon = when (mode) {
                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                        ThemeMode.DARK -> Icons.Default.DarkMode
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) EmeraldContainer else SurfaceDarkVariant)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) EmeraldBright.copy(alpha = 0.55f) else SurfaceBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) EmeraldBright else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = mode.label,
                            color = if (isSelected) EmeraldPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = when (selectedMode) {
                    ThemeMode.SYSTEM -> "Follows your phone's light or dark setting."
                    ThemeMode.LIGHT -> "Always use the bright, easy-to-read theme."
                    ThemeMode.DARK -> "Use a low-glare dark theme."
                },
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SettingsMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder,
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDarkVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = EmeraldBright,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
