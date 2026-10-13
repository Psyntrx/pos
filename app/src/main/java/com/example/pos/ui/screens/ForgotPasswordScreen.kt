package com.example.pos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.ui.components.AbstractBlobHeader
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.posTextFieldColors
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.AuthViewModel

/**
 * Forgot Password Composable Screen
 * Redesigned with consistent decorative organic blob header, clean form inputs,
 * and emerald palette branding while preserving reset logic.
 */
@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    onBackToLogin: () -> Unit
) {
    val forgotState by authViewModel.forgotPasswordState.collectAsState()

    BackHandler {
        authViewModel.resetForgotState()
        onBackToLogin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Decorative Header with Back Button ─────────────────────────────
            Box {
                AbstractBlobHeader(headerHeight = 220.dp)

                TextButton(
                    onClick = {
                        authViewModel.resetForgotState()
                        onBackToLogin()
                    },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 8.dp, top = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // ── Form Area ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = TextPrimary,
                        fontSize = 30.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Enter your registered email address and we'll send you instructions to reset your account password.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Email Address Input
                OutlinedTextField(
                    value = forgotState.email,
                    onValueChange = { authViewModel.updateForgotEmail(it) },
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your registered email") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Email, contentDescription = null, tint = EmeraldBright)
                    },
                    isError = forgotState.email.isNotBlank() && !forgotState.isEmailValid,
                    supportingText = {
                        if (forgotState.email.isNotBlank() && !forgotState.isEmailValid) {
                            Text("Please enter a valid email format.", color = ErrorRed, fontSize = 12.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Success Message Card
                AnimatedVisibility(
                    visible = forgotState.isResetSent,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldBright.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Password reset link sent to ${forgotState.email}. Please check your inbox.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                if (forgotState.errorMessage != null) {
                    Text(
                        text = forgotState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Send Reset Link Button
                PrimaryGreenButton(
                    text = if (forgotState.isLoading) "Sending Link..." else "Send Reset Link",
                    enabled = forgotState.canSubmit,
                    onClick = { authViewModel.sendPasswordResetLink() }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Back to Login Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Remember your password? ",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Text(
                        text = "Back to Login",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EmeraldBright,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable {
                            authViewModel.resetForgotState()
                            onBackToLogin()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
            }
        }
    }
}
