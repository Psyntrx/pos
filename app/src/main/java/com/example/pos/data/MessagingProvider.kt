package com.example.pos.data

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

class WhatsAppMessagingProvider(
    private val supabaseService: SupabaseService?
) : MessagingProvider {
    override val channel: ReminderDeliveryChannel = ReminderDeliveryChannel.WHATSAPP
    override val providerName: String = "WhatsApp Business Cloud Platform"

    override val isConfigured: Boolean
        get() = supabaseService?.getAccessToken()?.isNotBlank() == true

    override suspend fun sendMessage(
        recipientPhone: String,
        messageText: String,
        shopId: String?,
        metadata: Map<String, String>
    ): MessagingDispatchResult {
        if (!isConfigured) {
            return MessagingDispatchResult(
                channel = ReminderDeliveryChannel.WHATSAPP,
                status = DeliveryStatus.FAILED,
                errorMessage = "Sign in to POS before requesting an automated WhatsApp reminder."
            )
        }
        val normalized = CreditLedgerUtils.normalizePhoneNumber(recipientPhone)
        if (normalized == null) {
            return MessagingDispatchResult(
                channel = ReminderDeliveryChannel.WHATSAPP,
                status = DeliveryStatus.FAILED,
                errorMessage = "Invalid phone number for WhatsApp."
            )
        }
        val cleanDigits = normalized.removePrefix("+")

        // Forward request via secure server-side authenticated Edge Function
        return try {
            val result = supabaseService?.dispatchEdgeFunction(
                functionName = "send-whatsapp-reminder",
                payload = mapOf(
                    "recipient" to cleanDigits,
                    "message" to messageText,
                    "shop_id" to (shopId ?: ""),
                    "metadata" to metadata
                )
            )
            if (result?.isSuccess == true) {
                MessagingDispatchResult(
                    channel = ReminderDeliveryChannel.WHATSAPP,
                    status = DeliveryStatus.ACCEPTED,
                    providerMessageId = result.getOrNull()
                )
            } else {
                MessagingDispatchResult(
                    channel = ReminderDeliveryChannel.WHATSAPP,
                    status = DeliveryStatus.FAILED,
                    errorMessage = result?.exceptionOrNull()?.message ?: "WhatsApp Gateway unreachable."
                )
            }
        } catch (e: Exception) {
            MessagingDispatchResult(
                channel = ReminderDeliveryChannel.WHATSAPP,
                status = DeliveryStatus.FAILED,
                errorMessage = e.message ?: "Network error connecting to WhatsApp service."
            )
        }
    }
}

class SmsMessagingProvider(
    private val supabaseService: SupabaseService?
) : MessagingProvider {
    override val channel: ReminderDeliveryChannel = ReminderDeliveryChannel.SMS
    override val providerName: String = "SMS Kenya Gateway (Safaricom/Airtel)"

    override val isConfigured: Boolean
        get() = supabaseService?.getAccessToken()?.isNotBlank() == true

    override suspend fun sendMessage(
        recipientPhone: String,
        messageText: String,
        shopId: String?,
        metadata: Map<String, String>
    ): MessagingDispatchResult {
        if (!isConfigured) {
            return MessagingDispatchResult(
                channel = ReminderDeliveryChannel.SMS,
                status = DeliveryStatus.FAILED,
                errorMessage = "Sign in to POS before requesting an automated SMS reminder."
            )
        }
        val normalized = CreditLedgerUtils.normalizePhoneNumber(recipientPhone)
        if (normalized == null) {
            return MessagingDispatchResult(
                channel = ReminderDeliveryChannel.SMS,
                status = DeliveryStatus.FAILED,
                errorMessage = "Invalid phone number for SMS."
            )
        }

        // Forward request via secure server-side authenticated Edge Function
        return try {
            val result = supabaseService?.dispatchEdgeFunction(
                functionName = "send-sms-reminder",
                payload = mapOf(
                    "recipient" to normalized,
                    "message" to messageText,
                    "shop_id" to (shopId ?: ""),
                    "metadata" to metadata
                )
            )
            if (result?.isSuccess == true) {
                MessagingDispatchResult(
                    channel = ReminderDeliveryChannel.SMS,
                    status = DeliveryStatus.ACCEPTED,
                    providerMessageId = result.getOrNull()
                )
            } else {
                MessagingDispatchResult(
                    channel = ReminderDeliveryChannel.SMS,
                    status = DeliveryStatus.FAILED,
                    errorMessage = result?.exceptionOrNull()?.message ?: "SMS Gateway unreachable."
                )
            }
        } catch (e: Exception) {
            MessagingDispatchResult(
                channel = ReminderDeliveryChannel.SMS,
                status = DeliveryStatus.FAILED,
                errorMessage = e.message ?: "Network error connecting to SMS service."
            )
        }
    }
}
