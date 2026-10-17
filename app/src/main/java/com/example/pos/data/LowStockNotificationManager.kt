package com.example.pos.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.pos.MainActivity
import com.example.pos.R

object LowStockNotificationManager {
    private const val OLD_CHANNEL_ID = "inventory_alerts"
    private const val CHANNEL_ID = "inventory_alerts_high_v2"
    private const val CHANNEL_NAME = "Inventory alerts"

    fun show(context: Context, notification: AppNotification) = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return@runCatching

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return@runCatching
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete legacy channel with IMPORTANCE_DEFAULT so Android doesn't retain old low priority
            runCatching { manager.deleteNotificationChannel(OLD_CHANNEL_ID) }

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when products are low on stock or sold out."
                enableLights(true)
                lightColor = 0xFF10B981.toInt()
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isOut = notification.kind == InventoryAlertKind.OUT_OF_STOCK
        val accentColor = if (isOut) 0xFFDC2626.toInt() else 0xFF10B981.toInt()

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(notification.title)
            .bigText(notification.message)
            .setSummaryText(if (isOut) "Out of Stock Alert" else "Low Stock Alert")

        val systemNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accentColor)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(bigTextStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setTicker(notification.title)
            .build()

        NotificationManagerCompat.from(context).notify(notification.id.hashCode(), systemNotification)
    }
}


