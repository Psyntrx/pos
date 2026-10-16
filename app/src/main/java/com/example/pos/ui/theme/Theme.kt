package com.example.pos.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

data class ThemeColors(
    val swatch: Color,
    val lightPrimary: Color,
    val lightSecondary: Color,
    val lightContainer: Color,
    val darkPrimary: Color,
    val darkSecondary: Color,
    val darkContainer: Color
) {
    companion object {
        fun fromCustomAccent(accent: Color): ThemeColors {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(accent.toArgb(), hsv)
            val hue = hsv[0]
            val saturation = hsv[1]
            val value = hsv[2]
            return ThemeColors(
                swatch = accent,
                lightPrimary = Color.hsv(hue, saturation.coerceAtLeast(0.58f), (value * 0.68f).coerceIn(0.34f, 0.68f)),
                lightSecondary = Color.hsv(hue, saturation.coerceAtLeast(0.68f), (value * 0.78f).coerceIn(0.40f, 0.76f)),
                lightContainer = Color.hsv(hue, 0.18f, 0.97f),
                darkPrimary = Color.hsv(hue, saturation.coerceAtLeast(0.42f), value.coerceAtLeast(0.76f)),
                darkSecondary = Color.hsv(hue, saturation.coerceAtLeast(0.32f), value.coerceAtLeast(0.86f)),
                darkContainer = Color.hsv(hue, 0.55f, 0.20f)
            )
        }
    }
}

enum class AppColorTheme(
    val storageValue: String,
    val label: String,
    val colors: ThemeColors
) {
    EMERALD(
        "emerald", "Emerald",
        ThemeColors(
            androidx.compose.ui.graphics.Color(0xFF10B981),
            androidx.compose.ui.graphics.Color(0xFF176B3A),
            androidx.compose.ui.graphics.Color(0xFF16834A),
            androidx.compose.ui.graphics.Color(0xFFE4F3E8),
            androidx.compose.ui.graphics.Color(0xFF10B981),
            androidx.compose.ui.graphics.Color(0xFF12D18E),
            androidx.compose.ui.graphics.Color(0xFF0C2B20)
        )
    ),
    BLUE(
        "blue", "Blue",
        ThemeColors(
            androidx.compose.ui.graphics.Color(0xFF3B82F6),
            androidx.compose.ui.graphics.Color(0xFF1D4ED8),
            androidx.compose.ui.graphics.Color(0xFF2563EB),
            androidx.compose.ui.graphics.Color(0xFFDBEAFE),
            androidx.compose.ui.graphics.Color(0xFF3B82F6),
            androidx.compose.ui.graphics.Color(0xFF60A5FA),
            androidx.compose.ui.graphics.Color(0xFF172554)
        )
    ),
    VIOLET(
        "violet", "Violet",
        ThemeColors(
            androidx.compose.ui.graphics.Color(0xFF8B5CF6),
            androidx.compose.ui.graphics.Color(0xFF6D28D9),
            androidx.compose.ui.graphics.Color(0xFF7C3AED),
            androidx.compose.ui.graphics.Color(0xFFEDE9FE),
            androidx.compose.ui.graphics.Color(0xFFA78BFA),
            androidx.compose.ui.graphics.Color(0xFFC4B5FD),
            androidx.compose.ui.graphics.Color(0xFF2E1065)
        )
    ),
    AMBER(
        "amber", "Amber",
        ThemeColors(
            androidx.compose.ui.graphics.Color(0xFFF59E0B),
            androidx.compose.ui.graphics.Color(0xFF92400E),
            androidx.compose.ui.graphics.Color(0xFFB45309),
            androidx.compose.ui.graphics.Color(0xFFFFF3D6),
            androidx.compose.ui.graphics.Color(0xFFFBBF24),
            androidx.compose.ui.graphics.Color(0xFFFCD34D),
            androidx.compose.ui.graphics.Color(0xFF3B2508)
        )
    ),
    ROSE(
        "rose", "Rose",
        ThemeColors(
            androidx.compose.ui.graphics.Color(0xFFF43F5E),
            androidx.compose.ui.graphics.Color(0xFFBE123C),
            androidx.compose.ui.graphics.Color(0xFFE11D48),
            androidx.compose.ui.graphics.Color(0xFFFFE4E9),
            androidx.compose.ui.graphics.Color(0xFFFB7185),
            androidx.compose.ui.graphics.Color(0xFFFDA4AF),
            androidx.compose.ui.graphics.Color(0xFF4C0519)
        )
    ),
    CUSTOM(
        "custom", "Custom",
        ThemeColors(
            Color(0xFF14B8A6),
            Color(0xFF0F766E),
            Color(0xFF0D9488),
            Color(0xFFCCFBF1),
            Color(0xFF2DD4BF),
            Color(0xFF5EEAD4),
            Color(0xFF042F2E)
        )
    );

    fun resolveColors(customAccent: Color): ThemeColors =
        if (this == CUSTOM) ThemeColors.fromCustomAccent(customAccent) else colors

    companion object {
        fun fromStorage(value: String?): AppColorTheme =
            entries.firstOrNull { it.storageValue == value } ?: EMERALD
    }
}

@Composable
fun POSTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorTheme: AppColorTheme = AppColorTheme.EMERALD,
    customAccentColor: Color = Color(0xFF14B8A6),
    content: @Composable () -> Unit
) {
    val useDarkTheme = themeMode.resolvesToDark(androidx.compose.foundation.isSystemInDarkTheme())
    updateColorPalette(useDarkTheme, colorTheme, customAccentColor)
    val paletteColors = colorTheme.resolveColors(customAccentColor)
    val lightOnAccent = if (paletteColors.lightPrimary.luminance() > 0.42f) {
        androidx.compose.ui.graphics.Color(0xFF090F0D)
    } else {
        androidx.compose.ui.graphics.Color.White
    }
    val colorScheme = if (useDarkTheme) {
        darkColorScheme(
            primary = paletteColors.darkPrimary,
            onPrimary = androidx.compose.ui.graphics.Color(0xFF090F0D),
            primaryContainer = paletteColors.darkContainer,
            onPrimaryContainer = paletteColors.darkSecondary,
            secondary = paletteColors.darkSecondary,
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
            primary = paletteColors.lightPrimary,
            onPrimary = lightOnAccent,
            primaryContainer = paletteColors.lightContainer,
            onPrimaryContainer = paletteColors.lightPrimary,
            secondary = paletteColors.lightSecondary,
            onSecondary = if (paletteColors.lightSecondary.luminance() > 0.42f) {
                androidx.compose.ui.graphics.Color(0xFF090F0D)
            } else {
                androidx.compose.ui.graphics.Color.White
            },
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
            updateColorPalette(useDarkTheme, colorTheme, customAccentColor)
            val window = (view.context as Activity).window
            val systemBarColor = if (useDarkTheme) {
                androidx.compose.ui.graphics.Color(0xFF090F0D)
            } else {
                androidx.compose.ui.graphics.Color(0xFFF1F7F2)
            }
            @Suppress("DEPRECATION")
            window.statusBarColor = systemBarColor.toArgb()
            @Suppress("DEPRECATION")
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