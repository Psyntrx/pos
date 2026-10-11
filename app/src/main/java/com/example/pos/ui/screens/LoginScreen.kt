package com.example.pos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.ui.components.GoogleIcon
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.AuthViewModel

/**
 * Login Screen Composable
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (email: String, pass: String) -> Unit,
    onNavigateToRegisterStep1: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onBackToSplash: () -> Unit
) {
    val loginState by authViewModel.loginState.collectAsState()

    BackHandler {
        onBackToSplash()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Header
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sign in to continue managing your shop and sales.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Email Input
                OutlinedTextField(
                    value = loginState.email,
                    onValueChange = { authViewModel.updateLoginEmail(it) },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Email, contentDescription = null, tint = EmeraldBright)
                    },
                    isError = loginState.email.isNotBlank() && !loginState.isEmailValid,
                    supportingText = {
                        if (loginState.email.isNotBlank() && !loginState.isEmailValid) {
                            Text("Please enter a valid email address.", color = ErrorRed, fontSize = 12.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Input
                OutlinedTextField(
                    value = loginState.password,
                    onValueChange = { authViewModel.updateLoginPassword(it) },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = EmeraldBright)
                    },
                    trailingIcon = {
                        IconButton(onClick = { authViewModel.toggleLoginPasswordVisibility() }) {
                            Icon(
                                imageVector = if (loginState.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (loginState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Forgot Password Link
                Text(
                    text = "Forgot password?",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = EmeraldBright,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { onNavigateToForgotPassword() }
                )

                if (loginState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = loginState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sign In Button
                PrimaryGreenButton(
                    text = "Sign In",
                    enabled = loginState.canSubmit,
                    onClick = {
                        authViewModel.login { email, pass ->
                            onLoginSuccess(email, pass)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = SurfaceBorder
                    )
                    Text(
                        text = " or ",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = SurfaceBorder
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Authentic Google Login Button with Official 4-Color 'G' Icon
                OutlinedButton(
                    onClick = {
                        authViewModel.login { email, pass ->
                            onLoginSuccess(email, pass)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceDarkVariant,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GoogleIcon(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Register Footer Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
                Text(
                    text = "Register",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EmeraldBright,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onNavigateToRegisterStep1() }
                )
            }
        }
    }
}
