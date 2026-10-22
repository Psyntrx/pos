package com.example.pos.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.pos.R

object ReminderIntentHelper {

    private const val WHATSAPP_CHANNEL_ID = "whatsapp_reminders_v1"
    private const val WHATSAPP_CHANNEL_NAME = "WhatsApp Reminder Queue"

    // -------------------------------------------------------------------------
    // 1. Silent SMS Draft Insertion
    // -------------------------------------------------------------------------

    /**
     * Inserts an SMS draft silently into the device SMS content provider
     * without launching the SMS composer screen.
     *
     * Shows a Toast confirming the draft was created.
     * Returns true on success, false on failure.
     */
    fun createSmsDraft(context: Context, phoneNumber: String, messageText: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, phoneNumber)
                put(Telephony.Sms.BODY, messageText)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_DRAFT)
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
            }
            val uri = context.contentResolver.insert(Uri.parse("content://sms/draft"), values)
            if (uri != null) {
                Toast.makeText(
                    context,
                    "SMS draft created for $phoneNumber",
                    Toast.LENGTH_SHORT
                ).show()
                true
            } else {
                Toast.makeText(
                    context,
                    "Could not create SMS draft for $phoneNumber",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }
        } catch (e: Exception) {
            // Fallback toast on SecurityException or provider error
            Toast.makeText(
                context,
                "SMS draft unavailable. Check SMS permissions.",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    // -------------------------------------------------------------------------
    // 2. Non-Intrusive WhatsApp Notification with PendingIntent
    // -------------------------------------------------------------------------

    /**
     * Posts a local Android notification for a scheduled WhatsApp reminder.
     * Tapping the notification or the "Send Now" action button launches WhatsApp
     * pre-filled with the customer's message — without interrupting the user immediately.
     *
     * @param notificationId Stable ID so each customer gets a distinct notification.
     * @param customerName   Displayed in the notification title.
     * @param phoneNumber    E.164 or local number (digits only, no leading +).
     * @param messageText    Pre-filled WhatsApp message body.
     */
    fun postWhatsAppReminderNotification(
        context: Context,
        notificationId: Int,
        customerName: String,
        phoneNumber: String,
        messageText: String
    ) {
        // Permission check (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Ensure channel exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WHATSAPP_CHANNEL_ID,
                WHATSAPP_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Queued WhatsApp payment reminders — tap to send."
                enableVibration(false)
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }

        // Build the WhatsApp deep-link intent
        val waUri = Uri.parse(
            "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(messageText)}"
        )
        val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Fallback: if WhatsApp isn't installed, open in browser
        val resolves = context.packageManager.queryIntentActivities(waIntent, 0)
        val finalIntent = if (resolves.isNotEmpty()) waIntent else Intent(Intent.ACTION_VIEW, waUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            finalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, WHATSAPP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF25D366.toInt()) // WhatsApp green
            .setContentTitle("💬 Remind $customerName via WhatsApp")
            .setContentText("Tap to send payment reminder — KSh balance outstanding.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(messageText)
                    .setBigContentTitle("💬 Reminder for $customerName")
                    .setSummaryText("WhatsApp · tap to open")
            )
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_notification, "Send Now", pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
