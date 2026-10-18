package com.example.pos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.pos.data.AuthSession
import com.example.pos.data.PosStorageManager
import com.example.pos.data.SupabaseClient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.pos.ui.screens.EmployeeWaitingApprovalScreen
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
    private var notificationNavigationRequest by mutableIntStateOf(0)

    companion object {
        const val EXTRA_OPEN_NOTIFICATIONS = "com.example.pos.OPEN_NOTIFICATIONS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeNotificationNavigation(intent)
        enableEdgeToEdge()
        setContent {
            val notificationRequest = notificationNavigationRequest
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
                    val authViewModel = remember { AuthViewModel(SupabaseClient.service, PosStorageManager(application)) }
                    val currentScreen by posViewModel.currentScreen.collectAsState()

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        posViewModel.setSystemNotificationsEnabled(granted)
                    }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    LaunchedEffect(notificationRequest) {
                        if (notificationRequest > 0) posViewModel.navigateTo("main")
                    }

                    when (currentScreen) {
                        "splash" -> SplashScreen(
                            onGetStarted = { posViewModel.navigateTo("login") }
                        )
                        "login" -> LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = { email: String, pass: String, session: AuthSession? ->
                                posViewModel.login(email, pass, session)
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
                            onEmployeeRegistered = { emp ->
                                posViewModel.onEmployeeRegistered(emp)
                            },
                            onNavigateToStep1 = {
                                posViewModel.navigateTo("register_step1")
                            }
                        )
                        "employee_waiting_approval" -> EmployeeWaitingApprovalScreen(
                            viewModel = posViewModel,
                            onApproved = {
                                posViewModel.navigateTo("main")
                            },
                            onLogout = {
                                posViewModel.logout()
                            }
                        )
                        else -> MainContainerScreen(
                            viewModel = posViewModel,
                            notificationNavigationRequest = notificationRequest,
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNotificationNavigation(intent)
    }

    private fun consumeNotificationNavigation(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false) == true) {
            notificationNavigationRequest += 1
            intent.removeExtra(EXTRA_OPEN_NOTIFICATIONS)
        }
    }
}
