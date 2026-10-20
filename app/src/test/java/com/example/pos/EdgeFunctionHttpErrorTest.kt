package com.example.pos

import com.example.pos.data.edgeFunctionHttpErrorMessage
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeFunctionHttpErrorTest {
    @Test
    fun identifiesMissingFunctionInsteadOfGenericGatewayFailure() {
        val message = edgeFunctionHttpErrorMessage("send-sms-reminder", 404)
        assertTrue(message.contains("endpoint was not found"))
        assertTrue(message.contains("HTTP 404"))
    }

    @Test
    fun distinguishesAuthenticationAuthorizationAndRateLimitErrors() {
        assertTrue(edgeFunctionHttpErrorMessage("send-sms-reminder", 401).contains("not authenticated"))
        assertTrue(edgeFunctionHttpErrorMessage("send-sms-reminder", 403).contains("not authorized"))
        assertTrue(edgeFunctionHttpErrorMessage("send-sms-reminder", 429).contains("rate limit"))
    }

    @Test
    fun redactsCredentialLikeTextFromBackendDetails() {
        val message = edgeFunctionHttpErrorMessage(
            "send-sms-reminder",
            400,
            "Invalid input; access_token=do-not-show"
        )
        assertTrue(message.contains("Invalid input"))
        assertTrue(!message.contains("do-not-show"))
        assertTrue(message.contains("[redacted]"))
    }
}
