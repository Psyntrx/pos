package com.example.pos

import com.example.pos.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = AuthViewModel(supabase = null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSharedStatePersistedAcrossRegisterStep1AndStep2() {
        // Step 1 input
        viewModel.updateStep1Data(
            shopName = "My Tech Store",
            ownerName = "Alex",
            phone = "+254 700 123 456",
            email = "alex@techstore.com",
            category = "Electronics",
            address = "Nairobi CBD"
        )

        val step1State = viewModel.registerState.value
        assertEquals("My Tech Store", step1State.shopName)
        assertEquals("Alex", step1State.ownerName)
        assertEquals("+254 700 123 456", step1State.phone)
        assertEquals("alex@techstore.com", step1State.email)
        assertEquals("Electronics", step1State.category)
        assertEquals("Nairobi CBD", step1State.address)
        assertTrue(step1State.isStep1Valid)

        // Step 2 input
        viewModel.updatePassword("Pass1234!")
        viewModel.updateConfirmPassword("Pass1234!")
        viewModel.updateBusinessRegNo("BN-998822")

        val step2State = viewModel.registerState.value
        assertEquals("Pass1234!", step2State.password)
        assertEquals("Pass1234!", step2State.confirmPassword)
        assertEquals("BN-998822", step2State.businessRegNo)
        assertTrue(step2State.isStep2Valid)
        assertTrue(step2State.canSubmit)

        // Verify Step 1 fields are still preserved intact
        assertEquals("My Tech Store", step2State.shopName)
        assertEquals("Alex", step2State.ownerName)
    }

    @Test
    fun testPasswordValidationRules() {
        // Too short (< 8 chars)
        viewModel.updatePassword("Pass1")
        viewModel.updateConfirmPassword("Pass1")
        var state = viewModel.registerState.value
        assertFalse(state.isMinLengthValid)
        assertFalse(state.isStep2Valid)

        // 8+ chars but no digit or special symbol
        viewModel.updatePassword("Password")
        viewModel.updateConfirmPassword("Password")
        state = viewModel.registerState.value
        assertTrue(state.isMinLengthValid)
        assertFalse(state.hasDigitOrSymbol)
        assertFalse(state.isStep2Valid)

        // Passwords do not match
        viewModel.updatePassword("Password123!")
        viewModel.updateConfirmPassword("Password123?")
        state = viewModel.registerState.value
        assertTrue(state.isPasswordStrong)
        assertFalse(state.doPasswordsMatch)
        assertFalse(state.isStep2Valid)

        // Valid strong matching password
        viewModel.updatePassword("Password123!")
        viewModel.updateConfirmPassword("Password123!")
        state = viewModel.registerState.value
        assertTrue(state.isPasswordStrong)
        assertTrue(state.doPasswordsMatch)
        assertTrue(state.isStep2Valid)
    }

    @Test
    fun testEmailRegexValidation() {
        // Invalid email
        viewModel.updateForgotEmail("invalid-email-address")
        assertFalse(viewModel.forgotPasswordState.value.isEmailValid)

        // Valid email
        viewModel.updateForgotEmail("user@domain.com")
        assertTrue(viewModel.forgotPasswordState.value.isEmailValid)
    }

    @Test
    fun testForgotPasswordStateAndReset() {
        viewModel.updateForgotEmail("store@example.com")
        assertTrue(viewModel.forgotPasswordState.value.canSubmit)

        viewModel.sendPasswordResetLink()
        assertTrue(viewModel.forgotPasswordState.value.isResetSent)

        viewModel.resetForgotState()
        assertFalse(viewModel.forgotPasswordState.value.isResetSent)
        assertEquals("", viewModel.forgotPasswordState.value.email)
    }

    @Test
    fun testLoginValidation() {
        viewModel.updateLoginEmail("faith@store.com")
        viewModel.updateLoginPassword("password123")
        assertTrue(viewModel.loginState.value.canSubmit)

        var loginSuccessCalled = false
        viewModel.login { email, pass ->
            assertEquals("faith@store.com", email)
            assertEquals("password123", pass)
            loginSuccessCalled = true
        }
        assertTrue(loginSuccessCalled)
    }
}
