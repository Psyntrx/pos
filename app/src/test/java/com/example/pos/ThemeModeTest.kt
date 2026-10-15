package com.example.pos

import com.example.pos.ui.theme.ThemeMode
import com.example.pos.ui.theme.AppColorTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun missingOrUnknownColorThemeDefaultsToEmerald() {
        assertEquals(AppColorTheme.EMERALD, AppColorTheme.fromStorage(null))
        assertEquals(AppColorTheme.EMERALD, AppColorTheme.fromStorage("unexpected"))
    }

    @Test
    fun savedColorThemesRestoreCorrectly() {
        AppColorTheme.entries.forEach { theme ->
            assertEquals(theme, AppColorTheme.fromStorage(theme.storageValue))
        }
    }

    @Test
    fun missingOrUnknownPreferenceDefaultsToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("unexpected"))
    }

    @Test
    fun savedThemeModesRestoreCorrectly() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorage("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorage("dark"))
    }

    @Test
    fun systemModeFollowsDeviceAndManualModesOverrideIt() {
        assertEquals(true, ThemeMode.SYSTEM.resolvesToDark(systemIsDark = true))
        assertEquals(false, ThemeMode.SYSTEM.resolvesToDark(systemIsDark = false))
        assertEquals(false, ThemeMode.LIGHT.resolvesToDark(systemIsDark = true))
        assertEquals(true, ThemeMode.DARK.resolvesToDark(systemIsDark = false))
    }
}
