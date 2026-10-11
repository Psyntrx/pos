package com.example.pos

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun testTypographyColorsAreUnspecifiedForDynamicTheming() {
        // Typography styles must not hardcode static colors so Compose themes (dark/light)
        // can dynamically apply the correct LocalContentColor and TextField colors
        assertEquals(androidx.compose.ui.graphics.Color.Unspecified, com.example.pos.ui.theme.Typography.headlineLarge.color)
        assertEquals(androidx.compose.ui.graphics.Color.Unspecified, com.example.pos.ui.theme.Typography.bodyLarge.color)
        assertEquals(androidx.compose.ui.graphics.Color.Unspecified, com.example.pos.ui.theme.Typography.titleMedium.color)
    }

    @Test
    fun testSemanticTextColorsSwitchBetweenThemes() {
        com.example.pos.ui.theme.updatePaletteForDarkTheme(false)
        val lightPrimary = com.example.pos.ui.theme.TextPrimary
        assertEquals(androidx.compose.ui.graphics.Color(0xFF172B1F), lightPrimary)

        com.example.pos.ui.theme.updatePaletteForDarkTheme(true)
        val darkPrimary = com.example.pos.ui.theme.TextPrimary
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF8FAFC), darkPrimary)
    }
}