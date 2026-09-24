package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MythosColorScheme = darkColorScheme(
    primary = MythosGoldPrimary,
    onPrimary = Color(0xFF1E1700),
    primaryContainer = MythosGoldDark,
    onPrimaryContainer = MythosGoldLight,
    secondary = MythosBlue,
    onSecondary = Color(0xFF001F33),
    secondaryContainer = MythosBlueDark,
    onSecondaryContainer = Color(0xFFCBE8FF),
    tertiary = MythosPurple,
    onTertiary = Color.White,
    background = MythosDarkBg,
    onBackground = Color(0xFFEBE6EF),
    surface = MythosDarkSurface,
    onSurface = Color(0xFFEBE6EF),
    surfaceVariant = MythosDarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFCBC4D6),
    outline = MythosDarkSurfaceBorder,
    error = MythosRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Mythos uses a dedicated dark mythological fantasy palette
    MaterialTheme(
        colorScheme = MythosColorScheme,
        typography = Typography,
        content = content
    )
}
