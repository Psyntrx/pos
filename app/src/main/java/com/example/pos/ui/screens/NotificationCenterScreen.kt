package com.example.pos.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.example.pos.data.AppNotification
import com.example.pos.data.InventoryAlertKind
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.BgDark
import com.example.pos.ui.theme.EmeraldBright
import com.example.pos.ui.theme.EmeraldContainer
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

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark).padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Notifications", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Inventory alerts and preferences", color = TextSecondary, fontSize = 12.sp)
            }
            if (notifications.isNotEmpty()) {
                TextButton(onClick = viewModel::markAllNotificationsRead) {
                    Text("Mark read", color = EmeraldBright, fontSize = 12.sp)
                }
            }
        }

        SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDark,
            borderColor = SurfaceBorder,
            cornerRadius = 18.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = EmeraldBright)
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text("System inventory alerts", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Show alerts when stock becomes low or runs out.", color = TextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = systemEnabled,
                    onCheckedChange = { enabled ->
                        permissionDenied = false
                        if (!enabled) {
                            viewModel.setSystemNotificationsEnabled(false)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setSystemNotificationsEnabled(true)
                        }
                    }
                )
            }
            if (permissionDenied) {
                Text(
                    "Permission denied. Allow notifications in Android app settings to receive system alerts.",
                    color = WarningYellow,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Recent alerts", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (notifications.isNotEmpty()) {
                IconButton(onClick = viewModel::clearNotifications) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear alerts", tint = TextMuted)
                }
            }
        }

        if (notifications.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(bottom = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(SurfaceDarkVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = TextMuted, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("You’re all caught up", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Low-stock and out-of-stock alerts will appear here.", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(notifications, key = AppNotification::id) { notification ->
                    InventoryNotificationCard(notification = notification, onViewInventory = onViewInventory)
                }
            }
        }
    }
}

@Composable
private fun InventoryNotificationCard(
    notification: AppNotification,
    onViewInventory: () -> Unit
) {
    val outOfStock = notification.kind == InventoryAlertKind.OUT_OF_STOCK
    val accent = if (outOfStock) ErrorRed else WarningYellow
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = if (notification.isRead) SurfaceBorder else accent.copy(alpha = 0.5f),
        cornerRadius = 17.dp
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (outOfStock) Icons.Default.Inventory2 else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = accent
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notification.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (!notification.isRead) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(EmeraldBright))
                    }
                }
                Text(notification.message, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(notification.createdAt)),
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                TextButton(onClick = onViewInventory, modifier = Modifier.padding(start = 0.dp)) {
                    Text("View inventory", color = EmeraldBright, fontSize = 11.sp)
                }
            }
        }
    }
}
