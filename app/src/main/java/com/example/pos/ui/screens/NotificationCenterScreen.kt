package com.example.pos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.pos.data.AppNotification
import com.example.pos.data.InventoryAlertKind
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.BgDark
import com.example.pos.ui.theme.EmeraldBright
import com.example.pos.ui.theme.EmeraldContainer
import com.example.pos.ui.theme.EmeraldPrimary
import com.example.pos.ui.theme.ErrorRed
import com.example.pos.ui.theme.SurfaceBorder
import com.example.pos.ui.theme.SurfaceDark
import com.example.pos.ui.theme.SurfaceDarkVariant
import com.example.pos.ui.theme.TextMuted
import com.example.pos.ui.theme.TextPrimary
import com.example.pos.ui.theme.TextSecondary
import com.example.pos.ui.theme.WarningYellow
import com.example.pos.viewmodel.PosViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun NotificationCenterScreen(
    viewModel: PosViewModel,
    onClose: () -> Unit,
    onViewInventory: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    val systemEnabled by viewModel.systemNotificationsEnabled.collectAsState()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        viewModel.setSystemNotificationsEnabled(granted)
    }

    val unreadCount = notifications.count { !it.isRead }
    val animatedCardBorder by animateColorAsState(
        targetValue = if (systemEnabled) EmeraldBright.copy(alpha = 0.5f) else SurfaceBorder,
        animationSpec = tween(300),
        label = "systemAlertCardBorder"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notifications",
                    color = TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (unreadCount > 0) "$unreadCount unread ${if (unreadCount == 1) "alert" else "alerts"}" else "Inventory alerts and preferences",
                    color = if (unreadCount > 0) EmeraldBright else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // Quick Actions: Test Alert, Mark All Read, Clear All
            IconButton(
                onClick = { viewModel.triggerTestNotification() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddAlert,
                    contentDescription = "Trigger test alert",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (unreadCount > 0) {
                IconButton(
                    onClick = { viewModel.markAllNotificationsRead() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Mark all read",
                        tint = EmeraldBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (notifications.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearNotifications() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear all",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // System Alerts Switch Card
        SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDark,
            borderColor = animatedCardBorder,
            cornerRadius = 18.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bell Icon Container
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (systemEnabled) EmeraldContainer else Color(0xFF263238))
                        .border(
                            width = 1.dp,
                            color = if (systemEnabled) EmeraldBright.copy(alpha = 0.35f) else Color(0xFF37474F),
                            shape = RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (systemEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (systemEnabled) EmeraldBright else Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "System alerts",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(6.dp))
                        // Status Badge Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (systemEnabled) EmeraldContainer else Color(0xFF334155).copy(alpha = 0.6f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (systemEnabled) "ACTIVE" else "OFF",
                                color = if (systemEnabled) EmeraldBright else Color(0xFFCBD5E1),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Show instant push alerts when products are low or out of stock.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Creative, Theme-Adaptive & High-Contrast Toggle Switch
                Switch(
                    checked = systemEnabled,
                    onCheckedChange = { enabled ->
                        permissionDenied = false
                        if (!enabled) {
                            viewModel.setSystemNotificationsEnabled(false)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel.setSystemNotificationsEnabled(true)
                            } else {
                                permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            viewModel.setSystemNotificationsEnabled(true)
                        }
                    },
                    thumbContent = {
                        Icon(
                            imageVector = if (systemEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (systemEnabled) EmeraldPrimary else Color(0xFF475569)
                        )
                    },
                    colors = SwitchDefaults.colors(
                        // When ON: Assumes the selected color theme!
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldBright,
                        checkedBorderColor = EmeraldBright,
                        checkedIconColor = EmeraldPrimary,
                        // When OFF: Crisp, bright, high-contrast & tactile! Never a dull invisible grey.
                        uncheckedThumbColor = Color(0xFFF8FAFC), // High-visibility bright silver-white thumb
                        uncheckedTrackColor = Color(0xFF1E293B), // Distinct deep slate track
                        uncheckedBorderColor = Color(0xFF64748B), // High-contrast border outline
                        uncheckedIconColor = Color(0xFF64748B)
                    )
                )
            }

            if (permissionDenied) {
                Text(
                    text = "Permission denied. Allow notifications in Android app settings to receive system alerts.",
                    color = WarningYellow,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Recent Alerts Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent alerts",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (notifications.isNotEmpty()) {
                Text(
                    text = "${notifications.size} total",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Alerts List or Empty State
        if (notifications.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SurfaceDarkVariant)
                        .border(1.dp, SurfaceBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "You’re all caught up",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Low-stock and out-of-stock alerts will appear here.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(18.dp))
                OutlinedButton(
                    onClick = { viewModel.triggerTestNotification() },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAlert,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EmeraldBright
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Trigger test alert",
                        color = EmeraldBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notifications, key = AppNotification::id) { notification ->
                    InventoryNotificationCard(
                        notification = notification,
                        onViewInventory = onViewInventory,
                        onMarkAsRead = { viewModel.markNotificationAsRead(notification.id) },
                        onDelete = { viewModel.deleteNotification(notification.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryNotificationCard(
    notification: AppNotification,
    onViewInventory: () -> Unit,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit
) {
    val outOfStock = notification.kind == InventoryAlertKind.OUT_OF_STOCK
    val accent = if (outOfStock) ErrorRed else WarningYellow

    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!notification.isRead) onMarkAsRead()
            },
        backgroundColor = SurfaceDark,
        borderColor = if (notification.isRead) SurfaceBorder else accent.copy(alpha = 0.6f),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (outOfStock) Icons.Default.Inventory2 else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = notification.title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldBright)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = notification.message,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(notification.createdAt)),
                        color = TextMuted,
                        fontSize = 10.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!notification.isRead) {
                            TextButton(
                                onClick = onMarkAsRead,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Mark read", color = TextMuted, fontSize = 11.sp)
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        TextButton(
                            onClick = onViewInventory,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Restock items", color = EmeraldBright, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
