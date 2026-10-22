package com.example.pos.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.pos.utils.ReminderIntentHelper

data class MessagingDispatchResult(
    val channel: ReminderDeliveryChannel,
    val status: DeliveryStatus,
    val providerMessageId: String? = null,
    val errorMessage: String? = null
)

interface MessagingProvider {
    val channel: ReminderDeliveryChannel
    val isConfigured: Boolean
    val providerName: String

    suspend fun sendMessage(
        recipientPhone: String,
        messageText: String,
        shopId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): MessagingDispatchResult
}

/**
 * WhatsApp provider for background/scheduled reminders.
 *
 * Instead of immediately launching WhatsApp (which would interrupt the user),
 * this posts a local Android notification with a "Send Now" action button
 * containing a PendingIntent pre-filled with the WhatsApp message.
 * The user taps the notification at their convenience to send.
 *
 * For manual sends triggered directly from the UI (CreditSalesScreen), the
 * direct intent (launchWhatsApp / launchSms) in the screen composable is used.
 */
class WhatsAppMessagingProvider(
    private val context: Context?
) : MessagingProvider {
    override val channel: ReminderDeliveryChannel = ReminderDeliveryChannel.WHATSAPP
    override val providerName: String = "WhatsApp (Device Notification Queue)"

    override val isConfigured: Boolean get() = context != null

    override suspend fun sendMessage(
        recipientPhone: String,
        messageText: String,
        shopId: String?,
        metadata: Map<String, String>
    ): MessagingDispatchResult {
        val ctx = context ?: return MessagingDispatchResult(
            channel = ReminderDeliveryChannel.WHATSAPP,
            status = DeliveryStatus.FAILED,
            errorMessage = "No application context available."
        )

        val normalized = CreditLedgerUtils.normalizePhoneNumber(recipientPhone)
            ?: return MessagingDispatchResult(
                channel = ReminderDeliveryChannel.WHATSAPP,
                status = DeliveryStatus.FAILED,
                errorMessage = "Invalid phone number for WhatsApp."
            )

        val formattedPhone = normalized.removePrefix("+")
        val customerName = metadata["customer_name"]?.takeIf { it.isNotBlank() } ?: recipientPhone

        // Stable notification ID derived from phone number so each customer
        // gets a distinct notification that gets replaced rather than duplicated.
        val notificationId = ("wa_reminder_$formattedPhone").hashCode()

        return try {
            ReminderIntentHelper.postWhatsAppReminderNotification(
                context = ctx,
                notificationId = notificationId,
                customerName = customerName,
                phoneNumber = formattedPhone,
                messageText = messageText
            )
            MessagingDispatchResult(
                channel = ReminderDeliveryChannel.WHATSAPP,
                status = DeliveryStatus.QUEUED // queued in notification tray, not yet sent
            )
        } catch (e: Exception) {
            MessagingDispatchResult(
                channel = ReminderDeliveryChannel.WHATSAPP,
                status = DeliveryStatus.FAILED,
                errorMessage = e.message ?: "Failed to post WhatsApp reminder notification."
            )
        }
    }
}

/**
 * SMS provider for background/scheduled reminders.
 *
 * Silently inserts an SMS draft into the device's SMS content provider
 * (content://sms/draft) without opening the SMS composer screen.
 * A Toast confirms the draft was created. The user can open their SMS app
 * and send the pre-filled message at their convenience.
 *
 * For manual sends triggered directly from the UI, the launchSms()
 * helper in CreditSalesScreen composable is used instead.
 */
class SmsMessagingProvider(
    private val context: Context?
) : MessagingProvider {
    override val channel: ReminderDeliveryChannel = ReminderDeliveryChannel.SMS
    override val providerName: String = "SMS (Silent Draft)"

    override val isConfigured: Boolean get() = context != null

    override suspend fun sendMessage(
        recipientPhone: String,
        messageText: String,
        shopId: String?,
        metadata: Map<String, String>
    ): MessagingDispatchResult {
        val ctx = context ?: return MessagingDispatchResult(
            channel = ReminderDeliveryChannel.SMS,
            status = DeliveryStatus.FAILED,
            errorMessage = "No application context available."
        )

        val normalized = CreditLedgerUtils.normalizePhoneNumber(recipientPhone)
            ?: recipientPhone.trim()
        if (normalized.isBlank()) {
            return MessagingDispatchResult(
                channel = ReminderDeliveryChannel.SMS,
                status = DeliveryStatus.FAILED,
                errorMessage = "Invalid phone number for SMS."
            )
        }

        val success = ReminderIntentHelper.createSmsDraft(
            context = ctx,
            phoneNumber = normalized,
            messageText = messageText
        )

        return if (success) {
            MessagingDispatchResult(
                channel = ReminderDeliveryChannel.SMS,
                status = DeliveryStatus.QUEUED // draft saved, not yet sent
            )
        } else {
            // Draft insert failed — fall back to launching the SMS composer
            val smsUri = Uri.parse("smsto:${Uri.encode(normalized)}")
            val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                ctx.startActivity(intent)
                MessagingDispatchResult(
                    channel = ReminderDeliveryChannel.SMS,
                    status = DeliveryStatus.ACCEPTED
                )
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(ctx, "SMS app is not available.", Toast.LENGTH_SHORT).show()
                MessagingDispatchResult(
                    channel = ReminderDeliveryChannel.SMS,
                    status = DeliveryStatus.FAILED,
                    errorMessage = "SMS app is not available."
                )
            }
        }
    }
}
