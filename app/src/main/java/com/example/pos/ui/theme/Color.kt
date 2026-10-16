package com.example.pos.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

/** Tracks the palette currently rendered by POSTheme. Reads from Compose snapshot state
 * so all screens using these shared tokens recompose when the user changes appearance.
 */
private val isDarkPalette = mutableStateOf(false)
private val selectedColorTheme = mutableStateOf(AppColorTheme.EMERALD)
private val selectedCustomAccentColor = mutableStateOf(Color(0xFF14B8A6))

internal fun updateColorPalette(isDark: Boolean, colorTheme: AppColorTheme, customAccentColor: Color) {
    if (isDarkPalette.value != isDark) {
        isDarkPalette.value = isDark
    }
    if (selectedColorTheme.value != colorTheme) {
        selectedColorTheme.value = colorTheme
    }
    if (selectedCustomAccentColor.value != customAccentColor) {
        selectedCustomAccentColor.value = customAccentColor
    }
}

internal fun updatePaletteForDarkTheme(isDark: Boolean) {
    updateColorPalette(isDark, selectedColorTheme.value, selectedCustomAccentColor.value)
}

private fun palette(light: Color, dark: Color): Color =
    if (isDarkPalette.value) dark else light

private fun accent(light: Color, dark: Color): Color =
    if (isDarkPalette.value) dark else light

// Semantic tokens retained for the existing UI; same components work in both themes.
val BgDark: Color get() = palette(Color(0xFFF1F7F2), Color(0xFF090F0D))
val SurfaceDark: Color get() = palette(Color(0xFFFFFFFF), Color(0xFF111A17))
val SurfaceDarkVariant: Color get() = palette(Color(0xFFF5F9F5), Color(0xFF16231E))
val SurfaceBorder: Color get() = palette(Color(0xFFDCE8DE), Color(0xFF1F332B))

val EmeraldPrimary: Color get() = accent(selectedColorTheme.value.resolveColors(selectedCustomAccentColor.value).lightPrimary, selectedColorTheme.value.resolveColors(selectedCustomAccentColor.value).darkPrimary)
val EmeraldBright: Color get() = accent(selectedColorTheme.value.resolveColors(selectedCustomAccentColor.value).lightSecondary, selectedColorTheme.value.resolveColors(selectedCustomAccentColor.value).darkSecondary)
val EmeraldGlow: Color get() = accent(selectedColorTheme.value.resolveColors(selectedCustomAccentColor.value).lightSecondary, selectedColorTheme.value.resolveColors(selectedCustomAccentColor.value).darkPrimary)
val EmeraldLight: Color get() = accent(selectedColorTheme.value.resolveColors(selectedCustomAccentColor.value).lightSecondary, selectedColorTheme.value.resolveColors(selectedCustomAccentColor.value).darkPrimary)
val EmeraldContainer: Color get() = accent(selectedColorTheme.value.resolveColors(selectedCustomAccentColor.value).lightContainer, selectedColorTheme.value.resolveColors(selectedCustomAccentColor.value).darkContainer)

val TextPrimary: Color get() = palette(Color(0xFF172B1F), Color(0xFFF8FAFC))
val TextSecondary: Color get() = palette(Color(0xFF5D7163), Color(0xFF94A3B8))
val TextMuted: Color get() = palette(Color(0xFF839187), Color(0xFF64748B))
val TextOnPrimary: Color get() = palette(Color(0xFFFFFFFF), Color(0xFF090F0D))

val WarningYellow: Color get() = palette(Color(0xFF9A6100), Color(0xFFF59E0B))
val WarningLightYellow: Color get() = WarningYellow
val WarningContainer: Color get() = palette(Color(0xFFFFF2D8), Color(0xFF2A200B))
val ErrorRed: Color get() = palette(Color(0xFFB93832), Color(0xFFEF4444))
val ErrorContainer: Color get() = palette(Color(0xFFFCE9E7), Color(0xFF2E1212))

val CardGradientStart: Color get() = palette(Color(0xFFF7FCF8), Color(0xFF14241F))
val CardGradientEnd: Color get() = palette(Color(0xFFEAF4EC), Color(0xFF0F1A16))
