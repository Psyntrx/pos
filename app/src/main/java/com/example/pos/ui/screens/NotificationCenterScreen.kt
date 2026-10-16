package com.example.pos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.pos.data.AppNotification
import com.example.pos.data.InventoryAlertKind
import com.example.pos.data.Product
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.BgDark
import com.example.pos.ui.theme.EmeraldBright
import com.example.pos.ui.theme.EmeraldContainer
import com.example.pos.ui.theme.EmeraldPrimary
import com.example.pos.ui.theme.ErrorContainer
import com.example.pos.ui.theme.ErrorRed
import com.example.pos.ui.theme.SurfaceBorder
import com.example.pos.ui.theme.SurfaceDark
import com.example.pos.ui.theme.SurfaceDarkVariant
import com.example.pos.ui.theme.TextMuted
import com.example.pos.ui.theme.TextOnPrimary
import com.example.pos.ui.theme.TextPrimary
import com.example.pos.ui.theme.TextSecondary
import com.example.pos.ui.theme.WarningContainer
import com.example.pos.ui.theme.WarningYellow
import com.example.pos.viewmodel.PosViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun NotificationCenterScreen(
    viewModel: PosViewModel,
    onClose: () -> Unit,
    onViewInventory: () -> Unit,
    onOpenStockIn: (String?) -> Unit = {}
) {
    val notifications by viewModel.notifications.collectAsState()
    val products by viewModel.products.collectAsState()
    val systemEnabled by viewModel.systemNotificationsEnabled.collectAsState()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    var selectedNotification by remember { mutableStateOf<AppNotification?>(null) }

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
                    color = if (unreadCount > 0) EmeraldBright else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
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
                        uncheckedThumbColor = Color(0xFFF8FAFC),
                        uncheckedTrackColor = Color(0xFF1E293B),
                        uncheckedBorderColor = Color(0xFF64748B),
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
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notifications, key = AppNotification::id) { notification ->
                    InventoryNotificationCard(
                        notification = notification,
                        onClick = {
                            if (!notification.isRead) {
                                viewModel.markNotificationAsRead(notification.id)
                            }
                            selectedNotification = notification.copy(isRead = true)
                        },
                        onDelete = { viewModel.deleteNotification(notification.id) }
                    )
                }
            }
        }
    }

    // Single-Tap Notification Details Dialog (Opens the notification with full product context & restock action)
    selectedNotification?.let { currentNotification ->
        val affectedProducts = products.filter { it.id in currentNotification.productIds }
        NotificationDetailDialog(
            notification = currentNotification,
            products = affectedProducts,
            onDismiss = { selectedNotification = null },
            onMarkUnread = {
                viewModel.markNotificationAsUnread(currentNotification.id)
                selectedNotification = null
            },
            onDelete = {
                viewModel.deleteNotification(currentNotification.id)
                selectedNotification = null
            },
            onRestock = { productId ->
                selectedNotification = null
                onClose()
                if (productId != null) {
                    onOpenStockIn(productId)
                } else {
                    onViewInventory()
                }
            }
        )
    }
}

@Composable
private fun InventoryNotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isUnread = !notification.isRead
    val outOfStock = notification.kind == InventoryAlertKind.OUT_OF_STOCK
    val accent = if (outOfStock) ErrorRed else WarningYellow

    // Dynamic colors: Unread is bold and high-contrast, read is visibly greyed out!
    val titleColor = if (isUnread) TextPrimary else TextMuted
    val titleWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
    val messageColor = if (isUnread) TextSecondary else TextMuted.copy(alpha = 0.65f)
    val timeColor = if (isUnread) TextMuted else TextMuted.copy(alpha = 0.50f)
    val cardBorderColor = if (isUnread) accent.copy(alpha = 0.45f) else SurfaceBorder.copy(alpha = 0.40f)

    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = SurfaceDark,
        borderColor = cardBorderColor,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon Container (desaturated / greyed when read, vibrant when unread)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isUnread) accent.copy(alpha = 0.16f) else SurfaceDarkVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (outOfStock) Icons.Default.Inventory2 else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (isUnread) accent else TextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 2.dp)
            ) {
                // Header Row: Title + Unread Badge / Status + Dismiss Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = notification.title,
                        color = titleColor,
                        fontSize = 13.5.sp,
                        fontWeight = titleWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(EmeraldBright)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = TextOnPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Read",
                            tint = TextMuted.copy(alpha = 0.5f),
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete notification",
                            tint = TextMuted.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Message Text (greyed out when read)
                Text(
                    text = notification.message,
                    color = messageColor,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )

                // Footer Row: Formatted Timestamp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(notification.createdAt)),
                        color = timeColor,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationDetailDialog(
    notification: AppNotification,
    products: List<Product>,
    onDismiss: () -> Unit,
    onMarkUnread: () -> Unit,
    onDelete: () -> Unit,
    onRestock: (String?) -> Unit
) {
    val outOfStock = notification.kind == InventoryAlertKind.OUT_OF_STOCK
    val accent = if (outOfStock) ErrorRed else WarningYellow

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = SurfaceDark,
        tonalElevation = 6.dp,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (outOfStock) Icons.Default.Inventory2 else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accent.copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (outOfStock) "OUT OF STOCK" else "LOW STOCK ALERT",
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = notification.title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
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
                    text = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(Date(notification.createdAt)),
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = notification.message,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (products.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Affected Items (${products.size})",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    products.forEach { product ->
                        SurfaceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            backgroundColor = SurfaceDarkVariant,
                            borderColor = SurfaceBorder,
                            cornerRadius = 12.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${product.category} • SKU: ${product.sku}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${product.stockQuantity} ${product.unit}",
                                        color = if (product.stockQuantity <= 0) ErrorRed else WarningYellow,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Min: ${product.minStock} ${product.unit}",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { onRestock(product.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddShoppingCart,
                                        contentDescription = "Stock In",
                                        tint = EmeraldBright,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRestock(products.firstOrNull()?.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldBright,
                    contentColor = TextOnPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restock Items", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onMarkUnread) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Keep Unread", color = TextMuted, fontSize = 12.sp)
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = ErrorRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}
