package com.example.pos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.AuthSession
import com.example.pos.ui.components.AbstractBlobHeader
import com.example.pos.ui.components.GoogleIcon
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.AuthViewModel

/**
 * Login / Sign-In Screen — inspired by reference panel 3.
 * Features an abstract blob decorative header with a back button overlaid,
 * and a clean white/surface form area below.
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (email: String, pass: String, session: AuthSession?) -> Unit,
    onNavigateToRegisterStep1: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onBackToSplash: () -> Unit
) {
    val loginState by authViewModel.loginState.collectAsState()

    BackHandler { onBackToSplash() }

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
            // ── Decorative abstract header with back button ──────────────
            Box {
                AbstractBlobHeader(headerHeight = 240.dp)

                // Back button overlaid on the header
                TextButton(
                    onClick = onBackToSplash,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 8.dp, top = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

            // ── Form area ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Heading
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = TextPrimary,
                        fontSize = 30.sp
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Email field
                OutlinedTextField(
                    value = loginState.email,
                    onValueChange = { authViewModel.updateLoginEmail(it) },
                    label = { Text("Email") },
                    placeholder = { Text("e.g. user@example.com") },
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
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password field
                OutlinedTextField(
                    value = loginState.password,
                    onValueChange = { authViewModel.updateLoginPassword(it) },
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
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
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Remember me + Forgot password row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            authViewModel.updateRememberMe(!loginState.rememberMe)
                        }
                    ) {
                        Checkbox(
                            checked = loginState.rememberMe,
                            onCheckedChange = { authViewModel.updateRememberMe(it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = EmeraldBright,
                                uncheckedColor = TextMuted,
                                checkmarkColor = TextOnPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Remember me",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        )
                    }

                    Text(
                        text = "Forgot password?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EmeraldBright,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        modifier = Modifier.clickable { onNavigateToForgotPassword() }
                    )
                }

                // Error message
                if (loginState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = loginState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sign In button
                PrimaryGreenButton(
                    text = if (loginState.isLoading) "Signing In..." else "Sign In",
                    enabled = loginState.canSubmit && !loginState.isLoading,
                    onClick = {
                        authViewModel.login { email, pass, session ->
                            onLoginSuccess(email, pass, session)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Divider with "Sign in with"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceBorder)
                    Text(
                        text = "  Sign in with  ",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceBorder)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Google sign-in button
                OutlinedButton(
                    onClick = {
                        authViewModel.login { email, pass, session ->
                            onLoginSuccess(email, pass, session)
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

                Spacer(modifier = Modifier.height(20.dp))

                // Register footer
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

                // Bottom navigation bar spacing
                Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
            }
        }
    }
}
