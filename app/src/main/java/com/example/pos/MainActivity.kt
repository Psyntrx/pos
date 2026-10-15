package com.example.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.pos.ui.screens.ForgotPasswordScreen
import com.example.pos.ui.screens.LoginScreen
import com.example.pos.ui.screens.MainContainerScreen
import com.example.pos.ui.screens.RegisterShopStep1Screen
import com.example.pos.ui.screens.RegisterShopStep2Screen
import com.example.pos.ui.screens.SplashScreen
import com.example.pos.ui.theme.BgDark
import com.example.pos.ui.theme.AppColorTheme
import com.example.pos.ui.theme.POSTheme
import com.example.pos.ui.theme.ThemeMode
import com.example.pos.viewmodel.AuthViewModel
import com.example.pos.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember {
                getSharedPreferences("appearance", MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(ThemeMode.fromStorage(preferences.getString("theme_mode", null)))
            }
            var colorTheme by remember {
                mutableStateOf(AppColorTheme.fromStorage(preferences.getString("color_theme", null)))
            }
            var customAccentColor by remember {
                mutableStateOf(Color(preferences.getInt("custom_accent_color", 0xFF14B8A6.toInt())))
            }
            var profilePhotoUri by remember {
                mutableStateOf(preferences.getString("profile_photo_uri", null))
            }

            POSTheme(
                themeMode = themeMode,
                colorTheme = colorTheme,
                customAccentColor = customAccentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    val posViewModel = remember { PosViewModel(application) }
                    val authViewModel = remember { AuthViewModel() }
                    val currentScreen by posViewModel.currentScreen.collectAsState()

                    when (currentScreen) {
                        "splash" -> SplashScreen(
                            onGetStarted = { posViewModel.navigateTo("login") }
                        )
                        "login" -> LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = { email: String, pass: String ->
                                posViewModel.login(email, pass)
                            },
                            onNavigateToRegisterStep1 = {
                                posViewModel.navigateTo("register_step1")
                            },
                            onNavigateToForgotPassword = {
                                posViewModel.navigateTo("forgot_password")
                            },
                            onBackToSplash = {
                                posViewModel.navigateTo("splash")
                            }
                        )
                        "forgot_password" -> ForgotPasswordScreen(
                            authViewModel = authViewModel,
                            onBackToLogin = {
                                posViewModel.navigateTo("login")
                            }
                        )
                        "register_step1" -> RegisterShopStep1Screen(
                            authViewModel = authViewModel,
                            onNavigateToStep2 = {
                                posViewModel.navigateTo("register_step2")
                            },
                            onBackToLogin = {
                                posViewModel.navigateTo("login")
                            }
                        )
                        "register_step2" -> RegisterShopStep2Screen(
                            authViewModel = authViewModel,
                            onRegisterSuccess = {
                                val regState = authViewModel.registerState.value
                                posViewModel.registerShop(
                                    shopName = regState.shopName,
                                    ownerName = regState.ownerName,
                                    email = regState.email,
                                    phone = regState.phone,
                                    address = regState.address,
                                    category = regState.category
                                )
                            },
                            onNavigateToStep1 = {
                                posViewModel.navigateTo("register_step1")
                            }
                        )
                        else -> MainContainerScreen(
                            viewModel = posViewModel,
                            themeMode = themeMode,
                            colorTheme = colorTheme,
                            customAccentColor = customAccentColor,
                            profilePhotoUri = profilePhotoUri,
                            onThemeModeChange = { selectedMode ->
                                themeMode = selectedMode
                                preferences.edit().putString("theme_mode", selectedMode.storageValue).apply()
                            },
                            onColorThemeChange = { selectedTheme ->
                                colorTheme = selectedTheme
                                preferences.edit().putString("color_theme", selectedTheme.storageValue).apply()
                            },
                            onCustomAccentColorChange = { selectedColor ->
                                customAccentColor = selectedColor
                                colorTheme = AppColorTheme.CUSTOM
                                preferences.edit()
                                    .putString("color_theme", AppColorTheme.CUSTOM.storageValue)
                                    .putInt("custom_accent_color", selectedColor.toArgb())
                                    .apply()
                            },
                            onProfilePhotoChange = { uri ->
                                profilePhotoUri = uri
                                preferences.edit().putString("profile_photo_uri", uri).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}
