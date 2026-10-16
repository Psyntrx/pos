package com.example.pos.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.ui.components.AbstractBlobHeader
import com.example.pos.ui.components.GoogleIcon
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.posTextFieldColors
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.AuthViewModel

/**
 * Unified Shop Registration screen file containing both Step 1 and Step 2.
 * Redesigned using inspiration from reference panel 2 ("Get Started" sign-up screen)
 * while preserving full 2-step registration workflow and emerald brand styling.
 */
@Composable
fun RegisterShopScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    when (step) {
        1 -> RegisterShopStep1Screen(
            authViewModel = authViewModel,
            onNavigateToStep2 = { step = 2 },
            onBackToLogin = onBackToLogin
        )
        2 -> RegisterShopStep2Screen(
            authViewModel = authViewModel,
            onRegisterSuccess = onRegisterSuccess,
            onNavigateToStep1 = { step = 1 }
        )
    }
}

/**
 * Register Shop Step 1 Composable (Shop Profile & Owner Details)
 * Follows reference panel 2 visual hierarchy: decorative blob header, back button,
 * "Get Started" italic header, form inputs, consent checkbox, primary action, and social auth divider.
 */
@Composable
fun RegisterShopStep1Screen(
    authViewModel: AuthViewModel,
    onNavigateToStep2: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val registerState by authViewModel.registerState.collectAsState()
    var agreedToTerms by remember { mutableStateOf(true) }

    BackHandler { onBackToLogin() }

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
            // ── Decorative abstract header with overlaid back button ──────────────
            Box {
                AbstractBlobHeader(headerHeight = 220.dp)

                TextButton(
                    onClick = onBackToLogin,
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

            // ── Form Area ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (matches reference panel 2 "Get Started")
                Text(
                    text = "Register",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = TextPrimary,
                        fontSize = 30.sp
                    )
                )




                Spacer(modifier = Modifier.height(24.dp))

                // Shop Name
                OutlinedTextField(
                    value = registerState.shopName,
                    onValueChange = {
                        authViewModel.updateStep1Data(
                            it, registerState.ownerName, registerState.phone,
                            registerState.email, registerState.category, registerState.address
                        )
                    },
                    label = { Text("Shop Name") },
                    placeholder = { Text("Enter shop name") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Storefront, contentDescription = null, tint = EmeraldBright)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Owner Name
                OutlinedTextField(
                    value = registerState.ownerName,
                    onValueChange = {
                        authViewModel.updateStep1Data(
                            registerState.shopName, it, registerState.phone,
                            registerState.email, registerState.category, registerState.address
                        )
                    },
                    label = { Text("Full Name / Owner") },
                    placeholder = { Text("Enter full name") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = EmeraldBright)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Email Address
                OutlinedTextField(
                    value = registerState.email,
                    onValueChange = {
                        authViewModel.updateStep1Data(
                            registerState.shopName, registerState.ownerName, registerState.phone,
                            it, registerState.category, registerState.address
                        )
                    },
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter email address") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Email, contentDescription = null, tint = EmeraldBright)
                    },
                    isError = registerState.email.isNotBlank() && !registerState.isEmailValid,
                    supportingText = {
                        if (registerState.email.isNotBlank() && !registerState.isEmailValid) {
                            Text("Enter a valid email format.", color = ErrorRed, fontSize = 12.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Phone Number
                OutlinedTextField(
                    value = registerState.phone,
                    onValueChange = {
                        authViewModel.updateStep1Data(
                            registerState.shopName, registerState.ownerName, it,
                            registerState.email, registerState.category, registerState.address
                        )
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("Enter phone number") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Phone, contentDescription = null, tint = EmeraldBright)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category
                OutlinedTextField(
                    value = registerState.category,
                    onValueChange = {
                        authViewModel.updateStep1Data(
                            registerState.shopName, registerState.ownerName, registerState.phone,
                            registerState.email, it, registerState.address
                        )
                    },
                    label = { Text("Shop Category") },
                    trailingIcon = {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = EmeraldBright)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Address
                OutlinedTextField(
                    value = registerState.address,
                    onValueChange = {
                        authViewModel.updateStep1Data(
                            registerState.shopName, registerState.ownerName, registerState.phone,
                            registerState.email, registerState.category, it
                        )
                    },
                    label = { Text("Shop Physical Address") },
                    placeholder = { Text("Street, town, or building") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Place, contentDescription = null, tint = EmeraldBright)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))



                // Continue / Next Button
                PrimaryGreenButton(
                    text = "Continue to Step 2",
                    enabled = registerState.isStep1Valid && agreedToTerms,
                    onClick = onNavigateToStep2
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Progress Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { 0.5f },
                        modifier = Modifier
                            .width(100.dp)
                            .height(5.dp)
                            .clip(CircleShape),
                        color = EmeraldBright,
                        trackColor = SurfaceDarkVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "50% Complete",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Social Sign Up Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceBorder)
                    Text(
                        text = "  Sign up with  ",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceBorder)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Google option
                OutlinedButton(
                    onClick = onNavigateToStep2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceDarkVariant,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GoogleIcon(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "sign up with Google",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sign in switch footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Text(
                        text = "Sign in",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EmeraldBright,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable { onBackToLogin() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
            }
        }
    }
}

/**
 * Register Shop Step 2 Composable (Security Credentials & Reg No)
 * Same consistent header style, password inputs, password requirement helper card, and submit action.
 */
@Composable
fun RegisterShopStep2Screen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToStep1: () -> Unit
) {
    val registerState by authViewModel.registerState.collectAsState()

    BackHandler { onNavigateToStep1() }

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
            // ── Decorative Header ──────────────────────────────────────────────
            Box {
                AbstractBlobHeader(headerHeight = 220.dp)

                TextButton(
                    onClick = onNavigateToStep1,
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
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Security Setup",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = TextPrimary,
                        fontSize = 30.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Step 2 of 2: Create a secure password",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Password Field
                OutlinedTextField(
                    value = registerState.password,
                    onValueChange = { authViewModel.updatePassword(it) },
                    label = { Text("Password") },
                    placeholder = { Text("Create password") },
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
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm Password Field
                OutlinedTextField(
                    value = registerState.confirmPassword,
                    onValueChange = { authViewModel.updateConfirmPassword(it) },
                    label = { Text("Confirm Password") },
                    placeholder = { Text("Re-enter password") },
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
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Business Reg No (Optional)
                OutlinedTextField(
                    value = registerState.businessRegNo,
                    onValueChange = { authViewModel.updateBusinessRegNo(it) },
                    label = { Text("Business Registration No. (Optional)") },
                    placeholder = { Text("e.g. BN/2026/0123") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Badge, contentDescription = null, tint = EmeraldBright)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = posTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Password requirement checklist card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDarkVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Password Requirements",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        ValidationRequirementRow(
                            label = "At least 8 characters",
                            isMet = registerState.isMinLengthValid
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ValidationRequirementRow(
                            label = "Contains a number or symbol",
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

                Spacer(modifier = Modifier.height(26.dp))

                // Complete registration button
                PrimaryGreenButton(
                    text = if (registerState.isLoading) "Creating Account..." else "Complete Registration",
                    enabled = registerState.canSubmit && !registerState.isLoading,
                    onClick = {
                        authViewModel.submitRegistration {
                            onRegisterSuccess()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Progress Indicator 100%
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { 1.0f },
                        modifier = Modifier
                            .width(100.dp)
                            .height(5.dp)
                            .clip(CircleShape),
                        color = EmeraldBright,
                        trackColor = SurfaceDarkVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Step 2 of 2",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun ValidationRequirementRow(
    label: String,
    isMet: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
