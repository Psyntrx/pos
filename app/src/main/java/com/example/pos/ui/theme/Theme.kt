package com.example.pos.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode(val storageValue: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    fun resolvesToDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

@Composable
fun POSTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = themeMode.resolvesToDark(androidx.compose.foundation.isSystemInDarkTheme())
    updatePaletteForDarkTheme(useDarkTheme)
    val colorScheme = if (useDarkTheme) {
        darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF10B981),
            onPrimary = androidx.compose.ui.graphics.Color(0xFF090F0D),
            primaryContainer = androidx.compose.ui.graphics.Color(0xFF0C2B20),
            onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF12D18E),
            secondary = androidx.compose.ui.graphics.Color(0xFF12D18E),
            onSecondary = androidx.compose.ui.graphics.Color(0xFF090F0D),
            background = androidx.compose.ui.graphics.Color(0xFF090F0D),
            onBackground = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
            surface = androidx.compose.ui.graphics.Color(0xFF111A17),
            onSurface = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFF16231E),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF94A3B8),
            outline = androidx.compose.ui.graphics.Color(0xFF1F332B),
            error = androidx.compose.ui.graphics.Color(0xFFEF4444),
            errorContainer = androidx.compose.ui.graphics.Color(0xFF2E1212)
        )
    } else {
        lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF176B3A),
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = androidx.compose.ui.graphics.Color(0xFFE4F3E8),
            onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF176B3A),
            secondary = androidx.compose.ui.graphics.Color(0xFF16834A),
            onSecondary = androidx.compose.ui.graphics.Color.White,
            background = androidx.compose.ui.graphics.Color(0xFFF1F7F2),
            onBackground = androidx.compose.ui.graphics.Color(0xFF172B1F),
            surface = androidx.compose.ui.graphics.Color.White,
            onSurface = androidx.compose.ui.graphics.Color(0xFF172B1F),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF5F9F5),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF5D7163),
            outline = androidx.compose.ui.graphics.Color(0xFFDCE8DE),
            error = androidx.compose.ui.graphics.Color(0xFFB93832),
            errorContainer = androidx.compose.ui.graphics.Color(0xFFFCE9E7)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            updatePaletteForDarkTheme(useDarkTheme)
            val window = (view.context as Activity).window
            val systemBarColor = if (useDarkTheme) {
                androidx.compose.ui.graphics.Color(0xFF090F0D)
            } else {
                androidx.compose.ui.graphics.Color(0xFFF1F7F2)
            }
            window.statusBarColor = systemBarColor.toArgb()
            window.navigationBarColor = systemBarColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}