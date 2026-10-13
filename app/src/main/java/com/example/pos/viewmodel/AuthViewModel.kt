package com.example.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos.data.SupabaseClient
import com.example.pos.data.Shop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared ViewModel managing state, seamless data retention across registration steps,
 * login, and forgot password screens with real-time state-driven validation.
 */
data class RegisterUiState(
    // Step 1 Fields
    val shopName: String = "Faith's Store",
    val ownerName: String = "Faith",
    val phone: String = "+254 712 345 678",
    val email: String = "faith@store.com",
    val category: String = "Groceries & Retail",
    val address: String = "Plot 42, Biashara Street, Nairobi",

    // Step 2 Fields
    val password: String = "",
    val confirmPassword: String = "",
    val businessRegNo: String = "", // Optional
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,

    // UI Status
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegisteredSuccessfully: Boolean = false
) {
    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun isValidEmail(emailStr: String): Boolean =
            EMAIL_REGEX.matches(emailStr.trim())
    }

    // Step 1 Validation
    val isEmailValid: Boolean get() = isValidEmail(email)
    val isStep1Valid: Boolean get() =
        shopName.isNotBlank() && ownerName.isNotBlank() && phone.isNotBlank() &&
                isEmailValid && category.isNotBlank() && address.isNotBlank()

    // Step 2 Password Validation
    val isMinLengthValid: Boolean get() = password.length >= 8
    val hasDigitOrSymbol: Boolean get() = password.any { it.isDigit() || !it.isLetterOrDigit() }
    val isPasswordStrong: Boolean get() = isMinLengthValid && hasDigitOrSymbol
    val doPasswordsMatch: Boolean get() = password.isNotEmpty() && password == confirmPassword

    val isStep2Valid: Boolean get() = isPasswordStrong && doPasswordsMatch
    val canSubmit: Boolean get() = isStep1Valid && isStep2Valid
}

data class LoginUiState(
    val email: String = "faith@store.com",
    val password: String = "password123",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isEmailValid: Boolean get() = RegisterUiState.isValidEmail(email)
    val canSubmit: Boolean get() = isEmailValid && password.isNotBlank()
}

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isResetSent: Boolean = false,
    val errorMessage: String? = null
) {
    val isEmailValid: Boolean get() = RegisterUiState.isValidEmail(email)
    val canSubmit: Boolean get() = isEmailValid && !isLoading
}

class AuthViewModel : ViewModel() {

    private val supabase = SupabaseClient.service

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordUiState())
    val forgotPasswordState: StateFlow<ForgotPasswordUiState> = _forgotPasswordState.asStateFlow()

    // -------------------------------------------------------------
    // Register Step 1 Actions
    // -------------------------------------------------------------
    fun updateStep1Data(
        shopName: String,
        ownerName: String,
        phone: String,
        email: String,
        category: String,
        address: String
    ) {
        _registerState.update {
            it.copy(
                shopName = shopName,
                ownerName = ownerName,
                phone = phone,
                email = email,
                category = category,
                address = address,
                errorMessage = null
            )
        }
    }

    // -------------------------------------------------------------
    // Register Step 2 Actions
    // -------------------------------------------------------------
    fun updatePassword(password: String) {
        _registerState.update {
            it.copy(password = password, errorMessage = null)
        }
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _registerState.update {
            it.copy(confirmPassword = confirmPassword, errorMessage = null)
        }
    }

    fun updateBusinessRegNo(businessRegNo: String) {
        _registerState.update {
            it.copy(businessRegNo = businessRegNo)
        }
    }

    fun togglePasswordVisibility() {
        _registerState.update {
            it.copy(passwordVisible = !it.passwordVisible)
        }
    }

    fun toggleConfirmPasswordVisibility() {
        _registerState.update {
            it.copy(confirmPasswordVisible = !it.confirmPasswordVisible)
        }
    }

    fun submitRegistration(onSuccess: (Shop) -> Unit) {
        val state = _registerState.value
        if (!state.canSubmit) {
            _registerState.update {
                it.copy(errorMessage = "Please fulfill all required fields and password requirements.")
            }
            return
        }

        val createdShop = Shop(
            name = state.shopName.trim(),
            ownerName = state.ownerName.trim(),
            email = state.email.trim(),
            phone = state.phone.trim(),
            address = state.address.trim(),
            category = state.category.trim()
        )

        _registerState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = supabase.signUp(state.email.trim(), state.password)
            result.fold(
                onSuccess = {
                    _registerState.update {
                        it.copy(isLoading = false, isRegisteredSuccessfully = true)
                    }
                    onSuccess(createdShop)
                },
                onFailure = { error ->
                    _registerState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Registration failed.")
                    }
                }
            )
        }
    }

    // -------------------------------------------------------------
    // Login Actions
    // -------------------------------------------------------------
    fun updateLoginEmail(email: String) {
        _loginState.update { it.copy(email = email, errorMessage = null) }
    }

    fun updateLoginPassword(password: String) {
        _loginState.update { it.copy(password = password, errorMessage = null) }
    }

    fun toggleLoginPasswordVisibility() {
        _loginState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun login(onSuccess: (email: String, pass: String) -> Unit) {
        val state = _loginState.value
        if (!state.canSubmit) {
            _loginState.update { it.copy(errorMessage = "Please enter a valid email address and password.") }
            return
        }

        _loginState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = supabase.signIn(state.email.trim(), state.password)
            result.fold(
                onSuccess = {
                    _loginState.update { it.copy(isLoading = false) }
                    onSuccess(state.email, state.password)
                },
                onFailure = { error ->
                    _loginState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Login failed.")
                    }
                }
            )
        }
    }

    // -------------------------------------------------------------
    // Forgot Password Actions
    // -------------------------------------------------------------
    fun updateForgotEmail(email: String) {
        _forgotPasswordState.update {
            it.copy(email = email, isResetSent = false, errorMessage = null)
        }
    }

    fun sendPasswordResetLink() {
        val state = _forgotPasswordState.value
        if (!state.isEmailValid) {
            _forgotPasswordState.update {
                it.copy(errorMessage = "Please enter a valid email address.")
            }
            return
        }

        _forgotPasswordState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = supabase.resetPasswordForEmail(state.email.trim())
            result.fold(
                onSuccess = {
                    _forgotPasswordState.update { it.copy(isLoading = false, isResetSent = true) }
                },
                onFailure = { error ->
                    _forgotPasswordState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Could not send reset email.")
                    }
                }
            )
        }
    }

    fun resetForgotState() {
        _forgotPasswordState.value = ForgotPasswordUiState()
    }
}
