package com.example.pos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.ui.components.GoogleIcon
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.SecondaryDarkButton
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
            .navigationBarsPadding()
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
                    label = { Text("Email Address", color = TextSecondary) },
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDarkVariant,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = EmeraldBright,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Input
                OutlinedTextField(
                    value = loginState.password,
                    onValueChange = { authViewModel.updateLoginPassword(it) },
                    label = { Text("Password", color = TextSecondary) },
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDarkVariant,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = EmeraldBright,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
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

/**
 * Register Shop Step 1 Composable
 */
@Composable
fun RegisterShopStep1Screen(
    authViewModel: AuthViewModel,
    onNavigateToStep2: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val registerState by authViewModel.registerState.collectAsState()

    BackHandler {
        onBackToLogin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLogin) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Login",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Register Shop (Step 1 of 2)",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Enter basic shop details.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Shop Badge Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmeraldContainer)
                    .border(1.5.dp, EmeraldBright, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = EmeraldBright,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Fields
            OutlinedTextField(
                value = registerState.shopName,
                onValueChange = {
                    authViewModel.updateStep1Data(
                        it, registerState.ownerName, registerState.phone,
                        registerState.email, registerState.category, registerState.address
                    )
                },
                label = { Text("Shop Name", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = registerState.ownerName,
                onValueChange = {
                    authViewModel.updateStep1Data(
                        registerState.shopName, it, registerState.phone,
                        registerState.email, registerState.category, registerState.address
                    )
                },
                label = { Text("Owner Name", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = registerState.phone,
                onValueChange = {
                    authViewModel.updateStep1Data(
                        registerState.shopName, registerState.ownerName, it,
                        registerState.email, registerState.category, registerState.address
                    )
                },
                label = { Text("Phone Number", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = registerState.email,
                onValueChange = {
                    authViewModel.updateStep1Data(
                        registerState.shopName, registerState.ownerName, registerState.phone,
                        it, registerState.category, registerState.address
                    )
                },
                label = { Text("Email Address", color = TextSecondary) },
                isError = registerState.email.isNotBlank() && !registerState.isEmailValid,
                supportingText = {
                    if (registerState.email.isNotBlank() && !registerState.isEmailValid) {
                        Text("Enter a valid email format.", color = ErrorRed, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = registerState.category,
                onValueChange = {
                    authViewModel.updateStep1Data(
                        registerState.shopName, registerState.ownerName, registerState.phone,
                        registerState.email, it, registerState.address
                    )
                },
                label = { Text("Shop Category", color = TextSecondary) },
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = EmeraldBright)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = registerState.address,
                onValueChange = {
                    authViewModel.updateStep1Data(
                        registerState.shopName, registerState.ownerName, registerState.phone,
                        registerState.email, registerState.category, it
                    )
                },
                label = { Text("Shop Address", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryGreenButton(
                text = "Continue to Step 2",
                enabled = registerState.isStep1Valid,
                onClick = onNavigateToStep2
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Step Progress Indicator 1/2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier
                        .width(120.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = EmeraldBright,
                    trackColor = SurfaceDarkVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Step 1 of 2",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Register Shop Step 2 Composable
 */
@Composable
fun RegisterShopStep2Screen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToStep1: () -> Unit
) {
    val registerState by authViewModel.registerState.collectAsState()

    BackHandler {
        onNavigateToStep1()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToStep1) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Step 1",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Register Shop (Step 2 of 2)",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Set security and business identification.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Security Badge Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmeraldContainer)
                    .border(1.5.dp, EmeraldBright, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = EmeraldBright,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Password Field
            OutlinedTextField(
                value = registerState.password,
                onValueChange = { authViewModel.updatePassword(it) },
                label = { Text("Password", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = EmeraldBright)
                },
                trailingIcon = {
                    IconButton(onClick = { authViewModel.togglePasswordVisibility() }) {
                        Icon(
                            imageVector = if (registerState.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = TextSecondary
                        )
                    }
                },
                visualTransformation = if (registerState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = registerState.confirmPassword,
                onValueChange = { authViewModel.updateConfirmPassword(it) },
                label = { Text("Confirm Password", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = EmeraldBright)
                },
                trailingIcon = {
                    IconButton(onClick = { authViewModel.toggleConfirmPasswordVisibility() }) {
                        Icon(
                            imageVector = if (registerState.confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle confirm password visibility",
                            tint = TextSecondary
                        )
                    }
                },
                visualTransformation = if (registerState.confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Business Registration Number
            OutlinedTextField(
                value = registerState.businessRegNo,
                onValueChange = { authViewModel.updateBusinessRegNo(it) },
                label = { Text("Business Registration Number (Optional)", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Outlined.Badge, contentDescription = null, tint = EmeraldBright)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Password Validation Rules Guidance Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDarkVariant,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Password Requirements",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ValidationRequirementRow(
                        label = "At least 8 characters",
                        isMet = registerState.isMinLengthValid
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ValidationRequirementRow(
                        label = "Contains a number or special character",
                        isMet = registerState.hasDigitOrSymbol
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ValidationRequirementRow(
                        label = "Passwords match",
                        isMet = registerState.doPasswordsMatch
                    )
                }
            }

            if (registerState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = registerState.errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryGreenButton(
                text = if (registerState.isLoading) "Creating Account..." else "Submit & Complete Registration",
                enabled = registerState.canSubmit && !registerState.isLoading,
                onClick = {
                    authViewModel.submitRegistration {
                        onRegisterSuccess()
                    }
                }
            )

            if (!registerState.canSubmit && !registerState.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fulfill password requirements above to enable submission.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step Progress Indicator 2/2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { 1.0f },
                    modifier = Modifier
                        .width(120.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = EmeraldBright,
                    trackColor = SurfaceDarkVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Step 2 of 2",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Forgot Password Composable Screen
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
            .navigationBarsPadding()
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
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        authViewModel.resetForgotState()
                        onBackToLogin()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Login",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Icon Badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer)
                        .border(1.5.dp, EmeraldBright, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = EmeraldBright,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Forgot Your Password?",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your registered email address below and we will send you a reset link.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Email Address Input
                OutlinedTextField(
                    value = forgotState.email,
                    onValueChange = { authViewModel.updateForgotEmail(it) },
                    label = { Text("Registered Email Address", color = TextSecondary) },
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
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDarkVariant,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = EmeraldBright,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
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
            }

            // Back to Login Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
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
        }
    }
}

@Composable
private fun ValidationRequirementRow(
    label: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = if (isMet) EmeraldBright else TextMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isMet) TextPrimary else TextMuted,
                fontWeight = if (isMet) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}
